package it.smartcommunitylab.playandgo.engine.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.manager.CampaignManager;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

public class ExternalController extends PlayAndGoController {
	private static transient final Logger logger = LoggerFactory.getLogger(ExternalController.class);

	@Autowired
	CampaignManager campaignManager;

	@PostMapping("/api/ext/campaign/{campaignId}/subscribe/territory")
	public CampaignSubscription subscribeCampaignByTerritory(
			@PathVariable String campaignId,
			@RequestParam String nickname,
			@RequestBody Map<String, Object> campaignData,
			HttpServletRequest request) throws Exception {
		//TODO check integration token
		checkAdminRole(request);
		Campaign campaign = campaignManager.getCampaign(campaignId);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		return campaignManager.subscribePlayerByTerritory(nickname, campaign, campaignData);
	}
	
	@DeleteMapping("/api/ext/campaign/{campaignId}/unsubscribe/territory")
	public CampaignSubscription unsubscribeCampaignByTerritory(
			@PathVariable String campaignId,
			@RequestParam String nickname,
			@RequestBody Map<String, Object> campaignData,
			HttpServletRequest request) throws Exception {
		//TODO check integration token
		checkAdminRole(request);
		Campaign campaign = campaignManager.getCampaign(campaignId);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		return campaignManager.unsubscribePlayerByTerritory(nickname, campaign);
	}
	
}
