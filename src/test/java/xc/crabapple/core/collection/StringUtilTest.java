package xc.crabapple.core.collection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 2026-10-17
 * StringUtil 单元测试
 */
public class StringUtilTest {

    @Test
    public void insert() {
        assertEquals("hABello", StringUtil.insert("hello", 1, "AB"));
        assertEquals("hello", StringUtil.insert("hello", 99, ""));
    }

    @Test
    public void getCount() {
        assertEquals(3, StringUtil.getCount("a-b-c-d", "-"));
        assertEquals(2, StringUtil.getCount("aba", "a"));
        assertEquals(0, StringUtil.getCount(null, "-"));
    }

    @Test
    public void isBlank() {
        assertTrue(StringUtil.isBlank((String) null));
        assertTrue(StringUtil.isBlank(""));
        assertFalse(StringUtil.isBlank(" "));
        assertFalse(StringUtil.isBlank("x"));
    }

    @Test
    public void suplyZero() {
        assertEquals("005", StringUtil.suplyZero(5, 3));
        assertEquals("12345", StringUtil.suplyZero(12345, 3));
        assertEquals("0001", StringUtil.suplyZero(1L, 4));
    }

}
