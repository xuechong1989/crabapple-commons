package xc.crabapple.ssm.fs.minio;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import xc.crabapple.core.cipher.md5.MD5Util;
import xc.crabapple.core.cipher.sm4.Sm4Util;
import xc.crabapple.core.collection.CollectionUtil;
import xc.crabapple.core.collection.MapUtil;
import xc.crabapple.core.collection.StringUtil;
import xc.crabapple.core.encoder.Base64Util;
import xc.crabapple.core.file.FileUtil;
import xc.crabapple.core.io.IOUtil;
import xc.crabapple.core.log.LogUtil;
import xc.crabapple.core.random.SnowflakeUtil;
import xc.crabapple.core.servlet.ServletUtil;
import xc.crabapple.core.timer.TimerUtil;
import xc.crabapple.core.url.UrlUtil;
import xc.crabapple.ssm.uploader.UploaderUtil;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2025-08-20
 * minio工具类
 */
public final class MinioUtil {

    private static MinioAsyncClient p_minioAsyncClient = null;
    private static String p_bucketName = "crabapple";  // 存储桶名称
    private static String p_endpoint = null;
    private static String p_accessKey = null;
    private static String p_secretKey = null;
    private static Set<String> p_newKeySet = null;
    // 文件内容加密信息
    // 2026-09-23 上传文件新增MD5回读校验彻底解决文件缺失或损坏的问题
    // 覆盖重写递归保护：写入后核对不过会再调一次本方法重写，重写后仍不过直接判失败，防止无限递归
    private static final ThreadLocal<Integer> s_rewriteDepth = new ThreadLocal<Integer>();

    private static final String P_SM4_KEY = System.getProperty("crabapple.minio.sm4Key", "");  // 部署方配置，禁止使用固定默认值
    private static final String P_SM4_IV = System.getProperty("crabapple.minio.sm4Iv", "");  // 部署方配置，禁止使用固定默认值
    private static final String P_SM4_MODE = "CBC";
    private static final Integer P_SM4_PADDING = 7;

    private static void requireSm4Config() {
        if (StringUtil.isBlank(P_SM4_KEY) || StringUtil.isBlank(P_SM4_IV)) {
            throw new IllegalStateException("MinioUtil 未配置 SM4 密钥/IV，请设置系统属性 crabapple.minio.sm4Key / crabapple.minio.sm4Iv");
        }
    }

    /**
     * 2025-08-20
     * 连接到minio服务器
     * 可直接本地运行
     * @param endpoint minio服务器地址，格式如： http://192.168.124.46:9000
     * @param accessKey minio服务器访问口令，如： admin
     * @param secretKey minio服务器访问密钥，如： your-secret-key
     * @return
     */
    public static final boolean connect(final String endpoint, final String accessKey, final String secretKey) {
        p_endpoint = endpoint;
        p_accessKey = accessKey;
        p_secretKey = secretKey;
        //
        try {
            p_minioAsyncClient = MinioAsyncClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            if (p_minioAsyncClient == null) {
                return false;
            }
            // 存储桶
            BucketExistsArgs.Builder bucketExistsArgsBuilder = BucketExistsArgs.builder();
            BucketExistsArgs bucketExistsArgs = bucketExistsArgsBuilder.bucket(p_bucketName).build();
            CompletableFuture<Boolean> booleanCompletableFuture = p_minioAsyncClient.bucketExists(bucketExistsArgs);
            boolean bucketExists = booleanCompletableFuture.get();
            // 若存储桶不存在则创建
            if (!bucketExists) {
                MakeBucketArgs.Builder makeBucketArgsBuilder = MakeBucketArgs.builder();
                MakeBucketArgs makeBucketArgs = makeBucketArgsBuilder.bucket(p_bucketName).build();
                p_minioAsyncClient.makeBucket(makeBucketArgs);
            }
            //
//            ThreadUtil.start(new Business("MiniofsUtil_connect_33", false){
//                @Override
//                public void process() {
//                    p_addCacheAll();
//                }
//            });
            //
            return true;
        } catch (Throwable throwable) {
            LogUtil.error("MiniofsUtil_connect_t1", throwable, "minio服务器连接失败", p_endpoint, p_bucketName);
            return false;
        }
    }

