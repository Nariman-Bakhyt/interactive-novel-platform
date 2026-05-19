package project.interactivenovelplatform.service.impl;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.error.GlobalExceptionHandler;
import project.interactivenovelplatform.service.StorageService;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class StorageServiceImpl implements StorageService {
    private final MinioClient minioClient;
    private final String bucketName;
    private final String endpoint; 
    private final static Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final String storageBaseUrl; 
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif");
    private final Tika tika = new Tika();
    public StorageServiceImpl(
            MinioClient minioClient,
            @Value("${minio.bucketName}") String bucketName,
            @Value("${minio.endpoint}") String endpoint,
            @Value("${app.storage.base-url}") String storageBaseUrl 
    ) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        this.endpoint = endpoint;
        this.storageBaseUrl = storageBaseUrl;
    }
    @Override
    public Set<String> getAllowedExtensions() {
        return ALLOWED_EXTENSIONS;
    }

    @Override
    public String uploadFile(MultipartFile file, String folder, String uniqueFileName) throws Exception {

        String blobName = folder + "/" + uniqueFileName;

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(blobName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        
        return blobName;
    }
    @Override
    public void deleteFile(String blobName) {
        if (blobName == null || blobName.trim().isEmpty()) {
            log.warn("Попытка удаления файла с пустым или некорректным blobName: {}", blobName);
            return;
        }
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(blobName)
                            .build()
            );
            log.info("Файл успешно удален: {} из бакета {}", blobName, bucketName);
        }
        catch (Exception e) {
            log.error("Ошибка при удалении файла {}: {}", blobName, e.getMessage(), e);
        }
    }

    @Override
    public String getPublicUrl(String blobName) {
        
        return String.format("%s/%s", storageBaseUrl, blobName);
    }

    public String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex);
    }
    public String verifyRealImageType(MultipartFile file) {
        try {
            String actualMimeType = tika.detect(file.getInputStream());
            List<String> secureTypes = List.of("image/jpeg", "image/png", "image/webp", "image/gif");
            if (!secureTypes.contains(actualMimeType)) {
                throw new RuntimeException("Безопасность: Файл маскируется под фото, но это " + actualMimeType);
            }
            return actualMimeType;
        } catch (IOException e) {
            throw new RuntimeException("Ошибка анализа файла", e);
        }
    }

    @Override
    public String getPresignedUrl(String objectPath) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectPath)
                            .expiry(2, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Ошибка при генерации временной ссылки для файла {}", objectPath, e);
            return null;
        }
    }



}
