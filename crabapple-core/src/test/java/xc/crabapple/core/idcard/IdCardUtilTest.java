package xc.crabapple.core.idcard;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 2026-10-17
 * IdCardUtil 单元测试
 */
public class IdCardUtilTest {

    @Test
    public void create() {
        String idCard = IdCardUtil.create();
        assertEquals(18, idCard.length());
        assertTrue(IdCardUtil.check(idCard));
    }

    @Test
    public void createByCount() {
        List<String> list = IdCardUtil.create(3);
        assertEquals(3, list.size());
        for (String idCard : list) {
            assertTrue(IdCardUtil.check(idCard));
        }
        assertEquals(0, IdCardUtil.create(0).size());
        assertEquals(0, IdCardUtil.create(-1).size());
    }

    @Test
    public void check() {
        assertFalse(IdCardUtil.check(null));
        assertFalse(IdCardUtil.check(""));
        assertFalse(IdCardUtil.check("123"));
        assertFalse(IdCardUtil.check("123456789012345678"));
    }

}
