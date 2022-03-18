package it.smartcommunitylab.playandgo.engine.mq;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

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
	
	Channel channel;
	ObjectMapper mapper = new ObjectMapper();
	
	Map<String, ManageGameNotification> manageGameNotificationMap = new HashMap<>();
	
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

		Connection connection = connectionFactory.newConnection();
		
		channel = connection.createChannel();
		channel.exchangeDeclare(geExchangeName, BuiltinExchangeType.DIRECT, true);
		
		gameNotificationCallback = (consumerTag, delivery) -> {
			String msg = new String(delivery.getBody(), "UTF-8");
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) mapper.readValue(msg, Map.class);
			String type = (String) map.get("type");
			String gameId = (String) map.get("gameId");
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
	}
	
	public void setGameNotification(String gameId) {
		String routingKey = geRoutingKeyPrefix + "-" + gameId;
		try {
			String queueName = "queue-" + gameId;
			channel.queueDeclare(queueName, true, false, false, null);
			channel.queueBind(queueName, geExchangeName, routingKey);
			channel.basicConsume(queueName, true, gameNotificationCallback, consumerTag -> {});
		} catch (Exception e) {
			logger.warn(String.format("setGameNotification: error in queue bind - %s - %s", routingKey, e.getMessage()));
		}
	}
	
	public void setManageGameNotification(ManageGameNotification manager, Type type) {
		String routingKey = type.toString();
		manageGameNotificationMap.put(routingKey, manager);
	}
	
}
