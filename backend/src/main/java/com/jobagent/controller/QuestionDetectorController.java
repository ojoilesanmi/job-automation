package com.jobagent.controller;

import com.jobagent.dto.BaseResponse;
import com.jobagent.dto.DetectQuestionsRequest;
import com.jobagent.dto.DetectedQuestion;
import com.jobagent.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/ats")
@RequiredArgsConstructor
public class QuestionDetectorController {

    @PostMapping("/detect-questions")
    @RequirePermission("applications:read")
    public ResponseEntity<BaseResponse<List<DetectedQuestion>>> detectQuestions(
            @RequestBody DetectQuestionsRequest request) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "node", "../playwright/question-detector.js",
                    "--url", request.url()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();

            List<DetectedQuestion> questions = parseQuestions(output);
            return ResponseEntity.ok(BaseResponse.success(questions));
        } catch (Exception e) {
            return ResponseEntity.ok(BaseResponse.success(Collections.emptyList()));
        }
    }

    private List<DetectedQuestion> parseQuestions(String jsonOutput) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(jsonOutput);
            List<DetectedQuestion> questions = new ArrayList<>();
            if (root.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode node : root) {
                    List<Map<String, String>> options = new ArrayList<>();
                    if (node.has("options") && node.get("options").isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode opt : node.get("options")) {
                            options.add(Map.of(
                                    "value", opt.has("value") ? opt.get("value").asText() : "",
                                    "label", opt.has("label") ? opt.get("label").asText() : ""
                            ));
                        }
                    }
                    questions.add(new DetectedQuestion(
                            node.path("fieldName").asText(""),
                            node.path("fieldType").asText("text"),
                            node.path("label").asText(""),
                            node.path("required").asBoolean(false),
                            options
                    ));
                }
            }
            return questions;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
