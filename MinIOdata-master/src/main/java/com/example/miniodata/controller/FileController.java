package com.example.miniodata.controller;

import com.example.miniodata.service.FileService;
import com.example.miniodata.service.FileService.FileDownload;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            String storedName = fileService.upload(file);
            return ResponseEntity.ok("上传成功: " + storedName);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload/batch")
    public ResponseEntity<?> uploadBatch(
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "folder", required = false) String folderName) {
        try {
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().body("文件列表不能为空。请在 Postman 中使用 form-data，添加多个 key 为 'files' 的文件字段");
            }

            List<MultipartFile> validFiles = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    validFiles.add(file);
                }
            }
            
            if (validFiles.isEmpty()) {
                return ResponseEntity.badRequest().body("没有有效的文件。请确保上传的文件不为空");
            }
            
            List<String> storedNames = fileService.uploadMultiple(validFiles, folderName);
            return ResponseEntity.ok(storedNames);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("批量上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<?> download(@PathVariable String fileName) {
        try {
            FileDownload download = fileService.download(fileName);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(download.contentType()))
                    .body(download.data());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("下载失败: " + e.getMessage());
        }
    }

    @PostMapping("/download/batch")
    public ResponseEntity<?> downloadBatch(@RequestBody List<String> fileNames) {
        try {
            if (fileNames == null || fileNames.isEmpty()) {
                return ResponseEntity.badRequest().body("文件名列表不能为空");
            }
            byte[] zipBytes = fileService.downloadMultiple(fileNames);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"files.zip\"")
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .body(zipBytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("批量下载失败: " + e.getMessage());
        }
    }

    @GetMapping("/download/folder/{folderPath}")
    public ResponseEntity<?> downloadByFolder(@PathVariable String folderPath) {
        try {
            byte[] zipBytes = fileService.downloadByFolder(folderPath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + folderPath + ".zip\"")
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .body(zipBytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("文件夹下载失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/folder/by-file-id/{fileId}")
    public ResponseEntity<?> deleteFolderByFileId(@PathVariable Long fileId) {
        try {
            fileService.deleteFolderByFileId(fileId);
            return ResponseEntity.ok("通过文件ID删除文件夹成功，文件ID: " + fileId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("删除文件夹失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/folder/{folderPath}")
    public ResponseEntity<?> deleteFolder(@PathVariable String folderPath) {
        try {
            fileService.deleteFolderByFolderPath(folderPath);
            return ResponseEntity.ok("文件夹及其所有文件删除成功: " + folderPath);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("删除文件夹失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id) {
        try {
            fileService.deleteFileById(id);
            return ResponseEntity.ok("文件删除成功，ID: " + id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("删除文件失败: " + e.getMessage());
        }
    }
}
