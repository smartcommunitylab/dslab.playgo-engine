package it.smartcommunitylab.playandgo.engine.controller;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.engine.manager.GameManager;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerGameStatus;

@RestController
public class GameController extends PlayAndGoController {
	private static transient final Logger logger = LoggerFactory.getLogger(GameController.class);
	
	@Autowired
	GameManager gameManager;

	@GetMapping("/api/game/campaign")
	public PlayerGameStatus getCampaignGameStatus(
			@RequestParam String campaignId,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		return gameManager.getCampaignGameStatus(player.getPlayerId(), campaignId);
	}
}
