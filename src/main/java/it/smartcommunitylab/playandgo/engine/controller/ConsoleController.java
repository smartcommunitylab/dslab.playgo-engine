package it.smartcommunitylab.playandgo.engine.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.manager.CampaignManager;
import it.smartcommunitylab.playandgo.engine.manager.PlayerManager;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerRole;
import it.smartcommunitylab.playandgo.engine.model.PlayerRole.Role;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRoleRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@RestController
public class ConsoleController extends PlayAndGoController {
	
	@Autowired
	PlayerRoleRepository playerRoleRepository;
	
	@Autowired
	PlayerManager playerManager;
	
	@Autowired
	CampaignManager campaignManager;
	
	@PostMapping("/api/console/role/territory")
	public void addTerritoryManager(
			@RequestParam String userName,
			@RequestParam String territoryId,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		PlayerRole r = new PlayerRole();
		r.setPreferredUsername(userName);
		r.setEntityId(territoryId);
		r.setRole(Role.territory);
		playerRoleRepository.save(r);
	}
	
	@DeleteMapping("/api/console/role/territory")
	public void removeTerritoryManager(
			@RequestParam String userName,
			@RequestParam String territoryId,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		PlayerRole r = playerRoleRepository.findByPreferredUsernameAndRoleAndEntityId(userName, 
				Role.territory, territoryId);
		if(r != null) {
			playerRoleRepository.delete(r);
		}
	}	
	
	@PostMapping("/api/console/role/campaign")
	public void addCampaignManager(
			@RequestParam String userName,
			@RequestParam String campaignId,
			HttpServletRequest request) throws Exception {
		Campaign campaign = campaignManager.getCampaign(campaignId);
		if(campaign == null) {
			throw new BadRequestException("campaign not found", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		checkRole(request, Role.territory, campaign.getTerritoryId());
		PlayerRole r = new PlayerRole();
		r.setPreferredUsername(userName);
		r.setEntityId(campaignId);
		r.setRole(Role.campaign);
		playerRoleRepository.save(r);
	}
	
	@DeleteMapping("/api/console/role/campaign")
	public void removeCampaignManager(
			@RequestParam String userName,
			@RequestParam String campaignId,
			HttpServletRequest request) throws Exception {
		Campaign campaign = campaignManager.getCampaign(campaignId);
		if(campaign == null) {
			throw new BadRequestException("campaign not found", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		checkRole(request, Role.territory, campaign.getTerritoryId());
		PlayerRole r = playerRoleRepository.findByPreferredUsernameAndRoleAndEntityId(userName, 
				Role.campaign, campaignId);
		if(r != null) {
			playerRoleRepository.delete(r);
		}
	}
	
	@GetMapping("/api/console/role/territory")
	public List<PlayerRole> getTerritoryManager(
			@RequestParam String territoryId,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		return playerRoleRepository.findByRoleAndEntityId(Role.territory, territoryId);
	}
	
	@GetMapping("/api/console/role/campaign")
	public List<PlayerRole> getCampaignManager(
			@RequestParam String campaignId,
			HttpServletRequest request) throws Exception {
		Campaign campaign = campaignManager.getCampaign(campaignId);
		if(campaign == null) {
			throw new BadRequestException("campaign not found", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		checkRole(request, Role.territory, campaign.getTerritoryId());
		return playerRoleRepository.findByRoleAndEntityId(Role.campaign, campaignId);
	}
	
	@GetMapping("/api/console/role/my")
	public List<PlayerRole> getMyRoles(HttpServletRequest request) throws Exception {
		String playerId = getCurrentSubject(request);
		return playerRoleRepository.findByPlayerId(playerId);
	}
	
	
	@GetMapping("/api/console/player/search")
	public Page<Player> searchPlayersByTerritory(
			@RequestParam String territoryId,
			@RequestParam(required = false) String text,
			Pageable pageRequest,
			HttpServletRequest request) throws Exception {
		checkRole(request, Role.territory, territoryId);
		return playerManager.searchPlayers(territoryId, text, pageRequest);
	}
	
	

}
