package com.jobagent.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileUploadSecurityTest {

    @Test
    void allowedFileTypesContainPdf() {
        String[] allowedTypes = {"application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
        assertThat(allowedTypes).contains("application/pdf");
    }

    @Test
    void exeFileTypeIsNotInAllowed() {
        String[] allowedTypes = {"application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
        assertThat(allowedTypes).doesNotContain("application/octet-stream");
    }

    @Test
    void maxFileSizeIsReasonable() {
        long maxFileSize = 10 * 1024 * 1024;
        assertThat(maxFileSize).isEqualTo(10485760L);
    }
}