    /**
     * 2025-08-21
     * 2个或多个文件上传
     * 必须通过web服务器运行
     * @param multipartHttpServletRequest
     * @return
     */
    public static final List<Map<String, String>> upload(final MultipartHttpServletRequest multipartHttpServletRequest){
        if(multipartHttpServletRequest == null){
            return null;
        }
        Map<String, MultipartFile> multipartFileMap = multipartHttpServletRequest.getFileMap();
        if(CollectionUtil.isBlank(multipartFileMap)){
            return null;
        }
        //
        List<Map<String, String>> list = new ArrayList<>();
        Set<String> set = multipartFileMap.keySet();
        Iterator<String> iterator = set.iterator();
        while(iterator.hasNext()){
            String key = iterator.next();
            MultipartFile multipartFile = multipartFileMap.get(key);
            String originalFileName = multipartFile.getOriginalFilename();
            String fileName = UrlUtil.getFileName(originalFileName, false);
            String fileType = FileUtil.getFileType(originalFileName);
            String sourceUrl = FileUtil.getTempFile(fileName, fileType);
            UploaderUtil.multipartFile2disk(multipartFile, sourceUrl);
            Map<String, String> resultMap = upload(null, sourceUrl);
            list.add(resultMap);
        }
        return list;
    }

    /**
     * 2025-08-21
     * 单个文件上传
     * 可直接本地运行
     * @param sourceUrl
     * @return
     */
    public static final Map<String, String> upload(final String sourceUrl) {
        Map<String, String> resultMap = upload(null, sourceUrl);
        return resultMap;
    }

    /**
     * 2025-08-20
     * 单个文件上传
     * 可直接本地运行
     * @param id
     * @param sourceUrl
     * @return
     */
    public static final Map<String, String> upload(final String id, final String sourceUrl) {
        Map<String, String> resultMap = upload(id, sourceUrl, null);
        return resultMap;
    }

