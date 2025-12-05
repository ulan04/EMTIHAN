package com.example.miniodata.service;

import com.example.miniodata.entity.FileMetadata;
import com.example.miniodata.repository.FileMetadataRepository;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileService {

    private final FileMetadataRepository fileRepo;
    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    public FileService(FileMetadataRepository fileRepo, MinioClient minioClient) {
        this.fileRepo = fileRepo;
        this.minioClient = minioClient;
    }

    public String upload(MultipartFile file) throws Exception {
        return upload(file, null);
    }

    public String upload(MultipartFile file, String folderPath) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        ensureBucket();

        String fileName = resolveFileName(file.getOriginalFilename());
        String objectPath = folderPath != null && !folderPath.isEmpty() 
            ? folderPath + "/" + fileName 
            : fileName;
        
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectPath)
                    .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .stream(inputStream, file.getSize(), -1)
                    .build();
            minioClient.putObject(args);
        }

        FileMetadata meta = new FileMetadata();
        meta.setFileName(objectPath);
        meta.setContentType(file.getContentType());
        meta.setSize(file.getSize());
        meta.setFolderPath(folderPath);
        fileRepo.save(meta);

        return objectPath;
    }

    public List<String> uploadMultiple(List<MultipartFile> files) throws Exception {
        return uploadMultiple(files, null);
    }

    public List<String> uploadMultiple(List<MultipartFile> files, String folderName) throws Exception {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("文件列表不能为空");
        }

        String folderPath = folderName != null && !folderName.isEmpty() 
            ? folderName 
            : "folder_" + System.currentTimeMillis();

        List<String> uploaded = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            try {
                if (file == null || file.isEmpty()) {
                    errors.add("文件 " + (i + 1) + " 为空，已跳过");
                    continue;
                }
                uploaded.add(upload(file, folderPath));
            } catch (Exception e) {
                String fileName = file != null ? file.getOriginalFilename() : "未知文件";
                errors.add("文件 " + (i + 1) + " (" + fileName + ") 上传失败: " + e.getMessage());
            }
        }
        
        if (uploaded.isEmpty() && !errors.isEmpty()) {
            throw new IllegalArgumentException("所有文件上传失败: " + String.join("; ", errors));
        }
        
        if (!errors.isEmpty()) {

            System.out.println("部分文件上传失败: " + String.join("; ", errors));
        }
        
        return uploaded;
    }

    public FileDownload download(String fileName) throws Exception {
        ensureBucket();
        byte[] bytes;
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(fileName)
                .build())) {
            bytes = stream.readAllBytes();
        }
        String contentType = fileRepo.findByFileName(fileName)
                .map(FileMetadata::getContentType)
                .orElse("application/octet-stream");
        return new FileDownload(bytes, contentType);
    }

    public byte[] downloadMultiple(List<String> fileNames) throws Exception {
        if (fileNames == null || fileNames.isEmpty()) {
            throw new IllegalArgumentException("文件名列表不能为空");
        }

        ensureBucket();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (String fileName : fileNames) {
                try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileName)
                        .build())) {

                    String entryName = fileName.contains("/") 
                        ? fileName.substring(fileName.lastIndexOf("/") + 1) 
                        : fileName;
                    zos.putNextEntry(new ZipEntry(entryName));
                    stream.transferTo(zos);
                    zos.closeEntry();
                }
            }
        }
        return baos.toByteArray();
    }

    public byte[] downloadByFolder(String folderPath) throws Exception {
        if (folderPath == null || folderPath.isEmpty()) {
            throw new IllegalArgumentException("文件夹路径不能为空");
        }

        ensureBucket();

        List<FileMetadata> files = fileRepo.findByFolderPath(folderPath);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("文件夹 '" + folderPath + "' 中没有文件");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (FileMetadata meta : files) {
                try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(meta.getFileName())
                        .build())) {

                    String entryName = meta.getFileName().contains("/") 
                        ? meta.getFileName().substring(meta.getFileName().lastIndexOf("/") + 1) 
                        : meta.getFileName();
                    zos.putNextEntry(new ZipEntry(entryName));
                    stream.transferTo(zos);
                    zos.closeEntry();
                }
            }
        }
        return baos.toByteArray();
    }

    private void ensureBucket() throws ServerException, InsufficientDataException, ErrorResponseException,
            IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException,
            XmlParserException, InternalException {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    public void deleteFileById(Long fileId) throws Exception {
        if (fileId == null) {
            throw new IllegalArgumentException("文件ID不能为空");
        }

        ensureBucket();

        FileMetadata fileMeta = fileRepo.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("文件不存在，ID: " + fileId));

        String fileName = fileMeta.getFileName();
        String folderPath = fileMeta.getFolderPath();

        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(fileName)
                    .build());
        } catch (Exception e) {
            throw new Exception("从 MinIO 删除文件失败: " + e.getMessage(), e);
        }

        fileRepo.deleteById(fileId);

        if (folderPath != null && !folderPath.isEmpty()) {
            List<FileMetadata> remainingFiles = fileRepo.findByFolderPath(folderPath);
            if (remainingFiles.isEmpty()) {
                System.out.println("文件夹 '" + folderPath + "' 已为空，所有文件已删除");
            }
        }
    }

    public void deleteFolderByFolderPath(String folderPath) throws Exception {
        if (folderPath == null || folderPath.isEmpty()) {
            throw new IllegalArgumentException("文件夹路径不能为空");
        }

        ensureBucket();

        List<FileMetadata> files = fileRepo.findByFolderPath(folderPath);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("文件夹 '" + folderPath + "' 不存在或为空");
        }

        List<String> errors = new ArrayList<>();
        for (FileMetadata fileMeta : files) {
            try {

                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileMeta.getFileName())
                        .build());

                fileRepo.deleteById(fileMeta.getId());
            } catch (Exception e) {
                errors.add("删除文件 '" + fileMeta.getFileName() + "' 失败: " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new Exception("部分文件删除失败: " + String.join("; ", errors));
        }
    }

    public void deleteFolderByFileId(Long fileId) throws Exception {
        if (fileId == null) {
            throw new IllegalArgumentException("文件ID不能为空");
        }


        FileMetadata fileMeta = fileRepo.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("文件不存在，ID: " + fileId));

        String folderPath = fileMeta.getFolderPath();
        if (folderPath == null || folderPath.isEmpty()) {
            throw new IllegalArgumentException("该文件不属于任何文件夹");
        }

        deleteFolderByFolderPath(folderPath);
    }

    private String resolveFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "unnamed_file_" + System.currentTimeMillis();
        }
        return originalName;
    }

    public record FileDownload(byte[] data, String contentType) {
    }
}
