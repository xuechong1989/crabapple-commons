package xc.crabapple.ssm.fs.mongodb;

import xc.crabapple.core.random.RandomUtil;
import xc.crabapple.core.random.SnowflakeUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.timer.TimerUtil;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoInternalException;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * xuechong 2017-08-22
 * xuechong 2026-05-23 合并MongoB.java，全部static化
 * 基于mongodb_v3.4.6开发
 *
 * 共3个集合：info, file, chunk
 *
 * 集合info:每个document对象的属性
 * "_id":ObjectId("111")  //(ObjectId), 主键id
 * "size":12345  //(long), 集合chunk0的字节大小
 *
 * 集合file：每个document对象的属性
 * "_id":ObjectId("222")  // (ObjectId), 主键id
 * "hash":"123abc456def"  // (String), 32位md5值
 * "name":"aaa.txt"  // (String), 文件名
 * "size":123  // (long), 文件字节大小
 * "writedSize":654321  // (long), 已经写入的源文件字节大小
 * "updatetime":"2017-08-22 10:11:12"  // (String), 完全写入或更新时的时间
 * "chunkname":"chunk1"  // (String), 此文件在哪个chunk集合中，集合名称，从chunk0开始
 *
 * 集合chunk：每个document对象的属性
 * "_id":ObjectId("333")  // (ObjectId), 主键id
 * "fid":ObjectId("222")  // (ObjectId), 块所属的文件的id，即集合file的主键
 * "n":3  // (int), 第几块，从0开始
 * "content":byte[]  // (byte[]), 文件内容
 */
public final class MongoUtil {

    public static final int CHUNK_SIZE = 1024 * 1024 * 2;  //文件分块大小(2M)，系统统一采用该变量
    public static final long COLL_MAXSIZE = 1024L * 1024L * 1024L * 100L;  //100GB
    private static final int MONGO_RETRY_MAX = 3;  //MongoDB操作最大重试次数
    private static final long MONGO_RETRY_SLEEP_MS = 200L;  //MongoDB重试间隔毫秒
    private static final long MONGO_READ_TIMEOUT_MS = 120000L;  //多线程读取超时120秒
    private static final int FILE_LOCKS_WARN_SIZE = 10000;  //FILE_LOCKS超过此大小时告警，不强制清理避免破坏并发安全

    private static MongoDatabase p_database;
    private static MongoCollection<Document> p_collectionInfo;
    private static MongoCollection<Document> p_collectionFile;
    private static final String P_INFO = "info";  //第1个集合名
    private static final String P_FILE = "file";  //第2个集合名
    private static final String P_CHUNK = "chunk";  //第3个集合名，实际名称为chunk1...
    private static AutoCloseable p_client;  //MongoDB客户端引用，用于优雅关闭
    private static volatile boolean p_initialized = false;  //初始化完成标志
    private static final ConcurrentHashMap<String, Object> FILE_LOCKS = new ConcurrentHashMap<>();  //按文件hash分锁
    private static final Object INFO_LOCK = new Object();  //info集合操作锁
    private static final Set<String> INDEXED_COLLECTIONS = Collections.synchronizedSet(new HashSet<>());  //已创建索引的集合名缓存
    private static String p_encryptKey;  // 部署方配置，禁止硬编码

    public static String getEncryptKey() {
        if (p_encryptKey == null) {
            p_encryptKey = System.getProperty("crabapple.mongo.encrypt.key");
        }
        return p_encryptKey;
    }
    //
    private static final String FILE_UPLOADING = "file_uploading";  //文件上传中
    private static final String FILE_UPLOAD_SUCCESS = "file_upload_success";  //文件成功
    private static final String FILE_UPLOAD_FAST = "file_upload_fast";  //文件秒传
    private static final String FILE_UPLOAD_ERROR = "file_upload_error";  //文件上传异常，上传失败错误代码
    private static final String MONGO_NOT_INIT = "mongo_not_init";  //MongoDB未初始化

    /**
     * 使用系统属性初始化：
     * - crabapple.mongo.uri
     * - crabapple.mongo.database
     * - crabapple.mongo.encrypt.key
     */
    public synchronized static void init() {
        init(MongoConfig.fromSystemProperties());
    }

