package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.CampaignWebhook;

@Repository
public interface CampaignWebhookRepository extends MongoRepository<CampaignWebhook, String> {
	public CampaignWebhook findByCampaignId(String campaignId);
}
