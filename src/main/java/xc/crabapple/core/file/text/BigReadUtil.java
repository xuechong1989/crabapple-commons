package xc.crabapple.core.file.text;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;
import xc.crabapple.core.log.LogUtil;

/**
 * 2018-02-11 1.0.0
 * 高效文件读取类
 */
public final class BigReadUtil extends RandomAccessFile {

    static final int LogBuffSz_ = 16; // 64K buffer
    public static final int BuffSz_ = (1 << LogBuffSz_);
    static final long BuffMask_ = ~(((long) BuffSz_) - 1L);
    private String path_;

    /*
     * This implementation is based on the buffer implementation in Modula-3's
     * "Rd", "Wr", "RdClass", and "WrClass" interfaces.
     */
    private boolean dirty_; // true iff unflushed bytes exist
    private boolean syncNeeded_; // dirty_ can be cleared by e.g. seek, so track sync separately
    private long curr_; // current position in file
    private long lo_, hi_; // bounds on characters in "buff"
    private byte[] buff_; // local buffer
    private long maxHi_; // this.lo + this.buff.length
    private boolean hitEOF_; // buffer contains last file block?
    private long diskPos_; // disk position

    /*
     * To describe the above fields, we introduce the following abstractions for
     * the file "f":
     *
     * len(f) the length of the file curr(f) the current position in the file
     * c(f) the abstract contents of the file disk(f) the contents of f's
     * backing disk file closed(f) true iff the file is closed
     *
     * "curr(f)" is an index in the closed interval [0, len(f)]. "c(f)" is a
     * character sequence of length "len(f)". "c(f)" and "disk(f)" may differ if
     * "c(f)" contains unflushed writes not reflected in "disk(f)". The flush
     * operation has the effect of making "disk(f)" identical to "c(f)".
     *
     * A file is said to be *valid* if the following conditions hold:
     *
     * V1. The "closed" and "curr" fields are correct:
     *
     * f.closed == closed(f) f.curr == curr(f)
     *
     * V2. The current position is either contained in the buffer, or just past
     * the buffer:
     *
     * f.lo <= f.curr <= f.hi
     *
     * V3. Any (possibly) unflushed characters are stored in "f.buff":
     *
     * (forall i in [f.lo, f.curr): c(f)[i] == f.buff[i - f.lo])
     *
     * V4. For all characters not covered by V3, c(f) and disk(f) agree:
     *
     * (forall i in [f.lo, len(f)): i not in [f.lo, f.curr) => c(f)[i] ==
     * disk(f)[i])
     *
     * V5. "f.dirty" is true iff the buffer contains bytes that should be
     * flushed to the file; by V3 and V4, only part of the buffer can be dirty.
     *
     * f.dirty == (exists i in [f.lo, f.curr): c(f)[i] != f.buff[i - f.lo])
     *
     * V6. this.maxHi == this.lo + this.buff.length
     *
     * Note that "f.buff" can be "null" in a valid file, since the range of
     * characters in V3 is empty when "f.lo == f.curr".
     *
     * A file is said to be *ready* if the buffer contains the current position,
     * i.e., when:
     *
     * R1. !f.closed && f.buff != null && f.lo <= f.curr && f.curr < f.hi
     *
     * When a file is ready, reading or writing a single byte can be performed
     * by reading or writing the in-memory buffer without performing a disk
     * operation.
     */

    /**
     * Open a new <code>BufferedRandomAccessFile</code> on <code>file</code>
     * in mode <code>mode</code>, which should be "r" for reading only, or
     * "rw" for reading and writing.
     */
    public BigReadUtil(final File file, final String mode) throws Throwable {
        this(file, mode, 0);
    }

    public BigReadUtil(final File file, final String mode, final int size) throws Throwable {
        super(file, mode);
        path_ = file.getAbsolutePath();
        this.init(size);
    }

