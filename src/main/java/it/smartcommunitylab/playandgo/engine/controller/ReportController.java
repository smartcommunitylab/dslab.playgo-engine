package it.smartcommunitylab.playandgo.engine.controller;

import java.text.SimpleDateFormat;
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

import io.swagger.annotations.ApiParam;
import it.smartcommunitylab.playandgo.engine.manager.PlayerCampaignPlacingManager;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.report.CampaignPlacing;
import it.smartcommunitylab.playandgo.engine.report.GameStats;
import it.smartcommunitylab.playandgo.engine.report.PlayerStatus;
import it.smartcommunitylab.playandgo.engine.report.TransportStat;
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
	public Page<CampaignPlacing> getCampaingPlacingByTransportStats(
			@RequestParam String campaignId,
			@RequestParam String metric,
			@RequestParam(required = false) String mean,
			@RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateFrom,
			@RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateTo,
			Pageable pageRequest,
			HttpServletRequest request) throws Exception {
		Page<CampaignPlacing> page = playerReportManager.getCampaignPlacing(campaignId, metric, mean, 
				dateFrom, dateTo, pageRequest);
		return page;			
	}
	
	@GetMapping("/api/report/campaign/placing/player/transport")
	public CampaignPlacing getPlayerCampaingPlacingByTransportMode(
			@RequestParam String campaignId,
			@RequestParam String playerId,
			@RequestParam String metric,
			@RequestParam(required = false) String mean,
			@RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateFrom,
			@RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateTo,
			HttpServletRequest request) throws Exception {
		CampaignPlacing placing = playerReportManager.getCampaignPlacingByPlayer(playerId, campaignId, 
				metric, mean, dateFrom, dateTo);
		return placing;
	}
	
	@GetMapping("/api/report/campaign/placing/game")
	public Page<CampaignPlacing> getCampaingPlacingByGame(
			@RequestParam String campaignId,
			@RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateFrom,
			@RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateTo,
			Pageable pageRequest,
			HttpServletRequest request) throws Exception {
		Page<CampaignPlacing> page = playerReportManager.getCampaignPlacingByGame(campaignId,  
				dateFrom, dateTo, pageRequest);
		return page;			
	}

	@GetMapping("/api/report/campaign/placing/player/game")
	public CampaignPlacing getPlayerCampaingPlacingByGame(
			@RequestParam String campaignId,
			@RequestParam String playerId,
			@RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateFrom,
			@RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateTo,
			HttpServletRequest request) throws Exception {
		CampaignPlacing placing = playerReportManager.getCampaignPlacingByGameAndPlayer(playerId, campaignId, 
				dateFrom, dateTo);
		return placing;
	}

	@GetMapping("/api/report/player/transport/stats")
	public List<TransportStat> getPlayerTransportStats(
			@RequestParam String campaignId,
			@RequestParam String metric,
			@RequestParam(required = false) String groupMode,
			@RequestParam(required = false) String mean,
			@RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateFrom,
			@RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateTo,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		if(Utils.isEmpty(groupMode)) {
			return playerReportManager.getPlayerTransportStats(player.getPlayerId(), campaignId, metric, 
					mean, dateFrom, dateTo);
		} else {
			return playerReportManager.getPlayerTransportStats(player.getPlayerId(), campaignId, groupMode, metric, 
					mean, dateFrom, dateTo);
		}
	}
	
	@GetMapping("/api/report/player/game/stats")
	public List<GameStats> getPlayerGameStats(
			@RequestParam String campaignId,
			@RequestParam String groupMode,
			@RequestParam @ApiParam(value = "yyyy-MM-dd") String dateFrom,
			@RequestParam @ApiParam(value = "yyyy-MM-dd") String dateTo,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		return playerReportManager.getPlayerGameStats(player.getPlayerId(), groupMode, campaignId, dateFrom, dateTo);
	}

	@GetMapping("/api/report/player/transport/record")
	public List<TransportStat> getPlayerTransportRecord(
			@RequestParam String campaignId,
			@RequestParam String metric,
			@RequestParam String groupMode,
			@RequestParam(required = false) String mean,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		return playerReportManager.getPlayerTransportRecord(player.getPlayerId(), campaignId, groupMode, metric, mean);
	}
 }
