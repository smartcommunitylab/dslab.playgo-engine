package it.smartcommunitylab.playandgo.engine.notification;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stringtemplate.v4.ST;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import it.smartcommunitylab.playandgo.engine.ge.BadgeManager;
import it.smartcommunitylab.playandgo.engine.ge.model.BadgesData;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.repository.TerritoryRepository;


@Component
public class CampaignNotificationManager {

	@SuppressWarnings("rawtypes")
	private static final List<Class> notificationClasses = Lists.newArrayList(new Class[] 
	{ LevelGainedNotification.class, ChallengeInvitationAcceptedNotification.class, ChallengeInvitationRefusedNotification.class, ChallengeInvitationCanceledNotification.class,
		ChallengeAssignedNotification.class, ChallengeCompletedNotication.class, ChallengeFailedNotication.class, BadgeNotification.class });
	@SuppressWarnings("rawtypes")
	private Map<String, Class> notificationClassesMap;
	
	private static transient final Logger logger = LoggerFactory.getLogger(CampaignNotificationManager.class);
	
	@Value("${notificationDir}")
	private String notificationDir;

	@Autowired
	PlayerRepository playerRepository;
	
	@Autowired
	TerritoryRepository territoryRepository;
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	CampaignSubscriptionRepository campaignSubscriptionRepository;
	
	@Autowired
	private BadgeManager badgeManager;

	@Autowired
	private CommunicationHelper notificatioHelper;

	private ObjectMapper mapper = new ObjectMapper(); {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}	
	
	private Map<String, NotificationMessage> notificationsMessages;
	private Map<String, BadgesData> badges;
	
	@PostConstruct
	public void init() throws Exception {
		notificationClassesMap = Maps.newTreeMap();
		notificationClasses.forEach(x -> {
			notificationClassesMap.put(x.getSimpleName(), x);
		});
		List<NotificationMessage> messages = mapper.readValue(new File(notificationDir + "/personal_notifications.json"), new TypeReference<List<NotificationMessage>>() {
		});
		notificationsMessages = messages.stream().collect(Collectors.toMap(NotificationMessage::getId, Function.identity()));
		logger.info("init:" + notificationsMessages);
		
		badges = badgeManager.getAllBadges();
	}
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void processNotification(Map<String, Object> map) throws Exception {
		String type = (String) map.get("type");
		Map obj = (Map) map.get("obj");
		if (type == null || obj == null) {
			logger.error("Bad notification content: " + map);
			return;
		}

		NotificationGe not = null;
		
		Optional<String> opt = notificationClassesMap.keySet().stream().filter(x -> type.contains(x)).findFirst();
		if (opt.isPresent()) {
			Class clz = notificationClassesMap.get(opt.get());
			not = (NotificationGe) mapper.convertValue(obj, clz);
		} else {
		    if(type.endsWith("MessageNotification")) {
		        MessageNotification messageNotification = mapper.convertValue(obj, MessageNotification.class);
		        not = (NotificationGe) messageNotification;
	        }
	    }
		if(not == null) {
		    logger.error("Bad notification type: " + type);
		    return;
		}
	
		Player p = playerRepository.findById(not.getPlayerId()).orElse(null);	
		if ((p != null) && (!p.getDeleted())) {
			Territory territory = territoryRepository.findById(p.getTerritoryId()).orElse(null);
			if(territory != null) {
				Campaign campaign = campaignRepository.findByGameId(not.getGameId());
				if(campaign != null) {
				    CampaignSubscription cs = campaignSubscriptionRepository.findByCampaignIdAndPlayerId(campaign.getCampaignId(), p.getPlayerId());
				    if(cs != null) {
				        Notification notification = null;
	                    
	                    try {
	                        notification = buildNotification(campaign.getCampaignId(), not.getGameId(), p.getPlayerId(), p.getLanguage(), not);
	                    } catch (Exception e) {
	                        logger.error("Error building notification", e);
	                    }
	                    if (notification != null) {
	                            try {
	                                logger.info("Sending '" + not.getClass().getSimpleName() + "' notification to " + not.getPlayerId() + " (" + territory.getTerritoryId() + "):" 
	                                        + mapper.writeValueAsString(notification));
	                                notificatioHelper.notify(notification, not.getPlayerId(), territory.getTerritoryId(), campaign.getCampaignId(), true);
	                            } catch (Exception e) {
	                                logger.warn("Error sending notification:" + e.getMessage());
	                            }
	                    }                                           				        
				    } else {
				        logger.warn("Player " + not.getPlayerId() + " not subscribed to campaign " + campaign.getCampaignId()); 
				    }
				} else {
					logger.warn("Game " + not.getGameId() + " campaign not found");
				}
			} else {
				logger.warn("Player " + not.getPlayerId() + " territory not found");
			}
		} else {
			logger.warn("Player " + not.getPlayerId() + " not found");
		}
	}
	
	public void sendDirectNotification(String playerId, String campaignId, String type, Map<String, String> extraData) {
		Player player = playerRepository.findById(playerId).orElse(null);
		if(player != null) {
			Notification notification = buildSimpleNotification(player.getLanguage(), type, extraData);
			try {
				notificatioHelper.notify(notification, playerId, player.getTerritoryId(), campaignId, true);
			} catch (Exception e) {
				logger.warn(String.format("sendDirectNotification error: %s - %s - %s - %s", 
						playerId, campaignId, type, e.getMessage()));
			}
		}
		
	}
	
