package it.smartcommunitylab.playandgo.engine.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;

@Component
public class PlayerManager {
	private static transient final Logger logger = LoggerFactory.getLogger(PlayerManager.class);
	
	@Autowired
	private PlayerRepository playerRepository;
	
	public Player registerPlayer(Player player) {
		Player playerDb = playerRepository.findById(player.getPlayerId()).orElse(null);
		if(playerDb != null) {
			return updatePlayer(player);
		}
		return playerRepository.save(player);
	}
	
	public Player updatePlayer(Player player) {
		Player playerDb = playerRepository.findById(player.getPlayerId()).orElse(null);
		if(playerDb != null) {
			playerDb.setLanguage(player.getLanguage());
			playerDb.setMail(player.getMail());
			playerDb.setName(player.getName());
			playerDb.setNickname(player.getNickname());
			playerDb.setSendMail(player.isSendMail());
			playerDb.setSurname(player.getSurname());
			return playerRepository.save(playerDb);
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
