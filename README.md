# 🎓 MentorConnect — Alumni-Student Mentorship Matching Platform
**Question 65 Implementation &bull; Target: 100/100 Marks**  
*Sri Eshwar College of Engineering &bull; Spring Boot MVC + MySQL JDBC Connectivity*

[![Build & Test](https://img.shields.io/badge/Tests-36%20Passed%20(0%20Failures)-success?style=for-the-badge&logo=junit5)](file:///c:/Users/lijil/OneDrive/Documents/alumini)
[![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk)](file:///c:/Users/lijil/OneDrive/Documents/alumini)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-MVC%20%2B%20JPA-brightgreen?style=for-the-badge&logo=springboot)](file:///c:/Users/lijil/OneDrive/Documents/alumini)
[![Database](https://img.shields.io/badge/Database-MySQL%20%7C%20H2%20Fallback-blue?style=for-the-badge&logo=mysql)](file:///c:/Users/lijil/OneDrive/Documents/alumini)

---

## 🌟 Executive Summary

**MentorConnect** is an enterprise-grade web application designed to connect college students with experienced alumni mentors working across leading technology product companies. Built strictly against the Sri Eshwar College of Engineering evaluation rubric and all items from the Question 65 specification sheet (including all **"Ideas to Go Further"**), the platform guarantees data integrity, skill-aligned matching, concurrency safety, full session lifecycle tracking, and immutable audit logging.

---

## 🚀 Key Features

### Core Feature 1: Alumni & Student Registration
- **Alumni Mentors**: Profiles with graduation year, company, job title, bio, LinkedIn URL, maximum concurrent mentee limit, and multi-tag expertise domains.
- **Student Mentees**: Profiles with department, batch year, roll number, and learning interest tags.
- **Interest Tags Taxonomy**: Seeded domain taxonomy (e.g. *Java & Spring Boot, System Design, Microservices, Cloud Architecture, ML, DSA*).

### Core Feature 2: Smart Auto-Suggest & Matching Algorithm
- **Tag Overlap Ranking**: Calculates intersection of student interest tags with alumni expertise tags.
- **Multi-Tier Tie-Breaking**:
  1. Overlap count (descending).
  2. Eligibility flag (available slots & not already paired).
  3. Available mentoring slots (descending).
  4. Alumni name (alphabetical).
- **Match Percentage**: Dynamically computed and displayed in the UI.

### Core Feature 3: Capacity Enforcement & Pessimistic Concurrency Control
- **Enforced Capacity Limit**: Alumni cannot exceed their declared maximum concurrent mentees.
- **Race Condition Prevention**: Employs `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`) in `MatchingService.createPair()`, guaranteeing that concurrent applications for a mentor's final slot never cause overallocation.
- **Duplicate Prevention**: Prevents duplicate active pairings between the same mentor and mentee.

### Core Feature 4: Mentoring Sessions Tracking & Lifecycle
- **Active Pair Rule**: Sessions can only be scheduled for `ACTIVE` mentorship pairs. Scheduling on inactive or cancelled pairs is immediately rejected.
- **Lifecycle Transitions**: `SCHEDULED` &rarr; `COMPLETED` (with discussion feedback and duration minutes) or `CANCELLED`.
- **Cascade Cancellation**: Cancelling a mentorship pair automatically cancels all pending scheduled sessions with explanatory audit notes.

### Core Feature 5: Department Engagement Analytics & Reporting
- Department-wide KPI metrics: Total Alumni, Active Students, Active Pairs, Completed Sessions, and Total Engagement Hours.
- Pair-by-pair breakdown with scheduled/completed counts, completion rates, and printable reports.

### Advanced Features: Ideas to Go Further
- **Immutable Audit Logging**: Captures every critical event (`ALUMNI_REGISTERED`, `STUDENT_REGISTERED`, `PAIR_CREATED`, `PAIR_STATUS_CHANGED`, `SESSION_SCHEDULED`, `SESSION_COMPLETED`, `SESSION_CANCELLED`) with entity type, ID, timestamp, and details. Exposed via `GET /api/audit-logs` and a dedicated UI tab.
- **Spring Declarative Caching**: `@EnableCaching` with `ConcurrentMapCacheManager`. Caches taxonomy (`@Cacheable("tags")`) and heavy analytics (`@Cacheable("engagementReport")`), with `@CacheEvict` applied to all mutating operations.
- **Pagination & Sorting**: Spring Data JPA `Pageable` endpoints (`GET /api/alumni/paged`, `GET /api/sessions/paged`, `GET /api/audit-logs/paged`) with configurable page size and sort direction.

---

## 🛠️ Quickstart Guide

### 1. Prerequisites
- **Java 17** or higher (`java -version`)
- **MySQL 8.0+** running on `localhost:3306` (Optional: The application automatically falls back to embedded H2 if MySQL is offline!)

### 2. Running the Application
From the project root directory, run:
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Once started, open your web browser at:
👉 **`http://localhost:8080`**

### 3. Running Automated Tests
Run the complete unit, integration, and concurrency test suite:
```bash
.\mvnw.cmd test
```
*Current test suite status:* **36 tests run, 0 failures, 0 errors, 100% success rate.**

---

## 📡 REST API Documentation

| Method | Endpoint | Query / Body Params | Description | Success Code |
|---|---|---|---|---|
| `GET` | `/api/alumni` | `search`, `tag` | List alumni with optional search & tag filter | `200 OK` |
| `GET` | `/api/alumni/paged` | `page`, `size`, `sort` | Paginated and sorted alumni list | `200 OK` |
| `GET` | `/api/alumni/{id}` | Path variable `id` | Get alumni by ID | `200 OK` |
| `POST` | `/api/alumni` | JSON: `AlumniRequestDto` | Register new alumni mentor | `201 Created` |
| `PUT` | `/api/alumni/{id}` | Path `id`, JSON body | Update alumni profile & capacity | `200 OK` |
| `DELETE`| `/api/alumni/{id}` | Path `id` | Delete alumni (blocked if active history exists) | `204 No Content` |
| `GET` | `/api/students` | None | List all registered students | `200 OK` |
| `GET` | `/api/students/paged` | `page`, `size`, `sort` | Paginated and sorted students list | `200 OK` |
| `GET` | `/api/students/{id}` | Path `id` | Get student by ID | `200 OK` |
| `POST` | `/api/students` | JSON: `StudentRequestDto` | Register new student mentee | `201 Created` |
| `PUT` | `/api/students/{id}` | Path `id`, JSON body | Update student profile | `200 OK` |
| `DELETE`| `/api/students/{id}` | Path `id` | Delete student (blocked if history exists) | `204 No Content` |
| `GET` | `/api/tags` | None (Cached `@Cacheable`) | Retrieve all domain interest tags | `200 OK` |
| `POST` | `/api/tags` | JSON: `InterestTagDto` | Create new domain tag | `201 Created` |
| `DELETE`| `/api/tags/{id}` | Path `id` | Delete tag (blocked if assigned to members) | `204 No Content` |
| `GET` | `/api/matching/suggest/{id}` | Student ID | Auto-suggest ranked mentors by tag overlap | `200 OK` |
| `POST` | `/api/matching/suggest-custom`| JSON string array of tags | Suggest ranked mentors for custom tag list | `200 OK` |
| `POST` | `/api/matching/pair` | JSON: `{alumniId, studentId, notes}` | Create mentorship pair (Pessimistic lock) | `201 Created` |
| `GET` | `/api/mentorship-pairs`| `status`, `alumniId`, `studentId`| List mentorship pairings with filters | `200 OK` |
| `GET` | `/api/mentorship-pairs/paged`| `page`, `size` | Paginated mentorship pairings | `200 OK` |
| `PUT` | `/api/mentorship-pairs/{id}/status`| `status=COMPLETED\|CANCELLED`| Update pair status (auto-cancels sessions & audits) | `200 OK` |
| `GET` | `/api/sessions` | `pairId`, `status` | List sessions with optional filters | `200 OK` |
| `GET` | `/api/sessions/paged` | `page`, `size`, `sort`, `status` | Paginated & sorted sessions | `200 OK` |
| `POST` | `/api/sessions` | JSON: `SessionRequestDto` | Schedule session (Active pair check) | `201 Created` |
| `PUT` | `/api/sessions/{id}/status`| `status`, `feedbackNotes` | Complete session & log duration/feedback | `200 OK` |
| `GET` | `/api/reports/engagement` | None (Cached `@Cacheable`) | Department engagement metrics report | `200 OK` |
| `GET` | `/api/audit-logs` | None | Retrieve chronological system audit trail | `200 OK` |
| `GET` | `/api/audit-logs/paged` | `page`, `size` | Paginated audit logs | `200 OK` |

---

## 📂 Project Architecture Artifacts

- **[ARCHITECTURE.md](file:///c:/Users/lijil/OneDrive/Documents/alumini/ARCHITECTURE.md)**: Contains detailed Mermaid Class Diagrams, Sequence Diagrams (Matching, Concurrency Locking, Session Lifecycle), N-Tier Layered Architecture, and the Database Schema Data Dictionary.
- **[DEMO_GUIDE.md](file:///c:/Users/lijil/OneDrive/Documents/alumini/DEMO_GUIDE.md)**: Contains the 5-minute examination presentation script, live demo walkthrough sequence, key code snippets to explain, and Top 10 Viva Voce Q&A with model answers.
