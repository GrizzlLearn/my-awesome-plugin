package com.noname.plugin.dto;

import lombok.Data;
import java.util.Date;

/**
 * @author dl
 * @date 19.06.2025 21:20
 */

@Data
public class SimpleIssueCreationLogDto {
    private String projectKey;
    private String summary;
    private String issueKey;
    private String authorKey;
    private Date attemptCreate;
    private boolean isIssueCreated;
    private String errorMessage;
}
