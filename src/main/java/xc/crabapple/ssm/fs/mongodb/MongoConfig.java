package xc.crabapple.ssm.fs.mongodb;

/**
 * MongoDB 分块文件存储配置。
 *
 * 只支持 mongodb-driver-sync 5.x，不再保留 mongo3 双驱动。
 */
public final class MongoConfig {

    private final String uri;
    private final String database;
    private final String encryptionKey;

    public MongoConfig(final String uri, final String database, final String encryptionKey) {
        if (uri == null || uri.trim().isEmpty()) {
            throw new IllegalArgumentException("mongo.uri 不能为空");
        }
        if (database == null || database.trim().isEmpty()) {
            throw new IllegalArgumentException("mongo.database 不能为空");
        }
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            throw new IllegalArgumentException("mongo.encrypt.key 不能为空");
        }
        this.uri = uri.trim();
        this.database = database.trim();
        this.encryptionKey = encryptionKey;
    }

    /**
     * 从系统属性读取：
     * - crabapple.mongo.uri
     * - crabapple.mongo.database
     * - crabapple.mongo.encrypt.key
     */
    public static MongoConfig fromSystemProperties() {
        return new MongoConfig(
                System.getProperty("crabapple.mongo.uri"),
                System.getProperty("crabapple.mongo.database"),
                System.getProperty("crabapple.mongo.encrypt.key"));
    }

    public String getUri() {
        return uri;
    }

    public String getDatabase() {
        return database;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }
}
