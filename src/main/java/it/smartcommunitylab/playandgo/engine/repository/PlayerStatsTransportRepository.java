package it.smartcommunitylab.playandgo.engine.repository;

import java.time.LocalDate;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.PlayerStatsTransport;

@Repository
public interface PlayerStatsTransportRepository extends MongoRepository<PlayerStatsTransport, String> {
	
	public PlayerStatsTransport findByPlayerIdAndCampaignIdAndScoreTypeAndGlobal(String playerId, String campaignId, 
			String scoreType, Boolean global);
	
	public PlayerStatsTransport findByPlayerIdAndCampaignIdAndScoreTypeAndGlobalAndWeeklyDay(String playerId, String campaignId, 
			String scoreType, Boolean global, LocalDate weeklyDay);
}
