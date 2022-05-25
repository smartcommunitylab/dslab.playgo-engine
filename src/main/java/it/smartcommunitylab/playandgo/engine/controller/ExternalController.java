package it.smartcommunitylab.playandgo.engine.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.manager.CampaignManager;
import it.smartcommunitylab.playandgo.engine.manager.PlayerCampaignPlacingManager;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@RestController
public class ExternalController extends PlayAndGoController {
	private static transient final Logger logger = LoggerFactory.getLogger(ExternalController.class);

	@Autowired
	CampaignManager campaignManager;
	
	@Autowired
	PlayerCampaignPlacingManager placingManager;
	
	@Autowired
	PlayerRepository playerRepository;

	@PostMapping("/api/ext/campaign/subscribe/territory")
	public CampaignSubscription subscribeCampaignByTerritory(
			@RequestParam String campaignId,
			@RequestParam String nickname,
			@RequestBody Map<String, Object> campaignData,
			HttpServletRequest request) throws Exception {
		checkAPIRole(request);
		Campaign campaign = campaignManager.getCampaign(campaignId);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		return campaignManager.subscribePlayerByTerritory(nickname, campaign, campaignData);
	}
	
	@DeleteMapping("/api/ext/campaign/unsubscribe/territory")
	public CampaignSubscription unsubscribeCampaignByTerritory(
			@RequestParam String campaignId,
			@RequestParam String nickname,
			@RequestBody Map<String, Object> campaignData,
			HttpServletRequest request) throws Exception {
		checkAPIRole(request);
		Campaign campaign = campaignManager.getCampaign(campaignId);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		return campaignManager.unsubscribePlayerByTerritory(nickname, campaign);
	}
	
	@PostMapping("/api/ext/campaign/game/placing")
	public Map<String, Double> getCampaignPlacing(
			@RequestParam String campaignId,
			@RequestBody List<String> nicknames,
			HttpServletRequest request) throws Exception {
		checkAPIRole(request);
		Campaign campaign = campaignManager.getCampaign(campaignId);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		Map<String, Double> result = new HashMap<>();
		for(String nickname : nicknames) {
			Player player = playerRepository.findByNicknameIgnoreCase(nickname);
			if(player != null) {
				result.put(nickname, placingManager.getPlayerGameTotalScore(nickname, campaignId));
			}
		}
		return result;
	}
	
	
	
}
