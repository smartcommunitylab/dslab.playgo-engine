package it.smartcommunitylab.playandgo.engine.controller;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsTransport;
import it.smartcommunitylab.playandgo.engine.report.CampaignPlacing;
import it.smartcommunitylab.playandgo.engine.report.PlayerCampaignPlacingManager;
import it.smartcommunitylab.playandgo.engine.report.PlayerStatus;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@RestController
public class ReportController extends PlayAndGoController {
	private static transient final Logger logger = LoggerFactory.getLogger(ReportController.class);
	
	@Autowired
	PlayerCampaignPlacingManager playerReportManager;
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	
	@GetMapping("/api/report/player/status")
	public PlayerStatus getPlayerStatsu(
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		PlayerStatus status = playerReportManager.getPlayerStatus(player);
		return status;
	}
	
	@GetMapping("/api/report/campaign/placing/transport")
	public Page<CampaignPlacing> getCampaingPlacingByTransportMode(
			@RequestParam String campaignId,
			@RequestParam String scoreType,
			@RequestParam(required = false) String weeklyDay,
			Pageable pageRequest,
			HttpServletRequest request) throws Exception {
		LocalDate weeklyDayDate = null;
		if(Utils.isNotEmpty(weeklyDay)) {
			weeklyDayDate = LocalDate.parse(weeklyDay);
		}		
		Page<CampaignPlacing> page = playerReportManager.getCampaignPlacingByTransportMode(campaignId, scoreType, 
				weeklyDayDate, pageRequest);
		return page;			
	}
	
	@GetMapping("/api/report/campaign/placing/player/transport")
	public CampaignPlacing getPlayerCampaingPlacingByTransportMode(
			@RequestParam String campaignId,
			@RequestParam String playerId,
			@RequestParam String scoreType,
			@RequestParam(required = false) String weeklyDay,
			HttpServletRequest request) throws Exception {
		LocalDate weeklyDayDate = null;
		if(Utils.isNotEmpty(weeklyDay)) {
			weeklyDayDate = LocalDate.parse(weeklyDay);
		}
		CampaignPlacing placing = playerReportManager.getCampaignPlacingByPlayerAndTransportMode(playerId, campaignId, 
				scoreType, weeklyDayDate);
		return placing;
	}
	
	@GetMapping("/api/report/player/transport")
	public List<PlayerStatsTransport> getPlayerPersonalTransportStats(
			@RequestParam(required = false) String weeklyDay,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		LocalDate weeklyDayDate = null;
		if(Utils.isNotEmpty(weeklyDay)) {
			weeklyDayDate = LocalDate.parse(weeklyDay);
		}
		return playerReportManager.getPlayerPersonalTransportStats(player, weeklyDayDate);
	}
	
 }
