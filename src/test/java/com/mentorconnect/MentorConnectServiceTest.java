package com.mentorconnect;

import com.mentorconnect.dto.*;
import com.mentorconnect.exception.DuplicateResourceException;
import com.mentorconnect.exception.InvalidOperationException;
import com.mentorconnect.exception.MentorshipCapacityExceededException;
import com.mentorconnect.model.PairStatus;
import com.mentorconnect.model.SessionStatus;
import com.mentorconnect.service.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
public class MentorConnectServiceTest {

    @Autowired
    private AlumniService alumniService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private InterestTagService interestTagService;

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private MentorshipPairService mentorshipPairService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    @Order(1)
    void testInitialDataSeededSuccessfully() {
        List<AlumniResponseDto> alumni = alumniService.getAllAlumni(null, null);
        assertNotNull(alumni);
        assertTrue(alumni.size() >= 5, "Expected at least 5 seeded alumni");

        List<StudentResponseDto> students = studentService.getAllStudents();
        assertNotNull(students);
        assertTrue(students.size() >= 4, "Expected at least 4 seeded students");

        List<InterestTagDto> tags = interestTagService.getAllTags();
        assertNotNull(tags);
        assertTrue(tags.size() >= 8, "Expected at least 8 seeded interest tags");
    }

    @Test
    @Order(2)
    void testAlumniRegistrationAndDuplicateCheck() {
        AlumniRequestDto req = new AlumniRequestDto();
        req.setName("Harish Chandran");
        req.setEmail("harish.c@freshworks.com");
        req.setGraduationYear(2017);
        req.setCompany("Freshworks");
        req.setJobTitle("Staff Architect");
        req.setMaxConcurrentMentees(2);
        req.setBio("Specializes in high scale distributed caching and REST API architecture.");
        req.setExpertiseTags(new HashSet<>(Arrays.asList("System Design", "Microservices", "Java & Spring Boot")));

        AlumniResponseDto created = alumniService.createAlumni(req);
        assertNotNull(created.getId());
        assertEquals("Harish Chandran", created.getName());
        assertEquals(2, created.getAvailableSlots());
        assertEquals(0, created.getActiveMenteeCount());

        // Test duplicate email rejection
        assertThrows(DuplicateResourceException.class, () -> alumniService.createAlumni(req));
    }

    @Test
    @Order(3)
    void testStudentRegistrationAndDuplicateCheck() {
        StudentRequestDto req = new StudentRequestDto();
        req.setName("Pavithra Selvam");
        req.setEmail("pavithra.s@college.edu");
        req.setDepartment("Computer Science and Engineering");
        req.setBatchYear(2026);
        req.setRollNumber("22CS155");
        req.setInterestTags(new HashSet<>(Arrays.asList("Machine Learning", "Cloud Architecture (AWS)")));

        StudentResponseDto created = studentService.createStudent(req);
        assertNotNull(created.getId());
        assertEquals("Pavithra Selvam", created.getName());
        assertEquals(2, created.getInterestTags().size());

        // Test duplicate email rejection
        assertThrows(DuplicateResourceException.class, () -> studentService.createStudent(req));
    }

    @Test
    @Order(4)
    void testAutoSuggestRankingByTagOverlap() {
        // Find Rahul (Student id=1, has tags: Java & Spring Boot, Cloud Architecture (AWS), Microservices, System Design)
        List<MatchSuggestionDto> suggestions = matchingService.suggestMentorsForStudent(1L);
        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());

        // Verify ranked descending by tag overlap count
        for (int i = 0; i < suggestions.size() - 1; i++) {
            MatchSuggestionDto current = suggestions.get(i);
            MatchSuggestionDto next = suggestions.get(i + 1);
            assertTrue(current.getMatchCount() >= next.getMatchCount(),
                    "Mentor at rank " + i + " (" + current.getMatchCount() +
                            ") should have >= matchCount than rank " + (i + 1) + " (" + next.getMatchCount() + ")");
        }

