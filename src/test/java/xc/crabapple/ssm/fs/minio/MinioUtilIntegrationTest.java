package xc.crabapple.ssm.fs.minio;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import xc.crabapple.core.cipher.md5.MD5Util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 2026-10-18
 * 真实 MinIO 环境测试，仅在提供以下环境变量时执行：
 * TEST_MINIO_ENDPOINT / TEST_MINIO_ACCESS_KEY / TEST_MINIO_SECRET_KEY。
 *
 * 测试对象会使用 MinioUtil 的固定 bucket "crabapple"，结束后通过 SDK 删除。
 */
@EnabledIfEnvironmentVariable(named = "TEST_MINIO_ENDPOINT", matches = ".+")
public class MinioUtilIntegrationTest {

    @Test
    public void uploadDownloadExistAndCleanup() throws Throwable {
        final String endpoint = System.getenv("TEST_MINIO_ENDPOINT");
        final String accessKey = System.getenv("TEST_MINIO_ACCESS_KEY");
        final String secretKey = System.getenv("TEST_MINIO_SECRET_KEY");
        assertNotNull(endpoint);
        assertNotNull(accessKey);
        assertNotNull(secretKey);

        // MinIO 写入使用 SM4 加密；测试使用随机临时密钥，不写入仓库。
        final String sm4Key = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        System.setProperty("crabapple.minio.sm4Key", sm4Key);
        System.setProperty("crabapple.minio.sm4Iv", sm4Key);
        assertTrue(MinioUtil.connect(endpoint, accessKey, secretKey), "MinIO connect failed");

        byte[] content = "crabapple minio real environment".getBytes(StandardCharsets.UTF_8);
        File source = File.createTempFile("crabapple-minio-", ".txt");
        File target = File.createTempFile("crabapple-minio-download-", ".txt");
        String md5 = null;
        try {
            Files.write(source.toPath(), content);
            md5 = MD5Util.getFileHash(source);

            Map<String, String> upload = MinioUtil.upload(source.getAbsolutePath());
            assertNotNull(upload, "upload result is null");
            assertTrue(upload.containsKey("hash") || upload.containsKey("id"), "upload result has no hash/id");

            Map<String, String> exist = MinioUtil.exist(md5);
            assertNotNull(exist, "object does not exist after upload");
            assertTrue(MinioUtil.verifyObject(md5), "object MD5 verify failed");

            Map<String, String> download = MinioUtil.download2disk(md5, target.getAbsolutePath());
            assertNotNull(download, "download result is null");
            assertArrayEquals(content, Files.readAllBytes(target.toPath()));
        } finally {
            Files.deleteIfExists(source.toPath());
            Files.deleteIfExists(target.toPath());
            System.clearProperty("crabapple.minio.sm4Key");
            System.clearProperty("crabapple.minio.sm4Iv");
            if (md5 != null) {
                try {
                    MinioClient client = MinioClient.builder()
                            .endpoint(endpoint)
                            .credentials(accessKey, secretKey)
                            .build();
                    client.removeObject(RemoveObjectArgs.builder()
                            .bucket("crabapple")
                            .object(md5)
                            .build());
                } catch (Throwable ignored) {
                    // 清理失败不掩盖测试结果；测试对象只含随机文本
                }
            }
        }
    }
}
