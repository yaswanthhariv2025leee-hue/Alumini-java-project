package com.mentorconnect.dto;

import java.util.ArrayList;
import java.util.List;

public class DepartmentEngagementSummaryDto {
    private long totalAlumni;
    private long totalStudents;
    private long totalPairs;
    private long activePairs;
    private long completedPairs;
    private long cancelledPairs;
    private long totalSessions;
    private long scheduledSessions;
    private long completedSessions;
    private long cancelledSessions;
    private double totalCompletedHours;
    private List<EngagementPairReportDto> pairReports = new ArrayList<>();

    public DepartmentEngagementSummaryDto() {
    }

    public long getTotalAlumni() {
        return totalAlumni;
    }

    public void setTotalAlumni(long totalAlumni) {
        this.totalAlumni = totalAlumni;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalPairs() {
        return totalPairs;
    }

    public void setTotalPairs(long totalPairs) {
        this.totalPairs = totalPairs;
    }

    public long getActivePairs() {
        return activePairs;
    }

    public void setActivePairs(long activePairs) {
        this.activePairs = activePairs;
    }

    public long getCompletedPairs() {
        return completedPairs;
    }

    public void setCompletedPairs(long completedPairs) {
        this.completedPairs = completedPairs;
    }

    public long getCancelledPairs() {
        return cancelledPairs;
    }

    public void setCancelledPairs(long cancelledPairs) {
        this.cancelledPairs = cancelledPairs;
    }

    public long getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(long totalSessions) {
        this.totalSessions = totalSessions;
    }

    public long getScheduledSessions() {
        return scheduledSessions;
    }

    public void setScheduledSessions(long scheduledSessions) {
        this.scheduledSessions = scheduledSessions;
    }

    public long getCompletedSessions() {
        return completedSessions;
    }

    public void setCompletedSessions(long completedSessions) {
        this.completedSessions = completedSessions;
    }

    public long getCancelledSessions() {
        return cancelledSessions;
    }

    public void setCancelledSessions(long cancelledSessions) {
        this.cancelledSessions = cancelledSessions;
    }

    public double getTotalCompletedHours() {
        return totalCompletedHours;
    }

    public void setTotalCompletedHours(double totalCompletedHours) {
        this.totalCompletedHours = totalCompletedHours;
    }

    public List<EngagementPairReportDto> getPairReports() {
        return pairReports;
    }

    public void setPairReports(List<EngagementPairReportDto> pairReports) {
        this.pairReports = pairReports;
    }
}
