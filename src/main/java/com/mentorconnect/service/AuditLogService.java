package com.mentorconnect.service;

import com.mentorconnect.dto.AuditLogDto;
import com.mentorconnect.model.AuditLog;
import com.mentorconnect.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog recordLog(String action, String entityType, Long entityId, String details) {
        String safeDetails = details;
        if (safeDetails != null && safeDetails.length() > 1000) {
            safeDetails = safeDetails.substring(0, 997) + "...";
        }
        AuditLog log = new AuditLog(action, entityType, entityId, safeDetails);
        return auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getLogsPaged(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable)
                .map(this::toDto);
    }

    public AuditLogDto toDto(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getTimestamp()
        );
    }
}
