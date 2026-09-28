package com.mentorconnect.dto;

import com.mentorconnect.model.PairStatus;
import java.time.LocalDate;

public class EngagementPairReportDto {
    private Long pairId;
    private Long alumniId;
    private String alumniName;
    private String alumniEmail;
    private String alumniCompany;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String studentDepartment;
    private PairStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalSessionsScheduled;
    private int completedSessions;
    private int cancelledSessions;
    private long totalCompletedMinutes;
    private double totalCompletedHours;

    public EngagementPairReportDto() {
    }

    public Long getPairId() {
        return pairId;
    }

    public void setPairId(Long pairId) {
        this.pairId = pairId;
    }

    public Long getAlumniId() {
        return alumniId;
    }

    public void setAlumniId(Long alumniId) {
        this.alumniId = alumniId;
    }

    public String getAlumniName() {
        return alumniName;
    }

    public void setAlumniName(String alumniName) {
        this.alumniName = alumniName;
    }

    public String getAlumniEmail() {
        return alumniEmail;
    }

    public void setAlumniEmail(String alumniEmail) {
        this.alumniEmail = alumniEmail;
    }

    public String getAlumniCompany() {
        return alumniCompany;
    }

    public void setAlumniCompany(String alumniCompany) {
        this.alumniCompany = alumniCompany;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getStudentDepartment() {
        return studentDepartment;
    }

    public void setStudentDepartment(String studentDepartment) {
        this.studentDepartment = studentDepartment;
    }

    public PairStatus getStatus() {
        return status;
    }

    public void setStatus(PairStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getTotalSessionsScheduled() {
        return totalSessionsScheduled;
    }

    public void setTotalSessionsScheduled(int totalSessionsScheduled) {
        this.totalSessionsScheduled = totalSessionsScheduled;
    }

    public int getCompletedSessions() {
        return completedSessions;
    }

    public void setCompletedSessions(int completedSessions) {
        this.completedSessions = completedSessions;
    }

    public int getCancelledSessions() {
        return cancelledSessions;
    }

    public void setCancelledSessions(int cancelledSessions) {
        this.cancelledSessions = cancelledSessions;
    }

    public long getTotalCompletedMinutes() {
        return totalCompletedMinutes;
    }

    public void setTotalCompletedMinutes(long totalCompletedMinutes) {
        this.totalCompletedMinutes = totalCompletedMinutes;
    }

    public double getTotalCompletedHours() {
        return totalCompletedHours;
    }

    public void setTotalCompletedHours(double totalCompletedHours) {
        this.totalCompletedHours = totalCompletedHours;
    }
}
