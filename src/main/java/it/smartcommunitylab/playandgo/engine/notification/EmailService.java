package it.smartcommunitylab.playandgo.engine.notification;

import java.util.Locale;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    @Autowired 
    private JavaMailSender mailSender;
    
    @Autowired 
    private TemplateEngine templateEngine;
    
    @Autowired
    @Value("${mail.from}")
    private String mailFrom;
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
  

    public void sendGenericMail(String body, String subject, final String recipientName, final String recipientEmail, final Locale locale) throws MessagingException {
        
    	logger.debug(String.format("Gamification Generic Mail Prepare for %s - OK", recipientName));
    	
        // Prepare the evaluation context
        final Context ctx = new Context(locale);
        ctx.setVariable("name", recipientName);
        ctx.setVariable("body", body);
                
        // Prepare message using a Spring helper
        final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
        final MimeMessageHelper message = 
                new MimeMessageHelper(mimeMessage, true /* multipart */, "UTF-8");
        message.setSubject(subject);
        message.setFrom(mailFrom);
        message.setTo(recipientEmail.split(","));

        // Create the HTML body using Thymeleaf
        final String htmlContent = this.templateEngine.process("mail/email-generic-template", ctx);
        message.setText(htmlContent, true /* isHtml */);        
        
        // Send mail
        this.mailSender.send(mimeMessage);
        logger.debug(String.format("Gamification Generic Mail Sent to %s - OK", recipientName));
        
    }
    
}