	private Notification buildNotification(String campaignId, String gameId, String playerId, String lang, NotificationGe not) {
	    String type = not.getClass().getSimpleName();
	    if(not instanceof MessageNotification) {
	        type = ((MessageNotification)not).getKey();
	    }
	    logger.debug("buildNotification type:" + type);
	    
	    if(type == null) return null;
		
		Map<String, String> extraData = buildExtraData(not, type, lang);
		logger.debug("buildExtraData:" + extraData);
		
		Notification result = new Notification();
		
		NotificationMessage message = notificationsMessages.get(type);
		logger.debug("NotificationMessage:" + message);
			
		fillNotification(result, lang, message, extraData);
		return result;
	}
	
	private Notification buildSimpleNotification(String lang, String type, Map<String, String> extraData) {
		Notification result = new Notification();
		
		NotificationMessage message = notificationsMessages.get(type);
			
		fillNotification(result, lang, message, extraData);
		
		return result;
	}	
	
	private Map<String, String> buildExtraData(NotificationGe not, String type, String lang) {
		Map<String, String> result = Maps.newTreeMap();

		switch (type) {
			case "LevelGainedNotification": {
				result.put("levelName", ((LevelGainedNotification) not).getLevelName());
				result.put("levelIndex", ((LevelGainedNotification) not).getLevelIndex() != null ? ((LevelGainedNotification) not).getLevelIndex().toString() : "");
				break;
			}
			case "ChallengeInvitationAcceptedNotification": {
				Player guest = playerRepository.findById(((ChallengeInvitationAcceptedNotification) not).getGuestId()).orElse(null);
				result.put("assigneeName", guest.getNickname());
				break;
			}
			case "ChallengeInvitationRefusedNotification": {
				Player guest = playerRepository.findById(((ChallengeInvitationRefusedNotification) not).getGuestId()).orElse(null);
				result.put("assigneeName", guest.getNickname());
				break;
			}
			case "ChallengeInvitationCanceledNotification": {
				Player proposer = playerRepository.findById(((ChallengeInvitationCanceledNotification) not).getProposerId()).orElse(null);
				result.put("challengerName", proposer.getNickname());
				break;
			}
			case "ChallengeAssignedNotification":
				result.put("challengeId", ((ChallengeAssignedNotification)not).getChallengeName());
				break;		
			case "ChallengeCompletedNotication":
				result.put("challengeId", ((ChallengeCompletedNotication)not).getChallengeName());
				break;
			case "ChallengeFailedNotication": {
				result.put("challengeId", ((ChallengeFailedNotication)not).getChallengeName());
				break;
			}
			case "BadgeNotification": {
				String badge = ((BadgeNotification)not).getBadge();
				BadgesData badgesData = badges.get(badge);
				if(badgesData != null) {
					result.put("badgeName", badgesData.getText().get(lang));
				}
				break;
			}
			case "HSCRegistrationBonus": {
			    String playerId = (String) ((MessageNotification)not).getData().get("registeredPlayerId");
			    Player player = playerRepository.findById(playerId).orElse(null);
			    if(player != null) {
			        result.put("nickname", player.getNickname());
			        result.put("points", String.valueOf(((MessageNotification)not).getData().get("points")));
			    }
			    break;
			}
		}
		return result;
	}
	
	private void fillNotification(Notification notification, String lang, NotificationMessage message, Map<String, String> extraData) {
		if (message != null) {
			notification.setTitle(message.getTitle().get(lang));
			notification.setDescription(fillDescription(lang, message, extraData));
			Map<String, Object> content = Maps.newTreeMap();
			content.put("type", message.getType());
			notification.setContent(content);
		}
	}
	
	private String fillDescription(String lang, NotificationMessage message, Map<String, String> extraData) {
		StringBuilder descr = new StringBuilder(message.getDescription().get(lang));
		String result = null;

		if (message.getExtras() != null && message.getExtras().get(lang) != null && extraData != null) {

			List<NotificationMessageExtra> extras = message.getExtras().get(lang);

			List<NotificationMessageExtra> append = extras.stream().filter(x -> "APPEND".equals(x.getType())).collect(Collectors.toList());

			for (NotificationMessageExtra extra : append) {
				boolean ok = true;
				if (extra.getValue() != null) {
					String keyValue = extraData.get(extra.getKey());
					if (keyValue != null && !keyValue.equals(extra.getValue())) {
						ok = false;
					}
				}
				if (ok) {
					descr.append(extra.getString());
				}
			}

			ST st = new ST(descr.toString());

			List<NotificationMessageExtra> replace = extras.stream().filter(x -> "REPLACE".equals(x.getType())).collect(Collectors.toList());

			for (NotificationMessageExtra extra : replace) {
				boolean ok = true;
				if (extra.getValue() != null) {
					String keyValue = extraData.get(extra.getKey());
					if (keyValue != null && !keyValue.equals(extra.getKey())) {
						ok = false;
					}
				}
				if (ok) {
					st.add(extra.getKey(), extraData.get(extra.getString()));
				}
			}

			result = st.render();
		} else {
			result = descr.toString();
		}

		return result;
	}
	
}
