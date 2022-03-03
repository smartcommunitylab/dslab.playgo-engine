package it.smartcommunitylab.playandgo.engine.mq;

import java.io.IOException;
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
public class MessageQueueManager {
	private static transient final Logger logger = LoggerFactory.getLogger(MessageQueueManager.class);
	
	public static final String validateTripRequest = "playgo-vt-request";
	public static final String validateTripResponse = "playgo-vt-response";
	public static final String validateCampaignTripRequest = "playgo-campaign-vt-request";
	public static final String validateCampaignTripResponse = "playgo-campaign-vt-r";
	public static final String gamificationEngineRequest = "playgo-ge-request";
	public static final String gamificationEngineResponse = "playgo-ge-response";
	
	@Value("${rabbitmq_pg.host}")
	private String rabbitMQHost;	

	@Value("${rabbitmq_pg.virtualhost}")
	private String rabbitMQVirtualHost;		
	
	@Value("${rabbitmq_pg.port}")
	private Integer rabbitMQPort;
	
	@Value("${rabbitmq_pg.user}")
	private String rabbitMQUser;
	
	@Value("${rabbitmq_pg.password}")
	private String rabbitMQPassword;	
		
	private Channel validateTripChannel; 
	private Channel validateCampaignTripChannel;
	
	private ManageValidateTripRequest manageValidateTripRequest;
	
	private Map<String, ManageValidateCampaignTripRequest> manageValidateCampaignTripRequestMap = new HashMap<>();
	
	DeliverCallback validateCampaignTripRequestCallback;
	
	ObjectMapper mapper = new ObjectMapper();
	
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
		
		validateTripChannel = connection.createChannel();
		validateTripChannel.queueDeclare(validateTripRequest, true, false, false, null);
		
		validateCampaignTripChannel = connection.createChannel();
		validateCampaignTripChannel.exchangeDeclare(validateCampaignTripRequest, BuiltinExchangeType.DIRECT, true);
		
		DeliverCallback validateTripRequestCallback = (consumerTag, delivery) -> {
			String json = new String(delivery.getBody(), "UTF-8");
			logger.debug("validateTripRequestCallback:" + json);
			ValidateTripRequest message = mapper.readValue(json, ValidateTripRequest.class);
			validateTripChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
			if(manageValidateTripRequest != null) {
				manageValidateTripRequest.validateTripRequest(message);
			}			
		};
		validateTripChannel.basicConsume(validateTripRequest, false, validateTripRequestCallback, consumerTag -> {});
		
		validateCampaignTripRequestCallback = (consumerTag, delivery) -> {
			String json = new String(delivery.getBody(), "UTF-8");
			logger.debug("validateCampaignTripRequestCallback:" + json);
			ValidateCampaignTripRequest message = mapper.readValue(json, ValidateCampaignTripRequest.class);
			validateCampaignTripChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
			String routingKey = delivery.getEnvelope().getRoutingKey();
			ManageValidateCampaignTripRequest manager = manageValidateCampaignTripRequestMap.get(routingKey);
			if(manager != null) {
				manager.validateTripRequest(message);
			}
		};
	}
	
	public void setManageValidateTripRequest(ManageValidateTripRequest manager) {
		this.manageValidateTripRequest = manager;
	}
	
	public void sendValidateTripRequest(ValidateTripRequest message) throws Exception {
		String msg = mapper.writeValueAsString(message);
		validateTripChannel.basicPublish("", validateTripRequest, null, msg.getBytes("UTF-8"));
	}
	
	public String getValidateCampaignTripRequestRoutingKey(ValidateCampaignTripRequest message) {
		return message.getTerritoryId() + "__" + message.getCampaignId();
	}
	
	public String getValidateCampaignTripRequestRoutingKey(String territoryId, String campaignId) {
		return territoryId + "__" + campaignId;
	}
	
	public void setManageValidateCampaignTripRequest(ManageValidateCampaignTripRequest manager, String territoryId, String campaignId) {
		String routingKey = getValidateCampaignTripRequestRoutingKey(territoryId, campaignId);
		String queueName = validateCampaignTripRequest + "__" + routingKey;
		if(!manageValidateCampaignTripRequestMap.containsKey(routingKey)) {
			try {
				validateCampaignTripChannel.queueDeclare(queueName, true, false, false, null); 
				validateCampaignTripChannel.queueBind(queueName, validateCampaignTripRequest, routingKey);
				validateCampaignTripChannel.basicConsume(queueName, true, validateCampaignTripRequestCallback, consumerTag -> {});
				manageValidateCampaignTripRequestMap.put(routingKey, manager);
			} catch (IOException e) {
				logger.warn(String.format("setManageValidateCampaignTripRequest: error in queue bind - %s - %s", routingKey, e.getMessage()));
			}
		}		
	}
	
	public void sendValidateCampaignTripRequest(ValidateCampaignTripRequest message) throws Exception {
		String routingKey = getValidateCampaignTripRequestRoutingKey(message);
		String msg = mapper.writeValueAsString(message);
		validateTripChannel.basicPublish(validateCampaignTripRequest, routingKey, null, msg.getBytes("UTF-8"));
	}
	
}
