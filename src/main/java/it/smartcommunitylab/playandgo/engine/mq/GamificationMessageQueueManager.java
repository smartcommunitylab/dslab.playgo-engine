package it.smartcommunitylab.playandgo.engine.mq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class GamificationMessageQueueManager {
	private static transient final Logger logger = LoggerFactory.getLogger(GamificationMessageQueueManager.class);
	
	@Value("${rabbitmq_ge.host}")
	private String rabbitMQHost;	

	@Value("${rabbitmq_ge.virtualhost}")
	private String rabbitMQVirtualHost;		
	
	@Value("${rabbitmq_ge.port}")
	private Integer rabbitMQPort;
	
	@Value("${rabbitmq_ge.user}")
	private String rabbitMQUser;
	
	@Value("${rabbitmq_ge.password}")
	private String rabbitMQPassword;	
		
	@Value("${rabbitmq_ge.geExchangeName}")
	private String geExchangeName;
	
	@Value("${rabbitmq_ge.geRoutingKeyPrefix}")
	private String geRoutingKeyPrefix;	
	
	@Autowired
	CampaignRepository campaignRepository;
	
	Connection connection;
	
	Channel channel;
	
	ObjectMapper mapper = new ObjectMapper();
	
	Map<String, ManageGameNotification> manageGameNotificationMap = new HashMap<>();
	
	List<String> gameList = new ArrayList<>(); 
	private Map<String, String> consumerTagMap = new HashMap<>();
	
	DeliverCallback gameNotificationCallback;
	
	@PostConstruct
	public void init() throws Exception {
		logger.info("Connecting to RabbitMQ");
		ConnectionFactory connectionFactory = new ConnectionFactory();
		connectionFactory.setUsername(rabbitMQUser);
		connectionFactory.setPassword(rabbitMQPassword);
		connectionFactory.setVirtualHost(rabbitMQVirtualHost);
		connectionFactory.setHost(rabbitMQHost);
		connectionFactory.setPort(rabbitMQPort);
		connectionFactory.setAutomaticRecoveryEnabled(true);
		connectionFactory.setTopologyRecoveryEnabled(true);
		

		connection = connectionFactory.newConnection();
		
        gameNotificationCallback = (consumerTag, delivery) -> {
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
        };
        
		initChannel(false);
	}

    private void initChannel(boolean forceConsumer) throws Exception {
        logger.info("init channel");
        channel = connection.createChannel();
        channel.exchangeDeclare(geExchangeName, BuiltinExchangeType.DIRECT, true);
        for(String gameId : gameList) {
            setGameNotification(gameId, forceConsumer);
        }
    }
    
	@PreDestroy
	public void destroy() {
		if(channel != null) {
			try {
				channel.close();
				logger.info("close channel");
			} catch (Exception e) {
				logger.warn("destroy:" + e.getMessage());
			}
		}
		if(connection != null) {
			try {
				connection.close();
				logger.info("close connection");
			} catch (Exception e) {
				logger.warn("destroy:" + e.getMessage());
			}
		}		
	}
	
	public void setGameNotification(String gameId) {
	    setGameNotification(gameId, false);
	}
	
	public void setGameNotification(String gameId, boolean forceConsumer) {
	    if(!gameList.contains(gameId)) {
	        gameList.add(gameId);
	    }
	    if(!channel.isOpen()) {
	        try {
                initChannel(true);
            } catch (Exception e) {
                logger.info(String.format("init channel error:%s", e.getMessage()));
            }
	    } else {
	        String routingKey = geRoutingKeyPrefix + "-" + gameId;
            String queueName = "queue-" + gameId;
            if(!forceConsumer && consumerTagMap.containsKey(gameId))
                return;
	        try {
	            Map<String, Object> args = new HashMap<>();
	            args.put("x-queue-type", "quorum");	            
	            channel.queueDeclare(queueName, true, false, false, args);
	            channel.queueBind(queueName, geExchangeName, routingKey);
	            String consumerTag = channel.basicConsume(queueName, true, gameNotificationCallback, cTag -> {});
	            consumerTagMap.put(gameId, consumerTag);
	            logger.info(String.format("add consumer[%s]:%s", queueName, consumerTag));
	        } catch (Exception e) {
	            logger.warn(String.format("setGameNotification: error in queue bind - %s - %s", routingKey, e.getMessage()));
	        }	        
	    }
	}
	
	public void setManageGameNotification(ManageGameNotification manager, Type type) {
		String routingKey = type.toString();
		manageGameNotificationMap.put(routingKey, manager);
	}
	
}
