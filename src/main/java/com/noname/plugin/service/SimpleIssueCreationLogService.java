package com.noname.plugin.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.noname.plugin.ao.SimpleIssueCreationLog;
import com.noname.plugin.dto.SimpleIssueCreationLogDto;
import com.noname.plugin.mapper.SimpleIssueCreationLogMapper;
import org.springframework.stereotype.Component;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author dl
 * @date 19.06.2025 21:40
 */

@Component
public class SimpleIssueCreationLogService {
    @ComponentImport
    private final ActiveObjects ao;

    @Inject
    public SimpleIssueCreationLogService(ActiveObjects ao) {
        this.ao = ao;
    }

    public void logIssueCreation(SimpleIssueCreationLogDto dto) {
        ao.executeInTransaction(() -> {
            SimpleIssueCreationLog entity = ao.create(SimpleIssueCreationLog.class);
            SimpleIssueCreationLogMapper.updateEntity(entity, dto);
            entity.save();
            return null;
        });
    }

    public List<SimpleIssueCreationLogDto> getAllLogs() {
        return Arrays.stream(ao.find(SimpleIssueCreationLog.class))
                .map(SimpleIssueCreationLogMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<SimpleIssueCreationLogDto> getLogsByAuthor(String userId) {
        return Arrays.stream(ao.find(SimpleIssueCreationLog.class, "AUTHOR_KEY = ?", userId))
                .map(SimpleIssueCreationLogMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<SimpleIssueCreationLogDto> getLogsAfter(Date date) {
        return Arrays.stream(ao.find(SimpleIssueCreationLog.class, "CREATED_AT > ?", date))
                .map(SimpleIssueCreationLogMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<SimpleIssueCreationLogDto> getAllFailedCreations(Boolean isCreated) {
        return Arrays.stream(ao.find(SimpleIssueCreationLog.class, "ISSUE_IS_CREATED = ?", isCreated))
                .map(SimpleIssueCreationLogMapper::toDto)
                .collect(Collectors.toList());
    }
}
