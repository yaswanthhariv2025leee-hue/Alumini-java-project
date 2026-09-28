package com.mentorconnect.service;

import com.mentorconnect.dto.DepartmentEngagementSummaryDto;
import com.mentorconnect.dto.EngagementPairReportDto;
import com.mentorconnect.model.MentorshipPair;
import com.mentorconnect.model.PairStatus;
import com.mentorconnect.model.Session;
import com.mentorconnect.model.SessionStatus;
import com.mentorconnect.repository.AlumniRepository;
import com.mentorconnect.repository.MentorshipPairRepository;
import com.mentorconnect.repository.SessionRepository;
import com.mentorconnect.repository.StudentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final AlumniRepository alumniRepository;
    private final StudentRepository studentRepository;
    private final MentorshipPairRepository mentorshipPairRepository;
    private final SessionRepository sessionRepository;

    public ReportService(AlumniRepository alumniRepository,
                         StudentRepository studentRepository,
                         MentorshipPairRepository mentorshipPairRepository,
                         SessionRepository sessionRepository) {
        this.alumniRepository = alumniRepository;
        this.studentRepository = studentRepository;
        this.mentorshipPairRepository = mentorshipPairRepository;
        this.sessionRepository = sessionRepository;
    }

    @Cacheable("engagementReport")
    public DepartmentEngagementSummaryDto getEngagementReport() {
        DepartmentEngagementSummaryDto summary = new DepartmentEngagementSummaryDto();

        summary.setTotalAlumni(alumniRepository.count());
        summary.setTotalStudents(studentRepository.count());

        List<MentorshipPair> pairs = mentorshipPairRepository.findAll();
        summary.setTotalPairs(pairs.size());

        long activePairs = 0;
        long completedPairs = 0;
        long cancelledPairs = 0;

        List<EngagementPairReportDto> pairReports = new ArrayList<>();
        long grandTotalCompletedMinutes = 0;

        for (MentorshipPair pair : pairs) {
            if (pair.getStatus() == PairStatus.ACTIVE) activePairs++;
            else if (pair.getStatus() == PairStatus.COMPLETED) completedPairs++;
            else if (pair.getStatus() == PairStatus.CANCELLED) cancelledPairs++;

            List<Session> pairSessions = sessionRepository.findByMentorshipPairId(pair.getId());
            int scheduled = 0;
            int completed = 0;
            int cancelled = 0;
            long pairMinutes = 0;

            for (Session s : pairSessions) {
                if (s.getStatus() == SessionStatus.SCHEDULED) scheduled++;
                else if (s.getStatus() == SessionStatus.COMPLETED) {
                    completed++;
                    if (s.getDurationMinutes() != null) {
                        pairMinutes += s.getDurationMinutes();
                    }
                } else if (s.getStatus() == SessionStatus.CANCELLED) cancelled++;
            }

            grandTotalCompletedMinutes += pairMinutes;

            EngagementPairReportDto pr = new EngagementPairReportDto();
            pr.setPairId(pair.getId());
            pr.setAlumniId(pair.getAlumni().getId());
            pr.setAlumniName(pair.getAlumni().getName());
            pr.setAlumniEmail(pair.getAlumni().getEmail());
            pr.setAlumniCompany(pair.getAlumni().getCompany());
            pr.setStudentId(pair.getStudent().getId());
            pr.setStudentName(pair.getStudent().getName());
            pr.setStudentEmail(pair.getStudent().getEmail());
            pr.setStudentDepartment(pair.getStudent().getDepartment());
            pr.setStatus(pair.getStatus());
            pr.setStartDate(pair.getStartDate());
            pr.setEndDate(pair.getEndDate());
            pr.setTotalSessionsScheduled(pairSessions.size());
            pr.setCompletedSessions(completed);
            pr.setCancelledSessions(cancelled);
            pr.setTotalCompletedMinutes(pairMinutes);

            double hours = BigDecimal.valueOf(pairMinutes / 60.0)
                    .setScale(1, RoundingMode.HALF_UP).doubleValue();
            pr.setTotalCompletedHours(hours);

            pairReports.add(pr);
        }

        summary.setActivePairs(activePairs);
        summary.setCompletedPairs(completedPairs);
        summary.setCancelledPairs(cancelledPairs);

        long totalSessions = sessionRepository.count();
        long schedSessions = sessionRepository.countByStatus(SessionStatus.SCHEDULED);
        long compSessions = sessionRepository.countByStatus(SessionStatus.COMPLETED);
        long cancSessions = sessionRepository.countByStatus(SessionStatus.CANCELLED);

        summary.setTotalSessions(totalSessions);
        summary.setScheduledSessions(schedSessions);
        summary.setCompletedSessions(compSessions);
        summary.setCancelledSessions(cancSessions);

        double totalHours = BigDecimal.valueOf(grandTotalCompletedMinutes / 60.0)
                .setScale(1, RoundingMode.HALF_UP).doubleValue();
        summary.setTotalCompletedHours(totalHours);

        summary.setPairReports(pairReports);

        return summary;
    }

    @CacheEvict(value = "engagementReport", allEntries = true)
    public void clearReportCache() {
        // Evicts cached engagement report
    }
}
