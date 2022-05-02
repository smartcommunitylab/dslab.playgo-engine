package it.smartcommunitylab.playandgo.engine.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.PlayerGameStatus;
import it.smartcommunitylab.playandgo.engine.repository.PlayerGameStatusRepository;

@Component
public class GameManager {
	private static transient final Logger logger = LoggerFactory.getLogger(GameManager.class);
	
	@Autowired
	PlayerGameStatusRepository playerGameStatusRepository;
	
	public PlayerGameStatus getCampaignGameStatus(String playerId, String campaignId) {
		return playerGameStatusRepository.findByPlayerIdAndCampaignId(playerId, campaignId);
	}
	
	
	
}
