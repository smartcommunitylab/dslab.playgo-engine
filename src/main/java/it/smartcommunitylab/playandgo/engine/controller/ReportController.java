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

import it.smartcommunitylab.playandgo.engine.manager.PlayerCampaignPlacingManager;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.report.CampaignPlacing;
import it.smartcommunitylab.playandgo.engine.report.PlayerStatus;
import it.smartcommunitylab.playandgo.engine.report.TransportStats;
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
			@RequestParam String modeType,
			@RequestParam(required = false) String dateFrom,
			@RequestParam(required = false) String dateTo,
			Pageable pageRequest,
			HttpServletRequest request) throws Exception {
		LocalDate dateFromDate = null;
		LocalDate dateToDate = null;
		if(Utils.isNotEmpty(dateFrom)) {
			dateFromDate = LocalDate.parse(dateFrom);
		}		
		if(Utils.isNotEmpty(dateTo)) {
			dateToDate = LocalDate.parse(dateTo);
		}		
		Page<CampaignPlacing> page = playerReportManager.getCampaignPlacingByTransportMode(campaignId, modeType, 
				dateFromDate, dateToDate, pageRequest);
		return page;			
	}
	
	@GetMapping("/api/report/campaign/placing/player/transport")
	public CampaignPlacing getPlayerCampaingPlacingByTransportMode(
			@RequestParam String campaignId,
			@RequestParam String playerId,
			@RequestParam String modeType,
			@RequestParam(required = false) String dateFrom,
			@RequestParam(required = false) String dateTo,
			HttpServletRequest request) throws Exception {
		LocalDate dateFromDate = null;
		LocalDate dateToDate = null;
		if(Utils.isNotEmpty(dateFrom)) {
			dateFromDate = LocalDate.parse(dateFrom);
		}		
		if(Utils.isNotEmpty(dateTo)) {
			dateToDate = LocalDate.parse(dateTo);
		}		
		CampaignPlacing placing = playerReportManager.getCampaignPlacingByPlayerAndTransportMode(playerId, campaignId, 
				modeType, dateFromDate, dateToDate);
		return placing;
	}
	
	@GetMapping("/api/report/player/transport/stats")
	public List<TransportStats> getPlayerTransportStats(
			@RequestParam String dateFrom,
			@RequestParam String dateTo,
			@RequestParam String groupMode,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		LocalDate dateFromDate = null;
		LocalDate dateToDate = null;
		if(Utils.isNotEmpty(dateFrom)) {
			dateFromDate = LocalDate.parse(dateFrom);
		}		
		if(Utils.isNotEmpty(dateTo)) {
			dateToDate = LocalDate.parse(dateTo);
		}		
		return playerReportManager.getPlayerTransportStats(player, dateFromDate, dateToDate, groupMode);
	}

	@GetMapping("/api/report/campaign/placing/co2")
	public Page<CampaignPlacing> getCampaingPlacingByCo2(
			@RequestParam String campaignId,
			@RequestParam(required = false) String dateFrom,
			@RequestParam(required = false) String dateTo,
			Pageable pageRequest,
			HttpServletRequest request) throws Exception {
		LocalDate dateFromDate = null;
		LocalDate dateToDate = null;
		if(Utils.isNotEmpty(dateFrom)) {
			dateFromDate = LocalDate.parse(dateFrom);
		}		
		if(Utils.isNotEmpty(dateTo)) {
			dateToDate = LocalDate.parse(dateTo);
		}		
		Page<CampaignPlacing> page = playerReportManager.getCampaignPlacingByCo2(campaignId,  
				dateFromDate, dateToDate, pageRequest);
		return page;			
	}

	@GetMapping("/api/report/campaign/placing/player/co2")
	public CampaignPlacing getPlayerCampaingPlacingByCo2(
			@RequestParam String campaignId,
			@RequestParam String playerId,
			@RequestParam(required = false) String dateFrom,
			@RequestParam(required = false) String dateTo,
			HttpServletRequest request) throws Exception {
		LocalDate dateFromDate = null;
		LocalDate dateToDate = null;
		if(Utils.isNotEmpty(dateFrom)) {
			dateFromDate = LocalDate.parse(dateFrom);
		}		
		if(Utils.isNotEmpty(dateTo)) {
			dateToDate = LocalDate.parse(dateTo);
		}		
		CampaignPlacing placing = playerReportManager.getCampaignPlacingByPlayerAndCo2(playerId, campaignId, 
				dateFromDate, dateToDate);
		return placing;
	}

 }
