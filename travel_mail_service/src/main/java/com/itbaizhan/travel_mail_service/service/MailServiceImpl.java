package com.itbaizhan.travel_mail_service.service;

import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import com.itbaizhan.travelcommon.service.MailService;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.apache.dubbo.config.annotation.DubboService;
import org.eclipse.angus.mail.util.MailSSLSocketFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Properties;

@DubboService
@Service
public class MailServiceImpl implements MailService {
    @Value("${mail.user}")
    private String USER;
    @Value("${mail.password}")
    private String PASSWORD;

    @Override
    public BaseResult<?> sendMail(String to, String text, String title) {
        try {
            final Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", "smtp.qq.com");
            props.put("mail.smtp.port", "465");          // 关键：指定 SSL 端口
            // 发件人的账号
            props.put("mail.user", USER);
            // 发件人的密码
            props.put("mail.password", PASSWORD);
            // 开启SSL加密
            props.put("mail.smtp.ssl.enable",true);
            MailSSLSocketFactory sslSocketFactory = new MailSSLSocketFactory();
            sslSocketFactory.setTrustAllHosts(true);
            props.put("mail.smtp.ssl.socketFactory",sslSocketFactory);

            //props.put("mail.debug", "true");


            // 构建授权信息，用于进行SMTP进行身份验证
            Authenticator authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    // 用户名、密码
                    String userName = props.getProperty("mail.user");
                    String password = props.getProperty("mail.password");
                    return new PasswordAuthentication(userName, password);
                }
            };
            // 使用环境属性和授权信息，创建邮件会话
            Session mailSession = Session.getInstance(props, authenticator);
            // 创建邮件消息
            MimeMessage message = new MimeMessage(mailSession);
            // 设置发件人
            String username = props.getProperty("mail.user");
            InternetAddress form = new InternetAddress(username);
            message.setFrom(form);

            //mailSession.setDebug(true);

            // 设置收件人
            InternetAddress toAddress = new InternetAddress(to);
            message.setRecipient(Message.RecipientType.TO, toAddress);
            // 设置邮件标题
            message.setSubject(title);
            // 设置邮件的内容体
            message.setContent(text, "text/html;charset=UTF-8");

            // 发送邮件
            Transport.send(message);
            return BaseResult.success();
        }catch (Exception e){
            e.printStackTrace();
            throw new BusException(CodeEnum.MAIL_SEND_ERROR);
        }
    }
}