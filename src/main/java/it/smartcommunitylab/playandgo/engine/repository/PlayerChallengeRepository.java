package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.PlayerChallenge;

@Repository
public interface PlayerChallengeRepository extends MongoRepository<PlayerChallenge, String> {
	public PlayerChallenge findByPlayerIdAndCampaignIdAndChallangeId(String playerId, String campaignId, String challangeId);
}
