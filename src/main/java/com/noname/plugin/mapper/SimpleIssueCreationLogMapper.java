package com.noname.plugin.mapper;

import com.noname.plugin.ao.SimpleIssueCreationLog;
import com.noname.plugin.dto.SimpleIssueCreationLogDto;

/**
 * @author dl
 * @date 19.06.2025 21:26
 */
public class SimpleIssueCreationLogMapper {
    public static SimpleIssueCreationLogDto toDto(SimpleIssueCreationLog entity) {
        SimpleIssueCreationLogDto dto = new SimpleIssueCreationLogDto();
        dto.setProjectKey(entity.getProjectKey());
        dto.setSummary(entity.getSummary());
        dto.setIssueKey(entity.getIssueKey());
        dto.setAuthorKey(entity.getAuthorKey());
        dto.setAttemptCreate(entity.getCreatedAt());
        dto.setIssueCreated(entity.getIssueIsCreated());
        dto.setErrorMessage(entity.getErrorMessage());
        return dto;
    }

    public static void updateEntity(SimpleIssueCreationLog entity, SimpleIssueCreationLogDto dto) {
        entity.setProjectKey(dto.getProjectKey());
        entity.setSummary(dto.getSummary());
        entity.setIssueKey(dto.getIssueKey());
        entity.setAuthorKey(dto.getAuthorKey());
        entity.setCreatedAt(dto.getAttemptCreate());
        entity.setIssueIsCreated(dto.isIssueCreated());
        entity.setErrorMessage(dto.getErrorMessage());
    }
}
