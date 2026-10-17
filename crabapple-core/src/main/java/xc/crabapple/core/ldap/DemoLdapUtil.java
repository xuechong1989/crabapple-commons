package xc.crabapple.core.ldap;

import xc.crabapple.core.log.LogUtil;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 2019-02-22 2.0.1
 * ldap工具测试类
 * 本类为java操作ldap示例性的文件，基于服务器版本openLdap_x64_v2.4.42
 * //服务器端：openLdap_x64_v2.4.42.exe
 * 1. 执行openLdap_x64_v2.4.42.exe，所有均为默认设置，安装完成，
 * 2. 安装完成后在任务管理器中会出现名称为OpenLDAP-slapd的任务。
 * 3. 打开OpenLDAP安装的根目录，重点关注配置文件slapd.conf和maxcrc.ldif文件
 * //客户端：ldapBrowser.zip
 * 1. 解压ldapBrowser.zip（该软件是绿色版），执行文件lbe.bat
 */
public final class DemoLdapUtil {

    private static DirContext dirContext = null;

    public static final void test() {
        connect();
        //
        //organCreate();  //部门添加
        //organDestroy();  //部门删除
        //organModify();  //部门修改
        //organSearch();  //部门搜索
        //
        //userCreate();  //人员添加
        //
        //rename();//指定节点重命名
        //
        close();
    }

    /**
     * 连接ldap服务器
     */
    public static final void connect() {
        if(dirContext != null){
            close();
        }
        Hashtable hashtable = new Hashtable();
        String LDAP_URL = "ldap://localhost:389";  //LDAP访问地址
        String adminName = "cn=Manager,dc=maxcrc,dc=com";  //注意用户名的写法：domain\User或
        String adminPassword = "secret";  //密码
        hashtable.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        hashtable.put(Context.PROVIDER_URL, LDAP_URL);
        hashtable.put(Context.SECURITY_AUTHENTICATION, "simple");
        hashtable.put(Context.SECURITY_PRINCIPAL, adminName);
        hashtable.put(Context.SECURITY_CREDENTIALS, adminPassword);
        try {
            dirContext = new InitialDirContext(hashtable);
        }catch(Throwable throwable){
            LogUtil.error("LdapDemoUtil_connect_t2", throwable, "认证出错");
        }
    }

    /**
     * 添加部门，在根目录下添加
     * ou就是部门单元的意思，Organization Unit
     * 部门下面的属性都是固定的，共三个：ou, description, objectClass
     * 注意：部门和用户的属性名都是固定的，不能随意给其新增其他名称的属性，否则会报错。不过同一个属性可以添加多个值
     */
    public static final void organCreate() {
        String organName = "xcA";  //部门名称
        try {
            //
            BasicAttribute basicAttribute = new BasicAttribute("objectClass");
            basicAttribute.add("top");
            basicAttribute.add("organizationalUnit");
            //
            BasicAttributes basicAttributes = new BasicAttributes();
            basicAttributes.put(basicAttribute);
            basicAttributes.put("ou", organName);
            basicAttributes.put("description", "aaa");
            //
            String name = "ou="+organName+",dc=maxcrc,dc=com";  //在根目录添加部门
            dirContext.createSubcontext(name, basicAttributes);
        }catch(Throwable throwable){
            LogUtil.error("LdapDemoUtil_organCreate_t1", throwable);
        }
    }

    /**
     * 删除部门
     */
    public static final void organDestroy(){
        String organName = "xcA";  //部门名称
        try{
            String name = "ou="+organName+",dc=maxcrc,dc=com";
            dirContext.destroySubcontext(name);
        }catch(Throwable throwable){
            LogUtil.error("LdapDemoUtil_organDestroy_t1", throwable);
        }
    }

