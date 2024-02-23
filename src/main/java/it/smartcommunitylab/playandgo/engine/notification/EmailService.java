package it.smartcommunitylab.playandgo.engine.notification;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import it.smartcommunitylab.playandgo.engine.ge.model.BadgesData;
import it.smartcommunitylab.playandgo.engine.ge.model.MailImage;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengesData;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;
import it.smartcommunitylab.playandgo.engine.model.CampaignWeekConf;

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
  	
    @PostConstruct
	public void init() {
		templateEngine.addTemplateResolver(new StringTemplateResolver());
	}

    public void sendGenericMail(String body, String subject, final String recipientName, final String recipientEmail, 
    		final Locale locale) throws MessagingException {
        
    	logger.info(String.format("Gamification Generic Mail Prepare for %s - OK (%s)", recipientName, mailFrom));
    	
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
    
    public void sendWeeklyNotificationMail(final String recipientName, final double point_green, final double point_green_w, 
    		CampaignWeekConf weekConf, final boolean isLastWeek, final List<BadgesData> badges, final List<ChallengesData> challenges,
    		final List<ChallengesData> last_week_challenges, final List<MailImage> standardImages, final String recipientEmail,
    		final String unsubscribtionLink, String campaignTitle, String lang) throws Exception {
    	logger.debug(String.format("Gamification Mail Prepare for %s - OK", recipientName));
    	
    	List<ChallengesData> winChallenges = Lists.newArrayList();
    	if(last_week_challenges != null){
	    	last_week_challenges.stream().filter(x -> x.getSuccess()).collect(Collectors.toList());
    	}
    	
    	String challengesStartingTime = "";
    	String challengesEndingTime = "";
    	String challengesStartingDate = "";
    	String challengesEndingDate = "";
    	Date ch_startTime = null;
    	Date ch_endTime = null;
    	SimpleDateFormat dt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    	if(challenges != null && challenges.size() > 0){
    		long startTime = challenges.get(0).getStartDate();
    		long endTime = challenges.get(0).getEndDate();
    		ch_startTime = new Date(startTime);
    		ch_endTime = new Date(endTime);
    		String challStartingAll = dt.format(ch_startTime);
    		String challEndingAll = dt.format(ch_endTime);
    		String[] completeStart = challStartingAll.split(" ");
    		String[] completeEnd = challEndingAll.split(" ");
    		challengesStartingDate = completeStart[0];
    		challengesStartingTime = completeStart[1];
    		challengesEndingDate = completeEnd[0];
    		challengesEndingTime = completeEnd[1];
    	}

        // Prepare the evaluation context
    	Locale locale = Locale.forLanguageTag(lang);
        final Context ctx = new Context(locale);
        ctx.setVariable("lang", lang);
        ctx.setVariable("name", recipientName);
        ctx.setVariable("g_point", point_green);
        ctx.setVariable("g_point_w", point_green_w);
        ctx.setVariable("n_badges", badges);
        ctx.setVariable("next_week_num", weekConf.getWeekNumber());
        ctx.setVariable("show_last_week", isLastWeek);
        ctx.setVariable("week_num", weekConf.getWeekNumber() - 1);
        ctx.setVariable("n_challenges", challenges);
        ctx.setVariable("n_lw_challenges", last_week_challenges);
        ctx.setVariable("n_lw_win_challenges", winChallenges);
        ctx.setVariable("chs_start_date", challengesStartingDate);
        ctx.setVariable("chs_start_time", challengesStartingTime);
        ctx.setVariable("chs_end_date", challengesEndingDate);
        ctx.setVariable("chs_end_time", challengesEndingTime);
        ctx.setVariable("n_prizes", weekConf.getRewards());
        ctx.setVariable("unsubscribtionLink", unsubscribtionLink);
        ctx.setVariable("imageRNFoglie03", standardImages.get(0).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageRNFoglie04", standardImages.get(1).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageRNGreenScore", standardImages.get(2).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageRNHealthScore", standardImages.get(3).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageRNPrScore", standardImages.get(4).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageResourceName", standardImages.get(5).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("appTitle", campaignTitle);

        final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
        final MimeMessageHelper message = 
                new MimeMessageHelper(mimeMessage, true /* multipart */, "UTF-8");
        message.setSubject("Play&Go - Notifica");
        message.setFrom(mailFrom);
        message.setTo(recipientEmail);

        // Create the HTML body using Thymeleaf
        String htmlContent = (locale == Locale.ITALIAN) ? this.templateEngine.process("mail/email-gamification-it", ctx) 
        		: this.templateEngine.process("mail/email-gamification-en", ctx);
        message.setText(htmlContent, true /* isHtml */);
        
        final InputStreamSource imageSourceFoglia03 = new ByteArrayResource(standardImages.get(0).getImageByte());
        message.addInline(standardImages.get(0).getImageName(), imageSourceFoglia03, standardImages.get(0).getImageType());
        final InputStreamSource imageSourceFoglia04 = new ByteArrayResource(standardImages.get(1).getImageByte());
        message.addInline(standardImages.get(1).getImageName(), imageSourceFoglia04, standardImages.get(1).getImageType());
        
        final InputStreamSource imageSourceGreen = new ByteArrayResource(standardImages.get(2).getImageByte());
        message.addInline(standardImages.get(2).getImageName(), imageSourceGreen, standardImages.get(2).getImageType());
        
        Set<String> badgeImages = Sets.newHashSet();
        if(badges != null){
        	// Add the inline images for badges
	        for(BadgesData bd: badges){
	        	String imgName = bd.getImageName();
	        	if (badgeImages.contains(imgName)) {
	        		continue;
	        	}
	        	final InputStreamSource tmp = new ByteArrayResource(bd.getImageByte());
	            message.addInline(imgName, tmp, bd.getImageType());
	            badgeImages.add(imgName);
	        }
        }
        
        // Send mail
        this.mailSender.send(mimeMessage);
        logger.info(String.format("Gamification Mail Sent to %s - OK", recipientName));
    }
    
    public void sendSurveyInvite(String link, String campaignName, String email, String lang) throws MessagingException {
        logger.info(String.format("sendSurveyInvite to %s", email));
        
        // Prepare the evaluation context
        final Context ctx = new Context();
        ctx.setVariable("surveylink", link);
        ctx.setVariable("campaignTitle", campaignName);
        ctx.setVariable("name", email);
                
        // Prepare message using a Spring helper
        final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
        final MimeMessageHelper message = 
                new MimeMessageHelper(mimeMessage, true /* multipart */, "UTF-8");
        message.setSubject("Play&Go - Notifica");
        message.setFrom(mailFrom);
        message.setTo(email);

        // Create the HTML body using Thymeleaf
        String htmlContent = (lang.equals("it")) ? this.templateEngine.process("mail/email-survey-it", ctx) 
                : this.templateEngine.process("mail/email-survey-en", ctx);
        message.setText(htmlContent, true /* isHtml */);
        
        // Send mail
        this.mailSender.send(mimeMessage);
    }

    public void sendSurveyInvite(String link, String campaignName, String email, String lang,
        String subject, String template) throws MessagingException {
            logger.info(String.format("sendSurveyInvite to %s", email));
        
            // Prepare the evaluation context
            final Context ctx = new Context();
            ctx.setVariable("surveylink", link);
            ctx.setVariable("campaignTitle", campaignName);
            ctx.setVariable("name", email);
                    
            // Prepare message using a Spring helper
            final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
            final MimeMessageHelper message = 
                    new MimeMessageHelper(mimeMessage, true /* multipart */, "UTF-8");
            message.setSubject(subject);
            message.setFrom(mailFrom);
            message.setTo(email);
    
            // Create the HTML body using Thymeleaf
            String htmlContent = this.templateEngine.process(template, ctx);
            message.setText(htmlContent, true /* isHtml */);
            
            // Send mail
            this.mailSender.send(mimeMessage);        
    }
    
}