    /**
     * Open a new <code>BufferedRandomAccessFile</code> on the file named
     * <code>name</code> in mode <code>mode</code>, which should be "r" for
     * reading only, or "rw" for reading and writing.
     */
    public BigReadUtil(final String name, final String mode) throws Throwable {
        this(name, mode, 0);
    }

    public BigReadUtil(final String name, final String mode, final int size) throws Throwable {
        super(name, mode);
        path_ = name;
        this.init(size);
    }

    private void init(final int size) {
        this.dirty_ = false;
        this.lo_ = this.curr_ = this.hi_ = 0;
        this.buff_ = (size > BuffSz_) ? new byte[size] : new byte[BuffSz_];
        this.maxHi_ = (long) BuffSz_;
        this.hitEOF_ = false;
        this.diskPos_ = 0L;
    }

    public String getPath() {
        return path_;
    }

    public void sync() throws Throwable {
        if (syncNeeded_) {
            flush();
            getChannel().force(true);
            syncNeeded_ = false;
        }
    }

    public void close() {
        try{
            this.flush();
            this.buff_ = null;
            super.close();
        }catch(Throwable throwable){
            LogUtil.error("BigReadUtil_close_t1", throwable);
        }
    }

    /**
     * Flush any bytes in the file's buffer that have not yet been written to
     * disk. If the file was created read-only, this method is a no-op.
     */
    public void flush() {
        try{
            this.flushBuffer();
        }catch(Throwable throwable){
            LogUtil.error("BigReadUtil_close_t1", throwable);
        }
    }

    /* Flush any dirty bytes in the buffer to disk. */
    private void flushBuffer() throws Throwable {
        if (this.dirty_) {
            if (this.diskPos_ != this.lo_){
                super.seek(this.lo_);
            }
            int len = (int) (this.curr_ - this.lo_);
            super.write(this.buff_, 0, len);
            this.diskPos_ = this.curr_;
            this.dirty_ = false;
        }
    }

    /*
     * Read at most "this.buff.length" bytes into "this.buff", returning the
     * number of bytes read. If the return result is less than
     * "this.buff.length", then EOF was read.
     */
    private int fillBuffer() throws Throwable {
        int cnt = 0;
        int rem = this.buff_.length;
        while (rem > 0) {
            int n = super.read(this.buff_, cnt, rem);
            if (n < 0)
                break;
            cnt += n;
            rem -= n;
        }
        if ((cnt < 0) && (this.hitEOF_ = (cnt < this.buff_.length))) {
            // make sure buffer that wasn't read is initialized with -1
            Arrays.fill(this.buff_, cnt, this.buff_.length, (byte) 0xff);
        }
        this.diskPos_ += cnt;
        return cnt;
    }

    /*
     * This method positions <code>this.curr</code> at position <code>pos</code>.
     * If <code>pos</code> does not fall in the current buffer, it flushes the
     * current buffer and loads the correct one.<p>
     *
     * On exit from this routine <code>this.curr == this.hi</code> iff <code>pos</code>
     * is at or past the end-of-file, which can only happen if the file was
     * opened in read-only mode.
     */
    public void seek(final long pos) {
        try{
            if (pos >= this.hi_ || pos < this.lo_) {
                // seeking outside of current buffer -- flush and read
                this.flushBuffer();
                this.lo_ = pos & BuffMask_; // start at BuffSz boundary
                this.maxHi_ = this.lo_ + (long) this.buff_.length;
                if (this.diskPos_ != this.lo_) {
                    super.seek(this.lo_);
                    this.diskPos_ = this.lo_;
                }
                int n = this.fillBuffer();
                this.hi_ = this.lo_ + (long) n;
            } else {
                // seeking inside current buffer -- no read required
                if (pos < this.curr_) {
                    // if seeking backwards, we must flush to maintain V4
                    this.flushBuffer();
                }
            }
            this.curr_ = pos;
        }catch(Throwable throwable){
            LogUtil.error("BigReadUtil_seek_t1", throwable);
        }
    }

