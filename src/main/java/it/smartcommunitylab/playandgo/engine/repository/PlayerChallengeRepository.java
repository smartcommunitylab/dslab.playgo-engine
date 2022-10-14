package it.smartcommunitylab.playandgo.engine.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.PlayerChallenge;

@Repository
public interface PlayerChallengeRepository extends MongoRepository<PlayerChallenge, String> {
	public PlayerChallenge findByPlayerIdAndCampaignIdAndChallangeId(String playerId, String campaignId, String challangeId);
	
	@Query("{'playerId':?0, 'campaignId':?1, 'challengeData.startDate' : { $gte: ?2, $lte: ?3 }}")
	public List<PlayerChallenge> findByDate(String playerId, String campaignId, long start, long end, Sort sort);
}
