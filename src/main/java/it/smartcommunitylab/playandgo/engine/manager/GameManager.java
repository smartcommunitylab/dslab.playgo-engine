package it.smartcommunitylab.playandgo.engine.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.PlayerGameStatus;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerGameStatusRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;

@Component
public class GameManager {
	private static transient final Logger logger = LoggerFactory.getLogger(GameManager.class);
	
	@Autowired
	PlayerGameStatusRepository playerGameStatusRepository;
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	PlayerRepository playerRepository;
	
	public PlayerGameStatus getCampaignGameStatus(String playerId, String campaignId) {
		return playerGameStatusRepository.findByPlayerIdAndCampaignId(playerId, campaignId);
	}
	
}
