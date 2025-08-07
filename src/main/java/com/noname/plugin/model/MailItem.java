package com.noname.plugin.model;

import lombok.*;

import java.io.File;
import java.util.List;

/**
 * @author dl
 * @date 24.06.2025 22:02
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class MailItem {
    @NonNull
    String id = java.util.UUID.randomUUID().toString();
    @NonNull
    String from;
    @NonNull
    List<String> to;
    List<String> cc;
    List<String> bcc;
    @NonNull
    String subject;
    @NonNull
    String body;
    String attachmentsName;
}
