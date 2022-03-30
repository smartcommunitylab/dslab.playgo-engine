package it.smartcommunitylab.playandgo.engine.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;

@Repository
public interface CampaignPlayerTrackRepository extends MongoRepository<CampaignPlayerTrack, String> {
	
	public CampaignPlayerTrack findByPlayerIdAndCampaignIdAndTrackedInstanceId(String playerId, String campaignId, String trackedInstanceId);
	
	public List<CampaignPlayerTrack> findByPlayerIdAndTrackedInstanceId(String playerId, String trackedInstanceId);
}