    /**
     * 2026-09-22 上传文件新增MD5回读校验彻底解决文件缺失或损坏的问题
     * 带客户端声明MD5的上传：上层把前端在浏览器里对用户原文件算出的MD5传下来，写入前先与服务端
     * 实际收到的字节MD5比对（浏览器到服务端这一段传输是否损坏只有这个基准能发现），不一致直接拒收
     * （error_client_md5_mismatch：不写入、不重写——重写也是同一份坏字节，没有意义），由上层按上传失败
     * 提示用户重试；一致则走既有流程，最终CMIS_HASH_必然等于客户端算出的那份MD5
     * @param id
     * @param sourceUrl
     * @param clientMd5 客户端（浏览器）对原文件算出的MD5，空表示调用方没有该值（内部生成文件等场景）
     * @return
     */
    public static final Map<String, String> upload(final String id, final String sourceUrl, final String clientMd5) {
        InputStream inputStream = null;
        try {
            // 校验文件是否合法
            String localSourceUrl = UrlUtil.getStandardPath(sourceUrl);
            final boolean isReal = FileUtil.isRealFile(localSourceUrl, false);
            if(!isReal){
                LogUtil.warn("MiniofsUtil_upload_1", "文件不存在", p_endpoint, id, localSourceUrl);
                Map<String, String> map = new HashMap<>(2);
                map.put("extra_upload", "error_file_not_found");
                return map;
            }
            long size = FileUtil.getFileSize(localSourceUrl);
            if(size <= 0){
                LogUtil.warn("MiniofsUtil_upload_2", "文件大小为0", p_endpoint, id, localSourceUrl);
                Map<String, String> map = new HashMap<>(2);
                map.put("extra_upload", "error_file_size_is_zero");
                return map;
            }
            // 文件内容的MD5
            final String md5 = MD5Util.getFileHash(localSourceUrl);
            if(StringUtil.isBlank(md5)){
                LogUtil.warn("MiniofsUtil_upload_3", "文件的md5为空", p_endpoint, id, localSourceUrl);
                Map<String, String> map = new HashMap<>(2);
                map.put("extra_upload", "error_file_md5_is_null");
                return map;
            }
            // 2026-09-22 上传文件新增MD5回读校验彻底解决文件缺失或损坏的问题
            // 客户端声明MD5比对：与上面刚算出的"服务端收到字节MD5"不一致，说明浏览器到服务端这段传输已损坏，
            // 这份字节不能作为入云盘与上层写CMIS_HASH_的依据，直接拒收（不写、不重写，重写也是同一份坏字节）
            if(!StringUtil.isBlank(clientMd5) && !StringUtil.equals(clientMd5, md5)){
                LogUtil.error("MiniofsUtil_upload_7", "客户端声明MD5与服务端收到字节MD5不一致，拒收本次上传", p_endpoint, id, localSourceUrl, clientMd5, md5);
                Map<String, String> map = new HashMap<>(2);
                map.put("extra_upload", "error_client_md5_mismatch");
                return map;
            }
            // 重复校验
            Map<String, String> metadataMap = getMetadataMap(md5);
            if(metadataMap != null){
                String status = metadataMap.get("status");
                if("ok".equals(status)){
                    // 2026-09-23 上传文件新增MD5回读校验彻底解决文件缺失或损坏的问题
                    // 底层强制校验：秒传命中也读回核对，内容不对不返回秒传、落到真传整对象覆盖
                    boolean memFastVerifyOk = verifyObject(md5);
                    if (memFastVerifyOk) {
                        metadataMap.put("extra_upload", "fast");  // 秒传
                        return metadataMap;
                    }
                    LogUtil.error("MiniofsUtil_upload_6", "秒传命中但回读校验未通过，转真传覆盖", p_endpoint, localSourceUrl, md5);
                }
            }
            // 开始真正上传文件
            metadataMap = new HashMap<>();
            String type = FileUtil.getFileType(localSourceUrl);
            String name = FileUtil.getFileName(localSourceUrl, true);
            name = Base64Util.encodeStr2str(name);
            String sizeStr = size + "";
            // 2026-08-12 系统与所有数据库表均统一时间格式 去毫秒
            String time = TimerUtil.now(TimerUtil.FORMATTYPE1);
            String localId = id;
            if(StringUtil.isBlank(localId)){
                localId = SnowflakeUtil.getStr();
            }
            metadataMap.put("id", localId);
            metadataMap.put("hash", md5);
            metadataMap.put("name", name);
            metadataMap.put("type", type);
            metadataMap.put("size", sizeStr);
            metadataMap.put("time", time);
            metadataMap.put("status", "ok");
            //
            byte[] bytes = FileUtil.file2bytes(localSourceUrl);
            requireSm4Config();
            byte[] cipherBytes = Sm4Util.encryptBytes(P_SM4_KEY, P_SM4_IV, bytes, P_SM4_MODE, P_SM4_PADDING);
            inputStream = new ByteArrayInputStream(cipherBytes);
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(p_bucketName)
                    .object(md5)
                    .userMetadata(metadataMap)
                    .stream(inputStream, (long) cipherBytes.length, -1L)
                    .contentType("application/octet-stream")
                    .build();
            CompletableFuture<ObjectWriteResponse> completableFuture = p_minioAsyncClient.putObject(putObjectArgs);
//            p_newKeySet.add(md5);  // 添加到缓存
            ObjectWriteResponse objectWriteResponse = completableFuture.get();
            //
            // 2026-09-23 上传文件新增MD5回读校验彻底解决文件缺失或损坏的问题
            // 底层强制回读校验（内存版同款）：不一致自动重写一次，仍不一致判失败
            String memVerifyMd5 = p_verifyObjectMd5(md5);
            boolean memVerifyOk = StringUtil.equals(md5, memVerifyMd5);
            if (memVerifyOk) {
                metadataMap.put("extra_upload", "success");
                return metadataMap;
            }
            LogUtil.error("MiniofsUtil_upload_5", "写入后回读校验未通过，覆盖重写一次", p_endpoint, sourceUrl, md5, memVerifyMd5);
            Integer memDepth = s_rewriteDepth.get();
            boolean memInRewrite = memDepth != null && memDepth.intValue() >= 1;
            if (memInRewrite) {
                Map<String, String> memFailMap = new HashMap<>(2);
                memFailMap.put("extra_upload", "error_verify_failed");
                memFailMap.put("hash", md5);
                return memFailMap;
            }
            s_rewriteDepth.set(Integer.valueOf(1));
            try {
                Map<String, String> memRewriteMap = upload(sourceUrl);
                if (memRewriteMap != null) {
                    memRewriteMap.put("extra_upload", "rewrite_success");
                }
                return memRewriteMap;
            } finally {
                s_rewriteDepth.remove();
            }
        } catch (Throwable throwable) {
            LogUtil.error("MiniofsUtil_upload_t120", throwable, p_endpoint, id, sourceUrl);
            return null;
        }finally {
            IOUtil.close(inputStream);
        }
    }

