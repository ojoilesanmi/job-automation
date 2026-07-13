package com.jobagent.controller;

import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.CvDocument;
import com.jobagent.repository.CvDocumentRepository;
import com.jobagent.security.SecurityUtils;
import com.jobagent.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final CvDocumentRepository cvDocumentRepository;
    private final FileStorageService fileStorageService;

    @GetMapping("/cvs/{cvId}")
    public ResponseEntity<byte[]> getCv(@PathVariable UUID cvId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        CvDocument cv = cvDocumentRepository.findByIdAndUserId(cvId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("CV not found"));
        byte[] bytes = fileStorageService.retrieve(cv.getFileUrl());
        return ResponseEntity.ok()
                .contentType(detectContentType(cv.getFileName()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(cv.getFileName()).build().toString())
                .body(bytes);
    }

    private MediaType detectContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        if (lower.endsWith(".docx")) {
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
