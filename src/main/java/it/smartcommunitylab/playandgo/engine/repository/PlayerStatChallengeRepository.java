package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.PlayerStatChallenge;

@Repository
public interface PlayerStatChallengeRepository extends MongoRepository<PlayerStatChallenge, String> {
	
	PlayerStatChallenge findByPlayerIdAndCampaignIdAndTypeAndDay(String playerId, String campaignId, String type, String day);
}
