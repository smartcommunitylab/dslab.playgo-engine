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
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.engine.manager.CampaignManager;
import it.smartcommunitylab.playandgo.engine.manager.TerritoryManager;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Territory;

@RestController
public class CampaignController implements PlayAndGoController {
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
	public List<Campaign> getTerritories(HttpServletRequest request) throws Exception {
		return campaignManager.getCampaigns();
	}
	
	@DeleteMapping("/api/campaign/{campaignId}")
	public Campaign deleteTerritory(
			@PathVariable String campaignId,
			HttpServletRequest request) throws Exception {
		return campaignManager.deleteCampaign(campaignId);
	}
}
