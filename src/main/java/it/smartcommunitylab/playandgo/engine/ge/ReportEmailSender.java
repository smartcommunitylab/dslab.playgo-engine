package it.smartcommunitylab.playandgo.engine.ge;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import it.smartcommunitylab.playandgo.engine.campaign.city.CityGameDataConverter;
import it.smartcommunitylab.playandgo.engine.ge.model.BadgeCollectionConcept;
import it.smartcommunitylab.playandgo.engine.ge.model.BadgesData;
import it.smartcommunitylab.playandgo.engine.ge.model.MailImage;
import it.smartcommunitylab.playandgo.engine.ge.model.PlayerStatus;
import it.smartcommunitylab.playandgo.engine.ge.model.PointConcept;
import it.smartcommunitylab.playandgo.engine.ge.model.PointConceptPeriod;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeConceptInfo;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeConceptInfo.ChallengeDataType;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengesData;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.CampaignWeekConf;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.notification.EmailService;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.util.Utils;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
@EnableScheduling
public class ReportEmailSender {
	private static final Logger logger = LoggerFactory.getLogger(ReportEmailSender.class);
	
	private static final String IMAGE_PNG = "image/png";
	private static final long MILLIS_IN_WEEK = 1000L * 60 * 60 * 24 * 7;
	private static final String UNSUBSCRIBE_URL = "%s/unsubscribeMail/%s";
	
	@Value("${playgoURL}")
	private String playgoURL;

	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	CampaignSubscriptionRepository campaignSubscriptionRepository;
	
	@Autowired
	PlayerRepository playerRepository;
	
	@Autowired
	CityGameDataConverter gameDataConverter;
	
	@Autowired
	GamificationEngineManager gamificationEngineManager;

	@Autowired
	EmailService emailService;
	
	@Scheduled(cron="0 0 17 * * SUN")
	@SchedulerLock(name = "ReportEmailSender.sendWeeklyNotification")
	public void sendWeeklyNotification() {
		List<Campaign> list = campaignRepository.findByType(Campaign.Type.city, Sort.by(Sort.Direction.DESC, "dateFrom"));
		for(Campaign campaign : list) {
			if(campaign.currentlyActive() && campaign.getCommunications()) {
				try {
					sendWeeklyNotification(campaign);
				} catch (Exception e) {
					logger.error("sendWeeklyNotification:" + e.getMessage(), e);
				}
			}
		}
	}
	
	private boolean nextWeek(CampaignWeekConf conf) {
		Date utcDate = Utils.getUTCDate(System.currentTimeMillis() + MILLIS_IN_WEEK);
		return !utcDate.before(conf.getDateFrom()) && !utcDate.after(conf.getDateTo());
	}	
	
	private boolean currentWeek(CampaignWeekConf conf) {
		Date utcDate = Utils.getUTCDate(System.currentTimeMillis());
		return !utcDate.before(conf.getDateFrom()) && !utcDate.after(conf.getDateTo());
	}
	
	private boolean isLastWeek(Campaign campaign, CampaignWeekConf conf) {
		return conf.getWeekNumber() == (campaign.getWeekConfs().size() + 1);
	}
	
	private String createUnsubscribeUrl(String playerId, String gameId) throws Exception {
		String id = gameDataConverter.encryptIdentity(playerId, gameId);
		String compileSurveyUrl = String.format(UNSUBSCRIBE_URL, playgoURL, id);
		return compileSurveyUrl;
	}
	
	private List<BadgesData> filterBadges(List<BadgesData> allB, PlayerStatus status) throws IOException {
		List<BadgesData> correctBadges = Lists.newArrayList();
		for (BadgeCollectionConcept collection: status.getBadgeCollectionConcept()) {
			if (collection.getBadgeEarned() != null) {
				List<String> badgeNames = collection.getBadgeEarned().stream().map(x -> x.getName()).collect(Collectors.toList());
				correctBadges.addAll(allB.stream().filter(x -> badgeNames.contains(x.getTextId())).collect(Collectors.toList()));
			}
		}
		return correctBadges;
	}	
	
