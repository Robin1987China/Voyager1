/*
 * Copyright (c) 2026 Voyager1
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.voyager1.util;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.util.Collection;
import java.util.Date;
import java.util.Properties;

/**
 * 邮件工具，"" {@code .extra.mail.MailUtil}。
 */
public class MailUtil {

    public static Session getSession(MailAccount account, boolean isSingleton) {
        Properties props = new Properties();
        if (account.getHost() != null) {
            props.put("mail.smtp.host", account.getHost());
        }
        if (account.getPort() != null) {
            props.put("mail.smtp.port", account.getPort());
        }
        props.put("mail.smtp.auth", String.valueOf(account.isAuth()));
        if (account.getTimeout() > 0) {
            props.put("mail.smtp.timeout", account.getTimeout());
        }
        if (account.getConnectionTimeout() > 0) {
            props.put("mail.smtp.connectiontimeout", account.getConnectionTimeout());
        }
        if (account.isSslEnable()) {
            props.put("mail.smtp.ssl.enable", "true");
            if (account.getSocketFactoryPort() != null) {
                props.put("mail.smtp.socketFactory.port", account.getSocketFactoryPort());
            }
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }

        final String user = account.getUser();
        final String pass = account.getPass();
        Authenticator authenticator = null;
        if (account.isAuth() && user != null) {
            authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass == null ? "" : pass);
                }
            };
        }
        return Session.getInstance(props, authenticator);
    }

    public static String send(MailAccount account, Collection<String> tos, String subject, String content, boolean isHtml) {
        try {
            Session session = getSession(account, false);
            MimeMessage message = new MimeMessage(session);
            String from = account.getFrom();
            if (from == null || from.isEmpty()) {
                from = account.getUser();
            }
            message.setFrom(new InternetAddress(from));
            for (String to : tos) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(to.trim()));
            }
            message.setSubject(subject, account.getCharset());
            if (isHtml) {
                message.setContent(content, "text/html;charset=" + account.getCharset());
            } else {
                message.setText(content, account.getCharset());
            }
            message.setSentDate(new Date());
            Transport.send(message);
            return message.getMessageID();
        } catch (MessagingException e) {
            throw new MailException("发送邮件失败", e);
        }
    }
}
