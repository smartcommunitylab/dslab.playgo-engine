package it.smartcommunitylab.playandgo.engine.mq;

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
	
	@Value("${rabbitmq.host}")
	private String rabbitMQHost;	

	@Value("${rabbitmq.virtualhost}")
	private String rabbitMQVirtualHost;		
	
	@Value("${rabbitmq.port}")
	private Integer rabbitMQPort;
	
	@Value("${rabbitmq.user}")
	private String rabbitMQUser;
	
	@Value("${rabbitmq.password}")
	private String rabbitMQPassword;	
	
	private Channel validateTripChannel; 
	private Channel validateCampaignTripChannel;
	private Channel gamificationEngineChannel;
	
	ObjectMapper mapper = new ObjectMapper();
	
	//@PostConstruct
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
		validateTripChannel.queueDeclare(validateTripResponse, true, false, false, null);
		
		
		validateCampaignTripChannel = connection.createChannel();
		validateCampaignTripChannel.exchangeDeclare(validateCampaignTripRequest, BuiltinExchangeType.DIRECT);
		validateCampaignTripChannel.queueDeclare(validateCampaignTripResponse, true, false, false, null);
		
		gamificationEngineChannel = connection.createChannel();
		gamificationEngineChannel.queueDeclare(gamificationEngineRequest, true, false, false, null);
		gamificationEngineChannel.queueDeclare(gamificationEngineResponse, true, false, false, null);
		
		DeliverCallback validateTripRequestCallback = (consumerTag, delivery) -> {
			String json = new String(delivery.getBody(), "UTF-8");
			logger.debug("validateTripRequestCallback:" + json);
			ValidateTripRequest message = mapper.readValue(json, ValidateTripRequest.class);
	    validateTripChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
		};
		validateTripChannel.basicConsume(validateTripRequest, false, validateTripRequestCallback, consumerTag -> { });
		
		DeliverCallback validateTripResponseCallback = (consumerTag, delivery) -> {
	    String message = new String(delivery.getBody(), "UTF-8");
	    System.out.println(" [x] Received '" + message + "'");
	    validateTripChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
		};
		validateTripChannel.basicConsume(validateTripResponse, false, validateTripResponseCallback, consumerTag -> { });

		DeliverCallback gamificationEngineResponseCallback = (consumerTag, delivery) -> {
	    String message = new String(delivery.getBody(), "UTF-8");
	    System.out.println(" [x] Received '" + message + "'");
	    gamificationEngineChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
		};
		gamificationEngineChannel.basicConsume(gamificationEngineResponse, false, gamificationEngineResponseCallback, consumerTag -> { });
	}
	
	public void sendValidateTripRequest(ValidateTripRequest message) throws Exception {
		String msg = mapper.writeValueAsString(message);
		validateTripChannel.basicPublish("", validateTripRequest, null, msg.getBytes("UTF-8"));
	}
	
	public void sendValidateCampaignTripRequest(String message, String territory, String campaign) throws Exception {
		String routingKey = territory + "." + campaign;
		validateTripChannel.basicPublish(validateCampaignTripRequest, routingKey, null, message.getBytes("UTF-8"));
	}
	
	public void sendGamificationEngineRequest(String message) throws Exception {
		gamificationEngineChannel.basicPublish("", gamificationEngineRequest, null, message.getBytes());
	}
	
	


}
