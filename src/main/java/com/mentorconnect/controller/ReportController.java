package com.mentorconnect.controller;

import com.mentorconnect.dto.DepartmentEngagementSummaryDto;
import com.mentorconnect.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/engagement")
    public ResponseEntity<DepartmentEngagementSummaryDto> getEngagementReport() {
        return ResponseEntity.ok(reportService.getEngagementReport());
    }
}
