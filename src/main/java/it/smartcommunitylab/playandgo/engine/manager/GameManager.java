package it.smartcommunitylab.playandgo.engine.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.ge.GameDataConverter;
import it.smartcommunitylab.playandgo.engine.ge.GamificationEngineManager;
import it.smartcommunitylab.playandgo.engine.ge.model.PlayerStatus;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerGameStatus;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerGameStatusRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@Component
public class GameManager {
	private static transient final Logger logger = LoggerFactory.getLogger(GameManager.class);
	
	@Autowired
	PlayerGameStatusRepository playerGameStatusRepository;
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	PlayerRepository playerRepository;
	
	@Autowired
	private GamificationEngineManager gamificationEngineManager;
	
	@Autowired
	private GameDataConverter gameDataConverter;
	
	public PlayerGameStatus getCampaignGameStatus(String playerId, String campaignId) {
		return playerGameStatusRepository.findByPlayerIdAndCampaignId(playerId, campaignId);
	}
	
	public PlayerStatus getGamePlayerStatus(String playerId, String campaignId) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		Player player = playerRepository.findById(playerId).orElse(null);
		if(player == null) {
			throw new BadRequestException("player not found", ErrorCode.PLAYER_NOT_FOUND);
		}
		String json = gamificationEngineManager.getGameStatus(playerId, campaign.getGameId());
		if(json == null) {
			throw new BadRequestException("error in GE invocation", ErrorCode.EXT_SERVICE_INVOCATION);
		}
		PlayerStatus playerStatus = gameDataConverter.convertPlayerData(json, playerId, campaign.getGameId(), player.getNickname(), 
				1, player.getLanguage());
		return playerStatus;
	}
	
}