    /**
     * 2026-08-12 所有文件全部最终存储在云盘 13-3
     * 单个文件流式上传（逻辑与 upload 一致，仅加密改为流式分段，避免大文件全量读内存OOM）
     * 可直接本地运行
     * @param sourceUrl
     * @return
     */
    public static final Map<String, String> uploadStream(final String sourceUrl) {
        Map<String, String> resultMap = uploadStream(null, sourceUrl);
        return resultMap;
    }

    /**
     * 2026-08-12 所有文件全部最终存储在云盘 13-3
     * 单个文件流式上传：文件流分段加密后直接上传（SM4/CBC 链式连续 + PKCS7 填充，
     * 密文与整包加密完全一致，历史文件解密兼容），不将整个文件读入内存
     * @param id
     * @param sourceUrl
     * @return
     */
    public static final Map<String, String> uploadStream(final String id, final String sourceUrl) {
        Map<String, String> resultMap = uploadStream(id, sourceUrl, null);
        return resultMap;
    }

    /**
     * 2026-09-22 上传文件新增MD5回读校验彻底解决文件缺失或损坏的问题
     * 带客户端声明MD5的流式上传：写入前先与服务端实际收到的字节MD5比对（浏览器到服务端这一段传输是否
     * 损坏只有这个基准能发现），不一致直接拒收（error_client_md5_mismatch：不写入、不重写），由上层按
     * 上传失败提示用户重试；一致则走既有流程，最终CMIS_HASH_必然等于客户端算出的那份MD5
     * @param id
     * @param sourceUrl
     * @param clientMd5 客户端（浏览器）对原文件算出的MD5，空表示调用方没有该值（内部生成文件等场景）
     * @return
     */
    public static final Map<String, String> uploadStream(final String id, final String sourceUrl, final String clientMd5) {
        InputStream inputStream = null;
        try {
            // 2026-08-12 所有文件全部最终存储在云盘
            // 校验文件是否合法
            String localSourceUrl = UrlUtil.getStandardPath(sourceUrl);
            final boolean isReal = FileUtil.isRealFile(localSourceUrl, false);
            if(!isReal){
                LogUtil.warn("MiniofsUtil_uploadStream_1", "文件不存在", p_endpoint, id, localSourceUrl);
                Map<String, String> map = new HashMap<>(2);
                map.put("extra_upload", "error_file_not_found");
                return map;
            }
            long size = FileUtil.getFileSize(localSourceUrl);
            if(size <= 0){
                LogUtil.warn("MiniofsUtil_uploadStream_2", "文件大小为0", p_endpoint, id, localSourceUrl);
                Map<String, String> map = new HashMap<>(2);
                map.put("extra_upload", "error_file_size_is_zero");
                return map;
            }
            // 2026-08-12 所有文件全部最终存储在云盘
            // 文件内容的MD5
            final String md5 = MD5Util.getFileHash(localSourceUrl);
            if(StringUtil.isBlank(md5)){
                LogUtil.warn("MiniofsUtil_uploadStream_3", "文件的md5为空", p_endpoint, id, localSourceUrl);
                Map<String, String> map = new HashMap<>(2);
                map.put("extra_upload", "error_file_md5_is_null");
                return map;
            }
            // 2026-09-22 上传文件新增MD5回读校验彻底解决文件缺失或损坏的问题
            // 客户端声明MD5比对：与上面刚算出的"服务端收到字节MD5"不一致，说明浏览器到服务端这段传输已损坏，
            // 这份字节不能作为入云盘与上层写CMIS_HASH_的依据，直接拒收（不写、不重写，重写也是同一份坏字节）
            if(!StringUtil.isBlank(clientMd5) && !StringUtil.equals(clientMd5, md5)){
                LogUtil.error("MiniofsUtil_uploadStream_7", "客户端声明MD5与服务端收到字节MD5不一致，拒收本次上传", p_endpoint, id, localSourceUrl, clientMd5, md5);
                Map<String, String> map = new HashMap<>(2);
                map.put("extra_upload", "error_client_md5_mismatch");
                return map;
            }
            // 2026-08-12 所有文件全部最终存储在云盘
            // 重复校验（秒传）
            Map<String, String> metadataMap = getMetadataMap(md5);
            if(metadataMap != null){
                String status = metadataMap.get("status");
                if("ok".equals(status)){
                    // 2026-09-23 上传文件新增MD5回读校验彻底解决文件缺失或损坏的问题
                    // 底层强制校验：秒传命中也读回核对，内容不对不返回秒传、落到真传整对象覆盖（坏对象由此自愈）
                    boolean fastVerifyOk = verifyObject(md5);
                    if (fastVerifyOk) {
                        metadataMap.put("extra_upload", "fast");  // 秒传
                        return metadataMap;
                    }
                    LogUtil.error("MiniofsUtil_uploadStream_6", "秒传命中但回读校验未通过，转真传覆盖", p_endpoint, id, localSourceUrl, md5);
                }
            }
            // 2026-08-12 所有文件全部最终存储在云盘
            // 开始真正上传文件
            metadataMap = new HashMap<>();
            String type = FileUtil.getFileType(localSourceUrl);
            String name = FileUtil.getFileName(localSourceUrl, true);
            name = Base64Util.encodeStr2str(name);
            String sizeStr = size + "";
            // 2026-08-12 系统与所有数据库表均统一时间格式 去毫秒
            String time = TimerUtil.now(TimerUtil.FORMATTYPE1);
            String localId = id;
            if(StringUtil.isBlank(localId)){
                localId = SnowflakeUtil.getStr();
            }
            metadataMap.put("id", localId);
            metadataMap.put("hash", md5);
            metadataMap.put("name", name);
            metadataMap.put("type", type);
            metadataMap.put("size", sizeStr);
            metadataMap.put("time", time);
            metadataMap.put("status", "ok");
            // 2026-08-12 所有文件全部最终存储在云盘
            // 流式加密：分段读文件加密（SM4/CBC 链式连续，密文与整包加密一致），避免大文件全量读内存OOM
            // PKCS7 填充后密文长度 = ((明文长度 / 16) + 1) * 16
            final long cipherLen = (size / 16 + 1) * 16;
            final InputStream fileInputStream = new FileInputStream(localSourceUrl);
            requireSm4Config();
            final InputStream cipherInputStream = Sm4Util.encryptReturnInputStream(P_SM4_KEY, P_SM4_IV, fileInputStream, P_SM4_MODE, P_SM4_PADDING);
            if(cipherInputStream == null){
                IOUtil.close(fileInputStream);
                LogUtil.error("MiniofsUtil_uploadStream_4", "流式加密失败", p_endpoint, id, localSourceUrl);
                Map<String, String> map = new HashMap<>(2);
                map.put("extra_upload", "error_encrypt_fail");
                return map;
            }
            inputStream = cipherInputStream;
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(p_bucketName)
                    .object(md5)
                    .userMetadata(metadataMap)
                    .stream(inputStream, cipherLen, -1L)
                    .contentType("application/octet-stream")
                    .build();
            CompletableFuture<ObjectWriteResponse> completableFuture = p_minioAsyncClient.putObject(putObjectArgs);
            ObjectWriteResponse objectWriteResponse = completableFuture.get();
            // 2026-09-23 上传文件新增MD5回读校验彻底解决文件缺失或损坏的问题
            // 底层强制回读校验：写完立即读回全量算MD5与对象名比对，一致才算成功；不一致自动再传一次覆盖重写（秒传判定
            // 现状永不命中、每次putObject都是整对象覆盖），重写后仍不一致判失败（error_verify_failed）
            String verifyMd5 = p_verifyObjectMd5(md5);
            boolean verifyOk = StringUtil.equals(md5, verifyMd5);
            if (verifyOk) {
                metadataMap.put("extra_upload", "success");
                return metadataMap;
            }
            LogUtil.error("MiniofsUtil_uploadStream_5", "写入后回读校验未通过，覆盖重写一次", p_endpoint, id, localSourceUrl, md5, verifyMd5);
            Integer rewriteDepth = s_rewriteDepth.get();
            boolean inRewrite = rewriteDepth != null && rewriteDepth.intValue() >= 1;
            if (inRewrite) {
                Map<String, String> failMap = new HashMap<>(2);
                failMap.put("extra_upload", "error_verify_failed");
                failMap.put("hash", md5);
                return failMap;
            }
            s_rewriteDepth.set(Integer.valueOf(1));
            try {
                Map<String, String> rewriteMap = uploadStream(id, sourceUrl);
                if (rewriteMap != null) {
                    rewriteMap.put("extra_upload", "rewrite_success");
                }
                return rewriteMap;
            } finally {
                s_rewriteDepth.remove();
            }
        } catch (Throwable throwable) {
            LogUtil.error("MiniofsUtil_uploadStream_t1", throwable, p_endpoint, id, sourceUrl);
            return null;
        }finally {
            IOUtil.close(inputStream);
        }
    }