    public long getFilePointer() {
        return this.curr_;
    }

    public long length() {
        try{
            return Math.max(this.curr_, super.length());
        }catch(Throwable throwable){
            LogUtil.error("BigReadUtil_length_t1", throwable, this.curr_);
            return 0;
        }
    }

    public int read() {
        if (this.curr_ >= this.hi_) {
            // test for EOF
            // if (this.hi < this.maxHi) return -1;
            if (this.hitEOF_)
                return -1;

            // slow path -- read another buffer
            try{
                this.seek(this.curr_);
            }catch(Throwable throwable){
                LogUtil.error("BigReadUtil_read_t1", throwable);
                return -1;
            }
            if (this.curr_ == this.hi_)
                return -1;
        }
        byte res = this.buff_[(int) (this.curr_ - this.lo_)];
        this.curr_++;
        return ((int) res) & 0xFF; // convert byte -> int
    }

    public int read(final byte[] b) {
        try{
            return this.read(b, 0, b.length);
        }catch(Throwable throwable){
            LogUtil.error("BigReadUtil_read_t1", throwable);
            return -1;
        }
    }

    public int read(final byte[] b, final int off, final int len) {
        if (this.curr_ >= this.hi_) {
            // test for EOF
            // if (this.hi < this.maxHi) return -1;
            if (this.hitEOF_)
                return -1;

            // slow path -- read another buffer
            this.seek(this.curr_);
            if (this.curr_ == this.hi_)
                return -1;
        }
        int lenLocal = Math.min(len, (int) (this.hi_ - this.curr_));
        int buffOff = (int) (this.curr_ - this.lo_);
        System.arraycopy(this.buff_, buffOff, b, off, lenLocal);
        this.curr_ += lenLocal;
        return lenLocal;
    }

    public void write(final int b) {
        if (this.curr_ >= this.hi_) {
            if (this.hitEOF_ && this.hi_ < this.maxHi_) {
                // at EOF -- bump "hi"
                this.hi_++;
            } else {
                // slow path -- write current buffer; read next one
                this.seek(this.curr_);
                if (this.curr_ == this.hi_) {
                    // appending to EOF -- bump "hi"
                    this.hi_++;
                }
            }
        }
        this.buff_[(int) (this.curr_ - this.lo_)] = (byte) b;
        this.curr_++;
        this.dirty_ = true;
        syncNeeded_ = true;
    }

    public void write(final byte[] b) {
        this.write(b, 0, b.length);
    }

    public void write(final byte[] b, final int off, final int len) {
        int offLocal = off;
        int lenLocal = len;
        while (lenLocal > 0) {
            int n = 0;
            try{
                n = this.writeAtMost(b, offLocal, lenLocal);
            }catch(Throwable throwable){
                LogUtil.error("BigReadUtil_read_t1", throwable);
                return;
            }
            offLocal += n;
            lenLocal -= n;
            this.dirty_ = true;
            syncNeeded_ = true;
        }
    }

    /*
     * Write at most "len" bytes to "b" starting at position "off", and return
     * the number of bytes written.
     */
    private int writeAtMost(final byte[] b, final int off, final int len) throws Throwable {
        if (this.curr_ >= this.hi_) {
            if (this.hitEOF_ && this.hi_ < this.maxHi_) {
                // at EOF -- bump "hi"
                this.hi_ = this.maxHi_;
            } else {
                // slow path -- write current buffer; read next one
                this.seek(this.curr_);
                if (this.curr_ == this.hi_) {
                    // appending to EOF -- bump "hi"
                    this.hi_ = this.maxHi_;
                }
            }
        }
        int lenLocal = Math.min(len, (int) (this.hi_ - this.curr_));
        int buffOff = (int) (this.curr_ - this.lo_);
        System.arraycopy(b, off, this.buff_, buffOff, lenLocal);
        this.curr_ += lenLocal;
        return lenLocal;
    }

}
