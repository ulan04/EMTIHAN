package com.example.fileupload.controller;

import com.example.fileupload.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {
    
    private final MinioService minioService;
    
    @PostMapping("/txt")
    public ResponseEntity<Map<String, String>> uploadTxt(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("File is empty"));
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".txt")) {
                return ResponseEntity.badRequest().body(createErrorResponse("File must be a TXT file"));
            }
            
            String fileName = file.getOriginalFilename();
            minioService.uploadFile(fileName, file.getInputStream(), "text/plain", file.getSize());
            
            return ResponseEntity.ok(createSuccessResponse(fileName, "TXT file uploaded successfully"));
        } catch (Exception e) {
            log.error("Error uploading TXT file: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(createErrorResponse("Failed to upload file: " + e.getMessage()));
        }
    }
    
    @PostMapping("/png")
    public ResponseEntity<Map<String, String>> uploadPng(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("File is empty"));
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".png")) {
                return ResponseEntity.badRequest().body(createErrorResponse("File must be a PNG file"));
            }
            
            String fileName = file.getOriginalFilename();
            minioService.uploadFile(fileName, file.getInputStream(), "image/png", file.getSize());
            
            return ResponseEntity.ok(createSuccessResponse(fileName, "PNG file uploaded successfully"));
        } catch (Exception e) {
            log.error("Error uploading PNG file: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(createErrorResponse("Failed to upload file: " + e.getMessage()));
        }
    }
    
    @PostMapping("/json")
    public ResponseEntity<Map<String, String>> uploadJson(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("File is empty"));
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".json")) {
                return ResponseEntity.badRequest().body(createErrorResponse("File must be a JSON file"));
            }
            
            String fileName = file.getOriginalFilename();
            minioService.uploadFile(fileName, file.getInputStream(), "application/json", file.getSize());
            
            return ResponseEntity.ok(createSuccessResponse(fileName, "JSON file uploaded successfully"));
        } catch (Exception e) {
            log.error("Error uploading JSON file: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(createErrorResponse("Failed to upload file: " + e.getMessage()));
        }
    }
    
    private Map<String, String> createSuccessResponse(String fileName, String message) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", message);
        response.put("fileName", fileName);
        return response;
    }
    
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return response;
    }
}

