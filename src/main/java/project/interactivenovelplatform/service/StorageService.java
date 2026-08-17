package project.interactivenovelplatform.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public interface StorageService {
    String uploadFile(MultipartFile file, String folder, String uniqueFileName) throws Exception;
    void deleteFile(String publicUrl);
    String getFileExtension(String fileName);
    Set<String> getAllowedExtensions();
    String verifyRealImageType(MultipartFile file);
    String getPublicUrl(String blobName);

    String getPresignedUrl(String objectPath);

    String getBlobNameFromUrl(String url);
}
