package project.interactivenovelplatform.service.impl;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.error.GlobalException;
import project.interactivenovelplatform.service.StorageService;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
public class StorageServiceImpl implements StorageService {
    private final MinioClient minioClient;
    private final String bucketName;
    private final String endpoint; // Нужно добавить для формирования URL
    private final static Logger log = LoggerFactory.getLogger(GlobalException.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif");
    private final Tika tika = new Tika();
    public StorageServiceImpl(
            MinioClient minioClient,
            @Value("${minio.bucketName}") String bucketName,
            @Value("${minio.endpoint}") String endpoint
    ) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        this.endpoint = endpoint;
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
        String baseUrl=endpoint.endsWith("/")?endpoint.substring(0,endpoint.length()-1):endpoint;
        return String.format("%s/%s/%s", baseUrl,bucketName, blobName);
    }
    @Override
    public void deleteFile(String publicUrl) {
        if (publicUrl == null || !publicUrl.contains(bucketName)) {
            log.warn("Попытка удаления файла по некорректному URL: {}", publicUrl);
            return;
        }
        try {
            String marker = bucketName + "/";
            String blobName = publicUrl.substring(publicUrl.indexOf(marker)+marker.length());
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(blobName)
                            .build()
            );
        }
        catch (Exception e) {
            log.error("Ошибка при удалении файла: {}", e.getMessage());
        }
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



}
