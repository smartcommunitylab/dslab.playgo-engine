package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.PlayerStatsGame;

@Repository
public interface PlayerStatsGameRepository extends MongoRepository<PlayerStatsGame, String> {
	
	public PlayerStatsGame findByPlayerIdAndCampaignId(String playerId, String campaignId);
}
