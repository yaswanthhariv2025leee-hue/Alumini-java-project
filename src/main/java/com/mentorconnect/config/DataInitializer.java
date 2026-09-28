package com.mentorconnect.config;

import com.mentorconnect.model.*;
import com.mentorconnect.repository.*;
import com.mentorconnect.service.AuditLogService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AlumniRepository alumniRepository;
    private final StudentRepository studentRepository;
    private final InterestTagRepository interestTagRepository;
    private final MentorshipPairRepository mentorshipPairRepository;
    private final SessionRepository sessionRepository;
    private final AuditLogService auditLogService;

    public DataInitializer(AlumniRepository alumniRepository,
                           StudentRepository studentRepository,
                           InterestTagRepository interestTagRepository,
                           MentorshipPairRepository mentorshipPairRepository,
                           SessionRepository sessionRepository,
                           AuditLogService auditLogService) {
        this.alumniRepository = alumniRepository;
        this.studentRepository = studentRepository;
        this.interestTagRepository = interestTagRepository;
        this.mentorshipPairRepository = mentorshipPairRepository;
        this.sessionRepository = sessionRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (interestTagRepository.count() > 0) {
            return; // Data already initialized
        }

        // 1. Create Tags
        InterestTag tagJava = interestTagRepository.save(new InterestTag("Java & Spring Boot", "Backend Development"));
        InterestTag tagMicroservices = interestTagRepository.save(new InterestTag("Microservices", "Backend Development"));
        InterestTag tagML = interestTagRepository.save(new InterestTag("Machine Learning", "Artificial Intelligence"));
        InterestTag tagCloud = interestTagRepository.save(new InterestTag("Cloud Architecture (AWS)", "Cloud & DevOps"));
        InterestTag tagSystemDesign = interestTagRepository.save(new InterestTag("System Design", "Architecture"));
        InterestTag tagDevOps = interestTagRepository.save(new InterestTag("DevOps & CI/CD", "Cloud & DevOps"));
        InterestTag tagFrontend = interestTagRepository.save(new InterestTag("React & Frontend", "Web Development"));
        InterestTag tagDSA = interestTagRepository.save(new InterestTag("Data Structures & Algorithms", "Core CS"));
        InterestTag tagDb = interestTagRepository.save(new InterestTag("Database Management (MySQL)", "Data"));
        InterestTag tagCyber = interestTagRepository.save(new InterestTag("Cybersecurity", "Security"));

        // 2. Create Alumni
        Alumni priya = new Alumni("Priya Sharma", "priya.sharma@google.com", 2018, "Google", "Senior Software Engineer", 2);
        priya.setBio("Passionate about distributed systems, backend scaling, and mentoring students for top-tier product company interviews.");
        priya.setLinkedinUrl("https://linkedin.com/in/priya-sharma-google");
        priya.setExpertiseTags(new HashSet<>(Arrays.asList(tagSystemDesign, tagJava, tagCloud, tagMicroservices)));
        priya = alumniRepository.save(priya);

        Alumni karthik = new Alumni("Karthik Rajan", "karthik.rajan@microsoft.com", 2016, "Microsoft", "Staff Software Engineer", 2);
        karthik.setBio("10+ years in distributed storage, competitive programming coach, passionate about helping juniors master DSA and system design.");
        karthik.setLinkedinUrl("https://linkedin.com/in/karthik-rajan-msft");
        karthik.setExpertiseTags(new HashSet<>(Arrays.asList(tagDSA, tagSystemDesign, tagDevOps)));
        karthik = alumniRepository.save(karthik);

        Alumni ananya = new Alumni("Ananya Iyer", "ananya.iyer@amazon.com", 2019, "Amazon", "AI/ML Tech Lead", 1);
        ananya.setBio("Leading computer vision and recommendation engine teams at AWS. Mentoring on real-world ML deployments and production pipelines.");
        ananya.setLinkedinUrl("https://linkedin.com/in/ananya-iyer-aws");
        ananya.setExpertiseTags(new HashSet<>(Arrays.asList(tagML, tagCloud, tagMicroservices)));
        ananya = alumniRepository.save(ananya);

        Alumni vignesh = new Alumni("Vignesh Balaji", "vignesh.b@zoho.com", 2015, "Zoho", "Principal Database Architect", 3);
        vignesh.setBio("High-concurrency database internals, query optimization, and enterprise Spring Boot microservice architectures.");
        vignesh.setLinkedinUrl("https://linkedin.com/in/vignesh-balaji-zoho");
        vignesh.setExpertiseTags(new HashSet<>(Arrays.asList(tagDb, tagJava, tagMicroservices, tagSystemDesign)));
        vignesh = alumniRepository.save(vignesh);

        Alumni sneha = new Alumni("Sneha Patel", "sneha.patel@adobe.com", 2020, "Adobe", "Lead Full Stack Engineer", 2);
        sneha.setBio("UI/UX enthusiast, React micro-frontends, REST API integration, and preparing resumes for frontend/fullstack roles.");
        sneha.setLinkedinUrl("https://linkedin.com/in/sneha-patel-adobe");
        sneha.setExpertiseTags(new HashSet<>(Arrays.asList(tagFrontend, tagJava, tagSystemDesign)));
        sneha = alumniRepository.save(sneha);

        // 3. Create Students
        Student rahul = new Student("Rahul Verma", "rahul.verma@college.edu", "Computer Science and Engineering", 2026, "22CS045");
        rahul.setInterestTags(new HashSet<>(Arrays.asList(tagJava, tagCloud, tagMicroservices, tagSystemDesign)));
        rahul = studentRepository.save(rahul);

        Student deepa = new Student("Deepa Krishnan", "deepa.krishnan@college.edu", "Information Technology", 2025, "21IT023");
        deepa.setInterestTags(new HashSet<>(Arrays.asList(tagDSA, tagSystemDesign, tagML)));
        deepa = studentRepository.save(deepa);

        Student arvind = new Student("Arvind Kumar", "arvind.kumar@college.edu", "Computer Science and Engineering", 2026, "22CS112");
        arvind.setInterestTags(new HashSet<>(Arrays.asList(tagFrontend, tagJava)));
        arvind = studentRepository.save(arvind);

        Student meera = new Student("Meera Nair", "meera.nair@college.edu", "Artificial Intelligence & Data Science", 2027, "23AI018");
        meera.setInterestTags(new HashSet<>(Arrays.asList(tagML, tagCloud)));
        meera = studentRepository.save(meera);

        // 4. Create Initial Mentorship Pairs
        MentorshipPair pair1 = new MentorshipPair(priya, rahul, "Matched on Spring Boot and Cloud Architecture. Focusing on distributed caching and mock interviews.");
        pair1.setStatus(PairStatus.ACTIVE);
        pair1.setStartDate(LocalDate.now().minusWeeks(3));
        pair1 = mentorshipPairRepository.save(pair1);

        MentorshipPair pair2 = new MentorshipPair(karthik, deepa, "Focusing on LeetCode Medium/Hard graphs, dynamic programming, and low-level design.");
        pair2.setStatus(PairStatus.ACTIVE);
        pair2.setStartDate(LocalDate.now().minusWeeks(4));
        pair2 = mentorshipPairRepository.save(pair2);

        // 5. Create Initial Sessions
        Session session1 = new Session(pair1, LocalDateTime.now().minusWeeks(2).withHour(18).withMinute(0), 60,
                "Spring Boot Microservices & Spring Cloud Gateway Overview", "https://meet.google.com/abc-defg-hij");
        session1.setStatus(SessionStatus.COMPLETED);
        session1.setFeedbackNotes("Excellent interaction. Rahul walked through JWT filter architecture; assigned assignment on Redis caching.");
        sessionRepository.save(session1);

        Session session2 = new Session(pair1, LocalDateTime.now().minusWeeks(1).withHour(19).withMinute(0), 45,
                "Database Connection Pooling & HikariCP Tuning", "https://meet.google.com/klm-nopq-rst");
        session2.setStatus(SessionStatus.COMPLETED);
        session2.setFeedbackNotes("Analyzed slow query logs, explained index cardinality and MySQL query execution plan.");
        sessionRepository.save(session2);

        Session session3 = new Session(pair1, LocalDateTime.now().plusDays(3).withHour(18).withMinute(30), 60,
                "Mock Technical Interview — System Design (TinyURL & Rate Limiter)", "https://meet.google.com/xyz-uvwx-qrs");
        session3.setStatus(SessionStatus.SCHEDULED);
        sessionRepository.save(session3);

        Session session4 = new Session(pair2, LocalDateTime.now().minusWeeks(2).withHour(17).withMinute(0), 60,
                "Graph Algorithms: Dijkstra, Topological Sort & Cycle Detection", "https://teams.microsoft.com/l/meetup-join/123");
        session4.setStatus(SessionStatus.COMPLETED);
        session4.setFeedbackNotes("Deepa solved 2 out of 3 problems independently. Discussed time complexity optimizations.");
        sessionRepository.save(session4);

        Session session5 = new Session(pair2, LocalDateTime.now().plusDays(2).withHour(17).withMinute(0), 60,
                "Dynamic Programming: 2D Grids and Subsequence Problems", "https://teams.microsoft.com/l/meetup-join/456");
        session5.setStatus(SessionStatus.SCHEDULED);
        sessionRepository.save(session5);

        // 6. Seed Initial Audit Logs
        auditLogService.recordLog("ALUMNI_REGISTERED", "ALUMNI", priya.getId(), "Registered alumni: " + priya.getName() + " (" + priya.getCompany() + ", " + priya.getJobTitle() + ")");
        auditLogService.recordLog("ALUMNI_REGISTERED", "ALUMNI", karthik.getId(), "Registered alumni: " + karthik.getName() + " (" + karthik.getCompany() + ", " + karthik.getJobTitle() + ")");
        auditLogService.recordLog("STUDENT_REGISTERED", "STUDENT", rahul.getId(), "Registered student: " + rahul.getName() + " (" + rahul.getDepartment() + ")");
        auditLogService.recordLog("STUDENT_REGISTERED", "STUDENT", deepa.getId(), "Registered student: " + deepa.getName() + " (" + deepa.getDepartment() + ")");
        auditLogService.recordLog("PAIR_CREATED", "MENTORSHIP_PAIR", pair1.getId(), "Created mentorship pair between Mentor 'Priya Sharma' and Student 'Rahul Verma'");
        auditLogService.recordLog("PAIR_CREATED", "MENTORSHIP_PAIR", pair2.getId(), "Created mentorship pair between Mentor 'Karthik Rajan' and Student 'Deepa Krishnan'");
        auditLogService.recordLog("SESSION_COMPLETED", "SESSION", session1.getId(), "Session #1 status updated to COMPLETED");
        auditLogService.recordLog("SESSION_COMPLETED", "SESSION", session2.getId(), "Session #2 status updated to COMPLETED");
        auditLogService.recordLog("SESSION_SCHEDULED", "SESSION", session3.getId(), "Scheduled session on topic: 'Mock Technical Interview' for Pair #1");
        auditLogService.recordLog("SESSION_COMPLETED", "SESSION", session4.getId(), "Session #4 status updated to COMPLETED");
        auditLogService.recordLog("SESSION_SCHEDULED", "SESSION", session5.getId(), "Scheduled session on topic: 'Dynamic Programming' for Pair #2");
    }
}
