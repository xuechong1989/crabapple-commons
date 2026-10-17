package xc.crabapple.core.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 2026-10-17
 * DoubleUtil 单元测试
 */
public class DoubleUtilTest {

    @Test
    public void add() {
        assertEquals(0.3, DoubleUtil.add(0.1, 0.2));
        assertEquals(3.5, DoubleUtil.add(1.2, 2.3));
    }

    @Test
    public void sub() {
        assertEquals(1.4, DoubleUtil.sub(3.5, 2.1));
    }

    @Test
    public void mul() {
        assertEquals(7.0, DoubleUtil.mul(3.5, 2.0));
    }

    @Test
    public void div() {
        assertEquals(3.33, DoubleUtil.div(10, 3, 2));
        assertEquals(2.5, DoubleUtil.div(10L, 4L, 1));
        assertEquals(0.0, DoubleUtil.div(1.0, 3.0, -1));
    }

    @Test
    public void round() {
        assertEquals(3.142, DoubleUtil.round(3.1415926, 3));
        assertEquals(3.0, DoubleUtil.round(3.1415926, 0));
    }

}
