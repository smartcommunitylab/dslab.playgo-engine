package it.smartcommunitylab.playandgo.engine.mq;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

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
	
	Channel channel;
	ObjectMapper mapper = new ObjectMapper();
	
	Map<String, ManageGameNotification> manageGameNotificationMap = new HashMap<>();
	Map<String, ManageGameStatus> manageGameStatusMap = new HashMap<>();
	
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
			String routingKey = delivery.getEnvelope().getRoutingKey();
			ManageGameNotification manager = manageGameNotificationMap.get(routingKey);
			if(manager != null) {
				manager.manageGameNotification(msg, routingKey);
			}
		};
	}
	
	public void setManageGameNotification(ManageGameNotification manager, String gameId) {
		String routingKey = geRoutingKeyPrefix + "-" + gameId;
		if(!manageGameNotificationMap.containsKey(routingKey)) {
			try {
				String queueName = "queue-" + gameId;
				channel.queueDeclare(queueName, true, false, false, null);
				channel.queueBind(queueName, geExchangeName, routingKey);
				channel.basicConsume(queueName, true, gameNotificationCallback, consumerTag -> {});
				manageGameNotificationMap.put(routingKey, manager);
			} catch (Exception e) {
				logger.warn(String.format("setManageGameNotification: error in queue bind - %s - %s", routingKey, e.getMessage()));
			}
		}
	}
	
	public void unsetManageGameNotification(String gameId) {
		String routingKey = geRoutingKeyPrefix + "-" + gameId;
		String queueName = "queue-" + gameId;
		try {
			channel.queueUnbind(queueName, geExchangeName, routingKey);
		} catch (Exception e) {
			logger.warn(String.format("unsetManageGameNotification: error in queue bind - %s - %s", routingKey, e.getMessage()));
		}
		manageGameNotificationMap.remove(routingKey);
	}
	
	public void setManageGameStatus(ManageGameStatus manager, String gameId) {
		String routingKey = geRoutingKeyPrefix + "-" + gameId;
		if(!manageGameStatusMap.containsKey(routingKey)) {
			try {
				//channel.queueBind(geQueueName, geExchangeName, routingKey);
				manageGameStatusMap.put(routingKey, manager);
			} catch (Exception e) {
				logger.warn(String.format("setManageGameStatus: error in queue bind - %s - %s", routingKey, e.getMessage()));
			}
		}
	}
	
	public void unsetManageGameStatus(String gameId) {
		String routingKey = geRoutingKeyPrefix + "-" + gameId;
		manageGameStatusMap.remove(routingKey);		
	}

}
