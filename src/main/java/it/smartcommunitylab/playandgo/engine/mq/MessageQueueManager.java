package it.smartcommunitylab.playandgo.engine.mq;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
	private Channel gamificationEngineChannel;
	
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
		validateTripChannel.exchangeDeclare(validateTripRequest, BuiltinExchangeType.TOPIC);
		validateTripChannel.queueDeclare(validateTripResponse, true, false, false, null);
		
		gamificationEngineChannel = connection.createChannel();
		gamificationEngineChannel.exchangeDeclare(gamificationEngineRequest, BuiltinExchangeType.DIRECT);
		gamificationEngineChannel.queueDeclare(gamificationEngineResponse, true, false, false, null);
		
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
	
	public void sendValidateTripRequest(String message, String territory, String campaign) throws Exception {
		String routingKey = territory + "." + campaign;
		validateTripChannel.basicPublish(validateTripRequest, routingKey, null, message.getBytes("UTF-8"));
	}
	
	public void sendGamificationEngineRequest(String message) throws Exception {
		gamificationEngineChannel.basicPublish(gamificationEngineRequest, gamificationEngineRequest, null, message.getBytes());
	}


}
