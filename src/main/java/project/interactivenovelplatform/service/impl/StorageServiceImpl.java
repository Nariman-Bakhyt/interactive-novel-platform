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
        java.io.InputStream inputStream;
        long size;
        String contentType = file.getContentType();

        if (uniqueFileName.toLowerCase().endsWith(".gif") || "image/gif".equals(file.getContentType())) {
            long[] outSize = new long[1];
            inputStream = processGifToWebp(file, outSize);
            size = outSize[0];
            contentType = "image/webp";
            
            // Меняем расширение файла на .webp
            if (uniqueFileName.toLowerCase().endsWith(".gif")) {
                String nameWithoutExt = uniqueFileName.substring(0, uniqueFileName.length() - 4);
                blobName = folder + "/" + nameWithoutExt + ".webp";
            } else {
                blobName = folder + "/" + uniqueFileName + ".webp";
            }
        } else {
            inputStream = file.getInputStream();
            size = file.getSize();
        }

        try (inputStream) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(blobName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
        }
        
        return blobName;
    }

    private java.io.InputStream processGifToWebp(MultipartFile file, long[] outSize) throws Exception {
        if (file.getSize() > 15 * 1024 * 1024) {
            throw new IllegalArgumentException("Размер GIF-файла не должен превышать 15 МБ");
        }
        java.io.File tempIn = java.io.File.createTempFile("in_", ".gif");
        java.io.File tempOut = java.io.File.createTempFile("out_", ".webp");
        try {
            file.transferTo(tempIn);
            ProcessBuilder pb = new ProcessBuilder(
                    "gif2webp", "-lossy", "-q", "80", tempIn.getAbsolutePath(), "-o", tempOut.getAbsolutePath()
            );
            Process process = pb.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
                throw new RuntimeException("Превышено время ожидания сжатия GIF");
            }
            if (process.exitValue() != 0) {
                throw new RuntimeException("Конвертация gif2webp завершилась с кодом ошибки: " + process.exitValue() 
                        + ". Убедитесь, что установлен пакет libwebp-tools.");
            }
            outSize[0] = tempOut.length();
            return new java.io.FileInputStream(tempOut) {
                @Override
                public void close() throws IOException {
                    super.close();
                    tempIn.delete();
                    tempOut.delete();
                }
            };
        } catch (Exception e) {
            tempIn.delete();
            tempOut.delete();
            throw e;
        }
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

    @Override
    public String getBlobNameFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        String prefix = storageBaseUrl + "/";
        int index = url.indexOf(prefix);
        if (index != -1) {
            return url.substring(index + prefix.length());
        }
        return null;
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