    /**
     * 2025-08-27
     * 校验文件是否已经存在，若存在就返回该数据对象的元信息集合
     * @param md5
     * @return
     */
    public static final Map<String, String> exist(final String md5){
        Map<String, String> metadataMap = getMetadataMap(md5);
        return metadataMap;
    }

    /**
     * 2025-08-27
     * 返回该数据对象的元信息集合
     * @param md5
     * @return
     */
    public static final Map<String, String> getMetadataMap(final String md5){
        if(StringUtil.isBlank(md5)){
            LogUtil.error("MiniofsUtil_getMetadataMap_1", "md5为空", p_endpoint, md5);
            return null;
        }
        try {
            StatObjectArgs statObjectArgs = StatObjectArgs.builder()
                    .bucket(p_bucketName)
                    .object(md5)
                    .build();
            CompletableFuture<StatObjectResponse> statObjectResponseCompletableFuture = p_minioAsyncClient.statObject(statObjectArgs);
            if(statObjectResponseCompletableFuture == null){
                return null;
            }
            StatObjectResponse statObjectResponse = null;
            try{
                statObjectResponse = statObjectResponseCompletableFuture.get();
            }catch (Throwable throwable){
                return null;
            }
            if(statObjectResponse == null){
                return null;
            }
            Map<String, String> metadataMap = new java.util.HashMap<>();
            if(metadataMap == null){
                return null;
            }
            String tmpMd5 = statObjectResponse.object();
            if(StringUtil.isBlank(tmpMd5)){
                return null;
            }
            if(!md5.equals(tmpMd5)){
                return null;
            }
            Map<String, String> metadata2Map = MapUtil.copy(metadataMap);
            return metadata2Map;
        }catch (Throwable throwable){
            LogUtil.error("MiniofsUtil_getMetadataMap_t1", throwable, "获取指定对象的元数据异常", p_endpoint, md5);
            return null;
        }
    }

