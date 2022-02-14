package it.smartcommunitylab.playandgo.engine.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;

@Component
public class PlayerManager {
	private static transient final Logger logger = LoggerFactory.getLogger(PlayerManager.class);
	
	@Autowired
	private PlayerRepository playerRepository;
	
	@Autowired
	private CampaignManager campaignManager;
	
	public Player addPlayer(Player player) {
		Player playerDb = playerRepository.findById(player.getPlayerId()).orElse(null);
		if(playerDb == null) {
			return playerRepository.save(player);
		}
		return null;
	}
	
	public Player registerPlayer(Player player) throws Exception {
		Player playerDb = playerRepository.findById(player.getPlayerId()).orElse(null);
		if(playerDb != null) {
			return updatePlayer(player);
		} else {
			playerDb = playerRepository.save(player);
			//TODO subscribe default campaign?
			Campaign campaign = campaignManager.getDefaultCampaignByTerritory(player.getTerritoryId());
			if(campaign != null) {
				campaignManager.subscribePlayer(playerDb, campaign.getCampaignId());
			}
			return playerDb;
		}
	}
	
	public Player updatePlayer(Player player) {
		Player playerDb = playerRepository.findById(player.getPlayerId()).orElse(null);
		if(playerDb != null) {
			playerDb.setLanguage(player.getLanguage());
			playerDb.setName(player.getName());
			playerDb.setSurname(player.getSurname());
			playerDb.setNickname(player.getNickname());
			playerDb.setMail(player.getMail());
			playerDb.setSendMail(player.getSendMail());
			playerRepository.save(playerDb);
			campaignManager.updateDefaultCampaignSuscription(playerDb);
			return playerDb;
		}
		return null;
	}
	
	public Player getPlayer(String playerId) {
		return playerRepository.findById(playerId).orElse(null);
	}
	
	public Player deletePlayer(String playerId) {
		Player player = playerRepository.findById(playerId).orElse(null);
		if(player != null) {
			playerRepository.deleteById(playerId);
		}
		return player;
	}
}
