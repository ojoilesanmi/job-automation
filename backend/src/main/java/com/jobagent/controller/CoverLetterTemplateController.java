package com.jobagent.controller;

import com.jobagent.dto.*;
import com.jobagent.model.CoverLetterTemplate;
import com.jobagent.model.User;
import com.jobagent.repository.CoverLetterTemplateRepository;
import com.jobagent.security.RequirePermission;
import com.jobagent.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/cover-letter-templates")
@RequiredArgsConstructor
public class CoverLetterTemplateController {

    private final CoverLetterTemplateRepository templateRepository;

    @GetMapping
    @RequirePermission("cover_letter:read")
    public ResponseEntity<BaseResponse<List<CoverLetterTemplateResponse>>> getTemplates() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<CoverLetterTemplateResponse> templates = templateRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(t -> new CoverLetterTemplateResponse(t.getId(), t.getName(), t.getContent(),
                        t.getTone(), t.getTargetRole(), t.getCreatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(BaseResponse.success(templates));
    }

    @PostMapping
    @RequirePermission("cover_letter:write")
    public ResponseEntity<BaseResponse<CoverLetterTemplateResponse>> saveTemplate(
            @RequestBody SaveTemplateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = new User();
        user.setId(userId);

        CoverLetterTemplate template = CoverLetterTemplate.builder()
                .user(user)
                .name(request.name())
                .content(request.content())
                .tone(request.tone())
                .targetRole(request.targetRole())
                .build();
        template = templateRepository.save(template);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("Template saved",
                        new CoverLetterTemplateResponse(template.getId(), template.getName(),
                                template.getContent(), template.getTone(), template.getTargetRole(),
                                template.getCreatedAt())));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("cover_letter:write")
    public ResponseEntity<BaseResponse<Void>> deleteTemplate(@PathVariable UUID id) {
        CoverLetterTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new com.jobagent.exception.ResourceNotFoundException("Template not found"));
        if (!template.getUser().getId().equals(SecurityUtils.getCurrentUserId())) {
            throw new com.jobagent.exception.ForbiddenException("Access denied");
        }
        templateRepository.delete(template);
        return ResponseEntity.ok(BaseResponse.success("Template deleted", null));
    }
}
