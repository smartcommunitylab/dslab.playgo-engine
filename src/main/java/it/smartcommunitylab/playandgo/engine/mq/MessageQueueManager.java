package it.smartcommunitylab.playandgo.engine.mq;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.playandgo.engine.config.RabbitConf;
import it.smartcommunitylab.playandgo.engine.model.Campaign;

@Component
public class MessageQueueManager {
	private static transient final Logger logger = LoggerFactory.getLogger(MessageQueueManager.class);
	
	@Autowired
	RabbitTemplate rabbitTemplate;
	
	private ManageValidateTripRequest manageValidateTripRequest;
	
	private ManageWebhookRequest manageWebhookRequest;
	
	private Map<String, ManageValidateCampaignTripRequest> manageValidateCampaignTripRequestMap = new HashMap<>();
	
	ObjectMapper mapper = new ObjectMapper();

	@RabbitListener(queues = RabbitConf.validateTripRequest, concurrency = "5")
	public void validateTripRequestCallback(Message delivery) {
        try {
            String json = new String(delivery.getBody(), "UTF-8");
            logger.info("validateTripRequestCallback:" + json);
            ValidateTripRequest message = mapper.readValue(json, ValidateTripRequest.class);
            if(manageValidateTripRequest != null) {
                manageValidateTripRequest.validateTripRequest(message);
            }                   
        } catch (Exception e) {
            logger.warn(String.format("validateTripRequestCallback:", e.getMessage()));
        }
	}

	@RabbitListener(queues = RabbitConf.validateCampaignTripRequest, concurrency = "5")
    public void validateCampaignTripRequestCallback(Message delivery) {
        try {
            String json = new String(delivery.getBody(), "UTF-8");
            logger.info("validateCampaignTripRequestCallback:" + json);
            ValidateCampaignTripRequest message = mapper.readValue(json, ValidateCampaignTripRequest.class);
            String routingKey = message.getCampaignType();
            ManageValidateCampaignTripRequest manager = manageValidateCampaignTripRequestMap.get(routingKey);
            if(manager != null) {
                manager.validateTripRequest(message);
            }            
        } catch (Exception e) {
            logger.warn(String.format("validateCampaignTripRequestCallback:", e.getMessage()));
        }
    };

    @RabbitListener(queues = RabbitConf.invalidateCampaignTripRequest, concurrency = "1")
    public void invalidateCampaignTripRequestCallback(Message delivery) {
        try {
            String json = new String(delivery.getBody(), "UTF-8");
            logger.info("invalidateCampaignTripRequestCallback:" + json);
            UpdateCampaignTripRequest message = mapper.readValue(json, UpdateCampaignTripRequest.class);
            String routingKey = message.getCampaignType();
            ManageValidateCampaignTripRequest manager = manageValidateCampaignTripRequestMap.get(routingKey);
            if(manager != null) {
                manager.invalidateTripRequest(message);
            }            
        } catch (Exception e) {
            logger.warn(String.format("invalidateCampaignTripRequestCallback:", e.getMessage()));
        }
    };

    @RabbitListener(queues = RabbitConf.updateCampaignTripRequest, concurrency = "1")
    public void updateCampaignTripRequestCallback(Message delivery) {
        try {
            String json = new String(delivery.getBody(), "UTF-8");
            logger.info("updateCampaignTripRequestCallback:" + json);
            UpdateCampaignTripRequest message = mapper.readValue(json, UpdateCampaignTripRequest.class);
            String routingKey = message.getCampaignType();
            ManageValidateCampaignTripRequest manager = manageValidateCampaignTripRequestMap.get(routingKey);
            if(manager != null) {
                manager.updateTripRequest(message);
            }            
        } catch (Exception e) {
            logger.warn(String.format("updateCampaignTripRequestCallback:", e.getMessage()));
        }
    };

    @RabbitListener(queues = RabbitConf.revalidateCampaignTripRequest, concurrency = "1")
    public void revalidateCampaignTripRequestCallback(Message delivery) {
        try {
            String json = new String(delivery.getBody(), "UTF-8");
            logger.info("revalidateCampaignTripRequestCallback:" + json);
            UpdateCampaignTripRequest message = mapper.readValue(json, UpdateCampaignTripRequest.class);
            String routingKey = message.getCampaignType();
            ManageValidateCampaignTripRequest manager = manageValidateCampaignTripRequestMap.get(routingKey);
            if(manager != null) {
                manager.revalidateTripRequest(message);
            }            
        } catch (Exception e) {
            logger.warn(String.format("revalidateCampaignTripRequestCallback:", e.getMessage()));
        }
    };
    
    @RabbitListener(queues = RabbitConf.callWebhookRequest, concurrency = "5")
    public void callWebhookCallback(Message delivery) {
        try {
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
        } catch (Exception e) {
            logger.warn(String.format("callWebhookCallback:", e.getMessage()));
        }
    };

	public void setManageWebhookRequest(ManageWebhookRequest manager) {
		this.manageWebhookRequest = manager;
	}
	
	public void setManageValidateTripRequest(ManageValidateTripRequest manager) {
		this.manageValidateTripRequest = manager;
	}
	
	public void sendValidateTripRequest(ValidateTripRequest message) throws Exception {
		String msg = mapper.writeValueAsString(message);
		rabbitTemplate.convertAndSend(RabbitConf.validateTripRequest, msg);
	}
	
	public void setManageValidateCampaignTripRequest(ManageValidateCampaignTripRequest manager, Campaign.Type type) {
		manageValidateCampaignTripRequestMap.put(type.toString(), manager);
	}
	
	public void sendValidateCampaignTripRequest(ValidateCampaignTripRequest message) throws Exception {
		String msg = mapper.writeValueAsString(message);
		rabbitTemplate.convertAndSend(RabbitConf.validateCampaignTripRequest, msg);
	}

	public void sendInvalidateCampaignTripRequest(UpdateCampaignTripRequest message) throws Exception {
		String msg = mapper.writeValueAsString(message);
		rabbitTemplate.convertAndSend(RabbitConf.invalidateCampaignTripRequest, msg);
	}
	
	public void sendUpdateCampaignTripRequest(UpdateCampaignTripRequest message) throws Exception {
		String msg = mapper.writeValueAsString(message);
		rabbitTemplate.convertAndSend(RabbitConf.updateCampaignTripRequest, msg);
	}
	
	public void sendRevalidateCampaignTripRequest(UpdateCampaignTripRequest message) throws Exception {
		String msg = mapper.writeValueAsString(message);
		rabbitTemplate.convertAndSend(RabbitConf.revalidateCampaignTripRequest, msg);
	}
	
	public void sendCallWebhookRequest(WebhookRequest message) throws Exception {
		String msg = mapper.writeValueAsString(message);
		rabbitTemplate.convertAndSend(RabbitConf.callWebhookRequest, msg);
	}


}
