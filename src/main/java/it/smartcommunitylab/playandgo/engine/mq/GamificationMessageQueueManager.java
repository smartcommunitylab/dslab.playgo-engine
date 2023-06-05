package it.smartcommunitylab.playandgo.engine.mq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class GamificationMessageQueueManager {
	private static transient final Logger logger = LoggerFactory.getLogger(GamificationMessageQueueManager.class);
	
	@Value("${rabbitmq_ge.geExchangeName}")
	private String geExchangeName;
	
	@Value("${rabbitmq_ge.geRoutingKeyPrefix}")
	private String geRoutingKeyPrefix;	
    
	@Autowired
	CampaignRepository campaignRepository;
	
    @Autowired
    RabbitListenerEndpointRegistry listenerEdnpointRegistry;
    
    @Autowired
    RabbitAdmin rabbitAdmin;
    
    @Autowired
    @Qualifier("gameExchange")
    DirectExchange gameExchange;
	
	ObjectMapper mapper = new ObjectMapper();
	
	Map<String, ManageGameNotification> manageGameNotificationMap = new HashMap<>();
	
	List<String> gameList = new ArrayList<>();
    
	private void addNewQueueToExchange(String queueName, String routingKey) throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("x-queue-type", "quorum");
        Queue queue = new Queue(queueName, true, false, false, args);
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(gameExchange).with(routingKey));
    }
    
    private void addQueueToListener(String listenerId, String... queueNames) throws Exception {
        AbstractMessageListenerContainer listener = (AbstractMessageListenerContainer)listenerEdnpointRegistry.getListenerContainer(listenerId);
        listener.addQueueNames(queueNames);
    }
	
	public void addGameQueue(List<String> gameIds) {
	    List<String> newGames = new ArrayList<>();
	    for(String gameId : gameIds) {
	        try {
                String queueName = "queue-" + gameId;
                String routingKey = geRoutingKeyPrefix + "-" + gameId;
	            if(!gameList.contains(queueName)) {
	                addNewQueueToExchange(queueName, routingKey);
	                gameList.add(queueName);
	                newGames.add(queueName);
	            }
            } catch (Exception e) {
                logger.warn(String.format("addGameQueue[%s]:%s", gameId, e.getMessage())); 
            }
	    }
	    if(newGames.size() > 0) {
	        try {
	            addQueueToListener("gameListener", newGames.toArray(new String[0]));
	            logger.info(String.format("addGameQueue:%s", newGames.toString()));
            } catch (Exception e) {
                logger.error(String.format("addGameQueue - start listener:%s", e.getMessage()));
            }
	    }
    }
	
	@RabbitListener(id = "gameListener", concurrency = "10")
	public void onMessage(Message delivery) {
        try {
            String msg = new String(delivery.getBody(), "UTF-8");
            logger.info("gameNotificationCallback:" + msg);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) mapper.readValue(msg, Map.class);
            String type = (String) map.get("type");
            String gameId = null;
            @SuppressWarnings("unchecked")
            Map<String, Object> obj = (Map<String, Object>) map.get("obj");
            if(obj != null) {
                gameId = (String) obj.get("gameId");
            }
            if(Utils.isNotEmpty(type) && Utils.isNotEmpty(gameId)) {
                Campaign campaign = campaignRepository.findByGameId(gameId);
                if(campaign != null) {
                    String routingKey = campaign.getType().toString(); 
                    ManageGameNotification manager = manageGameNotificationMap.get(routingKey);
                    if(manager != null) {
                        manager.manageGameNotification(map, routingKey);
                    }                   
                } else {
                    logger.warn("campaign not found: " + gameId);
                }
            } else {
                logger.warn("Bad notification content: " + msg);
            }                
        } catch (Exception e) {
            logger.error("gameNotificationCallback error", e);
        }	    
	}
	
	public void setManageGameNotification(ManageGameNotification manager, Type type) {
		String routingKey = type.toString();
		manageGameNotificationMap.put(routingKey, manager);
	}
	
}
