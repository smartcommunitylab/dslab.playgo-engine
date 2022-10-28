package it.smartcommunitylab.playandgo.engine.mq;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import it.smartcommunitylab.playandgo.engine.model.Campaign;

@Component
public class MessageQueueManager {
	private static transient final Logger logger = LoggerFactory.getLogger(MessageQueueManager.class);
	
	public static final String validateTripRequest = "playgo-vt-request";
	//public static final String validateTripResponse = "playgo-vt-response";
	public static final String validateCampaignTripRequest = "playgo-campaign-vt-request";
	public static final String invalidateCampaignTripRequest = "playgo-campaign-invt-request";
	public static final String updateCampaignTripRequest = "playgo-campaign-updt-request";
	public static final String revalidateCampaignTripRequest = "playgo-campaign-revt-request";
	//public static final String validateCampaignTripResponse = "playgo-campaign-vt-r";
	public static final String callWebhookRequest = "playgo-campaign-webhook-request";
	
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
	
	private Connection connection;
	
	private Channel validateTripChannel; 
	private Channel validateCampaignTripChannel;
	private Channel callWebhookChannel;
	
	private ManageValidateTripRequest manageValidateTripRequest;
	
	private ManageWebhookRequest manageWebhookRequest;
	
	private Map<String, ManageValidateCampaignTripRequest> manageValidateCampaignTripRequestMap = new HashMap<>();
	
	DeliverCallback validateCampaignTripRequestCallback;
	DeliverCallback invalidateCampaignTripRequestCallback;
	DeliverCallback updateCampaignTripRequestCallback;
	DeliverCallback revalidateCampaignTripRequestCallback;
	DeliverCallback callWebhookCallback;
	
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

		connection = connectionFactory.newConnection();
		
		validateTripChannel = connection.createChannel();
		validateTripChannel.queueDeclare(validateTripRequest, true, false, false, null);		
		
		DeliverCallback validateTripRequestCallback = (consumerTag, delivery) -> {
			String json = new String(delivery.getBody(), "UTF-8");
			logger.info("validateTripRequestCallback:" + json);
			ValidateTripRequest message = mapper.readValue(json, ValidateTripRequest.class);
			if(manageValidateTripRequest != null) {
				manageValidateTripRequest.validateTripRequest(message);
			}			
		};
		validateTripChannel.basicConsume(validateTripRequest, true, validateTripRequestCallback, consumerTag -> {});

		validateCampaignTripChannel = connection.createChannel();
		validateCampaignTripChannel.queueDeclare(validateCampaignTripRequest, true, false, false, null);
		validateCampaignTripChannel.queueDeclare(invalidateCampaignTripRequest, true, false, false, null);
		validateCampaignTripChannel.queueDeclare(updateCampaignTripRequest, true, false, false, null);
		validateCampaignTripChannel.queueDeclare(revalidateCampaignTripRequest, true, false, false, null);

		validateCampaignTripRequestCallback = (consumerTag, delivery) -> {
			String json = new String(delivery.getBody(), "UTF-8");
			logger.info("validateCampaignTripRequestCallback:" + json);
			ValidateCampaignTripRequest message = mapper.readValue(json, ValidateCampaignTripRequest.class);
			String routingKey = message.getCampaignType();
			ManageValidateCampaignTripRequest manager = manageValidateCampaignTripRequestMap.get(routingKey);
			if(manager != null) {
				manager.validateTripRequest(message);
			}
		};
		validateCampaignTripChannel.basicConsume(validateCampaignTripRequest, true, validateCampaignTripRequestCallback, consumerTag -> {});
		
		invalidateCampaignTripRequestCallback = (consumerTag, delivery) -> {
			String json = new String(delivery.getBody(), "UTF-8");
			logger.info("invalidateCampaignTripRequestCallback:" + json);
			ValidateCampaignTripRequest message = mapper.readValue(json, ValidateCampaignTripRequest.class);
			String routingKey = message.getCampaignType();
			ManageValidateCampaignTripRequest manager = manageValidateCampaignTripRequestMap.get(routingKey);
			if(manager != null) {
				manager.invalidateTripRequest(message);
			}
		};
		validateCampaignTripChannel.basicConsume(invalidateCampaignTripRequest, true, invalidateCampaignTripRequestCallback, consumerTag -> {});

		updateCampaignTripRequestCallback = (consumerTag, delivery) -> {
			String json = new String(delivery.getBody(), "UTF-8");
			logger.info("updateCampaignTripRequestCallback:" + json);
			UpdateCampaignTripRequest message = mapper.readValue(json, UpdateCampaignTripRequest.class);
			String routingKey = message.getCampaignType();
			ManageValidateCampaignTripRequest manager = manageValidateCampaignTripRequestMap.get(routingKey);
			if(manager != null) {
				manager.updateTripRequest(message);
			}
		};
		validateCampaignTripChannel.basicConsume(updateCampaignTripRequest, true, updateCampaignTripRequestCallback, consumerTag -> {});
		