    /**
     * 部门修改就是对部门条目属性的修改，共三种操作：属性添加，属性修改，属性删除
     */
    public static final void organModify(){
        String action = "remove";  //add replace remove
        Map<String, String> map = new HashMap<>();
        map.put("description", "bbb2");
        int i = 0;
        ModificationItem[] modificationItems = new ModificationItem[map.size()];
        Set<Map.Entry<String, String>> set = map.entrySet();
        Iterator<Map.Entry<String, String>> entries = set.iterator();
        while(entries.hasNext()){
            Map.Entry<String, String> entry = entries.next();
            String key = entry.getKey();
            String value = entry.getValue();
            BasicAttribute basicAttribute = new BasicAttribute(key, value);
            ModificationItem modificationItem = null;
            if("add".equals(action)){  //添加
                modificationItem = new ModificationItem(DirContext.ADD_ATTRIBUTE, basicAttribute);
            }else if("replace".equals(action)){  //修改
                modificationItem = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, basicAttribute);
            }else if("remove".equals(action)){  //删除
                modificationItem = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, basicAttribute);
            }
            modificationItems[i] = modificationItem;
            i++;
        }
        try {
            String organName = "organ1";  //部门名称
            String name = "ou="+organName+",dc=maxcrc,dc=com";
            dirContext.modifyAttributes(name, modificationItems);
        }catch(Throwable throwable){
            LogUtil.error("LdapDemoUtil_organModify_t1", throwable);
        }
    }

    /**
     * 部门搜索
     * 部门搜索与人员搜索用法相同
     * 搜索范围，分为三类："base"(本节点), "one"(单层), ""(遍历)
     * 指定子节点，(格式为"(objectclass=*)", *是指全部，也可指定某一特定类型的树节点)
     * 返回值数据结构示例(Map类型)：
     * {
     *     "ou":"organ1",
     *     "description":"bbb",
     *     "objectClass":"top,organizationlUtit"
     * }
     */
    public static final Map<String, String> organSearch() {
        Map<String, String> messageMap = new HashMap<>();
        SearchControls searchCtls = new SearchControls();
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        String searchFiter = "ou=organ1";
        searchCtls.setReturningAttributes(null);
        try {
            NamingEnumeration<SearchResult> namingEnumeration = dirContext.search("dc=maxcrc,dc=com", searchFiter, searchCtls);
            while(namingEnumeration.hasMoreElements()){
                SearchResult searchResult = namingEnumeration.next();
                Attributes attributes = searchResult.getAttributes();
                if(attributes == null){
                    continue;
                }
                for(NamingEnumeration<? extends Attribute> enumeration = attributes.getAll(); enumeration.hasMore();){
                    String kv = enumeration.next().toString();
                    kv = kv.replace(" ", "");
                    String[] kvs = kv.split(":");
                    String key = kvs[0];
                    String value = kvs[1];
                    messageMap.put(key, value);
                }
            }
            return messageMap;
        }catch(Throwable throwable){
            Map<String, String> nullMap = new HashMap<>();
            return nullMap;
        }
    }

    /**
     * 添加员工
     */
    public static final void userCreate() {
        String cnName = "xc8";  //人员名称，也是人员在路径上的显示名
        String ouName = "organ1";  //部门名称
        //
        BasicAttribute basicAttribute = new BasicAttribute("objectclass");
        basicAttribute.add("inetOrgPerson");
        basicAttribute.add("posixAccount");
        basicAttribute.add("shadowAccount");
        //
        BasicAttributes basicAttributes = new BasicAttributes();
        basicAttributes.put(basicAttribute);
        basicAttributes.put("sn", "snA");  //人员名称简写
        basicAttributes.put("cn", cnName);
        basicAttributes.put("uid", cnName);
        basicAttributes.put("uidNumber", "2000");
        basicAttributes.put("gidNumber", "5");
        basicAttributes.put("homeDirectory", "/hello/" + cnName);
        //
        Map<String, String> map = new HashMap<>();
        Set<Map.Entry<String, String>> set = map.entrySet();
        Iterator<Map.Entry<String, String>> iterator = set.iterator();
        while(iterator.hasNext()){
            Map.Entry<String, String> entry = iterator.next();
            String key = entry.getKey();
            String value = entry.getValue();
            basicAttributes.put(key, value);
        }
        String baseDN = "dc=maxcrc,dc=com";
        String username = "cn="+cnName + ",ou="+ouName + "," + baseDN;
        try {
            //人员添加
            dirContext.createSubcontext(username, basicAttributes);
            //人员创建成功后，动态修改该人员的属性值
            BasicAttribute basicAttribute2 = new BasicAttribute("sn", "bbb");
            ModificationItem[] modificationItems = new ModificationItem[1];
            modificationItems[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, basicAttribute2);
            dirContext.modifyAttributes(username, modificationItems);
        }catch(Throwable throwable){
            LogUtil.error("LdapDemoUtil_userCreate_t1", throwable);
        }
    }

    /**
     * 重命名节点
     */
    public static final void rename() {
        String oldDN = "ou=depart1,dc=maxcrc,dc=com";
        String newDN = "ou=depart2,dc=maxcrc,dc=com";
        try {
            dirContext.rename(oldDN, newDN);
        }catch(Throwable throwable){
            LogUtil.error("LdapDemoUtil_rename_t1", throwable);
        }
    }

    /**
     * 关闭ldap连接
     */
    public static final void close(){
        if(dirContext == null){
            return;
        }
        try {
            dirContext.close();
        }catch(Throwable throwable){
            LogUtil.error("LdapDemoUtil_close_t1", throwable);
        }
    }

}
