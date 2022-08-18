package it.smartcommunitylab.playandgo.engine.manager;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.dto.PlayerInfo;
import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.model.Avatar;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Image;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class PlayerManager {
	private static transient final Logger logger = LoggerFactory.getLogger(PlayerManager.class);
	
	@Autowired
	private PlayerRepository playerRepository;
	
	@Autowired
	private CampaignManager campaignManager;
	
	@Autowired
	private AvatarManager avatarManager;
	
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
			throw new BadRequestException("player already exists", ErrorCode.PLAYER_EXISTS);
		}
		player.setNickname(player.getNickname().trim());
		playerDb = playerRepository.findByNicknameIgnoreCase(player.getNickname());
		if(playerDb != null) {
			throw new BadRequestException("nickname already exists", ErrorCode.PLAYER_NICK_EXISTS);
		}
		playerDb = playerRepository.save(player);
		Campaign campaign = campaignManager.getDefaultCampaignByTerritory(player.getTerritoryId());
		if(campaign != null) {
			campaignManager.subscribePlayer(playerDb, campaign.getCampaignId(), null);
		}
		return playerDb;
	}
	
	public Player updatePlayer(Player player, String playerId) {
		Player playerDb = playerRepository.findById(playerId).orElse(null);
		if(playerDb != null) {
			playerDb.setLanguage(player.getLanguage());
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
	
	public boolean findByNickname(String nickname) {
		Player player = playerRepository.findByNicknameIgnoreCase(nickname);
		if(player != null) {
			return true;
		}
		return false;
	}
	
	public List<PlayerInfo> findByNicknameRegEx(String nickname) throws Exception {
		if(nickname.length() < 3) {
			throw new BadRequestException("nick too short", ErrorCode.PARAM_NOT_CORRECT);
		}
		List<Player> list = playerRepository.findByNicknameRegex(nickname);
		List<PlayerInfo> result = new ArrayList<>();
		for(Player p : list) {
			PlayerInfo info = new PlayerInfo();
			info.setPlayerId(p.getPlayerId());
			info.setNickname(p.getNickname());
			info.setAvatar(avatarManager.getPlayerSmallAvatar(p.getPlayerId()));
			result.add(info);
		}
		return result;
	}
	
	public Page<Player> searchPlayers(String territoryId, String text, Pageable pageRequest) {
		if(Utils.isNotEmpty(text)) {
			return playerRepository.findByTerritoryIdAndText(territoryId, text, pageRequest);
		}
		return playerRepository.findByTerritoryId(territoryId, pageRequest);
	}
}
