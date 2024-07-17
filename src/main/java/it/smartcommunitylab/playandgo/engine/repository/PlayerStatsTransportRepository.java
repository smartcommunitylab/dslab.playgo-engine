package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.PlayerStatsTransport;

@Repository
public interface PlayerStatsTransportRepository extends MongoRepository<PlayerStatsTransport, String> {
	
//	public PlayerStatsTransport findByPlayerIdAndCampaignIdAndModeTypeAndGlobal(String playerId, String campaignId, 
//			String modeType, Boolean global);
	
	public PlayerStatsTransport findByPlayerIdAndCampaignIdAndModeTypeAndGlobalAndDay(String playerId, String campaignId, 
			String modeType, Boolean global, String day);
	
//	public List<PlayerStatsTransport> findByPlayerIdAndCampaignIdAndGlobal(String playerId, String campaignId, Boolean global);
	
//	public List<PlayerStatsTransport> findByPlayerIdAndCampaignIdAndGlobalAndDay(String playerId, String campaignId, 
//			Boolean global, String day);
}
