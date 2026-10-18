package xc.crabapple.ssm.fs.mongodb;

import org.bson.Document;
import org.bson.types.Binary;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 2026-10-18
 * MongoUtil 迁移后的基础测试：加密、分块索引、未初始化行为。
 */
public class MongoUtilTest {

    @Test
    public void aesRoundTrip() throws Throwable {
        byte[] plain = "crabapple mongo chunk".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = AESUtil.encryptBytes("test-key", plain, AESUtil.KEY256);
        assertNotNull(encrypted);
        byte[] decrypted = AESUtil.decryptBytes("test-key", encrypted, AESUtil.KEY256);
        assertEquals("crabapple mongo chunk", new String(decrypted, StandardCharsets.UTF_8));
    }

    @Test
    public void chunkIndex() {
        assertEquals(0, MongoUtil.getIndexByPos(0L));
        assertEquals(1, MongoUtil.getIndexByPos((long) MongoUtil.CHUNK_SIZE));
        assertEquals(2, MongoUtil.getIndexByPos((long) MongoUtil.CHUNK_SIZE * 2L));
        assertEquals(0, MongoUtil.getIndexByPos(-1L));
    }

    @Test
    public void readRunnableDecryptsChunk() throws Throwable {
        System.setProperty("crabapple.mongo.encrypt.key", "test-key");
        byte[] plain = "hello".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = AESUtil.encryptBytes("test-key", plain, AESUtil.KEY256);
        Document document = new Document("content", new Binary(encrypted));

        MongoReadRunnable runnable = new MongoReadRunnable(0, document);
        runnable.run();

        assertFalse(runnable.isError());
        assertEquals("hello", new String(runnable.getBuffer(), StandardCharsets.UTF_8));
    }

    @Test
    public void writeWithoutInitReturnsError() {
        Map<String, Object> result = MongoUtil.write((java.io.InputStream) null,
                "d41d8cd98f00b204e9800998ecf8427e", 1L, 0L);
        assertEquals("error", result.get("type"));
        assertEquals("mongo_not_init", result.get("code"));
        assertTrue(result.containsKey("reason"));
    }

    @Test
    public void configRejectsMissingValues() {
        try {
            new MongoConfig("", "db", "key");
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("mongo.uri"));
        }
    }
}
