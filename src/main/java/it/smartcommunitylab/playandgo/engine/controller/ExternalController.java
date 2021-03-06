package it.smartcommunitylab.playandgo.engine.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiParam;
import it.smartcommunitylab.playandgo.engine.dto.PlayerInfo;
import it.smartcommunitylab.playandgo.engine.dto.TrackedInstanceInfo;
import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.manager.CampaignManager;
import it.smartcommunitylab.playandgo.engine.manager.PlayerCampaignPlacingManager;
import it.smartcommunitylab.playandgo.engine.manager.TrackedInstanceManager;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.report.CampaignGroupPlacing;
import it.smartcommunitylab.playandgo.engine.report.GameStats;
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
	TrackedInstanceManager trackedInstanceManager;

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
	
	@GetMapping("/api/ext/campaign/game/group/placing")
	public List<CampaignGroupPlacing> getCampaingGroupPlacingByGame(
			@RequestParam String campaignId,
			@RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateFrom,
			@RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateTo,
			HttpServletRequest request) throws Exception {
		checkAPIRole(request);
		List<CampaignGroupPlacing> list = placingManager.getCampaignGroupPlacingByGame(campaignId,  
				dateFrom, dateTo);
		return list;			
	}

	@GetMapping("/api/ext/campaign/game/group/placing/player")
	public CampaignGroupPlacing getPlayerCampaingGroupPlacingByGame(
			@RequestParam String campaignId,
			@RequestParam String groupId,
			@RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateFrom,
			@RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateTo,
			HttpServletRequest request) throws Exception {
		checkAPIRole(request);
		CampaignGroupPlacing placing = placingManager.getCampaignGroupPlacingByGameAndPlayer(groupId, campaignId, 
				dateFrom, dateTo);
		return placing;
	}

	@GetMapping("/api/ext/campaign/game/group/stats")
	public List<GameStats> getPlayerGameStats(
			@RequestParam String campaignId,
			@RequestParam String groupId,
			@RequestParam String groupMode,
			@RequestParam @ApiParam(value = "yyyy-MM-dd") String dateFrom,
			@RequestParam @ApiParam(value = "yyyy-MM-dd") String dateTo,
			HttpServletRequest request) throws Exception {
		checkAPIRole(request);
		return placingManager.getGroupGameStats(groupId, campaignId, groupMode, dateFrom, dateTo);
	}

	@GetMapping("/api/ext/territory/players")
	public Page<PlayerInfo> searchPlayers(@RequestParam(required = false) String txt, @RequestParam String territory, Pageable pageRequest) {
		if (StringUtils.hasText(txt)) {
			return playerRepository.findByTerritoryIdAndNicknameText(territory, txt, pageRequest).map(p -> toPlayerInfo(p));
		} else {
			return playerRepository.findByTerritoryId(territory, pageRequest).map(p -> toPlayerInfo(p));			
		}
	}
	@GetMapping("/api/ext/territory/players/{playerId}")
	public PlayerInfo getPlayer(@RequestParam String territory, @PathVariable String playerId) {
		return playerRepository.findByTerritoryIdAndPlayerIdIn(territory, Collections.singleton(playerId)).stream().findFirst().map(p -> toPlayerInfo(p)).orElse(null);			
	}
	
	@GetMapping("/api/ext/track/{campaignId}/{playerId}/{trackedInstanceId}")
	public TrackedInstanceInfo getTrackedInstanceInfo(
			@PathVariable String campaignId,
			@PathVariable String playerId,
			@PathVariable String trackedInstanceId) throws Exception {
		return trackedInstanceManager.getTrackedInstanceInfo(playerId, trackedInstanceId, campaignId);
	}
	
	/**
	 * @param p
	 * @return
	 */
	private PlayerInfo toPlayerInfo(Player p) {
		PlayerInfo info = new PlayerInfo();
		info.setNickname(p.getNickname());
		info.setPlayerId(p.getPlayerId());
		return info;
	}
	
}
