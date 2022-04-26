package it.smartcommunitylab.playandgo.engine.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import it.smartcommunitylab.playandgo.engine.dto.PlayerInfo;
import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.manager.AvatarManager;
import it.smartcommunitylab.playandgo.engine.manager.PlayerManager;
import it.smartcommunitylab.playandgo.engine.model.Avatar;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@RestController
public class PlayerController extends PlayAndGoController {
	private static transient final Logger logger = LoggerFactory.getLogger(PlayerController.class);
	
	@Autowired
	PlayerManager playerManager;
	
	@Autowired
	AvatarManager avatarManager;
	
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
		return getCurrentPlayerOrNUll(request);
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
	
	@GetMapping("/api/player/search")
	public List<PlayerInfo> searchNickname(
			@RequestParam String nickname,
			HttpServletRequest request) throws Exception {
		return playerManager.findByNicknameRegEx(nickname);
	}
	
	@PostMapping("/api/player/avatar")
	public Avatar uploadPlayerAvatar(
			@RequestParam("data") MultipartFile data,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		return avatarManager.uploadPlayerAvatar(player, data);
	}
	
	@GetMapping("/api/player/avatar/small")
	public ResponseEntity<?> getPlayerAvatarDataSmall(
			HttpServletResponse response,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		Avatar avatar = avatarManager.getPlayerAvatar(player.getPlayerId());
		if(avatar != null) {
			response.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=86400");
			response.setHeader(HttpHeaders.CONTENT_TYPE, avatar.getContentType());
			response.setIntHeader(HttpHeaders.CONTENT_LENGTH, avatar.getAvatarDataSmall().getData().length);
			return ResponseEntity.ok().contentType(MediaType.parseMediaType(avatar.getContentType())).body(avatar.getAvatarDataSmall().getData());		
		}
		throw new BadRequestException("avatar not found", ErrorCode.IMAGE_NOT_FOUND);
	}

	@GetMapping("/api/player/avatar")
	public ResponseEntity<?> getPlayerAvatarData(
			HttpServletResponse response,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		Avatar avatar = avatarManager.getPlayerAvatar(player.getPlayerId());
		if(avatar != null) {
			response.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=86400");
			response.setHeader(HttpHeaders.CONTENT_TYPE, avatar.getContentType());
			response.setIntHeader(HttpHeaders.CONTENT_LENGTH, avatar.getAvatarData().getData().length);
			return ResponseEntity.ok().contentType(MediaType.parseMediaType(avatar.getContentType())).body(avatar.getAvatarData().getData());		
		}
		throw new BadRequestException("avatar not found", ErrorCode.IMAGE_NOT_FOUND);
	}

}