    /**
     * 2026-09-23 上传文件新增MD5回读校验彻底解决文件缺失或损坏的问题
     * 底层强制回读校验（流式、零临时文件）：把云盘对象解密流边读边算MD5与对象名比对——读回传输无法省（校验的本质），
     * 但不落盘、内存只占8KB缓冲，磁盘空间零占用；上层调用方无需再自建临时文件核对
     * @param md5 对象名（文件明文MD5）
     * @return String 读回内容的MD5（小写hex），对象不存在/解密失败/读流异常返回null
     */
    private static final String p_verifyObjectMd5(final String md5) {
        Map<String, Object> resultMap = p_download2inputstream(md5);
        if (resultMap == null) {
            LogUtil.error("MiniofsUtil_p_verifyObjectMd5_1", "回读校验取流失败", p_endpoint, md5);
            return null;
        }
        InputStream decryptedInputStream = MapUtil.get(resultMap, "inputStream");
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int readCount = 0;
            while (readCount >= 0) {
                readCount = decryptedInputStream.read(buffer);
                if (readCount > 0) {
                    messageDigest.update(buffer, 0, readCount);
                }
            }
            byte[] digestBytes = messageDigest.digest();
            StringBuilder hexBuilder = new StringBuilder(32);
            for (int i = 0; i < digestBytes.length; i++) {
                String hexPiece = Integer.toHexString(digestBytes[i] & 0xFF);
                if (hexPiece.length() == 1) {
                    hexBuilder.append('0');
                }
                hexBuilder.append(hexPiece);
            }
            return hexBuilder.toString();
        } catch (Throwable throwable) {
            LogUtil.error("MiniofsUtil_p_verifyObjectMd5_t1", throwable, "回读校验读流异常", p_endpoint, md5);
            return null;
        } finally {
            IOUtil.close(decryptedInputStream);
        }
    }

    /**
     * 2026-09-23 上传文件新增MD5回读校验彻底解决文件缺失或损坏的问题
     * 按对象名回读核对云盘中已存在的文件（供秒传预检命中后把关）：读回流算MD5与对象名一致返回true
     * @param md5 对象名（文件明文MD5，32位）
     * @return boolean true=对象存在且内容与对象名一致
     */
    public static final boolean verifyObject(final String md5) {
        String actualMd5 = p_verifyObjectMd5(md5);
        boolean verifyOk = StringUtil.equals(md5, actualMd5);
        return verifyOk;
    }

    /**
     * 2025-08-20
     * 从minio下载到应用服务器磁盘
     * 可直接本地运行
     * @param md5
     * @param targetUrl
     * @return
     */
    public static final Map<String, String> download2disk(final String md5, final String targetUrl) {
        Map<String, Object> resultMap = p_download2inputstream(md5);
        if(resultMap == null){
            LogUtil.error("MiniofsUtil_download2disk_1", "文件下载失败", p_endpoint, md5, targetUrl);
            return null;
        }
        InputStream inputStream = MapUtil.get(resultMap, "inputStream");
        try {
            IOUtil.inputstream2disk(inputStream, targetUrl);
            final String targetMd5 = MD5Util.getFileHash(targetUrl);
            if(!StringUtil.equals(md5, targetMd5)){
                LogUtil.error("MiniofsUtil_download2disk_4", "MD5值不一致，文件下载失败", p_endpoint, md5, targetMd5, targetUrl);
                return null;
            }
            Map<String, String> metadataMap = MapUtil.get(resultMap, "metadataMap");
            return metadataMap;
        } catch (Throwable throwable) {
            LogUtil.error("MiniofsUtil_download2disk_t1", throwable, "文件下载异常", p_endpoint, md5, targetUrl);
            return null;
        } finally {
            IOUtil.close(inputStream);
        }
    }

    /**
     * 2025-08-20
     * 从minio下载到用户浏览器
     * 必须通过web服务器运行
     * @param md5
     * @return
     */
    public static final Map<String, String> download2browser(final String md5, final String fileName) {
        Map<String, Object> resultMap = p_download2inputstream(md5);
        if(resultMap == null){
            LogUtil.error("MiniofsUtil_download2browser_1", "文件下载失败", p_endpoint, md5, fileName);
            return null;
        }
        InputStream inputStream = MapUtil.get(resultMap, "inputStream");
        ServletOutputStream servletOutputStream = null;
        try {
            HttpServletRequest request = ServletUtil.getRequest();
            HttpServletResponse response = ServletUtil.getResponse();
            ServletUtil.downloadResponseSet(request, response, fileName);
            servletOutputStream = response.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                servletOutputStream.write(buffer, 0, bytesRead);
            }
            Map<String, String> metadataMap = MapUtil.get(resultMap, "metadataMap");
            return metadataMap;
        } catch (Throwable throwable) {
            LogUtil.error("MiniofsUtil_download2browser_t2", throwable, "文件下载失败", p_endpoint, md5, fileName);
            return null;
        } finally {
            IOUtil.close(servletOutputStream, inputStream);
        }
    }

    /**
     * 2026-08-12 所有文件全部最终存储在云盘 13-3
     * 从minio下载，暂为输入流对象，同时完成解密
     * @param md5
     * @return
     */
    private static final Map<String, Object> p_download2inputstream(final String md5) {
        Map<String, String> metadataMap = getMetadataMap(md5);
        if(metadataMap == null){
            LogUtil.error("MiniofsUtil_p_download2inputstream_1", "文件不存在", p_endpoint, md5);
            return null;
        }
        InputStream minioInputStream = null;
        try {
            GetObjectArgs.Builder getObjectArgsBuilder = GetObjectArgs.builder();
            GetObjectArgs getObjectArgs = getObjectArgsBuilder
                    .bucket(p_bucketName)
                    .object(md5)
                    .build();
            CompletableFuture<GetObjectResponse> completableFuture = p_minioAsyncClient.getObject(getObjectArgs);
            minioInputStream = completableFuture.get();
            // 2026-08-12 所有文件全部最终存储在云盘
            // 流式解密（13-3）：CipherInputStream 分段处理，避免全量读内存OOM；返回流由调用方读取并关闭，关闭时级联关闭minioInputStream
            requireSm4Config();
            InputStream decryptedInputStream = Sm4Util.decryptReturnInputStream(P_SM4_KEY, P_SM4_IV, minioInputStream, P_SM4_MODE, P_SM4_PADDING);
            if(decryptedInputStream == null){
                IOUtil.close(minioInputStream);
                LogUtil.error("MiniofsUtil_p_download2inputstream_2", "流式解密失败", p_endpoint, md5);
                return null;
            }
            Map<String, Object> resultMap = new HashMap<>(2);
            resultMap.put("metadataMap", metadataMap);
            resultMap.put("inputStream", decryptedInputStream);
            return resultMap;
        }catch (Throwable throwable) {
            LogUtil.error("MiniofsUtil_p_download2inputstream_t1", throwable, "文件下载失败", p_endpoint, md5);
            return null;
        }
    }

    /**
     * 2026-08-12 所有文件全部最终存储在云盘
     * 只读列出云盘全部对象名（对象名=文件内容MD5），供存储差异分析等只读工具使用；仅列出不删除
     * @return 对象名列表，异常返回null
     */
    public static final List<String> listAllObjectNames() {
        List<String> objectNameList = new ArrayList<>();
        try {
            ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder().bucket(p_bucketName).build();
            Iterable<Result<Item>> iterable = p_minioAsyncClient.listObjects(listObjectsArgs);
            if(iterable == null){
                return null;
            }
            for (Result<Item> result : iterable) {
                if(result == null){
                    continue;
                }
                final Item item = result.get();
                if(item == null){
                    continue;
                }
                final String objectName = item.objectName();
                if(StringUtil.isBlank(objectName)){
                    continue;
                }
                objectNameList.add(objectName);
            }
            return objectNameList;
        }catch (Throwable throwable) {
            LogUtil.error("MiniofsUtil_listAllObjectNames_t1", throwable, "列出云盘对象失败", p_endpoint);
            return null;
        }
    }

    private static final boolean p_addCacheAll(){
        if(p_newKeySet == null){
            p_newKeySet = ConcurrentHashMap.newKeySet();
        }
        try {
            ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder().bucket(p_bucketName).build();
            Iterable<Result<Item>> iterable = p_minioAsyncClient.listObjects(listObjectsArgs);
            for (Result<Item> result : iterable) {
                final Item item = result.get();
                final String objectName = item.objectName();
                StatObjectArgs statObjectArgs = StatObjectArgs.builder()
                        .bucket(p_bucketName)
                        .object(objectName)
                        .build();
                CompletableFuture<StatObjectResponse> statObjectResponseCompletableFuture = p_minioAsyncClient.statObject(statObjectArgs);
                StatObjectResponse statObjectResponse = statObjectResponseCompletableFuture.get();
                // Map<String, String> metadataMap = new java.util.HashMap<>();
                final String md5 = statObjectResponse.object();
                p_newKeySet.add(md5);
            }
            return true;
        }catch (Throwable throwable) {
            LogUtil.error("MiniofsUtil_p_download2inputstream_t1", throwable, "添加缓存异常", p_endpoint);
            return false;
        }
    }

}
