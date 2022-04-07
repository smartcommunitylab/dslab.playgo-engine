package it.smartcommunitylab.playandgo.engine.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;

@Repository
public interface CampaignSubscriptionRepository extends MongoRepository<CampaignSubscription, String> {

	public CampaignSubscription findByCampaignIdAndPlayerId(String campaignId, String playerId);
	
	public List<CampaignSubscription> findByPlayerId(String playerId);
	
	public List<CampaignSubscription> findByPlayerIdAndTerritoryId(String playerId, String territoryId);
	
	public Long countByCampaignId(String campaignId);

}
