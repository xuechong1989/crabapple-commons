package xc.crabapple.ssm.fs.mongodb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 2026-10-18
 * 真实 MongoDB 集成测试：分块、续传、秒传、下载、MD5 校验。
 *
 * 没有 Docker 的环境自动跳过；GitHub Actions 的 ubuntu runner 会实际执行。
 */
@Testcontainers(disabledWithoutDocker = true)
public class MongoUtilIntegrationTest {

    @Container
    private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @BeforeAll
    public static void init() {
        MongoUtil.init(new MongoConfig(MONGO.getReplicaSetUrl(), "cloudkd_test", "integration-test-key"));
    }

    @AfterAll
    public static void destroy() {
        MongoUtil.destroy();
    }

    @Test
    public void smallFileUploadDownload() throws Throwable {
        byte[] content = "crabapple mongo integration".getBytes(StandardCharsets.UTF_8);
        String md5 = md5(content);

        Map<String, Object> upload = MongoUtil.write(new ByteArrayInputStream(content), md5, (long) content.length, 0L);
        assertEquals("success", upload.get("type"));
        assertEquals(MongoUtil.FILE_UPLOAD_SUCCESS, upload.get("code"));

        Map<String, Object> exist = MongoUtil.exist(md5);
        assertEquals("fileOK", exist.get("status"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Map<String, Object> download = MongoUtil.readByHash(md5, out);
        assertEquals(Boolean.TRUE, download.get("exist"));
        assertEquals(Boolean.TRUE, download.get("md5Match"));
        assertArrayEquals(content, out.toByteArray());
    }

    @Test
    public void chunkResumeAndFastUpload() throws Throwable {
        byte[] content = randomBytes(MongoUtil.CHUNK_SIZE + 1234);
        String md5 = md5(content);
        long fileSize = content.length;

        byte[] first = Arrays.copyOfRange(content, 0, MongoUtil.CHUNK_SIZE);
        Map<String, Object> firstResult = MongoUtil.write(new ByteArrayInputStream(first), md5, fileSize, 0L);
        assertEquals("success", firstResult.get("type"));
        assertEquals(MongoUtil.FILE_UPLOADING, firstResult.get("code"));
        assertEquals("ing", MongoUtil.exist(md5).get("status"));

        byte[] rest = Arrays.copyOfRange(content, MongoUtil.CHUNK_SIZE, content.length);
        Map<String, Object> restResult = MongoUtil.write(new ByteArrayInputStream(rest), md5, fileSize, (long) MongoUtil.CHUNK_SIZE);
        assertEquals("success", restResult.get("type"));
        assertEquals(MongoUtil.FILE_UPLOAD_SUCCESS, restResult.get("code"));
        assertEquals("fileOK", MongoUtil.exist(md5).get("status"));

        Map<String, Object> fast = MongoUtil.write((java.io.InputStream) null, md5, fileSize, -1L);
        assertEquals("success", fast.get("type"));
        assertEquals(MongoUtil.FILE_UPLOAD_FAST, fast.get("code"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Map<String, Object> download = MongoUtil.readByHash(md5, out);
        assertEquals(Boolean.TRUE, download.get("exist"));
        assertEquals(Boolean.TRUE, download.get("md5Match"));
        assertArrayEquals(content, out.toByteArray());
    }

    @Test
    public void downloadRejectsUnknownHash() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Map<String, Object> result = MongoUtil.readByHash("00000000000000000000000000000000", out);
        assertEquals(Boolean.FALSE, result.get("exist"));
        assertNotNull(result.get("error"));
        assertEquals(0, out.size());
    }

    private static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        new Random(20261018L).nextBytes(bytes);
        return bytes;
    }

    private static String md5(byte[] bytes) throws Throwable {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hash = digest.digest(bytes);
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}