        // Top match should be Priya Sharma or Vignesh Balaji (highest overlap)
        MatchSuggestionDto topMatch = suggestions.get(0);
        assertTrue(topMatch.getMatchCount() >= 3, "Top mentor should have at least 3 matching tags");
        assertNotNull(topMatch.getMatchingTags());
        assertFalse(topMatch.getMatchingTags().isEmpty());
    }

    @Test
    @Order(5)
    void testEnforceMaxConcurrentMenteeCapacityLimit() {
        // Ananya Iyer (Alumni id=3) has maxConcurrentMentees = 1 and 0 active pairs initially.
        // Match Ananya with Student 4 (Meera Nair)
        MentorshipPairRequestDto req1 = new MentorshipPairRequestDto(3L, 4L, "AI Research Mentorship");
        MentorshipPairResponseDto pair1 = matchingService.createPair(req1);
        assertNotNull(pair1.getId());
        assertEquals(PairStatus.ACTIVE, pair1.getStatus());

        // Now Ananya is at capacity (1/1 slots). Attempting to match another student (Student 3: Arvind) must fail immediately!
        MentorshipPairRequestDto req2 = new MentorshipPairRequestDto(3L, 3L, "Attempting overflow match");
        MentorshipCapacityExceededException ex = assertThrows(
                MentorshipCapacityExceededException.class,
                () -> matchingService.createPair(req2)
        );
        assertTrue(ex.getMessage().contains("maximum concurrent mentee limit"));
    }

    @Test
    @Order(6)
    void testEnforceDuplicateActivePairPrevention() {
        // Student 1 (Rahul) and Alumni 1 (Priya) already have an active mentorship pair from DataInitializer
        MentorshipPairRequestDto duplicateReq = new MentorshipPairRequestDto(1L, 1L, "Duplicate pair attempt");

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> matchingService.createPair(duplicateReq)
        );
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @Order(7)
    void testSessionSchedulingAndActivePairRule() {
        // Schedule valid session for Pair 1
        SessionRequestDto sessionReq = new SessionRequestDto();
        sessionReq.setMentorshipPairId(1L);
        sessionReq.setSessionDate(LocalDateTime.now().plusDays(4).withHour(16).withMinute(0));
        sessionReq.setDurationMinutes(60);
        sessionReq.setTopic("Distributed Caching with Redis & Cache Aside Pattern");
        sessionReq.setMeetingLink("https://meet.google.com/test-redis");

        SessionResponseDto scheduled = sessionService.scheduleSession(sessionReq);
        assertNotNull(scheduled.getId());
        assertEquals(SessionStatus.SCHEDULED, scheduled.getStatus());
        assertEquals(60, scheduled.getDurationMinutes());

        // Complete the session
        SessionResponseDto completed = sessionService.updateSessionStatus(
                scheduled.getId(),
                SessionStatus.COMPLETED,
                "Covered TTL, evictions, cluster sharding, and write-through caching."
        );
        assertEquals(SessionStatus.COMPLETED, completed.getStatus());
        assertTrue(completed.getFeedbackNotes().contains("evictions"));
    }

    @Test
    @Order(8)
    void testCannotScheduleSessionForInactivePair() {
        // Create a temporary pair and cancel it
        MentorshipPairRequestDto pairReq = new MentorshipPairRequestDto(2L, 3L, "Temp pair to cancel");
        MentorshipPairResponseDto pair = matchingService.createPair(pairReq);
        mentorshipPairService.updatePairStatus(pair.getId(), PairStatus.CANCELLED);

        // Attempt to schedule session on CANCELLED pair
        SessionRequestDto sessionReq = new SessionRequestDto();
        sessionReq.setMentorshipPairId(pair.getId());
        sessionReq.setSessionDate(LocalDateTime.now().plusDays(2));
        sessionReq.setDurationMinutes(45);
        sessionReq.setTopic("Should not be allowed");

        InvalidOperationException ex = assertThrows(
                InvalidOperationException.class,
                () -> sessionService.scheduleSession(sessionReq)
        );
        assertTrue(ex.getMessage().contains("inactive mentorship pair"));
    }

    @Test
    @Order(9)
    void testDepartmentEngagementReportMetrics() {
        DepartmentEngagementSummaryDto report = reportService.getEngagementReport();
        assertNotNull(report);
        assertTrue(report.getTotalAlumni() >= 5);
        assertTrue(report.getTotalStudents() >= 4);
        assertTrue(report.getTotalPairs() >= 2);
        assertTrue(report.getActivePairs() >= 1);
        assertTrue(report.getTotalCompletedHours() > 0.0);
        assertNotNull(report.getPairReports());
        assertFalse(report.getPairReports().isEmpty());

        // Check individual pair report breakdown
        EngagementPairReportDto firstReport = report.getPairReports().get(0);
        assertNotNull(firstReport.getAlumniName());
        assertNotNull(firstReport.getStudentName());
        assertTrue(firstReport.getTotalSessionsScheduled() >= 1);
    }

    @Test
    @Order(10)
    void testReactivatePairRejectsWhenActivePairAlreadyExists() {
        // Create an initial pair between Alumni 5 (Sneha) and Student 4 (Meera)
        MentorshipPairRequestDto req1 = new MentorshipPairRequestDto(5L, 4L, "Initial pair");
        MentorshipPairResponseDto pair1 = matchingService.createPair(req1);

        // Cancel this pair
        mentorshipPairService.updatePairStatus(pair1.getId(), PairStatus.CANCELLED);

        // Create a new active pair between same Alumni 5 and Student 4
        MentorshipPairRequestDto req2 = new MentorshipPairRequestDto(5L, 4L, "Second active pair");
        MentorshipPairResponseDto pair2 = matchingService.createPair(req2);
        assertEquals(PairStatus.ACTIVE, pair2.getStatus());

        // Now attempt to re-activate the first pair - must be rejected as duplicate active pair!
        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> mentorshipPairService.updatePairStatus(pair1.getId(), PairStatus.ACTIVE)
        );
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @Order(11)
    void testCancelMentorshipPairCancelsPendingSessions() {
        // Create pair and schedule session
        MentorshipPairRequestDto pairReq = new MentorshipPairRequestDto(4L, 3L, "Pair to cancel with session");
        MentorshipPairResponseDto pair = matchingService.createPair(pairReq);

        SessionRequestDto sessReq = new SessionRequestDto();
        sessReq.setMentorshipPairId(pair.getId());
        sessReq.setSessionDate(LocalDateTime.now().plusDays(5));
        sessReq.setDurationMinutes(60);
        sessReq.setTopic("Future Architecture Discussion");
        SessionResponseDto session = sessionService.scheduleSession(sessReq);
        assertEquals(SessionStatus.SCHEDULED, session.getStatus());

        // Cancel pair
        mentorshipPairService.updatePairStatus(pair.getId(), PairStatus.CANCELLED);

        // Verify the session was automatically transitioned to CANCELLED
        SessionResponseDto updatedSession = sessionService.getSessionById(session.getId());
        assertEquals(SessionStatus.CANCELLED, updatedSession.getStatus());
        assertTrue(updatedSession.getFeedbackNotes().contains("cancelled"));
    }

    @Test
    @Order(12)
    void testDeleteAlumniWithMentorshipHistoryThrowsException() {
        // Alumni 1 (Priya) has existing active and completed mentorship pairs
        InvalidOperationException ex = assertThrows(
                InvalidOperationException.class,
                () -> alumniService.deleteAlumni(1L)
        );
        assertTrue(ex.getMessage().contains("mentorship pair record"));
    }

    @Test
    @Order(13)
    void testDeleteStudentWithMentorshipHistoryThrowsException() {
        // Student 1 (Rahul) has existing active mentorship pairs
        InvalidOperationException ex = assertThrows(
                InvalidOperationException.class,
                () -> studentService.deleteStudent(1L)
        );
        assertTrue(ex.getMessage().contains("mentorship pair record"));
    }

    @Test
    @Order(14)
    void testDeleteTagAssignedToAlumniOrStudentThrowsException() {
        // Tag 1 (Java & Spring Boot) is assigned to multiple alumni and students
        InvalidOperationException ex = assertThrows(
                InvalidOperationException.class,
                () -> interestTagService.deleteTag(1L)
        );
        assertTrue(ex.getMessage().contains("currently assigned"));
    }

    @Test
    @Order(15)
    void testAutoSuggestForStudentIndicatesAlreadyMatched() {
        // Student 1 is paired with Alumni 1 (Priya)
        List<MatchSuggestionDto> suggestions = matchingService.suggestMentorsForStudent(1L);
        assertNotNull(suggestions);

        MatchSuggestionDto priyaSuggestion = suggestions.stream()
                .filter(s -> s.getAlumniId().equals(1L))
                .findFirst()
                .orElse(null);

        assertNotNull(priyaSuggestion);
        assertTrue(priyaSuggestion.isAlreadyMatched(), "Alumni 1 should be marked as alreadyMatched for Student 1");
        assertFalse(priyaSuggestion.isEligible(), "Alumni 1 should not be eligible for new match since already paired");
    }

    @Test
    @Order(16)
    void testCustomTagMatchingAutoSuggestsRankedMentors() {
        List<String> tags = Arrays.asList("Java & Spring Boot", "Microservices", "System Design");
        List<MatchSuggestionDto> suggestions = matchingService.suggestMentorsByTags(tags);
        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());

        // Verify ranked descending by matchCount
        for (int i = 0; i < suggestions.size() - 1; i++) {
            assertTrue(suggestions.get(i).getMatchCount() >= suggestions.get(i + 1).getMatchCount());
        }

        // Top match should have overlap with these tags
        assertTrue(suggestions.get(0).getMatchCount() >= 2);
    }

    @Test
    @Order(17)
    void testConcurrentMatchingOnLastSlotEnforcesCapacityLimit() throws Exception {
        // Register a mentor with exactly 1 available slot
        AlumniRequestDto mentorReq = new AlumniRequestDto();
        mentorReq.setName("Concurrency Mentor");
        mentorReq.setEmail("concurrency.mentor@test.com");
        mentorReq.setGraduationYear(2015);
        mentorReq.setCompany("Concurrency Corp");
        mentorReq.setJobTitle("Principal Engineer");
        mentorReq.setMaxConcurrentMentees(1);
        AlumniResponseDto mentor = alumniService.createAlumni(mentorReq);

        // Register 4 separate students
        Long[] studentIds = new Long[4];
        for (int i = 0; i < 4; i++) {
            StudentRequestDto studReq = new StudentRequestDto();
            studReq.setName("Concurrent Student " + i);
            studReq.setEmail("concurrent.student." + i + "@test.com");
            studReq.setDepartment("Computer Science");
            studReq.setBatchYear(2026);
            studentIds[i] = studentService.createStudent(studReq).getId();
        }

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger capacityExceededCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final Long sId = studentIds[i];
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    matchingService.createPair(new MentorshipPairRequestDto(mentor.getId(), sId, "Concurrent pair attempt"));
                    successCount.incrementAndGet();
                } catch (MentorshipCapacityExceededException e) {
                    capacityExceededCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        // Exactly 1 thread must succeed, and 3 must be rejected with capacity exceeded!
        assertEquals(1, successCount.get(), "Only 1 concurrent matching request should succeed for a 1-slot mentor");
        assertEquals(3, capacityExceededCount.get(), "3 concurrent matching requests should be rejected with capacity limit");

        // Verify mentor's active mentees is exactly 1 in repository
        AlumniResponseDto updatedMentor = alumniService.getAlumniById(mentor.getId());
        assertEquals(1, updatedMentor.getActiveMenteeCount());
        assertEquals(0, updatedMentor.getAvailableSlots());
    }

    @Test
    @Order(18)
    void testAuditLoggingRecordsActionsAcrossEntities() {
        List<AuditLogDto> initialLogs = auditLogService.getAllLogs();
        assertNotNull(initialLogs);
        assertFalse(initialLogs.isEmpty(), "Initial seed should have generated audit logs");

        // 1. Test alumni registration logs
        AlumniRequestDto mentorReq = new AlumniRequestDto();
        mentorReq.setName("Audit Alumni Test");
        mentorReq.setEmail("audit.alumni@example.com");
        mentorReq.setGraduationYear(2018);
        mentorReq.setCompany("AuditCorp");
        mentorReq.setJobTitle("Lead Architect");
        mentorReq.setMaxConcurrentMentees(2);
        mentorReq.setExpertiseTags(new HashSet<>(Arrays.asList("Java & Spring Boot")));
        AlumniResponseDto createdMentor = alumniService.createAlumni(mentorReq);

        List<AuditLogDto> updatedLogs = auditLogService.getAllLogs();
        boolean alumniAuditFound = updatedLogs.stream().anyMatch(l ->
                "ALUMNI_REGISTERED".equals(l.getAction()) &&
                "ALUMNI".equals(l.getEntityType()) &&
                createdMentor.getId().equals(l.getEntityId()));
        assertTrue(alumniAuditFound, "Audit log for ALUMNI_REGISTERED must be present");

        // 2. Test student registration logs
        StudentRequestDto studReq = new StudentRequestDto();
        studReq.setName("Audit Student Test");
        studReq.setEmail("audit.student@college.edu");
        studReq.setDepartment("Computer Science");
        studReq.setBatchYear(2026);
        StudentResponseDto createdStudent = studentService.createStudent(studReq);

        updatedLogs = auditLogService.getAllLogs();
        boolean studentAuditFound = updatedLogs.stream().anyMatch(l ->
                "STUDENT_REGISTERED".equals(l.getAction()) &&
                "STUDENT".equals(l.getEntityType()) &&
                createdStudent.getId().equals(l.getEntityId()));
        assertTrue(studentAuditFound, "Audit log for STUDENT_REGISTERED must be present");

        // 3. Test pair creation and status change logs
        MentorshipPairResponseDto pair = matchingService.createPair(
                new MentorshipPairRequestDto(createdMentor.getId(), createdStudent.getId(), "Audit pair test")
        );
        updatedLogs = auditLogService.getAllLogs();
        boolean pairAuditFound = updatedLogs.stream().anyMatch(l ->
                "PAIR_CREATED".equals(l.getAction()) &&
                "MENTORSHIP_PAIR".equals(l.getEntityType()) &&
                pair.getId().equals(l.getEntityId()));
        assertTrue(pairAuditFound, "Audit log for PAIR_CREATED must be present");

        // 4. Test session scheduled and completed logs
        SessionRequestDto sessReq = new SessionRequestDto();
        sessReq.setMentorshipPairId(pair.getId());
        sessReq.setSessionDate(LocalDateTime.now().plusDays(2));
        sessReq.setDurationMinutes(45);
        sessReq.setTopic("Audit Session Topic");
        SessionResponseDto session = sessionService.scheduleSession(sessReq);

        updatedLogs = auditLogService.getAllLogs();
        boolean sessScheduledFound = updatedLogs.stream().anyMatch(l ->
                "SESSION_SCHEDULED".equals(l.getAction()) &&
                "SESSION".equals(l.getEntityType()) &&
                session.getId().equals(l.getEntityId()));
        assertTrue(sessScheduledFound, "Audit log for SESSION_SCHEDULED must be present");

        sessionService.updateSessionStatus(session.getId(), SessionStatus.COMPLETED, "Audit feedback notes");
        updatedLogs = auditLogService.getAllLogs();
        boolean sessCompletedFound = updatedLogs.stream().anyMatch(l ->
                "SESSION_COMPLETED".equals(l.getAction()) &&
                "SESSION".equals(l.getEntityType()) &&
                session.getId().equals(l.getEntityId()));
        assertTrue(sessCompletedFound, "Audit log for SESSION_COMPLETED must be present");

        // 5. Test pair status change and auto-cancelled session logs
        SessionRequestDto sessReq2 = new SessionRequestDto();
        sessReq2.setMentorshipPairId(pair.getId());
        sessReq2.setSessionDate(LocalDateTime.now().plusDays(3));
        sessReq2.setDurationMinutes(30);
        sessReq2.setTopic("Session To Be Cancelled When Pair Cancels");
        SessionResponseDto session2 = sessionService.scheduleSession(sessReq2);

        mentorshipPairService.updatePairStatus(pair.getId(), PairStatus.CANCELLED);
        updatedLogs = auditLogService.getAllLogs();

        boolean pairStatusChangedFound = updatedLogs.stream().anyMatch(l ->
                "PAIR_STATUS_CHANGED".equals(l.getAction()) &&
                "MENTORSHIP_PAIR".equals(l.getEntityType()) &&
                pair.getId().equals(l.getEntityId()));
        assertTrue(pairStatusChangedFound, "Audit log for PAIR_STATUS_CHANGED must be present");

        boolean sessionAutoCancelledFound = updatedLogs.stream().anyMatch(l ->
                "SESSION_CANCELLED".equals(l.getAction()) &&
                "SESSION".equals(l.getEntityType()) &&
                session2.getId().equals(l.getEntityId()));
        assertTrue(sessionAutoCancelledFound, "Audit log for auto-cancelled SESSION_CANCELLED must be present");

        // 6. Test defensive truncation of long details (> 1000 characters)
        String longText = "A".repeat(1200);
        var longLog = auditLogService.recordLog("LONG_TEST", "TEST", 999L, longText);
        assertNotNull(longLog.getId());
        assertTrue(longLog.getDetails().length() <= 1000, "Details must be truncated to <= 1000 characters");
        assertTrue(longLog.getDetails().endsWith("..."));
    }

    @Test
    @Order(19)
    void testCachingBehaviorForTagsAndReports() {
        assertNotNull(cacheManager, "Spring CacheManager must be configured");
        assertNotNull(cacheManager.getCache("tags"), "Cache 'tags' must be defined");
        assertNotNull(cacheManager.getCache("engagementReport"), "Cache 'engagementReport' must be defined");

        // Deep verification: tags caching and eviction
        cacheManager.getCache("tags").clear();
        assertNull(cacheManager.getCache("tags").get(org.springframework.cache.interceptor.SimpleKey.EMPTY));
        List<InterestTagDto> tags1 = interestTagService.getAllTags();
        assertNotNull(tags1);
        assertNotNull(cacheManager.getCache("tags").get(org.springframework.cache.interceptor.SimpleKey.EMPTY),
                "Cache 'tags' must contain entry after getAllTags");

        // Eviction check: getOrCreateTag evicts "tags"
        interestTagService.getOrCreateTag("Cache Test Unique Tag " + System.currentTimeMillis());
        assertNull(cacheManager.getCache("tags").get(org.springframework.cache.interceptor.SimpleKey.EMPTY),
                "Cache 'tags' must be evicted after creating a tag");

        // Deep verification: report caching and manual eviction
        cacheManager.getCache("engagementReport").clear();
        DepartmentEngagementSummaryDto report1 = reportService.getEngagementReport();
        assertNotNull(report1);
        assertNotNull(cacheManager.getCache("engagementReport").get(org.springframework.cache.interceptor.SimpleKey.EMPTY),
                "Cache 'engagementReport' must contain entry after getEngagementReport");

        // Evict engagementReport cache
        reportService.clearReportCache();
        assertNull(cacheManager.getCache("engagementReport").get(org.springframework.cache.interceptor.SimpleKey.EMPTY),
                "Cache 'engagementReport' must be evicted after clearReportCache");
        DepartmentEngagementSummaryDto report3 = reportService.getEngagementReport();
        assertNotNull(report3);
    }

    @Test
    @Order(20)
    void testPaginationAndSortingForAlumniAndSessions() {
        // Test Alumni pagination with sorting by name asc
        Page<AlumniResponseDto> alumniPage = alumniService.getAlumniPaged(
                PageRequest.of(0, 3, Sort.by("name").ascending())
        );
        assertNotNull(alumniPage);
        assertTrue(alumniPage.getTotalElements() >= 5, "Total alumni should be >= 5");
        assertEquals(3, alumniPage.getContent().size());
        assertTrue(alumniPage.getTotalPages() >= 2);

        // Verify sorting by name asc
        String name0 = alumniPage.getContent().get(0).getName();
        String name1 = alumniPage.getContent().get(1).getName();
        assertTrue(name0.compareToIgnoreCase(name1) <= 0, "Alumni page should be sorted alphabetically");

        // Test Student pagination with sorting by name asc
        Page<StudentResponseDto> studentsPage = studentService.getStudentsPaged(
                PageRequest.of(0, 2, Sort.by("name").ascending())
        );
        assertNotNull(studentsPage);
        assertTrue(studentsPage.getTotalElements() >= 4, "Total students should be >= 4");
        assertEquals(2, studentsPage.getContent().size());

        // Test MentorshipPair pagination
        Page<MentorshipPairResponseDto> pairsPage = mentorshipPairService.getPairsPaged(
                PageRequest.of(0, 2)
        );
        assertNotNull(pairsPage);
        assertTrue(pairsPage.getTotalElements() >= 2, "Total pairs should be >= 2");
        assertEquals(2, pairsPage.getContent().size());

        // Test Session pagination with sorting by sessionDate desc
        Page<SessionResponseDto> sessionsPage = sessionService.getSessionsPaged(
                PageRequest.of(0, 2, Sort.by("sessionDate").descending()), null, null
        );
        assertNotNull(sessionsPage);
        assertTrue(sessionsPage.getTotalElements() >= 5, "Total sessions should be >= 5");
        assertEquals(2, sessionsPage.getContent().size());
        assertTrue(sessionsPage.getTotalPages() >= 3);
    }
}
