package xc.crabapple.core.random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 2026-10-17
 * RandomUtil 单元测试
 */
public class RandomUtilTest {

    @Test
    public void getUUID() {
        String uuid = RandomUtil.getUUID();
        assertEquals(36, uuid.length());
        assertNotEquals(uuid, RandomUtil.getUUID());
    }

    @Test
    public void getStr() {
        assertEquals(8, RandomUtil.getStr(8).length());
        assertEquals(16, RandomUtil.getStr(16).length());
        assertEquals(32, RandomUtil.getStr(32).length());
        assertEquals("", RandomUtil.getStr(0));
        assertEquals("", RandomUtil.getStr(-1));
    }

    @Test
    public void getStrWithStamp() {
        assertEquals(13, RandomUtil.getStrWithStamp(13).length());
        assertEquals(20, RandomUtil.getStrWithStamp(20).length());
        assertEquals("", RandomUtil.getStrWithStamp(12));
    }

    @Test
    public void getInt() {
        assertEquals(-1, RandomUtil.getInt(0).intValue());
        assertEquals(-2, RandomUtil.getInt(10).intValue());
        assertEquals(4, String.valueOf(RandomUtil.getInt(4)).length());
        assertEquals(5, RandomUtil.getInt(5, 5).intValue());
    }

    @Test
    public void getLong() {
        assertEquals(-1L, RandomUtil.getLong(0).longValue());
        assertEquals(-2L, RandomUtil.getLong(20).longValue());
        assertEquals(7L, RandomUtil.getLong(7L, 7L).longValue());
    }

}