	private void sendWeeklyNotification(Campaign campaign) throws Exception {
		String gameId = campaign.getGameId();
		
		List<MailImage> standardImages = Lists.newArrayList();

		standardImages.add(new MailImage("foglie03", Resources.asByteSource(getClass().getResource("/static/web/img/mail/foglie03.png")).read(), IMAGE_PNG));
		standardImages.add(new MailImage("foglie04", Resources.asByteSource(getClass().getResource("/static/web/img/mail/foglie04.png")).read(), IMAGE_PNG));
		standardImages.add(new MailImage("greenScore", Resources.asByteSource(getClass().getResource("/static/web/img/mail/green/greenLeavesbase.png")).read(), IMAGE_PNG));
		standardImages.add(new MailImage("healthScore", Resources.asByteSource(getClass().getResource("/static/web/img/mail/health/healthLeavesBase.png")).read(), IMAGE_PNG));
		standardImages.add(new MailImage("prScore", Resources.asByteSource(getClass().getResource("/static/web/img/mail/pr/prLeaves.png")).read(), IMAGE_PNG));
		standardImages.add(new MailImage("footer", Resources.asByteSource(getClass().getResource("/static/web/img/mail/templateMail.png")).read(), IMAGE_PNG));

		CampaignWeekConf nextWeekConfData = campaign.getWeekConfs().stream().filter(x -> nextWeek(x)).findFirst().orElse(new CampaignWeekConf());
		CampaignWeekConf currentWeekConfData = campaign.getWeekConfs().stream().filter(x -> currentWeek(x)).findFirst().orElse(new CampaignWeekConf());
		
		List<CampaignSubscription> subList = campaignSubscriptionRepository.findByCampaignId(campaign.getCampaignId());
		
		logger.info("Sending notifications for game " + gameId);

		for (CampaignSubscription cs : subList) {
			Player p = playerRepository.findById(cs.getPlayerId()).orElse(null);
			
			logger.info("Sending notifications to " + p.getNickname());
			logger.debug(String.format("Profile found  %s", p.getNickname()));

			if (!cs.getSendMail() || !p.getSendMail()) {
				logger.info("Mail non inviata a " + p.getNickname() + ". L'utente ha richiesto la disattivazione delle notifiche.");
				continue;
			}

			String unsubcribeLink = createUnsubscribeUrl(p.getPlayerId(), gameId);
//			List<Notification> notifications = null;
			List<BadgesData> someBadge = null;
			List<ChallengesData> challenges = null;
			List<ChallengesData> lastWeekChallenges = null;

			String completeState = gamificationEngineManager.getGameStatus(p.getPlayerId(), gameId);
			
			String language = p.getLanguage();

			PlayerStatus completePlayerStatus = gameDataConverter.convertPlayerData(completeState, p.getPlayerId(), gameId, p.getNickname(), 0, language);
			List<PointConcept> states = completePlayerStatus.getPointConcept();
			double point_green = 0;
			double point_green_w = 0;
			if (states != null && states.size() > 0) {
				//TODO timezone conversion?
				point_green = (double)states.get(0).getScore();
				LocalDate cws = currentWeekConfData.getDateFrom().toInstant().atZone(ZoneId.of("Z")).toLocalDate();
				LocalDate cwe = currentWeekConfData.getDateTo().toInstant().atZone(ZoneId.of("Z")).toLocalDate();

				PointConceptPeriod pcp = states.get(0).getInstances().stream().filter(x -> {
					LocalDate ws = Instant.ofEpochMilli(x.getStart()).atZone(ZoneId.systemDefault()).toLocalDate();
					LocalDate we = Instant.ofEpochMilli(x.getEnd()).atZone(ZoneId.systemDefault()).toLocalDate();
					return (cwe.compareTo(we) <= 0) && (cws.compareTo(ws) == 0);
				}).findFirst().orElse(null);

				if (pcp != null) {
					point_green_w = (double)pcp.getScore();
				}
			}
			
			ChallengeConceptInfo challLists = completePlayerStatus.getChallengeConcept();

			if (challLists != null) {
				challenges = challLists.getChallengeData().get(ChallengeDataType.ACTIVE);
				if (challenges != null) {
					challenges = challenges.stream().filter(x -> !x.getSuccess().booleanValue()).collect(Collectors.toList());
				}
				lastWeekChallenges = challLists.getChallengeData().get(ChallengeDataType.OLD);
			}

			List<BadgesData> allBadge = gameDataConverter.getAllBadges(campaign);
			someBadge = filterBadges(allBadge, completePlayerStatus);
			
			String mailto = null;
			mailto = p.getMail();
			String playerName = p.getNickname();
			if (mailto == null || mailto.isEmpty()) {
				continue;
			}

			if (playerName != null && !playerName.isEmpty()) {
				try {
					this.emailService.sendWeeklyNotificationMail(playerName, point_green, point_green_w, nextWeekConfData, 
							isLastWeek(campaign, nextWeekConfData), someBadge, challenges, lastWeekChallenges, standardImages, mailto,
							unsubcribeLink, campaign.getName().get(language), language);
				} catch (MessagingException e) {
					logger.error(String.format("Errore invio mail : %s", e.getMessage()));
				}
			} 
		}		
	}
}
