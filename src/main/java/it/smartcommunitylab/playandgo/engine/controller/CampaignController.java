package it.smartcommunitylab.playandgo.engine.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.engine.dto.PlayerCampaignDTO;
import it.smartcommunitylab.playandgo.engine.manager.CampaignManager;
import it.smartcommunitylab.playandgo.engine.manager.TerritoryManager;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@RestController
public class CampaignController extends PlayAndGoController {
	private static transient final Logger logger = LoggerFactory.getLogger(CampaignController.class);
	
	@Autowired
	private CampaignManager campaignManager;
	
	@PostMapping("/api/campaign")
	public void saveCampaign(
			@RequestBody Campaign campaign,
			HttpServletRequest request) throws Exception {
		campaignManager.saveTerritory(campaign);
	}
	
	@GetMapping("/api/campaign/{campaignId}")
	public Campaign getCampaign(
			@PathVariable String campaignId,
			HttpServletRequest request) throws Exception {
		return campaignManager.getCampaign(campaignId);
	}
	
	@GetMapping("/api/campaign")
	public List<Campaign> getCampaigns(
			@RequestParam(required=false) String territoryId,
			HttpServletRequest request) throws Exception {
		if(Utils.isNotEmpty(territoryId)) {
			return campaignManager.getCampaignsByTerritory(territoryId);
		}
		return campaignManager.getCampaigns();
	}
	
	@DeleteMapping("/api/campaign/{campaignId}")
	public Campaign deleteCampaign(
			@PathVariable String campaignId,
			HttpServletRequest request) throws Exception {
		return campaignManager.deleteCampaign(campaignId);
	}
	
	@GetMapping("/api/campaign/my")
	public List<PlayerCampaignDTO> getMyCampaigns(
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		return campaignManager.getPlayerCampaigns(player.getPlayerId());
	}
	
	@GetMapping("/api/campaign/{campaignId}/subscribe")
	public CampaignSubscription subscribeCampaign(
			@PathVariable String campaignId,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		return campaignManager.subscribePlayer(player, campaignId);
	}

}
