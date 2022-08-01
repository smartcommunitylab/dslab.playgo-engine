package it.smartcommunitylab.playandgo.engine.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.PlayerStatChallenge;

@Repository
public interface PlayerStatChallengeRepository extends MongoRepository<PlayerStatChallenge, String> {
	
	List<PlayerStatChallenge> findByPlayerIdAndCampaignIdAndTimestampBetween(String playerId, String campaignId, 
			Long dateFrom, Long dateTo);
}
