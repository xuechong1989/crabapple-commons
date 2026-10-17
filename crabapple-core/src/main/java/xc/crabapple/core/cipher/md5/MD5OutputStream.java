package xc.crabapple.core.cipher.md5;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import xc.crabapple.core.log.LogUtil;

/**
 * 2017-03-17 1.0.0
 * MD5OutputStream is a subclass of FilterOutputStream adding MD5
 * hashing of the output.
 * <p/>
 * Originally written by Santeri Paavolainen, Helsinki Finland 1996 <br>
 * (c) Santeri Paavolainen, Helsinki Finland 1996 <br>
 * Some changes Copyright (c) 2002 Timothy W Macinta <br>
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * <p/>
 * See http://www.twmacinta.com/myjava/fast_md5.php for more information
 * on this file.
 * <p/>
 * Please note: I (Timothy Macinta) have put this code in the
 * com.twmacinta.util package only because it came without a package.  I
 * was not the the original author of the code, although I did
 * optimize it (substantially) and fix some bugs.
 **/

public final class MD5OutputStream extends FilterOutputStream {
    /**
     * MD5 context
     */
    private MD5 md5;

    /**
     * Creates MD5OutputStream
     *
     * @param out The output stream
     */

    public MD5OutputStream(final OutputStream out) {
        super(out);
        md5 = new MD5();
    }

    /**
     * Writes a byte.
     *
     * @see FilterOutputStream
     */
    @Override
    public void write(final int b) {
        try{
            out.write(b);
            md5.Update((byte) b);
        }catch(Throwable throwable){
            LogUtil.error("MD5OutputStream_write_t1", throwable);
        }
    }

    /**
     * Writes a sub array of bytes.
     *
     * @see FilterOutputStream
     */
    @Override
    public void write(final byte[] b, final int off, final int len) {
        try{
            out.write(b, off, len);
            md5.Update(b, off, len);
        }catch(Throwable throwable){
            LogUtil.error("MD5OutputStream_write_t2", throwable);
        }
    }

    /**
     * Returns array of bytes representing hash of the stream as finalized
     * for the current state.
     *
     * @see MD5#Final
     */

    public byte[] hash() {
        return md5.Final();
    }

    public MD5 getMD5() {
        return md5;
    }

}

