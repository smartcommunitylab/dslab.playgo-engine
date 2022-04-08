package it.smartcommunitylab.playandgo.engine.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.PlayerStatsGame;

@Repository
public interface PlayerStatsGameRepository extends MongoRepository<PlayerStatsGame, String> {
	
	public List<PlayerStatsGame> findByPlayerIdAndCampaignId(String playerId, String campaignId);
	
	public PlayerStatsGame findByPlayerIdAndCampaignIdAndDay(String playerId, String campaignId, LocalDate day);
}
