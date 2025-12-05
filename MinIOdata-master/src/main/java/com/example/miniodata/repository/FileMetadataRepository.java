package com.example.miniodata.repository;

import com.example.miniodata.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    Optional<FileMetadata> findByFileName(String fileName);
    List<FileMetadata> findByFolderPath(String folderPath);
}
