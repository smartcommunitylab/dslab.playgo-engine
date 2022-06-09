package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.manager.ext.CampaignMsg;
import it.smartcommunitylab.playandgo.engine.manager.ext.CampaignMsg.Operation;

@Repository
public interface CampaignMsgRepository extends MongoRepository<CampaignMsg, String> {
	public CampaignMsg findByCampaignPlayerTrackIdAndOperation(String campaignPlayerTrackId, Operation operation);
}
