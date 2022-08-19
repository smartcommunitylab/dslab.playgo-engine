package it.smartcommunitylab.playandgo.engine.manager.webhook;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.CampaignWebhook;
import it.smartcommunitylab.playandgo.engine.mq.ManageWebhookRequest;
import it.smartcommunitylab.playandgo.engine.mq.MessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.WebhookRequest;
import it.smartcommunitylab.playandgo.engine.repository.CampaignWebhookRepository;
import it.smartcommunitylab.playandgo.engine.util.JsonUtils;

@Component
public class WebhookManager implements ManageWebhookRequest {
	
	@Autowired
	CampaignWebhookRepository campaignWebhookRepository;
	
	@Autowired
	MessageQueueManager queueManager;
	
	@Autowired
	WebhookCallService webhookCallService;
	
	@PostConstruct
	public void init() {
		queueManager.setManageWebhookRequest(this);
	}

	public CampaignWebhook getWebhook(String campaignId) {
		return campaignWebhookRepository.findByCampaignId(campaignId);
	}
	
	public CampaignWebhook setWebhook(CampaignWebhook hook) {
		CampaignWebhook hookDb = campaignWebhookRepository.findByCampaignId(hook.getCampaignId());
		if(hookDb == null) {
			campaignWebhookRepository.save(hook);
			return hook;
		} else {
			hookDb.setEndpoint(hook.getEndpoint());
			hookDb.setEvents(hook.getEvents());
			campaignWebhookRepository.save(hookDb);
			return hookDb;
		}
	}
	
	public void deleteWebhook(String campaignId) {
		CampaignWebhook hookDb = campaignWebhookRepository.findByCampaignId(campaignId);
		if(hookDb != null) {
			campaignWebhookRepository.delete(hookDb);
		}
	}

	@Override
	public void sendMessage(WebhookRequest msg) throws Exception {
		CampaignWebhook hookDb = campaignWebhookRepository.findByCampaignId(msg.getCampaignId());
		if(hookDb != null) {
			if(hookDb.getEvents().contains(msg.getEventType())) {
				String content = JsonUtils.toJSON(msg);
				webhookCallService.doPost(hookDb.getEndpoint(), content);				
			}
		}
	}

}
