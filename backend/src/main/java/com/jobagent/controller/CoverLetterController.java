package com.jobagent.controller;

import com.jobagent.dto.*;
import com.jobagent.security.RequirePermission;
import com.jobagent.security.SecurityUtils;
import com.jobagent.service.CoverLetterService;
import com.jobagent.service.DocxExportService;
import com.jobagent.service.PdfExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CoverLetterController {

    private final CoverLetterService coverLetterService;
    private final PdfExportService pdfExportService;
    private final DocxExportService docxExportService;

    @PostMapping("/jobs/{jobId}/cover-letters/generate")
    @RequirePermission("cover_letter:write")
    public ResponseEntity<BaseResponse<CoverLetterResponse>> generateCoverLetter(
            @PathVariable UUID jobId,
            @RequestBody(required = false) Map<String, String> body) {
        UUID cvDocumentId = body != null && body.containsKey("cvDocumentId")
                ? UUID.fromString(body.get("cvDocumentId")) : null;
        String tone = body != null ? body.get("tone") : null;

        GenerateCoverLetterRequest request = new GenerateCoverLetterRequest(jobId, cvDocumentId, tone);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("Cover letter generated",
                        coverLetterService.generateCoverLetter(SecurityUtils.getCurrentUserId(), request)));
    }

    @GetMapping("/cover-letters")
    @RequirePermission("cover_letter:read")
    public ResponseEntity<BaseResponse<CoverLetterListResponse>> getAllCoverLetters() {
        return ResponseEntity.ok(BaseResponse.success(
                coverLetterService.getAllCoverLetters(SecurityUtils.getCurrentUserId())));
    }

    @GetMapping("/jobs/{jobId}/cover-letters")
    @RequirePermission("cover_letter:read")
    public ResponseEntity<BaseResponse<CoverLetterListResponse>> getCoverLetters(@PathVariable UUID jobId) {
        return ResponseEntity.ok(BaseResponse.success(
                coverLetterService.getCoverLetters(SecurityUtils.getCurrentUserId(), jobId)));
    }

    @PutMapping("/cover-letters/{id}")
    @RequirePermission("cover_letter:write")
    public ResponseEntity<BaseResponse<CoverLetterResponse>> updateCoverLetter(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(BaseResponse.success("Cover letter updated",
                coverLetterService.updateCoverLetter(SecurityUtils.getCurrentUserId(), id, body.get("content"))));
    }

    @PostMapping("/cover-letters/{id}/regenerate")
    @RequirePermission("cover_letter:write")
    public ResponseEntity<BaseResponse<CoverLetterResponse>> regenerateCoverLetter(@PathVariable UUID id) {
        return ResponseEntity.ok(BaseResponse.success("Cover letter regenerated",
                coverLetterService.regenerateCoverLetter(SecurityUtils.getCurrentUserId(), id)));
    }

    @GetMapping("/cover-letters/{id}/export")
    @RequirePermission("cover_letter:read")
    public ResponseEntity<byte[]> exportCoverLetter(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "text") String format) {
        CoverLetterResponse cl = coverLetterService.getCoverLetterById(SecurityUtils.getCurrentUserId(), id);
        String content = cl.content();

        if ("pdf".equals(format)) {
            byte[] pdfBytes = pdfExportService.generatePdf(content);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cover-letter-" + id + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        }

        if ("docx".equals(format)) {
            byte[] docxBytes = docxExportService.generateDocx(content, "Cover Letter");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cover-letter-" + id + ".docx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .body(docxBytes);
        }

        byte[] textBytes = content.getBytes();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cover-letter-" + id + ".txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(textBytes);
    }
}
