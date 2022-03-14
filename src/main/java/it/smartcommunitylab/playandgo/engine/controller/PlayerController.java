package it.smartcommunitylab.playandgo.engine.controller;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.engine.manager.PlayerManager;
import it.smartcommunitylab.playandgo.engine.model.Player;

@RestController
public class PlayerController extends PlayAndGoController {
	private static transient final Logger logger = LoggerFactory.getLogger(PlayerController.class);
	
	@Autowired
	private PlayerManager playerManager;
	
	@PostMapping("/api/player")
	public Player addPlayer(
			@RequestBody Player player,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		return playerManager.addPlayer(player);
	}
	
	@PostMapping("/api/player/register")
	public Player registerPlayer(
			@RequestBody Player player,
			HttpServletRequest request) throws Exception {
		String subject = getCurrentSubject(request);
		player.setPlayerId(subject);
		return playerManager.registerPlayer(player);
	}
	
	@GetMapping("/api/player/{playerId}")
	public Player getPlayer(
			@PathVariable String playerId,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		return playerManager.getPlayer(playerId);
	}
	
	@DeleteMapping("/api/player/{playerId}")
	public Player deletePlayer(
			@PathVariable String playerId,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		Player player = playerManager.deletePlayer(playerId);
		//TODO delete other data
		return player;
	}
	
	@GetMapping("/api/player/profile")
	public Player getProfile(
			HttpServletRequest request) throws Exception {
		return getCurrentPlayer(request);
	}
	
	@PutMapping("/api/player/profile")
	public Player updateProfile(
			@RequestBody Player player,
			HttpServletRequest request) throws Exception {
		Player currentPlayer = getCurrentPlayer(request);
		return playerManager.updatePlayer(player, currentPlayer.getPlayerId());
	}
	
	@GetMapping("/api/player/nick")
	public Boolean checkNickname(
			@RequestParam String nickname,
			HttpServletRequest request) throws Exception {
		return playerManager.findByNickname(nickname);
	}

}