		revalidateCampaignTripRequestCallback = (consumerTag, delivery) -> {
			String json = new String(delivery.getBody(), "UTF-8");
			logger.info("revalidateCampaignTripRequestCallback:" + json);
			UpdateCampaignTripRequest message = mapper.readValue(json, UpdateCampaignTripRequest.class);
			String routingKey = message.getCampaignType();
			ManageValidateCampaignTripRequest manager = manageValidateCampaignTripRequestMap.get(routingKey);
			if(manager != null) {
				manager.revalidateTripRequest(message);
			}
		};
		validateCampaignTripChannel.basicConsume(revalidateCampaignTripRequest, true, revalidateCampaignTripRequestCallback, consumerTag -> {});
		
		callWebhookChannel = connection.createChannel();
		callWebhookChannel.queueDeclare(callWebhookRequest, true, false, false, null);
		callWebhookCallback = (consumerTag, delivery) -> {
			String json = new String(delivery.getBody(), "UTF-8");
			logger.info("callWebhookCallback:" + json);
			WebhookRequest msg = mapper.readValue(json, WebhookRequest.class);
			if(manageWebhookRequest != null) {
				try {
					manageWebhookRequest.sendMessage(msg);
				} catch (Exception e) {
					logger.error(String.format("callWebhookCallback error:%s - %s - %s", 
							msg.getCampaignId(), msg.getPlayerId(), e.getMessage()));
				}
			}
		};
		callWebhookChannel.basicConsume(callWebhookRequest, true, callWebhookCallback, consumerTag -> {});
	}
	
	@PreDestroy
	public void destroy() {
		if(validateTripChannel != null) {
			try {
				validateTripChannel.close();
				logger.info("close validateTripChannel");
			} catch (Exception e) {
				logger.warn("destroy:" + e.getMessage());
			}
		}
		if(validateCampaignTripChannel != null) {
			try {
				validateCampaignTripChannel.close();
				logger.info("close validateCampaignTripChannel");
			} catch (Exception e) {
				logger.warn("destroy:" + e.getMessage());
			}
		}
		if(callWebhookChannel != null) {
			try {
				callWebhookChannel.close();
				logger.info("close callWebhookChannel");
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
	
	public void setManageWebhookRequest(ManageWebhookRequest manager) {
		this.manageWebhookRequest = manager;
	}
	
	public void setManageValidateTripRequest(ManageValidateTripRequest manager) {
		this.manageValidateTripRequest = manager;
	}
	
	public void sendValidateTripRequest(ValidateTripRequest message) throws Exception {
		String msg = mapper.writeValueAsString(message);
		validateTripChannel.basicPublish("", validateTripRequest, null, msg.getBytes("UTF-8"));
	}
	
	public void setManageValidateCampaignTripRequest(ManageValidateCampaignTripRequest manager, Campaign.Type type) {
		manageValidateCampaignTripRequestMap.put(type.toString(), manager);
	}
	
	public void sendValidateCampaignTripRequest(ValidateCampaignTripRequest message) throws Exception {
		String msg = mapper.writeValueAsString(message);
		validateTripChannel.basicPublish("", validateCampaignTripRequest, null, msg.getBytes("UTF-8"));
	}

	public void sendInvalidateCampaignTripRequest(ValidateCampaignTripRequest message) throws Exception {
		String msg = mapper.writeValueAsString(message);
		validateTripChannel.basicPublish("", invalidateCampaignTripRequest, null, msg.getBytes("UTF-8"));
	}
	
	public void sendUpdateCampaignTripRequest(UpdateCampaignTripRequest message) throws Exception {
		String msg = mapper.writeValueAsString(message);
		validateTripChannel.basicPublish("", updateCampaignTripRequest, null, msg.getBytes("UTF-8"));
	}
	
	public void sendRevalidateCampaignTripRequest(UpdateCampaignTripRequest message) throws Exception {
		String msg = mapper.writeValueAsString(message);
		validateTripChannel.basicPublish("", revalidateCampaignTripRequest, null, msg.getBytes("UTF-8"));
	}
	
	public void sendCallWebhookRequest(WebhookRequest message) throws Exception {
		String msg = mapper.writeValueAsString(message);
		callWebhookChannel.basicPublish("", callWebhookRequest, null, msg.getBytes("UTF-8"));
	}


}
