package xc.crabapple.core.email;

import xc.crabapple.core.log.LogUtil;

import jakarta.mail.Address;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * 2019-12-05 2.2.0
 * 邮件发送
 */
public final class EmailUtil{

    public static final void send(final EmailBean emailBean){
        String host = emailBean.getHost();
        Integer port = emailBean.getPort();
        String fromUrl = emailBean.getFromUrl();
        String fromName = emailBean.getFromName();
        String password = emailBean.getPassword();
        String[] targetUrls = emailBean.getTargetUrls();
        String title = emailBean.getTitle();
        String content = emailBean.getContent();
        String debug = emailBean.getDebug();
        Map<String, String> params  = emailBean.getParams();
        //
        Properties properties = System.getProperties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port.toString());
        properties.put("mail.smtp.auth", "true");
        if("yes".equals(debug)){
            properties.put("mail.debug", "true");
        }else if("no".equals(debug)){
            properties.put("mail.debug", "false");
        }
        //properties.put("mail.smtp.auth.mechanisms", "NTLM");  //非windows是digest
        //properties.put("mail.smtp.auth.ntlm.domain", host);
        Set<String> set = params.keySet();
        Iterator<String> iterator = set.iterator();
        while(iterator.hasNext()){
            String key = iterator.next();
            String value = params.get(key);
            properties.put(key, value);
        }
        //
        Session session = Session.getDefaultInstance(properties);
        if("yes".equals(debug)){
            session.setDebug(true);
        }else if("no".equals(debug)){
            session.setDebug(false);
        }
        for(String targetUrl : targetUrls){
            if(targetUrl == null || "".equals(targetUrl)){
                continue;
            }
            targetUrl = targetUrl.trim();
            if("".equals(targetUrl)){
                continue;
            }
            Transport transport = null;
            try{
                MimeMessage message = new MimeMessage(session);
                InternetAddress internetAddress = new InternetAddress(fromUrl);
                message.setFrom(internetAddress);
                InternetAddress internetAddress1 = new InternetAddress(targetUrl);
                message.addRecipient(RecipientType.TO, internetAddress1);
                message.setSubject(title);
                message.setText(content);
                message.saveChanges();
                transport = session.getTransport("smtp");
                transport.connect(host, fromName, password);
                Address[] addresses = message.getAllRecipients();
                transport.sendMessage(message, addresses);
            }catch(Throwable throwable){
                LogUtil.error("EmailUtil_send_t1", throwable, emailBean);
            }finally {
                if(transport != null){
                    try{
                        transport.close();
                    }catch(Throwable throwable){
                        LogUtil.error("EmailUtil_send_t2", throwable, emailBean);
                    }
                }
            }
        }
    }

    public static final void sendDingtalk(final EmailBean emailBean){
        String host = "smtp.yczqoffice.com";
        Integer port = 25;
        String fromUrl = emailBean.getFromUrl();
        String fromName = emailBean.getFromName();
        String password = emailBean.getPassword();
        String[] targetUrls = emailBean.getTargetUrls();
        String title = emailBean.getTitle();
        String content = emailBean.getContent();
        String debug = emailBean.getDebug();
        Map<String, String> params  = emailBean.getParams();
        //
        Properties properties = System.getProperties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port.toString());
        properties.put("mail.smtp.auth", "true");
        if("yes".equals(debug)){
            properties.put("mail.debug", "true");
        }else{
            properties.put("mail.debug", "false");
        }
        //properties.put("mail.smtp.auth.mechanisms", "NTLM");  //非windows是digest
        //properties.put("mail.smtp.auth.ntlm.domain", host);
        Set<String> set = params.keySet();
        Iterator<String> iterator = set.iterator();
        while(iterator.hasNext()){
            String key = iterator.next();
            String value = params.get(key);
            properties.put(key, value);
        }
        //
        Session session = Session.getDefaultInstance(properties);
        if("yes".equals(debug)){
            session.setDebug(true);
        }else if("no".equals(debug)){
            session.setDebug(false);
        }
        //
        for(String targetUrl : targetUrls){
            if(targetUrl == null || "".equals(targetUrl)){
                continue;
            }
            targetUrl = targetUrl.trim();
            if("".equals(targetUrl)){
                continue;
            }
            Transport transport = null;
            try{
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(fromUrl));
                message.addRecipient(RecipientType.TO, new InternetAddress(targetUrl));
                message.setSubject(title);
                message.setText(content);
                message.saveChanges();
                transport = session.getTransport("smtp");
                transport.connect(host, fromName, password);
                transport.sendMessage(message, message.getAllRecipients());
                transport.close();
            }catch(Throwable throwable){
                LogUtil.error("EmailUtil_sendDingtalk_t1", throwable, emailBean);
            }finally {
                if(transport != null){
                    try{
                        transport.close();
                    }catch(Throwable throwable){
                        LogUtil.error("EmailUtil_sendDingtalk_t2", throwable, emailBean);
                    }
                }
            }
        }
    }

    /**
     * 发送邮件
     * 163邮箱配置步骤：
     * 1.注册邮箱账号
     * 2.登陆邮箱
     * 3.点击顶部标题栏的设置按钮，选择 "POP3/SMTP/IMAP" 选项
     * 4.勾选 "POP3/SMTP服务" 和 "IMAP/SMTP服务"
     * 5.设置客户端授权密码，不同于登陆密码。
     * 客户端授权密码："**********"
     */
    public static final void send163(final EmailBean emailBean){
        String host = "smtp.163.com";
        Integer port = 25;
        String fromUrl = emailBean.getFromUrl();
        String fromName = emailBean.getFromName();
        String password = emailBean.getPassword();
        String[] targetUrls = emailBean.getTargetUrls();
        String title = emailBean.getTitle();
        String content = emailBean.getContent();
        String debug = emailBean.getDebug();
        Map<String, String> params  = emailBean.getParams();
        //
        Properties properties = System.getProperties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.auth", "true");
        if("yes".equals(debug)){
            properties.put("mail.debug", "true");
        }else{
            properties.put("mail.debug", "false");
        }
        //properties.put("mail.smtp.auth.mechanisms", "NTLM");  //非windows是digest
        //properties.put("mail.smtp.auth.ntlm.domain", host);
        Set<String> set = params.keySet();
        Iterator<String> iterator = set.iterator();
        while(iterator.hasNext()){
            String key = iterator.next();
            String value = params.get(key);
            properties.put(key, value);
        }
        //
        Session session = Session.getDefaultInstance(properties);
        if("yes".equals(debug)){
            session.setDebug(true);
        }else if("no".equals(debug)){
            session.setDebug(false);
        }
        //
        for(String targetUrl : targetUrls){
            if(targetUrl == null || "".equals(targetUrl)){
            continue;
            }
            targetUrl = targetUrl.trim();
            if("".equals(targetUrl)){
            continue;
            }
            Transport transport = null;
            try{
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(fromUrl));
                message.addRecipient(RecipientType.TO, new InternetAddress(targetUrl));
                message.setSubject(title);
                message.setText(content);
                message.saveChanges();
                transport = session.getTransport("smtp");
                transport.connect(host, fromName, password);
                transport.sendMessage(message, message.getAllRecipients());
                transport.close();
            }catch(Throwable throwable){
                LogUtil.error("EmailUtil_send163_t1", throwable, emailBean);
            }finally {
                if(transport != null){
                    try{
                        transport.close();
                    }catch(Throwable throwable){
                        LogUtil.error("EmailUtil_send163_t2", throwable, emailBean);
                    }
                }
            }
        }
    }

}
