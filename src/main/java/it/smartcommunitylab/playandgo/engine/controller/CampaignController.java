package it.smartcommunitylab.playandgo.engine.controller;

import java.util.List;
import java.util.Map;

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
import org.springframework.web.multipart.MultipartFile;

import it.smartcommunitylab.playandgo.engine.dto.PlayerCampaign;
import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.manager.CampaignManager;
import it.smartcommunitylab.playandgo.engine.manager.TerritoryManager;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Image;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerRole.Role;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@RestController
public class CampaignController extends PlayAndGoController {
	private static transient final Logger logger = LoggerFactory.getLogger(CampaignController.class);
	
	@Autowired
	CampaignManager campaignManager;
	
	@Autowired
	TerritoryManager territoryManager;
	
	@PostMapping("/api/campaign")
	public void addCampaign(
			@RequestBody Campaign campaign,
			HttpServletRequest request) throws Exception {
		checkRole(request, Role.territory, campaign.getTerritoryId());
		campaignManager.addCampaign(campaign);
	}
	
	@PutMapping("/api/campaign")
	public void updateCampaign(
			@RequestBody Campaign campaign,
			HttpServletRequest request) throws Exception {
		checkRole(request, campaign.getTerritoryId(), campaign.getCampaignId());
		campaignManager.updateCampaign(campaign);
	}
	
	@GetMapping("/api/campaign/{campaignId}")
	public Campaign getCampaign(
			@PathVariable String campaignId,
			HttpServletRequest request) throws Exception {
		return campaignManager.getCampaign(campaignId);
	}
	
	@GetMapping("/api/campaign")
	public List<Campaign> getCampaigns(
			@RequestParam String territoryId,
			@RequestParam(required = false) Type type,
			HttpServletRequest request) throws Exception {
		if(Utils.isNotEmpty(territoryId)) {
			return campaignManager.getCampaignsByTerritory(territoryId, type);
		}
		return campaignManager.getCampaigns();
	}
	
	@GetMapping("/publicapi/campaign")
	public List<Campaign> getPublicCampaigns(
			@RequestParam(required = false) String territoryId,
			@RequestParam(required = false) Type type) throws Exception {
		return campaignManager.getActiveCampaigns(territoryId, type);
	}
	
	
	@DeleteMapping("/api/campaign/{campaignId}")
	public Campaign deleteCampaign(
			@PathVariable String campaignId,
			HttpServletRequest request) throws Exception {
		Campaign campaign = campaignManager.getCampaign(campaignId);
		if(campaign == null) {
			throw new BadRequestException("campaign not found", ErrorCode.CAMPAIGN_NOT_FOUND);
		}	
		checkRole(request, Role.territory, campaign.getTerritoryId());
		return campaignManager.deleteCampaign(campaignId);
	}
	
	@PostMapping("/api/campaign/{campaignId}/logo")
	public Image uploadCampaignLogo(
			@PathVariable String campaignId,
			@RequestParam("data") MultipartFile data,
			HttpServletRequest request) throws Exception {
		Campaign campaign = campaignManager.getCampaign(campaignId);
		if(campaign == null) {
			throw new BadRequestException("campaign not found", ErrorCode.CAMPAIGN_NOT_FOUND);
		}	
		checkRole(request, campaign.getTerritoryId(), campaign.getCampaignId());
		return campaignManager.uploadCampaignLogo(campaignId, data);
	}
	
	@PostMapping("/api/campaign/{campaignId}/banner")
	public Image uploadCampaignBanner(
			@PathVariable String campaignId,
			@RequestParam("data") MultipartFile data,
			HttpServletRequest request) throws Exception {
		Campaign campaign = campaignManager.getCampaign(campaignId);
		if(campaign == null) {
			throw new BadRequestException("campaign not found", ErrorCode.CAMPAIGN_NOT_FOUND);
		}	
		checkRole(request, campaign.getTerritoryId(), campaign.getCampaignId());
		return campaignManager.uploadCampaignBanner(campaignId, data);
	}
	
	@GetMapping("/api/campaign/my")
	public List<PlayerCampaign> getMyCampaigns(
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		return campaignManager.getPlayerCampaigns(player.getPlayerId());
	}
	
	@PostMapping("/api/campaign/{campaignId}/subscribe")
	public CampaignSubscription subscribeCampaign(
			@PathVariable String campaignId,
			@RequestBody(required = false) Map<String, Object> campaignData,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		return campaignManager.subscribePlayer(player, campaignId, campaignData);
	}
	
	@PutMapping("/api/campaign/{campaignId}/unsubscribe")
	public CampaignSubscription unsubscribeCampaign(
			@PathVariable String campaignId,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		return campaignManager.unsubscribePlayer(player, campaignId);
	}
	
	@PostMapping("/api/campaign/{campaignId}/survey")
	public Map<String, String> addSurvey(
			@PathVariable String campaignId,
			@RequestParam String name,
			@RequestParam String link,
			HttpServletRequest request) throws Exception {
		Campaign campaign = campaignManager.getCampaign(campaignId);
		if(campaign == null) {
			throw new BadRequestException("campaign not found", ErrorCode.CAMPAIGN_NOT_FOUND);
		}	
		checkRole(request, campaign.getTerritoryId(), campaign.getCampaignId());
		return campaignManager.addSurvey(campaignId, name, link);
	}
	
	@DeleteMapping("/api/campaign/{campaignId}/survey")
	public Map<String, String> deleteSurvey(
			@PathVariable String campaignId,
			@RequestParam String name,
			HttpServletRequest request) throws Exception {
		Campaign campaign = campaignManager.getCampaign(campaignId);
		if(campaign == null) {
			throw new BadRequestException("campaign not found", ErrorCode.CAMPAIGN_NOT_FOUND);
		}	
		checkRole(request, campaign.getTerritoryId(), campaign.getCampaignId());
		return campaignManager.deleteSurvey(campaignId, name);
	}
	

}
