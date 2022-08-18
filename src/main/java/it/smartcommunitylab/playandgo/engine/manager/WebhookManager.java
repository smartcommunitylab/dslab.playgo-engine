package it.smartcommunitylab.playandgo.engine.manager;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import it.smartcommunitylab.playandgo.engine.exception.ConnectorException;
import it.smartcommunitylab.playandgo.engine.model.CampaignWebhook;
import it.smartcommunitylab.playandgo.engine.mq.ManageWebhookRequest;
import it.smartcommunitylab.playandgo.engine.mq.MessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.WebhookRequest;
import it.smartcommunitylab.playandgo.engine.repository.CampaignWebhookRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;
import it.smartcommunitylab.playandgo.engine.util.JsonUtils;

@Component
public class WebhookManager implements ManageWebhookRequest {
	
	@Autowired
	CampaignWebhookRepository campaignWebhookRepository;
	
	@Autowired
	MessageQueueManager queueManager;
	
	@PostConstruct
	public void init() {
		queueManager.setManageWebhookRequest(this);
	}

	private RestTemplate buildRestTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(5000);
		factory.setReadTimeout(15000);
		return new RestTemplate(factory);
	}
	
	private String doPost(String url, String content) throws Exception {
		RestTemplate restTemplate = buildRestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json");
		ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<Object>(content, headers), String.class);
		if (!res.getStatusCode().is2xxSuccessful()) {
			throw new ConnectorException("Failed : HTTP error code : " + res.getStatusCode(), ErrorCode.HTTP_ERROR);
		}
		return res.getBody();		
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
				doPost(hookDb.getEndpoint(), content);				
			}
		}
	}

}
