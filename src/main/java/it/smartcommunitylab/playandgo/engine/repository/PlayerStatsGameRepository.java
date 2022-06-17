package it.smartcommunitylab.playandgo.engine.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.PlayerStatsGame;

@Repository
public interface PlayerStatsGameRepository extends MongoRepository<PlayerStatsGame, String> {
	
	public List<PlayerStatsGame> findByPlayerIdAndCampaignId(String playerId, String campaignId);
	
	public PlayerStatsGame findByPlayerIdAndCampaignIdAndDayAndGlobal(String playerId, String campaignId, String day, Boolean global);

	@Query("{'playerId':?0, 'campaignId': ?1, 'global': true}")
	public PlayerStatsGame findGlobalByPlayerIdAndCampaignId(String playerId, String campaignId);

}