    /**
     * 使用显式配置初始化。只支持 mongodb-driver-sync 5.x。
     */
    public synchronized static void init(final MongoConfig config) {
        if (p_initialized) {
            LogUtil.print("MongoUtil_init_0", "MongoDB已初始化，跳过重复调用");
            return;
        }
        if (config == null) {
            throw new IllegalArgumentException("MongoConfig 不能为空");
        }
        p_encryptKey = config.getEncryptionKey();
        try {
            final com.mongodb.client.MongoClient client = MongoClients.create(config.getUri());
            p_client = client;
            p_database = client.getDatabase(config.getDatabase());
            p_collectionInfo = p_getCollection(P_INFO);
            FindIterable<Document> findIterable = p_collectionInfo.find();
            findIterable = findIterable.batchSize(1);
            findIterable = findIterable.maxTime(30L, TimeUnit.SECONDS);
            MongoCursor<Document> cursorinfo = null;
            try {
                cursorinfo = findIterable.iterator();
                if (!cursorinfo.hasNext()) {
                    Document docinfo = new Document();
                    docinfo.put("_id", new ObjectId());
                    docinfo.put("size", 0L);
                    p_collectionInfo.insertOne(docinfo);
                }
            } finally {
                IOUtil.close(cursorinfo);
            }
            p_collectionFile = p_getCollection(P_FILE);
            ThreadPoolsUtil.addPoolFix("mongoRead", 200);
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    MongoUtil.destroy();
                }
            }));
            p_initialized = true;
            LogUtil.print("MongoUtil_init_2", "MongoDB云盘存储启动成功", config.getDatabase());
        } catch (Throwable throwable) {
            closeClient();
            p_database = null;
            p_collectionInfo = null;
            p_collectionFile = null;
            p_client = null;
            p_initialized = false;
            LogUtil.error("MongoUtil_init_t3", throwable, "MongoDB连接失败", config.getDatabase());
            throw new IllegalStateException("MongoDB初始化失败", throwable);
        }
    }
    /**
     * xuechong 2026-05-24 优雅关闭MongoDB连接
     */
    private static void closeClient() {
        if (p_client != null) {
            try {
                p_client.close();
            } catch (Throwable throwable) {
                LogUtil.error("MongoUtil_closeClient_t1", throwable);
            }
        }
    }

    public synchronized static void destroy() {
        if (p_client != null) {
            try {
                p_client.close();
                LogUtil.print("MongoUtil_destroy_1", "MongoDB连接已关闭");
            } catch (Throwable throwable) {
                LogUtil.error("MongoUtil_destroy_t1", throwable);
            }
            p_client = null;
        }
        p_database = null;
        p_collectionInfo = null;
        p_collectionFile = null;
        p_initialized = false;
        FILE_LOCKS.clear();
        INDEXED_COLLECTIONS.clear();
        try {
            ThreadPoolsUtil.destroyPool("mongoRead");
        } catch (Throwable throwable) {
            LogUtil.error("MongoUtil_destroy_t2", throwable, "mongoRead线程池关闭失败");
        }
    }

    public static Map<String, Object> write(InputStream inputStream, String md5, Long fileSize, Long pos) {
        if (!ensureInitialized("MongoUtil_write_390")) {
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("type", "error");
            resultMap.put("code", MONGO_NOT_INIT);
            resultMap.put("status", "error");
            resultMap.put("id", "xxxxxx");
            resultMap.put("hash", md5);
            resultMap.put("fileSize", fileSize);
            resultMap.put("pos", pos);
            resultMap.put("location", "MongoUtil_write_390");
            resultMap.put("reason", "MongoDB未初始化");
            return resultMap;
        }
        long writeStart = System.currentTimeMillis();
        if (StringUtil.isBlank(md5)) {
            LogUtil.print("MongoUtil_write_397a", "md5参数为空", md5, fileSize, pos);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("type", "error");
            resultMap.put("code", FILE_UPLOAD_ERROR);
            resultMap.put("status", "error");
            resultMap.put("id", "xxxxxx");
            resultMap.put("hash", md5);
            resultMap.put("fileSize", fileSize);
            resultMap.put("pos", pos);
            resultMap.put("location", "MongoUtil_write_396a");
            resultMap.put("reason", "md5参数为空");
            return resultMap;
        }
        if (fileSize == null) {
            LogUtil.print("MongoUtil_write_397b", "fileSize参数为空", md5, fileSize, pos);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("type", "error");
            resultMap.put("code", FILE_UPLOAD_ERROR);
            resultMap.put("status", "error");
            resultMap.put("id", "xxxxxx");
            resultMap.put("hash", md5);
            resultMap.put("fileSize", fileSize);
            resultMap.put("pos", pos);
            resultMap.put("location", "MongoUtil_write_396b");
            resultMap.put("reason", "fileSize参数为空");
            return resultMap;
        }
        if (fileSize <= 0L) {
            LogUtil.print("MongoUtil_write_392", "fileSize必须大于0", md5, fileSize, pos);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("type", "error");
            resultMap.put("code", FILE_UPLOAD_ERROR);
            resultMap.put("status", "error");
            resultMap.put("id", "xxxxxx");
            resultMap.put("hash", md5);
            resultMap.put("fileSize", fileSize);
            resultMap.put("pos", pos);
            resultMap.put("location", "MongoUtil_write_392");
            resultMap.put("reason", "fileSize必须大于0");
            return resultMap;
        }
        if (pos == -1L) {
            try {
                Map<String, Object> existMap = exist(md5);
                if (!CollectionUtil.isBlank(existMap)) {  //文件存在
                    String id = existMap.get("id").toString();
                    String hash = existMap.get("hash").toString();
                    String status = existMap.get("status").toString();
                    if ("corrupted".equals(status)) {
                        Object sizeFromMap = existMap.get("size");
                        Object wsFromMap = existMap.get("writedSize");
                        LogUtil.print("MongoUtil_write_200c", "文件元数据异常writedSize>size", md5, fileSize, pos, sizeFromMap, wsFromMap);
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("type", "error");
                        resultMap.put("code", FILE_UPLOAD_ERROR);
                        resultMap.put("status", "corrupted");
                        resultMap.put("id", id);
                        resultMap.put("hash", md5);
                        resultMap.put("fileSize", fileSize);
                        resultMap.put("pos", pos);
                        resultMap.put("location", "MongoUtil_write_200d");
                        resultMap.put("reason", "文件元数据异常writedSize>size");
                        return resultMap;
                    }
                    if ("fileOK".equals(status)) {  //秒传
                        LogUtil.print("MongoUtil_write_200", "文件秒传成功", (System.currentTimeMillis() - writeStart) + "ms", md5, fileSize, pos);
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("type", "success");
                        resultMap.put("code", FILE_UPLOAD_FAST);
                        resultMap.put("status", status);
                        resultMap.put("id", id);
                        resultMap.put("hash", md5);
                        resultMap.put("fileSize", fileSize);
                        resultMap.put("pos", pos);
                        resultMap.put("uploadSize", fileSize+"");
                        resultMap.put("location", "MongoUtil_write_201");
                        return resultMap;
                    }
                    // 已上传一部分但未全部上传
                    String writedSize = existMap.get("writedSize").toString();
                    LogUtil.print("MongoUtil_write_202", "已上传一部分但未全部上传", (System.currentTimeMillis() - writeStart) + "ms", md5, fileSize, pos);
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("type", "success");
                    resultMap.put("code", FILE_UPLOADING);
                    resultMap.put("status", status);
                    resultMap.put("id", id);
                    resultMap.put("hash", md5);
                    resultMap.put("fileSize", fileSize);
                    resultMap.put("pos", pos);
                    resultMap.put("uploadSize", writedSize);
                    resultMap.put("location", "MongoUtil_write_203");
                    return resultMap;
                }
                //文件不存在，从头开始上传
                LogUtil.print("MongoUtil_write_204", "文件不存在，从头开始上传", (System.currentTimeMillis() - writeStart) + "ms", md5, fileSize, pos);
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("type", "success");
                resultMap.put("code", FILE_UPLOADING);
                resultMap.put("status", "nothing");
                resultMap.put("id", "xxxxxx");
                resultMap.put("hash", md5);
                resultMap.put("fileSize", fileSize);
                resultMap.put("pos", pos);
                resultMap.put("location", "MongoUtil_write_205");
                return resultMap;
            } catch (Throwable throwable) {
                LogUtil.error("MongoUtil_write_t200", throwable, "查询文件状态异常", md5, fileSize, pos);
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("type", "error");
                resultMap.put("code", FILE_UPLOAD_ERROR);
                resultMap.put("status", "error");
                resultMap.put("id", "xxxxxx");
                resultMap.put("hash", md5);
                resultMap.put("fileSize", fileSize);
                resultMap.put("pos", pos);
                resultMap.put("location", "MongoUtil_write_210");
                resultMap.put("reason", "查询文件状态异常");
                return resultMap;
            }
        }
        //
        if (pos < -1L) {
            LogUtil.print("MongoUtil_write_398", "pos参数非法", md5, fileSize, pos);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("type", "error");
            resultMap.put("code", FILE_UPLOAD_ERROR);
            resultMap.put("status", "error");
            resultMap.put("id", "xxxxxx");
            resultMap.put("hash", md5);
            resultMap.put("fileSize", fileSize);
            resultMap.put("pos", pos);
            resultMap.put("location", "MongoUtil_write_399");
            resultMap.put("reason", "pos参数非法");
            return resultMap;
        }
        if (pos > fileSize) {
            LogUtil.print("MongoUtil_write_402", "pos超过文件大小", md5, fileSize, pos);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("type", "error");
            resultMap.put("code", FILE_UPLOAD_ERROR);
            resultMap.put("status", "error");
            resultMap.put("id", "xxxxxx");
            resultMap.put("hash", md5);
            resultMap.put("fileSize", fileSize);
            resultMap.put("pos", pos);
            resultMap.put("location", "MongoUtil_write_403");
            resultMap.put("reason", "pos超过文件大小");
            return resultMap;
        }
        if (pos == fileSize && fileSize > 0L) {
            Map<String, Object> existMap = exist(md5);
            if (!CollectionUtil.isBlank(existMap)) {
                String existStatus = existMap.get("status").toString();
                if ("corrupted".equals(existStatus)) {
                    Object sizeFromExist = existMap.get("size");
                    Object wsFromExist = existMap.get("writedSize");
                    LogUtil.print("MongoUtil_write_407c", "pos==fileSize但writedSize>size", md5, fileSize, pos, sizeFromExist, wsFromExist);
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("type", "error");
                    resultMap.put("code", FILE_UPLOAD_ERROR);
                    resultMap.put("status", "corrupted");
                    resultMap.put("id", existMap.get("id").toString());
                    resultMap.put("hash", md5);
                    resultMap.put("fileSize", fileSize);
                    resultMap.put("pos", pos);
                    resultMap.put("location", "MongoUtil_write_407d");
                    resultMap.put("reason", "文件元数据异常writedSize>size");
                    return resultMap;
                }
                if ("fileOK".equals(existStatus)) {
                    LogUtil.print("MongoUtil_write_407", "文件已上传完成，无需重复上传", (System.currentTimeMillis() - writeStart) + "ms", md5, fileSize, pos);
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("type", "success");
                    resultMap.put("code", FILE_UPLOAD_FAST);
                    resultMap.put("status", "fileOK");
                    resultMap.put("id", existMap.get("id").toString());
                    resultMap.put("hash", md5);
                    resultMap.put("fileSize", fileSize);
                    resultMap.put("pos", pos);
                    resultMap.put("uploadSize", fileSize + "");
                    resultMap.put("location", "MongoUtil_write_408");
                    return resultMap;
                }
                Object wsForStatus = existMap.get("writedSize");
                LogUtil.print("MongoUtil_write_408a", "pos==fileSize但文件未完成", md5, fileSize, pos, existStatus, wsForStatus);
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("type", "success");
                resultMap.put("code", FILE_UPLOADING);
                resultMap.put("status", existStatus);
                resultMap.put("id", existMap.get("id").toString());
                resultMap.put("hash", md5);
                resultMap.put("fileSize", fileSize);
                resultMap.put("pos", pos);
                resultMap.put("uploadSize", existMap.get("writedSize") != null ? existMap.get("writedSize").toString() : "0");
                resultMap.put("location", "MongoUtil_write_408b");
                return resultMap;
            }
            LogUtil.print("MongoUtil_write_408c", "pos==fileSize但文件不存在，无法确认完成", md5, fileSize, pos);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("type", "error");
            resultMap.put("code", FILE_UPLOAD_ERROR);
            resultMap.put("status", "error");
            resultMap.put("id", "xxxxxx");
            resultMap.put("hash", md5);
            resultMap.put("fileSize", fileSize);
            resultMap.put("pos", pos);
            resultMap.put("location", "MongoUtil_write_408d");
            resultMap.put("reason", "文件不存在，无法确认完成");
            return resultMap;
        }
        if (pos > 0L) {
            if (pos % MongoUtil.CHUNK_SIZE != 0) {
                LogUtil.print("MongoUtil_write_400", "pos不是块大小的整数倍", md5, fileSize, pos);
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("type", "error");
                resultMap.put("code", FILE_UPLOAD_ERROR);
                resultMap.put("status", "nothing");
                resultMap.put("id", "xxxxxx");
                resultMap.put("hash", md5);
                resultMap.put("fileSize", fileSize);
                resultMap.put("pos", pos);
                resultMap.put("location", "MongoUtil_write_401");
                return resultMap;
            }
            Map<String, Object> existMap = exist(md5);
            if (CollectionUtil.isBlank(existMap)) {
                LogUtil.print("MongoUtil_write_404", "文件不存在，无法断点续传", md5, fileSize, pos);
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("type", "error");
                resultMap.put("code", FILE_UPLOAD_ERROR);
                resultMap.put("status", "nothing");
                resultMap.put("id", "xxxxxx");
                resultMap.put("hash", md5);
                resultMap.put("fileSize", fileSize);
                resultMap.put("pos", pos);
                resultMap.put("location", "MongoUtil_write_405");
                resultMap.put("reason", "文件不存在，无法断点续传");
                return resultMap;
            }
        }
        //
        warnFileLocksIfNeeded();
        Object fileLock = FILE_LOCKS.get(md5);
        if (fileLock == null) {
            fileLock = new Object();
            Object existing = FILE_LOCKS.putIfAbsent(md5, fileLock);
            if (existing != null) {
                fileLock = existing;
            }
        }
        byte[] preEncryptedChunk = null;
        int preReadSize = 0;
        final InputStream sourceStream = inputStream;
        if (pos >= 0L && pos < fileSize && sourceStream != null) {
            int writeWill = (int) Math.min(CHUNK_SIZE, fileSize - pos);
            byte[] buffer = new byte[writeWill];
            try {
                int actualRead = readFully(sourceStream, buffer, writeWill);
                if (actualRead == writeWill) {
                    preEncryptedChunk = AESUtil.encryptBytes(getEncryptKey(), buffer, AESUtil.KEY256);
                    if (preEncryptedChunk != null) {
                        preReadSize = writeWill;
                    }
                }
                inputStream = new java.io.ByteArrayInputStream(buffer, 0, actualRead);
            } catch (Throwable throwable) {
                LogUtil.error("MongoUtil_write_preEncrypt", throwable, md5, fileSize, pos);
                preEncryptedChunk = null;
                preReadSize = 0;
                inputStream = new java.io.ByteArrayInputStream(buffer);
            }
        }
        try {
            Map<String, Object> resultMap;
            synchronized (fileLock) {
                resultMap = p_write(md5, fileSize, pos, null, inputStream, preEncryptedChunk, preReadSize);
            }
            String status = (String) resultMap.get("status");
            if (status == null) {
                status = "error";
                resultMap.put("status", "error");
                resultMap.put("type", "error");
                resultMap.put("code", FILE_UPLOAD_ERROR);
            }
            if ("chunkOK".equals(status)) {
                Object writedSizeObj = resultMap.get("writedSize");
                String writedSize = writedSizeObj != null ? writedSizeObj.toString() : "0";
                long chunkIndex = pos / CHUNK_SIZE;
                LogUtil.print("MongoUtil_write_206", "文件块上传成功", "第" + chunkIndex + "块", "耗时" + (System.currentTimeMillis() - writeStart) + "ms", md5, fileSize, pos);
                resultMap.put("type", "success");
                resultMap.put("code", FILE_UPLOADING);
                resultMap.put("fileSize", fileSize);
                resultMap.put("pos", pos);
                resultMap.put("uploadSize", writedSize);
                resultMap.put("location", "MongoUtil_write_207");
                String chunkOK = (String) resultMap.get("chunkOK");
                if (chunkOK != null) {
                    resultMap.put("chunkOK", chunkOK);
                }
                return resultMap;
            }
            if ("fileOK".equals(status)) {
                Object writedSizeObj2 = resultMap.get("writedSize");
                String writedSize2 = writedSizeObj2 != null ? writedSizeObj2.toString() : "0";
                long fileTotalTime = System.currentTimeMillis() - writeStart;
                try {
                    String fidStr = (String) resultMap.get("id");
                    if (fidStr != null && fidStr.length() == 24) {
                        org.bson.types.ObjectId fileObjectId = new org.bson.types.ObjectId(fidStr);
                        long createTimeMs = fileObjectId.getTimestamp() * 1000L;
                        fileTotalTime = System.currentTimeMillis() - createTimeMs;
                    }
                } catch (Throwable throwable) {
                    LogUtil.error("MongoUtil_write_208_t1", throwable, "ObjectId解析失败使用当前块耗时", md5, fileSize, pos);
                }
                LogUtil.print("MongoUtil_write_208", "文件上传成功", "耗时" + fileTotalTime + "ms", md5, fileSize, pos);
                resultMap.put("type", "success");
                resultMap.put("code", FILE_UPLOAD_SUCCESS);
                resultMap.put("fileSize", fileSize);
                resultMap.put("pos", pos);
                resultMap.put("uploadSize", writedSize2);
                resultMap.put("fileOK", "fileOK");
                resultMap.put("location", "MongoUtil_write_209");
                return resultMap;
            }
            return resultMap;
        } catch (Throwable throwable) {
            LogUtil.error("MongoUtil_write_t410", throwable, md5, fileSize, pos);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("type", "error");
            resultMap.put("code", FILE_UPLOAD_ERROR);
            resultMap.put("status", "error");
            resultMap.put("id", "xxxxxx");
            resultMap.put("hash", md5);
            resultMap.put("fileSize", fileSize);
            resultMap.put("pos", pos);
            resultMap.put("location", "MongoUtil_write_406");
            return resultMap;
        } finally {
            FILE_LOCKS.remove(md5);
            IOUtil.close(inputStream);
        }
    }

    /**
     * xuechong 2026-05-29 同机部署：接收本地文件路径，自行读取分块加密入库
     * @return Map包含type("success"/"error"), code, id, hash等字段
     */
    public static Map<String, Object> writeByUrl(String fileUrl) {
        if (StringUtil.isBlank(fileUrl)) {
            LogUtil.print("MongoUtil_writeByUrl_1", "fileUrl为空");
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("type", "error");
            resultMap.put("code", FILE_UPLOAD_ERROR);
            resultMap.put("reason", "fileUrl为空");
            return resultMap;
        }
        File file = new File(fileUrl);
        if (!file.exists()) {
            LogUtil.print("MongoUtil_writeByUrl_2a", "文件不存在", fileUrl);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("type", "error");
            resultMap.put("code", FILE_UPLOAD_ERROR);
            resultMap.put("reason", "文件不存在");
            return resultMap;
        }
        if (!file.isFile()) {
            LogUtil.print("MongoUtil_writeByUrl_2b", "文件不是普通文件", fileUrl);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("type", "error");
            resultMap.put("code", FILE_UPLOAD_ERROR);
            resultMap.put("reason", "文件不是普通文件");
            return resultMap;
        }
        long fileSize = file.length();
        if (fileSize <= 0L) {
            LogUtil.print("MongoUtil_writeByUrl_3", "文件大小为0", fileUrl);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("type", "error");
            resultMap.put("code", FILE_UPLOAD_ERROR);
            resultMap.put("reason", "文件大小为0");
            return resultMap;
        }
        long writeByUrlStart = System.currentTimeMillis();
        String md5 = null;
        try {
            md5 = MD5Util.getFileMd5(file);
        } catch (Throwable throwable) {
            LogUtil.error("MongoUtil_writeByUrl_t1", throwable, "MD5计算失败", "耗时" + (System.currentTimeMillis() - writeByUrlStart) + "ms", fileUrl, fileSize);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("type", "error");
            resultMap.put("code", FILE_UPLOAD_ERROR);
            resultMap.put("reason", "MD5计算失败");
            return resultMap;
        }
        LogUtil.print("MongoUtil_writeByUrl_4", "本地文件上传准备", (System.currentTimeMillis() - writeByUrlStart) + "ms", fileUrl, md5, fileSize);
        Map<String, Object> probeResult = write((InputStream) null, md5, fileSize, -1L);
        String probeType = probeResult.get("type") != null ? probeResult.get("type").toString() : "error";
        if ("error".equals(probeType)) {
            LogUtil.error("MongoUtil_writeByUrl_5", "云盘探测失败", "耗时" + (System.currentTimeMillis() - writeByUrlStart) + "ms", fileUrl, md5, fileSize);
            return probeResult;
        }
        Object codeObj = probeResult.get("code");
        String code = codeObj != null ? codeObj.toString() : "";
        if (FILE_UPLOAD_FAST.equals(code)) {
            LogUtil.print("MongoUtil_writeByUrl_6", "文件秒传成功", (System.currentTimeMillis() - writeByUrlStart) + "ms", fileUrl, md5, fileSize);
            return probeResult;
        }
        long pos = 0L;
        Object uploadSizeObj = probeResult.get("uploadSize");
        if (uploadSizeObj != null) {
            try {
                pos = Long.valueOf(uploadSizeObj.toString());
            } catch (NumberFormatException nfe) {
                LogUtil.error("MongoUtil_writeByUrl_t2", nfe, "uploadSize解析失败", fileUrl, uploadSizeObj, md5, fileSize);
            }
        }
        long maxChunks = fileSize / CHUNK_SIZE + 2L;
        long chunkCount = 0L;
        String fileId = null;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            while (true) {
                chunkCount++;
                if (chunkCount > maxChunks) {
                    LogUtil.error("MongoUtil_writeByUrl_7", "分块数异常", "耗时" + (System.currentTimeMillis() - writeByUrlStart) + "ms", fileUrl, chunkCount, maxChunks, md5, fileSize);
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("type", "error");
                    resultMap.put("code", FILE_UPLOAD_ERROR);
                    resultMap.put("reason", "分块数异常");
                    return resultMap;
                }
                if (pos >= fileSize) {
                    break;
                }
                long surplus = fileSize - pos;
                int bufferLen = (int) Math.min(surplus, CHUNK_SIZE);
                byte[] buffer = new byte[bufferLen];
                raf.seek(pos);
                int readOffset = 0;
                int readRemain = bufferLen;
                while (readRemain > 0) {
                    int n = raf.read(buffer, readOffset, readRemain);
                    if (n < 0) {
                        break;
                    }
                    if (n == 0) {
                        try {
                            Thread.sleep(10L);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    }
                    readOffset += n;
                    readRemain -= n;
                }
                if (readOffset == 0) {
                    LogUtil.error("MongoUtil_writeByUrl_9", "文件块读取为空", "耗时" + (System.currentTimeMillis() - writeByUrlStart) + "ms", fileUrl, md5, fileSize, pos);
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("type", "error");
                    resultMap.put("code", FILE_UPLOAD_ERROR);
                    resultMap.put("reason", "文件块读取为空");
                    return resultMap;
                }
                long chunkIndex = pos / CHUNK_SIZE;
                String chunkName = md5 + "_" + chunkIndex;
                Map<String, Object> chunkResult = write(new java.io.ByteArrayInputStream(buffer), md5, fileSize, pos);
                String chunkType = chunkResult.get("type") != null ? chunkResult.get("type").toString() : "error";
                if ("error".equals(chunkType)) {
                    LogUtil.error("MongoUtil_writeByUrl_10", "块写入失败", fileUrl, md5, fileSize, pos);
                    return chunkResult;
                }
                Object posObj = chunkResult.get("uploadSize");
                if (posObj != null) {
                    try {
                        pos = Long.valueOf(posObj.toString());
                    } catch (NumberFormatException nfe) {
                        LogUtil.error("MongoUtil_writeByUrl_t3", nfe, "块uploadSize解析失败，用本地偏移量推算", fileUrl, posObj, md5, fileSize);
                        pos += bufferLen;
                    }
                } else {
                    pos += bufferLen;
                }
                Object chunkCodeObj = chunkResult.get("code");
                String chunkCode = chunkCodeObj != null ? chunkCodeObj.toString() : "";
                if (FILE_UPLOAD_FAST.equals(chunkCode) || FILE_UPLOAD_SUCCESS.equals(chunkCode)) {
                    Object idObj = chunkResult.get("id");
                    if (idObj != null) {
                        fileId = idObj.toString();
                    }
                    break;
                }
            }
        } catch (Throwable throwable) {
            LogUtil.error("MongoUtil_writeByUrl_t4", throwable, "分块上传异常", "耗时" + (System.currentTimeMillis() - writeByUrlStart) + "ms", fileUrl, md5, fileSize);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("type", "error");
            resultMap.put("code", FILE_UPLOAD_ERROR);
            resultMap.put("reason", "分块上传异常");
            return resultMap;
        } finally {
            IOUtil.close(raf);
        }
        if (fileId != null) {
            LogUtil.print("MongoUtil_writeByUrl_11", "本地文件上传成功", (System.currentTimeMillis() - writeByUrlStart) + "ms", fileUrl, md5, fileSize);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("type", "success");
            resultMap.put("code", FILE_UPLOAD_SUCCESS);
            resultMap.put("id", fileId);
            resultMap.put("hash", md5);
            resultMap.put("fileSize", fileSize);
            return resultMap;
        }
        LogUtil.error("MongoUtil_writeByUrl_12", "本地文件上传失败", "耗时" + (System.currentTimeMillis() - writeByUrlStart) + "ms", fileUrl, md5, fileSize, pos);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("type", "error");
        resultMap.put("code", FILE_UPLOAD_ERROR);
        resultMap.put("reason", "文件上传未完成");
        return resultMap;
    }

    /**
     * xuechong 2026-05-24 MongoDB写操作回调接口，用于重试
     */
    private interface MongoWriteCallback {
        Object execute() throws Throwable;
    }

    /**
     * xuechong 2026-05-26 判断索引创建是否因"索引已存在"或"数据重复"而失败(可接受，不重试)
     * 其他异常(如网络超时)需向上抛出让外层finally标记INDEXED_COLLECTIONS，下次重启再试
     */
    private static boolean isIndexError(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        if (throwable instanceof MongoCommandException) {
            int code = ((MongoCommandException) throwable).getErrorCode();
            if (code == 11000 || code == 85 || code == 86) {
                return true;
            }
        }
        if (throwable instanceof MongoWriteException) {
            if (((MongoWriteException) throwable).getCode() == 11000) {
                return true;
            }
        }
        String msg = throwable.getMessage();
        if (msg != null && (msg.contains("E11000") || msg.contains("duplicate key")
                || msg.contains("IndexAlreadyExists") || msg.contains("IndexKeySpecsConflict"))) {
            return true;
        }
        return isIndexError(throwable.getCause());
    }

    /**
     * xuechong 2026-05-29 判断是否为重复数据导致唯一索引创建失败（区别于"索引已存在"）
     * 返回true表示集合中存在重复键值，需要降级为非唯一索引
     */
    private static boolean isDuplicateKeyError(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        if (throwable instanceof MongoCommandException) {
            if (((MongoCommandException) throwable).getErrorCode() == 11000) {
                return true;
            }
        }
        if (throwable instanceof MongoWriteException) {
            if (((MongoWriteException) throwable).getCode() == 11000) {
                return true;
            }
        }
        String msg = throwable.getMessage();
        if (msg != null && (msg.contains("E11000") || msg.contains("duplicate key"))) {
            return true;
        }
        return isDuplicateKeyError(throwable.getCause());
    }

    /**
     * xuechong 2026-05-24 判断是否为MongoDB瞬时异常（网络抖动、选举切换等），仅瞬时异常值得重试
     */
    private static boolean isTransientMongoException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        if (throwable instanceof MongoSocketException
                || throwable instanceof MongoTimeoutException) {
            return true;
        }
        String className = throwable.getClass().getName();
        if (className.startsWith("com.mongodb.") || className.startsWith("com.mongodb.client.")) {
            if (className.indexOf("NotPrimary") != -1
                    || className.indexOf("NodeIsRecovering") != -1
                    || className.indexOf("ExecutionTimeout") != -1
                    || className.indexOf("CursorNotFound") != -1
                    || className.indexOf("WaitQueueFull") != -1) {
                return true;
            }
        }
        return isTransientMongoException(throwable.getCause());
    }

    /**
     * xuechong 2026-05-24 MongoDB写操作重试包装，仅对网络瞬时异常重试，确定性异常直接抛出
     */
    private static Object retryMongoWrite(MongoWriteCallback callback, String location, String md5) throws Throwable {
        Throwable lastThrowable = null;
        for (int i = 0; i < MONGO_RETRY_MAX; i++) {
            try {
                return callback.execute();
            } catch (Throwable throwable) {
                lastThrowable = throwable;
                if (!isTransientMongoException(throwable)) {
                    LogUtil.print(location, "MongoDB写操作遇到非瞬时异常，不重试", md5, throwable.getClass().getSimpleName());
                    throw throwable;
                }
                if (i < MONGO_RETRY_MAX - 1) {
                    LogUtil.print(location, "MongoDB写操作失败，重试", md5, i + 1);
                    try {
                        Thread.sleep(MONGO_RETRY_SLEEP_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ie;
                    }
                }
            }
        }
        throw lastThrowable;
    }

    /**
     * 写入，用于单个文件上传
     */
    private static Map<String, Object> p_write(final String md5, final Long fileSize, Long pos, String fidOld, InputStream inputStream, final byte[] preEncryptedChunk, final int preReadSize) {
        if (!ensureInitialized("MongoUtil_p_write_0")) {
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("status", "error");
            resultMap.put("id", "xxxxxx");
            resultMap.put("hash", md5);
            resultMap.put("size", fileSize);
            resultMap.put("reason", "MongoDB未初始化");
            return resultMap;
        }
        Bson bson = Filters.eq("hash", md5);
        FindIterable<Document> findIterable = p_collectionFile.find(bson);
        findIterable = findIterable.batchSize(1);
        final FindIterable<Document> finalFindIterable = findIterable;
        MongoCursor<Document> cursor = null;
        try {
            cursor = (MongoCursor<Document>) retryMongoWrite(new MongoWriteCallback() {
                @Override
                public Object execute() throws Throwable {
                    return finalFindIterable.iterator();
                }
            }, "MongoUtil_p_write_iterator", md5);
        } catch (Throwable throwable) {
            LogUtil.error("MongoUtil_p_write_t0", throwable, "文件查询失败", md5, fileSize, pos);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("status", "error");
            resultMap.put("id", "xxxxxx");
            resultMap.put("hash", md5);
            resultMap.put("size", fileSize);
            resultMap.put("reason", "文件查询失败");
            return resultMap;
        }
        try {
            if (cursor.hasNext()) {
                Document docfile = cursor.next();
                ObjectId fid = (ObjectId) docfile.get("_id");
                String fidStr24 = fid.toString();
                Object sizeObj = docfile.get("size");
                Object writedSizeObj = docfile.get("writedSize");
                //提前提取并校验chunkname
                final String chunkname = (String) docfile.get("chunkname");
                if (sizeObj == null || writedSizeObj == null) {
                    LogUtil.print("MongoUtil_write_0", "文件元数据缺失size/writedSize字段", md5, fileSize, pos, chunkname);
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("status", "error");
                    resultMap.put("id", "xxxxxx");
                    resultMap.put("hash", md5);
                    resultMap.put("size", fileSize);
                    resultMap.put("reason", "文件元数据字段缺失");
                    return resultMap;
                }
                long size;
                long writedSize;
                try {
                    size = Long.parseLong(sizeObj.toString());
                    writedSize = Long.parseLong(writedSizeObj.toString());
                } catch (Throwable throwable) {
                    LogUtil.error("MongoUtil_write_0a", throwable, "文件元数据size/writedSize解析失败", md5, fileSize, pos, sizeObj, writedSizeObj, chunkname);
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("status", "error");
                    resultMap.put("id", "xxxxxx");
                    resultMap.put("hash", md5);
                    resultMap.put("size", fileSize);
                    resultMap.put("reason", "文件元数据字段解析失败");
                    return resultMap;
                }
                if (!fileSize.equals(size)) {
                    LogUtil.print("MongoUtil_write_0a2", "客户端fileSize与已存文件size不一致", md5, fileSize, size, chunkname);
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("status", "error");
                    resultMap.put("id", "xxxxxx");
                    resultMap.put("hash", md5);
                    resultMap.put("size", fileSize);
                    resultMap.put("reason", "客户端fileSize与已存文件size不一致");
                    return resultMap;
                }
                if (writedSize > size) {
                    LogUtil.print("MongoUtil_write_0b", "文件元数据异常：writedSize大于size", md5, fileSize, pos, size, writedSize, chunkname);
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("status", "error");
                    resultMap.put("id", "xxxxxx");
                    resultMap.put("hash", md5);
                    resultMap.put("size", fileSize);
                    resultMap.put("writedSize", writedSize);
                    resultMap.put("reason", "文件元数据异常：writedSize大于size");
                    return resultMap;
                }
                if (pos > writedSize) {
                    LogUtil.print("MongoUtil_write_0b2", "pos超过writedSize，客户端状态不一致", md5, pos, writedSize, chunkname);
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("status", "error");
                    resultMap.put("id", fidStr24);
                    resultMap.put("hash", md5);
                    resultMap.put("size", fileSize);
                    resultMap.put("writedSize", writedSize);
                    resultMap.put("reason", "pos超过writedSize，客户端状态不一致");
                    return resultMap;
                }
                //秒传源文件
                if (size == writedSize) {
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("status", "fileOK");
                    resultMap.put("id", fidStr24);
                    resultMap.put("hash", md5);
                    resultMap.put("writedSize", writedSize);
                    resultMap.put("isFast", "yes");
                    return resultMap;
                }
                if (StringUtil.isBlank(chunkname)) {
                    LogUtil.print("MongoUtil_write_0c", "文件元数据缺失chunkname字段", md5, fileSize, pos, chunkname);
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("status", "error");
                    resultMap.put("id", fidStr24);
                    resultMap.put("hash", md5);
                    resultMap.put("size", fileSize);
                    resultMap.put("reason", "文件元数据缺失chunkname字段");
                    return resultMap;
                }
                //秒传块：验证chunk确实存在且内容完整
                if (writedSize > pos) {
                    int chunkIdx2 = (int) (pos / CHUNK_SIZE);
                    MongoCollection<Document> cc2 = p_getCollection(chunkname);
                    if (cc2 == null) {
                        LogUtil.print("MongoUtil_write_0d", "p_getCollection返回null", md5, fileSize, pos, chunkname);
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("status", "error");
                        resultMap.put("id", fidStr24);
                        resultMap.put("hash", md5);
                        resultMap.put("size", fileSize);
                        resultMap.put("writedSize", writedSize);
                        resultMap.put("reason", "chunk集合获取失败");
                        return resultMap;
                    }
                    Bson cFilter2 = Filters.and(Filters.eq("fid", fid), Filters.eq("n", chunkIdx2));
                    Document existingChunk = null;
                    try {
                        existingChunk = (Document) retryMongoWrite(new MongoWriteCallback() {
                            @Override
                            public Object execute() throws Throwable {
                                return cc2.find(cFilter2).limit(1).first();
                            }
                        }, "MongoUtil_p_write_chunkCheck", md5);
                    } catch (Throwable throwable) {
                        LogUtil.error("MongoUtil_write_0e0", throwable, "秒传块检查失败", md5, fileSize, pos, chunkIdx2, chunkname);
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("status", "error");
                        resultMap.put("id", fidStr24);
                        resultMap.put("hash", md5);
                        resultMap.put("size", fileSize);
                        resultMap.put("writedSize", writedSize);
                        resultMap.put("isFast", "no");
                        resultMap.put("reason", "秒传块检查失败");
                        return resultMap;
                    }
                    if (existingChunk != null) {
                        Object contentObj = existingChunk.get("content");
                        if (contentObj != null && contentObj instanceof org.bson.types.Binary) {
                            Map<String, Object> resultMap = new HashMap<>();
                            resultMap.put("status", "chunkOK");
                            resultMap.put("id", fidStr24);
                            resultMap.put("hash", md5);
                            resultMap.put("writedSize", writedSize);
                            resultMap.put("isFast", "no");
                            resultMap.put("chunkOK", "yes");
                            return resultMap;
                        }
                        LogUtil.print("MongoUtil_write_0e", "chunk存在但content字段无效，将重新写入", md5, fileSize, pos, chunkIdx2, chunkname);
                    }
                }
                //真正写入块
                int chunkIndex = (int) (pos / CHUNK_SIZE);
                long surplus = size - writedSize;  //源文件剩余未上传的字节大小
                int writeWill = (int) Math.min(CHUNK_SIZE, surplus);  //将要上传的字节大小
                //插入chunk
                Document docchunk = new Document();
                String cidStr24 = new ObjectId().toHexString();
                ObjectId chunkid = new ObjectId(cidStr24);
                docchunk.put("_id", chunkid);
                docchunk.put("fid", fid);
                docchunk.put("n", chunkIndex);
                byte[] bufferEncrypt;
                int actualRead;
                if (preEncryptedChunk != null && preReadSize == writeWill) {
                    bufferEncrypt = preEncryptedChunk;
                    actualRead = preReadSize;
                } else {
                    byte[] buffer = new byte[writeWill];
                    actualRead = 0;
                    try {
                        actualRead = readFully(inputStream, buffer, writeWill);
                    } catch (Throwable throwable) {
                        LogUtil.error("MongoUtil_write_1", throwable, md5, fileSize, pos, chunkname);
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("status", "error");
                        resultMap.put("id", fidStr24);
                        resultMap.put("hash", md5);
                        resultMap.put("size", fileSize);
                        resultMap.put("writedSize", writedSize);
                        resultMap.put("isFast", "no");
                        resultMap.put("reason", "文件流读取失败");
                        return resultMap;
                    }
                    if (actualRead < writeWill) {
                        LogUtil.print("MongoUtil_write_1a", "文件流提前结束", md5, fileSize, pos, writeWill, actualRead, chunkname);
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("status", "error");
                        resultMap.put("id", fidStr24);
                        resultMap.put("hash", md5);
                        resultMap.put("size", fileSize);
                        resultMap.put("writedSize", writedSize);
                        resultMap.put("isFast", "no");
                        resultMap.put("reason", "文件流读取不完整");
                        return resultMap;
                    }
                    bufferEncrypt = null;
                    try {
                        bufferEncrypt = AESUtil.encryptBytes(getEncryptKey(), buffer, AESUtil.KEY256);
                    } catch (Throwable throwable) {
                        LogUtil.error("MongoUtil_write_4", throwable, md5, fileSize, pos);
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("status", "error");
                        resultMap.put("id", fidStr24);
                        resultMap.put("hash", md5);
                        resultMap.put("size", fileSize);
                        resultMap.put("writedSize", writedSize);
                        resultMap.put("isFast", "no");
                        resultMap.put("reason", "加密算法错误");
                        return resultMap;
                    }
                    if (bufferEncrypt == null) {
                        LogUtil.print("MongoUtil_write_4a", "加密结果为null", md5, fileSize, pos, chunkname);
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("status", "error");
                        resultMap.put("id", fidStr24);
                        resultMap.put("hash", md5);
                        resultMap.put("size", fileSize);
                        resultMap.put("writedSize", writedSize);
                        resultMap.put("isFast", "no");
                        resultMap.put("reason", "加密算法返回null");
                        return resultMap;
                    }
                }
                docchunk.put("content", bufferEncrypt);
                MongoCollection<Document> collectionChunk = p_getCollection(chunkname);
                if (collectionChunk == null) {
                    LogUtil.print("MongoUtil_write_0f", "p_getCollection返回null", md5, fileSize, pos, chunkname, chunkname);
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("status", "error");
                    resultMap.put("id", fidStr24);
                    resultMap.put("hash", md5);
                    resultMap.put("size", fileSize);
                    resultMap.put("writedSize", writedSize);
                    resultMap.put("isFast", "no");
                    resultMap.put("reason", "chunk集合获取失败");
                    return resultMap;
                }
                Bson chunkFilter3 = Filters.and(Filters.eq("fid", fid), Filters.eq("n", chunkIndex));
                // upsert：$setOnInsert保护_id/fid/n不被覆盖，$set覆盖content以支持损坏chunk重写
                Document setOnInsertDoc = new Document();
                setOnInsertDoc.put("_id", chunkid);
                setOnInsertDoc.put("fid", fid);
                setOnInsertDoc.put("n", chunkIndex);
                Document setDoc = new Document();
                setDoc.put("content", bufferEncrypt);
                Document updateDoc = new Document();
                updateDoc.put("$setOnInsert", setOnInsertDoc);
                updateDoc.put("$set", setDoc);
                UpdateOptions updateOpts = new UpdateOptions();
                updateOpts.upsert(true);
                final boolean[] chunkNewlyInserted = {false};
                try {
                    UpdateResult ur = (UpdateResult) retryMongoWrite(new MongoWriteCallback() {
                        @Override
                        public Object execute() throws Throwable {
                            return collectionChunk.updateOne(chunkFilter3, updateDoc, updateOpts);
                        }
                    }, "MongoUtil_p_write_chunkUpsert", md5);
                    if (ur.getUpsertedId() != null) {
                        chunkNewlyInserted[0] = true;
                    }
                } catch (Throwable throwable) {
                    LogUtil.error("MongoUtil_p_write_chunkUpsert_t1", throwable, "chunk写入失败", md5, fileSize, pos, chunkname);
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("status", "error");
                    resultMap.put("id", fidStr24);
                    resultMap.put("hash", md5);
                    resultMap.put("size", fileSize);
                    resultMap.put("writedSize", writedSize);
                    resultMap.put("isFast", "no");
                    resultMap.put("reason", "chunk写入失败");
                    return resultMap;
                }
                //更新file
                long writedSizeOld = writedSize;
                writedSize += writeWill;
                docfile.put("writedSize", writedSize);
                String now = TimerUtil.date2str();
                docfile.put("updatetime", now);
                ObjectId objectId = (ObjectId) docfile.get("_id");
                bson = Filters.eq("_id", objectId);
                final Bson finalBson = bson;
                try {
                    retryMongoWrite(new MongoWriteCallback() {
                        @Override
                        public Object execute() throws Throwable {
                            p_collectionFile.replaceOne(finalBson, docfile);
                            return null;
                        }
                    }, "MongoUtil_p_write_replaceOne", md5);
                } catch (Throwable throwable) {
                    LogUtil.error("MongoUtil_write_5", throwable, "文件元数据更新失败", md5, fileSize, pos, chunkname);
                    if (chunkNewlyInserted[0]) {
                        try {
                            retryMongoWrite(new MongoWriteCallback() {
                                @Override
                                public Object execute() throws Throwable {
                                    return collectionChunk.deleteOne(Filters.eq("_id", chunkid));
                                }
                            }, "MongoUtil_p_write_rollbackDelete", md5);
                        } catch (Throwable t) {
                            LogUtil.error("MongoUtil_write_6", t, "回滚删除chunk失败，残留孤儿chunk需手动清理", md5, chunkid, chunkname);
                        }
                    }
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("status", "error");
                    resultMap.put("id", fidStr24);
                    resultMap.put("hash", md5);
                    resultMap.put("size", fileSize);
                    resultMap.put("writedSize", writedSizeOld);
                    resultMap.put("isFast", "no");
                    resultMap.put("reason", "文件元数据更新失败");
                    return resultMap;
                }
                //
                Map<String, Object> resultMap = new HashMap<>();
                if (surplus > CHUNK_SIZE) {  //不是源文件最后一个块
                    resultMap.put("status", "chunkOK");
                } else {  //源文件的最后一个块
                    p_updateSize(fileSize);
                    resultMap.put("status", "fileOK");
                }
                resultMap.put("id", fidStr24);
                resultMap.put("hash", md5);
                resultMap.put("writedSize", writedSize);
                resultMap.put("isFast", "no");
                resultMap.put("chunkOK", "no");
                return resultMap;
            }
        } finally {
            IOUtil.close(cursor);
        }
        if (pos > 0L) {
            LogUtil.print("MongoUtil_write_9", "文件在写入前被删除，无法继续", md5, fileSize, pos);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("status", "error");
            resultMap.put("id", "xxxxxx");
            resultMap.put("hash", md5);
            resultMap.put("size", fileSize);
            resultMap.put("reason", "文件不存在，无法断点续传");
            return resultMap;
        }
        //从头开始写入
        String fidStr24 = getWriteFid(fidOld);  //fileidString24
        ObjectId fid = new ObjectId(fidStr24);
        long writedSize = Math.min(fileSize, CHUNK_SIZE);  //将要上传的字节大小
        int writeWill = (int) writedSize;
        //插入chunk
        Document docchunk = new Document();
        String cidStr24 = new ObjectId().toHexString();  //chunkidString24
        ObjectId cid = new ObjectId(cidStr24);  //chunkid
        docchunk.put("_id", cid);
        docchunk.put("fid", fid);
        docchunk.put("n", 0);
        byte[] bufferEncrypt;
        int actualRead;
        if (preEncryptedChunk != null && preReadSize == writeWill) {
            bufferEncrypt = preEncryptedChunk;
            actualRead = preReadSize;
        } else {
            byte[] buffer = new byte[writeWill];
            actualRead = 0;
            try {
                actualRead = readFully(inputStream, buffer, writeWill);
            } catch (Throwable throwable) {
                LogUtil.error("MongoUtil_write_2", throwable, md5, fileSize, pos);
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("status", "error");
                resultMap.put("id", fidStr24);
                resultMap.put("hash", md5);
                resultMap.put("size", fileSize);
                resultMap.put("writedSize", writedSize);
                resultMap.put("isFast", "no");
                resultMap.put("reason", "文件流读取失败");
                return resultMap;
            }
            if (actualRead < writeWill) {
                LogUtil.print("MongoUtil_write_2a", "文件流提前结束", md5, fileSize, pos, writeWill, actualRead);
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("status", "error");
                resultMap.put("id", fidStr24);
                resultMap.put("hash", md5);
                resultMap.put("size", fileSize);
                resultMap.put("writedSize", writedSize);
                resultMap.put("isFast", "no");
                resultMap.put("reason", "文件流读取不完整");
                return resultMap;
            }
            bufferEncrypt = null;
            try {
                bufferEncrypt = AESUtil.encryptBytes(getEncryptKey(), buffer, AESUtil.KEY256);
            } catch (Throwable throwable) {
                LogUtil.error("MongoUtil_write_3", throwable, md5, fileSize, pos);
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("status", "error");
                resultMap.put("id", fidStr24);
                resultMap.put("hash", md5);
                resultMap.put("size", fileSize);
                resultMap.put("writedSize", writedSize);
                resultMap.put("isFast", "no");
                resultMap.put("reason", "加密算法错误");
                return resultMap;
            }
            if (bufferEncrypt == null) {
                LogUtil.print("MongoUtil_write_3a", "加密结果为null", md5, fileSize, pos);
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("status", "error");
                resultMap.put("id", fidStr24);
                resultMap.put("hash", md5);
                resultMap.put("size", fileSize);
                resultMap.put("writedSize", writedSize);
                resultMap.put("isFast", "no");
                resultMap.put("reason", "加密算法返回null");
                return resultMap;
            }
        }
        docchunk.put("content", bufferEncrypt);
        String chunkname = getCollectionChunk(fileSize);
        MongoCollection<Document> collectionChunk = p_getCollection(chunkname);
        if (collectionChunk == null) {
            LogUtil.print("MongoUtil_write_0g", "p_getCollection返回null", md5, fileSize, pos, chunkname);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("status", "error");
            resultMap.put("id", fidStr24);
            resultMap.put("hash", md5);
            resultMap.put("size", fileSize);
            resultMap.put("writedSize", writedSize);
            resultMap.put("isFast", "no");
            resultMap.put("reason", "chunk集合获取失败");
            return resultMap;
        }
        Bson chunkFilter4 = Filters.and(Filters.eq("fid", fid), Filters.eq("n", 0));
        // upsert：$setOnInsert保护_id/fid/n不被覆盖，$set覆盖content以支持损坏chunk重写
        Document setOnInsertDocNew = new Document();
        setOnInsertDocNew.put("_id", cid);
        setOnInsertDocNew.put("fid", fid);
        setOnInsertDocNew.put("n", 0);
        Document setDocNew = new Document();
        setDocNew.put("content", bufferEncrypt);
        Document updateDocNew = new Document();
        updateDocNew.put("$setOnInsert", setOnInsertDocNew);
        updateDocNew.put("$set", setDocNew);
        UpdateOptions updateOptsNew = new UpdateOptions();
        updateOptsNew.upsert(true);
        final boolean[] chunkNewCreated = {false};
        try {
            UpdateResult ur = (UpdateResult) retryMongoWrite(new MongoWriteCallback() {
                @Override
                public Object execute() throws Throwable {
                    return collectionChunk.updateOne(chunkFilter4, updateDocNew, updateOptsNew);
                }
            }, "MongoUtil_p_write_chunkUpsertNew", md5);
            if (ur.getUpsertedId() != null) {
                chunkNewCreated[0] = true;
            }
        } catch (Throwable throwable) {
            LogUtil.error("MongoUtil_p_write_chunkUpsertNew_t1", throwable, "chunk写入失败", md5, fileSize, pos);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("status", "error");
            resultMap.put("id", fidStr24);
            resultMap.put("hash", md5);
            resultMap.put("size", fileSize);
            resultMap.put("writedSize", writedSize);
            resultMap.put("isFast", "no");
            resultMap.put("reason", "chunk写入失败");
            return resultMap;
        }
        //插入file
        Document docfile = new Document();
        docfile.put("_id", fid);
        docfile.put("hash", md5);
        docfile.put("size", fileSize);
        docfile.put("writedSize", writedSize);
        docfile.put("chunkname", chunkname);
        String now = TimerUtil.date2str();
        docfile.put("updatetime", now);
        try {
            retryMongoWrite(new MongoWriteCallback() {
                @Override
                public Object execute() throws Throwable {
                    p_collectionFile.insertOne(docfile);
                    return null;
                }
            }, "MongoUtil_p_write_insertFile", md5);
        } catch (Throwable throwable) {
            LogUtil.error("MongoUtil_write_7", throwable, "文件元数据插入失败", md5, fileSize, pos);
            if (chunkNewCreated[0]) {
                try {
                    retryMongoWrite(new MongoWriteCallback() {
                        @Override
                        public Object execute() throws Throwable {
                            return collectionChunk.deleteOne(Filters.eq("_id", cid));
                        }
                    }, "MongoUtil_p_write_rollbackDeleteNew", md5);
                } catch (Throwable t) {
                    LogUtil.error("MongoUtil_write_8", t, "回滚删除chunk失败，残留孤儿chunk需手动清理", md5, cid, chunkname);
                }
            }
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("status", "error");
            resultMap.put("id", fidStr24);
            resultMap.put("hash", md5);
            resultMap.put("size", fileSize);
            resultMap.put("writedSize", writedSize);
            resultMap.put("isFast", "no");
            resultMap.put("reason", "文件元数据插入失败");
            return resultMap;
        }
        //
        Map<String, Object> resultMap = new HashMap<>();
        if (fileSize > CHUNK_SIZE) {  //原文件上传未完成
            resultMap.put("status", "chunkOK");
        } else {  //原文件上传完成
            p_updateSize(fileSize);
            resultMap.put("status", "fileOK");
        }
        resultMap.put("id", fidStr24);
        resultMap.put("hash", md5);
        resultMap.put("writedSize", writedSize);
        resultMap.put("isFast", "no");
        return resultMap;
    }

    public static Map<String, Object> readById(String fid, OutputStream out) {
        if (StringUtil.isBlank(fid) || fid.length() != 24) {
            LogUtil.print("MongoUtil_readById_1", "fid格式不正确，需要24位十六进制", fid);
            Map<String, Object> map = new HashMap<>(2);
            map.put("exist", false);
            map.put("error", "invalid_fid_format");
            return map;
        }
        Map<String, Object> map = read("fid", fid, 0L, out);
        return map;
    }

    /**
     * xuechong 2026-05-29 下载前轻量验证：仅查询文件元数据，不读取chunk
     * @return {exist:true, size:xxx, chunkname:"chunk0", ...} 或 {exist:false, error:"..."}
     */
    public static Map<String, Object> verifyForDownload(String hash) {
        long verifyStart = System.currentTimeMillis();
        if (StringUtil.isBlank(hash) || hash.length() != 32) {
            Map<String, Object> map = new HashMap<>(2);
            map.put("exist", false);
            map.put("error", "invalid_hash_format");
            return map;
        }
        if (!ensureInitialized("MongoUtil_verifyForDownload_0")) {
            Map<String, Object> map = new HashMap<>(2);
            map.put("exist", false);
            map.put("error", "MONGO_NOT_INITialized");
            return map;
        }
        Map<String, Object> fileMap = findByHash(hash);
        if (fileMap.isEmpty()) {
            Map<String, Object> map = new HashMap<>(2);
            map.put("exist", false);
            map.put("error", "file_not_found");
            return map;
        }
        if (fileMap.containsKey("corrupted")) {
            Map<String, Object> map = new HashMap<>(2);
            map.put("exist", false);
            map.put("error", "file_corrupted");
            return map;
        }
        Object sizeObj = fileMap.get("size");
        Object writedSizeObj = fileMap.get("writedSize");
        if (sizeObj == null || writedSizeObj == null) {
            Map<String, Object> map = new HashMap<>(2);
            map.put("exist", false);
            map.put("error", "file_metadata_missing");
            return map;
        }
        long size;
        long writedSize;
        try {
            size = Long.parseLong(sizeObj.toString());
            writedSize = Long.parseLong(writedSizeObj.toString());
        } catch (Throwable throwable) {
            Map<String, Object> map = new HashMap<>(2);
            map.put("exist", false);
            map.put("error", "file_metadata_parse_error");
            return map;
        }
        if (writedSize != size) {
            Map<String, Object> map = new HashMap<>(2);
            map.put("exist", false);
            map.put("error", "upload_incomplete");
            map.put("size", size);
            map.put("writedSize", writedSize);
            return map;
        }
        Map<String, Object> map = new HashMap<>(6);
        map.put("exist", true);
        map.put("size", size);
        map.put("chunkname", fileMap.get("chunkname"));
        map.put("updatetime", fileMap.get("updatetime"));
        map.put("_id", fileMap.get("_id"));
        LogUtil.print("MongoUtil_verifyForDownload_1", "文件验证通过，可下载", "耗时" + (System.currentTimeMillis() - verifyStart) + "ms", hash, size);
        return map;
    }

    public static Map<String, Object> readByHash(String hash, OutputStream out) {
        if (StringUtil.isBlank(hash) || hash.length() != 32) {
            LogUtil.print("MongoUtil_readByHash_1", "hash格式不正确，需要32位十六进制", hash);
            Map<String, Object> map = new HashMap<>(2);
            map.put("exist", false);
            map.put("error", "invalid_hash_format");
            return map;
        }
        Map<String, Object> map = read("hash", hash, 0L, out);
        return map;
    }

    /**
     * xuechong 20180517
     * 多线程读取
     */
    private static Map<String, Object> read(String conditionType, String conditionValue, Long pos, OutputStream out) {
        LogUtil.print("MongoUtil_read_1001", "方法入口：准备读取文件", conditionType, conditionValue);
        long readStart = System.currentTimeMillis();
        if (out == null) {
            LogUtil.print("MongoUtil_read_1000b", "OutputStream为null，无法输出文件内容", conditionType, conditionValue);
            Map<String, Object> map = new HashMap<>(2);
            map.put("exist", false);
            map.put("error", "outputstream_null");
            return map;
        }
        if (!ensureInitialized("MongoUtil_read_1000a")) {
            Map<String, Object> map = new HashMap<>(2);
            map.put("exist", false);
            map.put("error", "MONGO_NOT_INITialized");
            return map;
        }
        if (pos == null) {
            pos = 0L;
        }
        Map<String, Object> map = new HashMap<>(2);
        Bson bson = null;
        if ("fid".equals(conditionType)) {
            ObjectId objectId = new ObjectId(conditionValue);
            bson = Filters.eq("_id", objectId);
        } else if ("hash".equals(conditionType)) {
            bson = Filters.eq("hash", conditionValue);
        } else {
            LogUtil.print("MongoUtil_read_1000", "未知conditionType", conditionType, conditionValue);
            map.put("exist", false);
            map.put("error", "unknown_condition_type");
            return map;
        }
        FindIterable<Document> findIterable = p_collectionFile.find(bson);
        findIterable = findIterable.batchSize(1);
        MongoCursor<Document> cursorFile = null;
        int retryFile = 0;
        while (retryFile < MONGO_RETRY_MAX) {
            try {
                cursorFile = findIterable.iterator();
                break;
            } catch (Throwable throwable) {
                if (throwable instanceof MongoInternalException) {
                    String msg = throwable.toString();
                    if (!StringUtil.isBlank(msg) && msg.indexOf("Unexpected exception") != -1) {
                        LogUtil.error("MongoUtil_read_1002", throwable, "读取完成，程序异常", conditionType, conditionValue, msg + "\n");
                        map.put("exist", false);
                        map.put("error", "cursor_file_mongo_internal_error");
                        return map;
                    }
                }
                if (!isTransientMongoException(throwable)) {
                    LogUtil.error("MongoUtil_read_1002a", throwable, "文件游标创建遇到非瞬时异常，不重试", conditionType, conditionValue);
                    map.put("exist", false);
                    map.put("error", "cursor_file_non_transient");
                    return map;
                }
                retryFile++;
                if (retryFile >= MONGO_RETRY_MAX) {
                    LogUtil.error("MongoUtil_read_1003", throwable, "文件游标创建失败，已重试" + MONGO_RETRY_MAX + "次", "耗时" + (System.currentTimeMillis() - readStart) + "ms", conditionType, conditionValue);
                    map.put("exist", false);
                    map.put("error", "cursor_file_retry_exhausted");
                    return map;
                }
                LogUtil.print("MongoUtil_read_1003a", "文件游标创建失败，重试", conditionType, conditionValue, retryFile);
                try {
                    Thread.sleep(MONGO_RETRY_SLEEP_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    map.put("exist", false);
                    map.put("error", "cursor_file_interrupted");
                    return map;
                }
            }
        }
        Document docfile = null;
        try {
            if (!cursorFile.hasNext()) {
                LogUtil.print("MongoUtil_read_1004", "读取完成，文件不存在", "耗时" + (System.currentTimeMillis() - readStart) + "ms", conditionType, conditionValue + "\n");
                map.put("exist", false);
                return map;
            }
            // 文件存在
            map.put("exist", true);
            docfile = cursorFile.next();
        } catch (Throwable throwable) {
            LogUtil.error("MongoUtil_read_1004a", throwable, "读取文件元数据时游标异常", conditionType, conditionValue);
            map.put("exist", false);
            map.put("error", "cursor_file_read_error");
            return map;
        } finally {
            cursorFile.close();
        }
        final ObjectId objectId = (ObjectId) docfile.get("_id");
        final String chunkname = (String) docfile.get("chunkname");
        final Object sizeObj = docfile.get("size");
        if (StringUtil.isBlank(chunkname) || sizeObj == null) {
            LogUtil.print("MongoUtil_read_1005", "文件元数据缺失chunkname/size字段", conditionType, conditionValue);
            map.put("exist", false);
            map.put("error", "file_metadata_missing");
            return map;
        }
        final long size = Long.parseLong(sizeObj.toString());
        final String hashFromDb = (String) docfile.get("hash");
        Object writedSizeObj2 = docfile.get("writedSize");
        long writedSizeFromDb = 0L;
        if (writedSizeObj2 != null) {
            try {
                writedSizeFromDb = Long.parseLong(writedSizeObj2.toString());
            } catch (Throwable throwable) {
                LogUtil.error("MongoUtil_read_1005a", throwable, "writedSize解析失败", conditionType, conditionValue);
                map.put("uploadIncomplete", true);
            }
        }
        if (writedSizeFromDb != size) {
            LogUtil.print("MongoUtil_read_1006a", "文件上传未完成，拒绝下载", "耗时" + (System.currentTimeMillis() - readStart) + "ms", conditionType, conditionValue, size, writedSizeFromDb);
            map.put("exist", false);
            map.put("error", "upload_incomplete");
            return map;
        }
        LogUtil.print("MongoUtil_read_1006", "文件存在，开始读取", conditionType, conditionValue, objectId, chunkname, size);
        MessageDigest md5Digest = null;
        if (pos == 0L) {
            try {
                md5Digest = MD5Util.getDigest();
            } catch (Throwable throwable) {
                LogUtil.error("MongoUtil_read_1007", throwable, "MD5算法不可用，跳过校验", conditionType, conditionValue);
            }
        }
        MongoCollection<Document> collectionChunk = p_getCollection(chunkname);
        if (collectionChunk == null) {
            LogUtil.print("MongoUtil_read_1100", "p_getCollection返回null", chunkname);
            map.put("exist", false);
            map.put("error", "chunk_collection_null");
            return map;
        }
        Bson bson2 = Filters.eq("fid", objectId);
        FindIterable<Document> findIterable2 = collectionChunk.find(bson2);
        findIterable2 = findIterable2.sort(Sorts.ascending("n"));
        findIterable2 = findIterable2.batchSize(500);
        MongoCursor<Document> cursorChunk = null;
        int retryChunk = 0;
        while (retryChunk < MONGO_RETRY_MAX) {
            try {
                cursorChunk = findIterable2.iterator();
                break;
            } catch (Throwable throwable) {
                if (throwable instanceof MongoInternalException) {
                    String msg = throwable.toString();
                    if (!StringUtil.isBlank(msg) && msg.indexOf("Unexpected exception") != -1) {
                        map.put("exist", false);
                        map.put("error", "cursor_chunk_mongo_internal_error");
                        return map;
                    }
                }
                if (!isTransientMongoException(throwable)) {
                    LogUtil.error("MongoUtil_read_104b", throwable, "chunk游标创建遇到非瞬时异常，不重试", conditionType, conditionValue);
                    map.put("exist", false);
                    map.put("error", "cursor_chunk_non_transient");
                    return map;
                }
                retryChunk++;
                if (retryChunk >= MONGO_RETRY_MAX) {
                    LogUtil.error("MongoUtil_read_104", throwable, "chunk游标创建失败，已重试" + MONGO_RETRY_MAX + "次", conditionType, conditionValue, pos);
                    map.put("exist", false);
                    map.put("error", "cursor_chunk_retry_exhausted");
                    return map;
                }
                LogUtil.print("MongoUtil_read_104a", "chunk游标创建失败，重试", conditionType, conditionValue, retryChunk);
                try {
                    Thread.sleep(MONGO_RETRY_SLEEP_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    map.put("exist", false);
                    map.put("error", "cursor_chunk_interrupted");
                    return map;
                }
            }
        }
        //
        int threadNum = 8;  //线程数（峰值内存=threadNum×2MB加密数据+threadNum×2MB解密数据≈32MB）
        long idleNum = ThreadPoolsUtil.idleThreadNum("mongoRead");
        int chunkNum = ((int) (size / CHUNK_SIZE)) + (size % CHUNK_SIZE == 0 ? 0 : 1);  //多少个文件块
        int chunkIndex = getIndexByPos(pos);  //块索引，从0开始
        int loopIndex = 0;  //循环索引，从0开始
        String groupName = null;
        if (idleNum > 0) {
            threadNum = Math.min(threadNum, (int) idleNum);
            Long id = SnowflakeUtil.getLong();
            groupName = Long.toString(id);
        }
        boolean brokenPipe = false;
        boolean decryptError = false;
        try {
            while (cursorChunk.hasNext()) {
                if (brokenPipe || decryptError) {
                    break;
                }
                Document chunk = cursorChunk.next();
                loopIndex++;  //从1开始
                if (loopIndex > chunkNum) {
                    LogUtil.print("MongoUtil_read_134", "chunk数量超过预期，忽略多余chunk", conditionType, conditionValue, loopIndex, chunkNum);
                    break;
                }
                if (chunkIndex > loopIndex - 1) {
                    continue;
                }
                MongoReadRunnable myRunnable = new MongoReadRunnable(loopIndex - 1, chunk);
                if (idleNum > 0) {  // idleNum > 0
                    ThreadPoolsUtil.addRunnable("mongoRead", groupName, myRunnable);
                    if (loopIndex % threadNum == 0 || loopIndex == chunkNum) {
                        List<YczqRunnable> blackList = ThreadPoolsUtil.blockGroup("mongoRead", groupName, false, MONGO_READ_TIMEOUT_MS);
                        if (blackList != null && !blackList.isEmpty()) {
                            StringBuilder sbTimeoutChunks = new StringBuilder();
                            for (YczqRunnable r : blackList) {
                                if (r instanceof MongoReadRunnable) {
                                    sbTimeoutChunks.append(((MongoReadRunnable) r).getCount()).append(",");
                                }
                            }
                            String timeoutChunks = sbTimeoutChunks.length() > 0 ? sbTimeoutChunks.substring(0, sbTimeoutChunks.length() - 1) : "unknown";
                            LogUtil.print("MongoUtil_read_135", "多线程读取超时，超时chunk索引=[" + timeoutChunks + "]，中止下载", "耗时" + (System.currentTimeMillis() - readStart) + "ms", conditionType, conditionValue, blackList.size(), chunkname);
                            decryptError = true;
                            break;
                        }
                        List<YczqRunnable> groupList = ThreadPoolsUtil.getGroupList("mongoRead", groupName);
                        if (groupList == null) {
                            groupList = new ArrayList<YczqRunnable>(0);
                        }
                        try {
                            for (YczqRunnable yczqRunnable : groupList) {
                                if (!(yczqRunnable instanceof MongoReadRunnable)) {
                                    LogUtil.print("MongoUtil_read_130", "线程池返回非MongoReadRunnable实例，跳过", yczqRunnable.getClass().getName());
                                    continue;
                                }
                                MongoReadRunnable runnable = (MongoReadRunnable) yczqRunnable;
                                if (runnable.isError()) {
                                    LogUtil.print("MongoUtil_read_132", "块解密失败，中止下载", conditionType, conditionValue, runnable.getCount());
                                    decryptError = true;
                                    break;
                                }
                                byte[] bytes = runnable.getBuffer();
                                if (bytes == null || bytes.length <= 0) {
                                    LogUtil.print("MongoUtil_read_131a", "块解密结果为空，中止下载", conditionType, conditionValue, runnable.getCount());
                                    decryptError = true;
                                    break;
                                }
                                try {
                                    out.write(bytes, 0, bytes.length);
                                    MD5Util.update(md5Digest, bytes, 0, bytes.length);
                                } catch (Throwable throwable) {
                                    final String msg = throwable.getMessage();
                                    if (!StringUtil.isBlank(msg) && msg.contains("Broken pipe")) {
                                        LogUtil.print("MongoUtil_read_103", "客户端断开连接，终止下载", conditionType, conditionValue);
                                        brokenPipe = true;
                                    } else {
                                        decryptError = true;
                                        LogUtil.error("MongoUtil_read_131", throwable, conditionType, conditionValue, pos, idleNum, loopIndex, chunkNum, threadNum, objectId, chunkname, size);
                                    }
                                    break;
                                }
                            }
                            // 每批次结束flush一次，而非每块flush
                            if (!brokenPipe && !decryptError) {
                                try {
                                    out.flush();
                                } catch (Throwable throwable) {
                                    final String msg = throwable.getMessage();
                                    if (!StringUtil.isBlank(msg) && msg.contains("Broken pipe")) {
                                        LogUtil.print("MongoUtil_read_103", "客户端断开连接，终止下载", conditionType, conditionValue);
                                        brokenPipe = true;
                                    } else {
                                        decryptError = true;
                                        LogUtil.error("MongoUtil_read_131b", throwable, conditionType, conditionValue, pos, idleNum, loopIndex, chunkNum, threadNum, objectId, chunkname, size);
                                    }
                                }
                            }
                        } catch (Throwable throwable) {
                            LogUtil.error("MongoUtil_read_101", throwable, conditionType, conditionValue, pos, idleNum, loopIndex, chunkNum, threadNum, objectId, chunkname, size);
                            map.put("exist", false);
                            map.put("error", "group_iteration_error");
                            return map;
                        } finally {
                            ThreadPoolsUtil.destroyGroup("mongoRead", groupName);
                            if (loopIndex < chunkNum) {
                                groupName = Long.toString(SnowflakeUtil.getLong());
                            }
                        }
                        if (brokenPipe || decryptError) {
                            break;
                        }
                    }
                } else {
                    myRunnable.run();
                    if (myRunnable.isError()) {
                        LogUtil.print("MongoUtil_read_133", "块解密失败，中止下载", conditionType, conditionValue, myRunnable.getCount());
                        decryptError = true;
                        break;
                    }
                    byte[] bytes = myRunnable.getBuffer();
                    if (bytes == null || bytes.length <= 0) {
                        LogUtil.print("MongoUtil_read_133a", "块解密结果为空，中止下载", conditionType, conditionValue, myRunnable.getCount());
                        decryptError = true;
                        break;
                    }
                    try {
                        out.write(bytes, 0, bytes.length);
                        MD5Util.update(md5Digest, bytes, 0, bytes.length);
                        // 每16块或最后一块时flush
                        if (loopIndex % 16 == 0 || loopIndex == chunkNum) {
                            out.flush();
                        }
                    } catch (Throwable throwable) {
                        final String msg = throwable.getMessage();
                        if (!StringUtil.isBlank(msg) && msg.contains("Broken pipe")) {
                            LogUtil.print("MongoUtil_read_103", "客户端断开连接，终止下载", conditionType, conditionValue);
                            brokenPipe = true;
                        } else {
                            decryptError = true;
                            LogUtil.error("MongoUtil_read_102", throwable, conditionType, conditionValue, pos, idleNum, loopIndex, chunkNum, threadNum, objectId, chunkname, size);
                        }
                        break;
                    }
                }
            }
        } finally {
            if (cursorChunk != null) {
                cursorChunk.close();
            }
        }
        if (decryptError) {
            map.put("exist", false);
            map.put("error", "decrypt_error");
            LogUtil.print("MongoUtil_read_1010", "文件读取中断，块解密失败", "耗时" + (System.currentTimeMillis() - readStart) + "ms", conditionType, conditionValue, objectId, chunkname, size);
        } else if (brokenPipe) {
            map.put("exist", false);
            map.put("error", "broken_pipe");
            LogUtil.print("MongoUtil_read_1009", "文件读取中断，客户端断开连接", "耗时" + (System.currentTimeMillis() - readStart) + "ms", conditionType, conditionValue, objectId, chunkname, size);
        } else {
            int actualChunkCount = loopIndex;
            if (actualChunkCount < chunkNum) {
                LogUtil.print("MongoUtil_read_136", "chunk数量不足，文件可能不完整", conditionType, conditionValue, actualChunkCount, chunkNum);
                map.put("exist", false);
                map.put("error", "chunk_incomplete");
                map.put("chunkIncomplete", true);
            }
            String actualMd5 = MD5Util.getHex(md5Digest);
            if (actualMd5 != null && !StringUtil.isBlank(hashFromDb)) {
                if (hashFromDb.equalsIgnoreCase(actualMd5)) {
                    map.put("md5Match", true);
                    LogUtil.print("MongoUtil_read_1011", "MD5校验通过", conditionType, conditionValue, hashFromDb, actualMd5);
                } else {
                    map.put("md5Match", false);
                    map.put("expectedMd5", hashFromDb);
                    map.put("actualMd5", actualMd5);
                    map.put("exist", false);
                    map.put("error", "md5_mismatch");
                    LogUtil.print("MongoUtil_read_1012", "MD5校验失败，数据已损坏", "耗时" + (System.currentTimeMillis() - readStart) + "ms", conditionType, conditionValue, hashFromDb, actualMd5);
                }
            }
            LogUtil.print("MongoUtil_read_1008", "文件读取结束，成功了", "耗时" + (System.currentTimeMillis() - readStart) + "ms", conditionType, conditionValue, objectId, chunkname, size);
        }
        return map;
    }

    /**
     * @param pos 从第0块开始
     */
    public static int getIndexByPos(Long pos) {
        int index = (int) (pos / CHUNK_SIZE);
        if (index < 0) {
            index = 0;
        }
        return index;
    }

    /**
     * 在files集合通过hash查找
     */
    public static Map<String, Object> findByHash(String hash) {
        if (!ensureInitialized("MongoUtil_findByHash_0")) {
            Map<String, Object> errorMap = new HashMap<>(2);
            errorMap.put("exist", false);
            errorMap.put("error", "MONGO_NOT_INITialized");
            return errorMap;
        }
        Bson bson = Filters.eq("hash", hash);
        FindIterable<Document> findIterable = p_collectionFile.find(bson);
        findIterable = findIterable.batchSize(1);
        MongoCursor<Document> cursorFile = null;
        int retryFind = 0;
        while (retryFind < MONGO_RETRY_MAX) {
            try {
                cursorFile = findIterable.iterator();
                break;
            } catch (Throwable throwable) {
                if (!isTransientMongoException(throwable)) {
                    LogUtil.error("MongoUtil_findByHash_t0", throwable, "findByHash游标创建遇到非瞬时异常", hash);
                    return new HashMap<>(0);
                }
                retryFind++;
                if (retryFind >= MONGO_RETRY_MAX) {
                    LogUtil.error("MongoUtil_findByHash_t1", throwable, "findByHash游标创建失败，已重试" + MONGO_RETRY_MAX + "次", hash);
                    return new HashMap<>(0);
                }
                try {
                    Thread.sleep(MONGO_RETRY_SLEEP_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return new HashMap<>(0);
                }
            }
        }
        try {
            if (cursorFile.hasNext()) {
                Document docfile = cursorFile.next();
                Object fidObj = docfile.get("_id");
                Object sizeObj = docfile.get("size");
                Object writedSizeObj = docfile.get("writedSize");
                if (fidObj == null || sizeObj == null || writedSizeObj == null) {
                    LogUtil.print("MongoUtil_findByHash_1", "文件记录存在但元数据字段缺失", hash);
                    Map<String, Object> corruptedMap = new HashMap<>(2);
                    corruptedMap.put("exist", true);
                    corruptedMap.put("corrupted", true);
                    return corruptedMap;
                }
                ObjectId fid = (ObjectId) fidObj;
                String fidStr24 = fid.toString();
                Map<String, Object> map = docfile2map(docfile);
                try {
                    Long size = Long.parseLong(sizeObj.toString());
                    Long writedSize = Long.parseLong(writedSizeObj.toString());
                    map.put("id", fidStr24);
                    map.put("hash", hash);
                    map.put("size", size);
                    map.put("writedSize", writedSize);
                } catch (Throwable throwable) {
                    LogUtil.error("MongoUtil_findByHash_2", throwable, "文件元数据size/writedSize解析失败", hash);
                    Map<String, Object> corruptedMap = new HashMap<>(2);
                    corruptedMap.put("exist", true);
                    corruptedMap.put("corrupted", true);
                    return corruptedMap;
                }
                return map;
            }
            return new HashMap<>(0);
        } finally {
            cursorFile.close();
        }
    }

    public static Map<String, Object> docfile2map(Document document) {
        Map<String, Object> map = new HashMap<>(7);
        Object idObj = document.get("_id");
        if (idObj != null) {
            map.put("_id", idObj.toString());
        }
        map.put("hash", document.get("hash"));
        map.put("name", document.get("name"));
        map.put("size", document.get("size"));
        map.put("writedSize", document.get("writedSize"));
        map.put("updatetime", document.get("updatetime"));
        map.put("chunkname", document.get("chunkname"));
        return map;
    }

    /**
     * 确定选用哪个chunk集合
     * @param filesize 要写入的源文件的大小
     */
    public static String getCollectionChunk(long filesize) {
        if (p_collectionInfo == null) {
            LogUtil.print("MongoUtil_getCollectionChunk_0", "p_collectionInfo为null");
            return P_CHUNK + 0;
        }
        long count;
        try {
            count = (Long) retryMongoWrite(new MongoWriteCallback() {
                @Override
                public Object execute() throws Throwable {
                    return p_collectionInfo.estimatedDocumentCount();
                }
            }, "MongoUtil_getCollectionChunk_count", "");
        } catch (Throwable throwable) {
            LogUtil.error("MongoUtil_getCollectionChunk_t2", throwable, "estimatedDocumentCount失败，降级使用chunk0", filesize);
            return P_CHUNK + 0;
        }
        if (count == 0L) {
            return P_CHUNK + 0;
        }
        Document docinfo;
        try {
            docinfo = (Document) retryMongoWrite(new MongoWriteCallback() {
                @Override
                public Object execute() throws Throwable {
                    return p_collectionInfo.find().sort(Sorts.descending("_id")).limit(1).first();
                }
            }, "MongoUtil_getCollectionChunk_find", "");
        } catch (Throwable throwable) {
            LogUtil.error("MongoUtil_getCollectionChunk_t3", throwable, "find info失败，降级使用chunk0", filesize);
            return P_CHUNK + 0;
        }
        if (docinfo == null) {
            LogUtil.print("MongoUtil_getCollectionChunk_1", "未找到info文档");
            return P_CHUNK + 0;
        }
        Object sizeObj = docinfo.get("size");
        long size = 0L;
        if (sizeObj != null) {
            size = Long.parseLong(sizeObj.toString());
        }
        // 仅锁住最终的容量判断与集合切换，避免阻塞p_updateSize中的info更新
        synchronized (INFO_LOCK) {
            long nowSize = size + filesize;
            int i = (int) count - 1;
            if (nowSize > COLL_MAXSIZE) {
                i++;
            }
            return P_CHUNK + i;
        }
    }

    /**
     * xuechong 2025-07-23
     * @param collectionName
     */
    private static MongoCollection<Document> p_getCollection(final String collectionName) {
        if (p_database == null) {
            LogUtil.print("MongoUtil_p_getCollection_0", "p_database为null", collectionName);
            return null;
        }
        MongoCollection<Document> mongoCollection = p_database.getCollection(collectionName);
        if (!INDEXED_COLLECTIONS.contains(collectionName)) {
            synchronized (INDEXED_COLLECTIONS) {
                if (!INDEXED_COLLECTIONS.contains(collectionName)) {
                    try {
                        IndexOptions indexOptions = new IndexOptions();
                        indexOptions.background(true);  // 后台创建（避免阻塞操作，适合生产环境）
                        if (P_FILE.equals(collectionName)) {
                            Bson bson = Indexes.ascending("hash");
                            boolean uniqueCreated = false;
                            try {
                                IndexOptions uniqueOpts = new IndexOptions();
                                uniqueOpts.unique(true);
                                uniqueOpts.background(true);
                                mongoCollection.createIndex(bson, uniqueOpts);
                                uniqueCreated = true;
                            } catch (Throwable throwable) {
                                if (isIndexError(throwable)) {
                                    if (isDuplicateKeyError(throwable)) {
                                        LogUtil.print("MongoUtil_p_getCollection_2", "file集合存在重复hash，唯一索引无法创建，退而创建非唯一索引", collectionName);
                                    } else {
                                        LogUtil.print("MongoUtil_p_getCollection_4", "file.hash唯一索引已存在，跳过", collectionName);
                                    }
                                } else {
                                    throw throwable;
                                }
                            }
                            if (!uniqueCreated) {
                                try {
                                    mongoCollection.createIndex(bson, indexOptions);
                                } catch (Throwable t) {
                                    if (!isIndexError(t)) {
                                        throw t;
                                    }
                                    LogUtil.print("MongoUtil_p_getCollection_3", "file.hash非唯一索引已存在，跳过", collectionName);
                                }
                            }
                        } else if (StringUtil.startsWith(collectionName, P_CHUNK)) {
                            Bson bsonUnique = Indexes.ascending("fid", "n");
                            try {
                                IndexOptions uniqueOpts = new IndexOptions();
                                uniqueOpts.unique(true);
                                uniqueOpts.background(true);
                                mongoCollection.createIndex(bsonUnique, uniqueOpts);
                            } catch (Throwable throwable) {
                                if (isIndexError(throwable)) {
                                    if (isDuplicateKeyError(throwable)) {
                                        LogUtil.print("MongoUtil_p_getCollection_1", "chunk集合存在重复{fid,n}数据，退而创建非唯一复合索引", collectionName);
                                        try {
                                            mongoCollection.createIndex(bsonUnique, indexOptions);
                                        } catch (Throwable t2) {
                                            if (!isIndexError(t2)) {
                                                throw t2;
                                            }
                                            LogUtil.print("MongoUtil_p_getCollection_1a", "chunk非唯一复合索引已存在，跳过", collectionName);
                                        }
                                    } else {
                                        LogUtil.print("MongoUtil_p_getCollection_1b", "chunk复合索引已存在，跳过", collectionName);
                                    }
                                } else {
                                    throw throwable;
                                }
                            }
                        }
                    } catch (Throwable throwable) {
                        LogUtil.error("MongoUtil_p_getCollection_t1", throwable, collectionName);
                    } finally {
                        INDEXED_COLLECTIONS.add(collectionName);
                    }
                }
            }
        }
        return mongoCollection;
    }

    private static void p_updateSize(long filesize) {
        synchronized (INFO_LOCK) {
            if (p_collectionInfo == null) {
                LogUtil.print("MongoUtil_p_updateSize_0", "p_collectionInfo为null");
                return;
            }
            final Document docinfo;
            try {
                docinfo = (Document) retryMongoWrite(new MongoWriteCallback() {
                    @Override
                    public Object execute() throws Throwable {
                        return p_collectionInfo.find().sort(Sorts.descending("_id")).limit(1).first();
                    }
                }, "MongoUtil_p_updateSize_find", "");
            } catch (Throwable throwable) {
                LogUtil.error("MongoUtil_p_updateSize_t1", throwable, "info查询失败，跳过size更新", filesize);
                return;
            }
            if (docinfo == null) {
                final Document docinfoNew = new Document();
                String fidStr24 = new ObjectId().toHexString();
                ObjectId id = new ObjectId(fidStr24);
                docinfoNew.put("_id", id);
                docinfoNew.put("size", filesize);
                try {
                    retryMongoWrite(new MongoWriteCallback() {
                        @Override
                        public Object execute() throws Throwable {
                            p_collectionInfo.insertOne(docinfoNew);
                            return null;
                        }
                    }, "MongoUtil_p_updateSize_insert", "");
                } catch (Throwable throwable) {
                    LogUtil.error("MongoUtil_p_updateSize_t2", throwable, "info insertOne失败，跳过size更新", filesize);
                }
                return;
            }
            Object sizeObj = docinfo.get("size");
            long size = 0L;
            if (sizeObj != null) {
                try {
                    size = Long.parseLong(sizeObj.toString());
                } catch (Throwable throwable) {
                    LogUtil.error("MongoUtil_p_updateSize_t5", throwable, "info size解析失败，跳过size更新", filesize);
                    return;
                }
            }
            long nowSize = size + filesize;
            if (nowSize <= COLL_MAXSIZE) {
                docinfo.put("size", nowSize);
                try {
                    retryMongoWrite(new MongoWriteCallback() {
                        @Override
                        public Object execute() throws Throwable {
                            p_collectionInfo.replaceOne(Filters.eq("_id", docinfo.get("_id")), docinfo);
                            return null;
                        }
                    }, "MongoUtil_p_updateSize_replace", "");
                } catch (Throwable throwable) {
                    LogUtil.error("MongoUtil_p_updateSize_t3", throwable, "info replaceOne失败，跳过size更新", filesize);
                }
            } else {
                final Document docinfoNew = new Document();
                String fidStr24 = new ObjectId().toHexString();
                ObjectId id = new ObjectId(fidStr24);
                docinfoNew.put("_id", id);
                docinfoNew.put("size", filesize);
                try {
                    retryMongoWrite(new MongoWriteCallback() {
                        @Override
                        public Object execute() throws Throwable {
                            p_collectionInfo.insertOne(docinfoNew);
                            return null;
                        }
                    }, "MongoUtil_p_updateSize_insertNew", "");
                } catch (Throwable throwable) {
                    LogUtil.error("MongoUtil_p_updateSize_t4", throwable, "info insertOne(new)失败，跳过size更新", filesize);
                }
            }
        }
    }

    public static long getSize() {
        if (p_collectionInfo == null) {
            LogUtil.print("MongoUtil_getSize_0", "p_collectionInfo为null");
            return 0L;
        }
        final Document groupStage = new Document("$group", new Document("_id", null).append("total", new Document("$sum", "$size")));
        try {
            return (Long) retryMongoWrite(new MongoWriteCallback() {
                @Override
                public Object execute() throws Throwable {
                    MongoCursor<Document> cursorinfo = null;
                    try {
                        cursorinfo = p_collectionInfo.aggregate(Arrays.asList(groupStage)).iterator();
                        if (cursorinfo.hasNext()) {
                            Document result = cursorinfo.next();
                            return result.getLong("total");
                        }
                        return 0L;
                    } finally {
                        IOUtil.close(cursorinfo);
                    }
                }
            }, "MongoUtil_getSize_aggregate", "");
        } catch (Throwable throwable) {
            LogUtil.error("MongoUtil_getSize_t1", throwable, "聚合查询失败");
            return 0L;
        }
    }

    /**
     * 查询文件状态
     * @return 空Map=不存在/损坏；非空时status值：nothing(size<=0) / ing(上传中) / fileOK(已完整上传) / corrupted(元数据异常)
     */
    public static Map<String, Object> exist(String hash) {
        long existStart = System.currentTimeMillis();
        Map<String, Object> map = findByHash(hash);
        if (map.isEmpty() || map.containsKey("corrupted")) {
            return new HashMap<>(0);
        }
        long size;
        long writedSize;
        try {
            size = Long.parseLong(map.get("size").toString());
            writedSize = Long.parseLong(map.get("writedSize").toString());
        } catch (Throwable throwable) {
            LogUtil.error("MongoUtil_exist_0", throwable, "size/writedSize解析失败", "耗时" + (System.currentTimeMillis() - existStart) + "ms", hash);
            return new HashMap<>(0);
        }
        if (size <= 0) {  //size异常（≤0），视为无效记录
            map.put("status", "nothing");
            map.put("size", 0L);
            map.put("writedSize", 0L);
            return map;
        }
        if (size > writedSize) {  //已上传一部分但未全部上传，上传未完成
            map.put("status", "ing");
            return map;
        }
        if (size == writedSize) {  //已完整上传，可下载
            map.put("status", "fileOK");
            map.put("writedSize", writedSize);
            map.put("isFast", "yes");
            return map;
        }
        // writedSize > size，元数据异常
        LogUtil.print("MongoUtil_exist_1", "文件元数据异常：writedSize大于size", "耗时" + (System.currentTimeMillis() - existStart) + "ms", hash, size, writedSize);
        map.put("status", "corrupted");
        map.put("corrupted", "yes");
        return map;
    }

    private static String getWriteFid(String fidOld) {
        if (StringUtil.isBlank(fidOld)) {
            return new ObjectId().toHexString();
        }
        try {
            ObjectId objectId = new ObjectId(fidOld);
            return objectId.toString();
        } catch (Throwable throwable) {
            LogUtil.error("MongoUtil_getWriteFid_1", throwable, fidOld);
            return new ObjectId().toHexString();
        }
    }

    /**
     * xuechong 2026-05-24 循环读取确保读满buffer，解决InputStream.read()不完整读取问题
     */
    private static int readFully(InputStream in, byte[] buffer, int length) throws IOException {
        int off = 0;
        int remain = length;
        while (remain > 0) {
            int n = in.read(buffer, off, remain);
            if (n == -1) {
                break;
            }
            off += n;
            remain -= n;
        }
        return off;
    }


    /**
     * xuechong 2026-05-24 确保MongoDB已初始化，未初始化时返回false
     */
    private static boolean ensureInitialized(String location) {
        if (!p_initialized || p_database == null || p_collectionFile == null) {
            LogUtil.print(location, "MongoDB未初始化");
            return false;
        }
        return true;
    }

    /**
     * xuechong 2026-05-26 FILE_LOCKS容量告警，不强制清理（clear会破坏正在进行的上传的并发安全）
     */
    private static void warnFileLocksIfNeeded() {
        if (FILE_LOCKS.size() > FILE_LOCKS_WARN_SIZE) {
            LogUtil.print("MongoUtil_warnFileLocks_1", "FILE_LOCKS超过阈值，当前size=" + FILE_LOCKS.size() + "，可能存在锁泄漏");
        }
    }

}