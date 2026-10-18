package xc.crabapple.ssm.fs.mongodb;

import java.util.Base64;

/**
 * 兼容 cloudkd 旧数据的 Base64 工具。
 * 仅用于 Mongo 分块加密结果的编码/解码，等价于 JDK Base64。
 */
final class Base64Utils {

    private Base64Utils() {
    }

    static byte[] encode(final byte[] src) {
        if (src == null) {
            return null;
        }
        return Base64.getEncoder().encode(src);
    }

    static byte[] decode(final byte[] src) {
        if (src == null) {
            return null;
        }
        return Base64.getDecoder().decode(src);
    }
}
