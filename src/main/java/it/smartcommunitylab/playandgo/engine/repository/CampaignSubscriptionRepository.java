package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;

public interface CampaignSubscriptionRepository extends MongoRepository<CampaignSubscription, String> {

	@Query ("{'campaignId': ?0, 'playerId' : ?1}")
	public CampaignSubscription findByPlayerId(String campaignId, String playerId);
}
