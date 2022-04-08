package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.PlayerGameStatus;

@Repository
public interface PlayerGameStatusRepository extends MongoRepository<PlayerGameStatus, String> {

	public PlayerGameStatus findByPlayerIdAndCampaignId(String playerId, String campaignId);
}
