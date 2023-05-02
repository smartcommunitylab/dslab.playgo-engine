package it.smartcommunitylab.playandgo.engine.campaign.school;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.mq.GamificationMessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.ManageGameNotification;
import it.smartcommunitylab.playandgo.engine.notification.CampaignNotificationManager;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.util.JsonUtils;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class SchoolCampaignGameNotification implements ManageGameNotification {
	private static Logger logger = LoggerFactory.getLogger(SchoolCampaignGameNotification.class);
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	CampaignSubscriptionRepository campaignSubscriptionRepository;
	
	@Autowired
	PlayerRepository playerRepository;
	
	@Autowired
	GamificationMessageQueueManager gamificationMessageQueueManager;
	
	@Autowired
	CampaignNotificationManager notificationManager;
	
	@Autowired
	SchoolCampaignGameStatusManager gameStatusManager;
	
	@EventListener(ContextRefreshedEvent.class)
	public void init() {
		gamificationMessageQueueManager.setManageGameNotification(this, Type.school);
		List<Campaign> list = campaignRepository.findByType(Type.school, Sort.by(Sort.Direction.DESC, "dateFrom"));
        List<String> gameIds = list.stream().filter(c -> Utils.isNotEmpty(c.getGameId())).map(c -> c.getGameId()).collect(Collectors.toList());
        if(!gameIds.isEmpty()) {
            gamificationMessageQueueManager.addGameQueue(gameIds);
            logger.info(String.format("campaigns type %s subscribes to games", Type.school));
        }
	}
	
	public void subcribeCampaing(Campaign c) {
        if(Utils.isNotEmpty(c.getGameId())) {
            gamificationMessageQueueManager.addGameQueue(Arrays.asList(c.getGameId()));
            logger.info(String.format("campaign %s subscribe to game %s", c.getCampaignId(), c.getGameId()));                   
        }
	}
	
	@SuppressWarnings("unchecked")
    @Override
	public void manageGameNotification(Map<String, Object> msg, String routingKey) {
		String type = (String) msg.get("type");
		if(type.endsWith("GameNotification")) {
			gameStatusManager.updatePlayerGameStatus(msg);
		} else {
		    //check if is a group notification
		    Map<String, Object> obj = (Map<String, Object>) msg.get("obj");
		    String playerId = (String) obj.get("playerId");
		    try {
			    Player player = playerRepository.findById(playerId).orElse(null);
			    if(player != null) {
			        if(player.getGroup()) {
			            String gameId = (String) obj.get("gameId");
			            Campaign campaign = campaignRepository.findByGameId(gameId);
			            if(campaign != null) {
			                String json = JsonUtils.toJSON(msg);
			                List<CampaignSubscription> list = campaignSubscriptionRepository.findByMetaData(campaign.getCampaignId(), 
			                        SchoolCampaignSubscription.groupIdKey, playerId);
			                list.forEach(cs -> {
			                    try {
	                                Map<String, Object> copyMsg = JsonUtils.toMap(json);
	                                if(copyMsg != null) {
	                                    Map<String, Object> copyObj = (Map<String, Object>) copyMsg.get("obj"); 
	                                    copyObj.put("playerId", cs.getPlayerId());
	                                    notificationManager.processNotification(copyMsg);	                                    
	                                }
                                } catch (Exception e) {
                                    logger.warn(String.format("manageGameNotification group error:%s - %s", routingKey, e.getMessage())); 
                                }
			                });
			            }
			        } else {
			            notificationManager.processNotification(msg); 
			        }
			    }
			} catch (Exception e) {
				logger.error(String.format("manageGameNotification error:%s - %s", routingKey, e.getMessage()));
			}					
		}
	}
	
}
