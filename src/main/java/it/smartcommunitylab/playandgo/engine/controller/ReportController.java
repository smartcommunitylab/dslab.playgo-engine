package it.smartcommunitylab.playandgo.engine.controller;

import java.text.SimpleDateFormat;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;
import it.smartcommunitylab.playandgo.engine.dto.CampaignPeriodStatsInfo;
import it.smartcommunitylab.playandgo.engine.manager.PlayerCampaignGamePlacingManager;
import it.smartcommunitylab.playandgo.engine.manager.PlayerCampaignPlacingManager;
import it.smartcommunitylab.playandgo.engine.report.CampaignPlacing;
import it.smartcommunitylab.playandgo.engine.report.GameStats;
import it.smartcommunitylab.playandgo.engine.report.TransportStat;

@RestController
public class ReportController extends PlayAndGoController {
	private static transient final Logger logger = LoggerFactory.getLogger(ReportController.class);
	
	@Autowired
	PlayerCampaignPlacingManager playerReportManager;
    
	@Autowired
	PlayerCampaignGamePlacingManager gamePlacingManager;
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	
//	@GetMapping("/api/report/player/status")
//	public PlayerStatusReport getPlayerStatus(
//			HttpServletRequest request) throws Exception {
//		Player player = getCurrentPlayer(request);
//		PlayerStatusReport status = playerReportManager.getPlayerStatus(player);
//		return status;
//	}
	
	@GetMapping("/api/report/campaign/placing/transport")
	public Page<CampaignPlacing> getCampaingPlacingByTransportStats(
			@RequestParam String campaignId,
			@RequestParam String metric,
			@RequestParam(required = false) String mean,
			@RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateFrom,
			@RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateTo,
			@RequestParam(required = false) boolean groupByGroupId,
			@RequestParam(required = false) String filterByGroupId,
			@ParameterObject Pageable pageRequest,
			HttpServletRequest request) throws Exception {
		Page<CampaignPlacing> page = playerReportManager.getCampaignPlacing(campaignId, metric, mean, 
				dateFrom, dateTo, pageRequest, filterByGroupId, groupByGroupId);
		return page;			
	}
	
	@GetMapping("/api/report/campaign/placing/player/transport")
	public CampaignPlacing getPlayerCampaingPlacingByTransportMode(
			@RequestParam String campaignId,
			@RequestParam String playerId,
			@RequestParam String metric,
			@RequestParam(required = false) String mean,
			@RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateFrom,
			@RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateTo,
			@RequestParam(required = false) String filterByGroupId,
			HttpServletRequest request) throws Exception {
		CampaignPlacing placing = playerReportManager.getCampaignPlacingByPlayer(playerId, campaignId, 
				metric, mean, dateFrom, dateTo, filterByGroupId);
		return placing;
	}
	
    @GetMapping("/api/report/campaign/placing/group/transport")
    public CampaignPlacing geGroupCampaingPlacingByTransportMode(
            @RequestParam String campaignId,
            @RequestParam String groupId,
            @RequestParam String metric,
            @RequestParam(required = false) String mean,
            @RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateFrom,
            @RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateTo,
            HttpServletRequest request) throws Exception {
        CampaignPlacing placing = playerReportManager.getCampaignPlacingByGroup(groupId, campaignId, 
                metric, mean, dateFrom, dateTo);
        return placing;
    }
    
	@GetMapping("/api/report/player/transport/stats")
	public List<TransportStat> getPlayerTransportStats(
			@RequestParam String campaignId,
			@RequestParam String playerId,
			@RequestParam String metric,
			@RequestParam(required = false) String groupMode,
			@RequestParam(required = false) String mean,
			@RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateFrom,
			@RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateTo,
			HttpServletRequest request) throws Exception {
        return playerReportManager.getPlayerTransportStats(playerId, campaignId, groupMode, metric, 
                mean, dateFrom, dateTo);
	}
	
    @GetMapping("/api/report/group/transport/stats")
    public List<TransportStat> getGroupTransportStats(
            @RequestParam String campaignId,
            @RequestParam String groupId,
            @RequestParam String metric,
            @RequestParam(required = false) String groupMode,
            @RequestParam(required = false) String mean,
            @RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateFrom,
            @RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateTo,
            HttpServletRequest request) throws Exception {
        return playerReportManager.getGroupTransportStats(groupId, campaignId, groupMode, metric, 
                mean, dateFrom, dateTo);
    }
    
	@GetMapping("/api/report/player/transport/stats/mean")
	public List<TransportStat> getPlayerTransportStatsGroupByMean(
			@RequestParam String campaignId,
			@RequestParam String playerId,
			@RequestParam String metric,
			@RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateFrom,
			@RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateTo,
			HttpServletRequest request) throws Exception {
		return playerReportManager.getPlayerTransportStatsGroupByMean(playerId, campaignId, metric, dateFrom, dateTo);
	}
	
    @GetMapping("/api/report/group/transport/stats/mean")
    public List<TransportStat> getGroupTransportStatsGroupByMean(
            @RequestParam String campaignId,
            @RequestParam String groupId,
            @RequestParam String metric,
            @RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateFrom,
            @RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateTo,
            HttpServletRequest request) throws Exception {
        return playerReportManager.getGroupTransportStatsGroupByMean(groupId, campaignId, metric, dateFrom, dateTo);
    }
    
    @GetMapping("/api/report/player/transport/record")
    public List<TransportStat> getPlayerTransportRecord(
            @RequestParam String campaignId,
            @RequestParam String playerId,
            @RequestParam String metric,
            @RequestParam String groupMode,
            @RequestParam(required = false) String mean,
            HttpServletRequest request) throws Exception {
        return playerReportManager.getPlayerTransportRecord(playerId, campaignId, groupMode, metric, mean);
    }
	
	@GetMapping("/api/report/player/game/stats")
	public List<GameStats> getPlayerGameStats(
			@RequestParam String campaignId,
			@RequestParam String playerId,
			@RequestParam String groupMode,
			@RequestParam @Parameter(example = "yyyy-MM-dd") String dateFrom,
			@RequestParam @Parameter(example = "yyyy-MM-dd") String dateTo,
			HttpServletRequest request) throws Exception {
		return gamePlacingManager.getPlayerGameStats(playerId, campaignId, groupMode, dateFrom, dateTo);
	}

    @GetMapping("/api/report/group/game/stats")
    public List<GameStats> getGroupGameStats(
            @RequestParam String campaignId,
            @RequestParam String groupId,
            @RequestParam String groupMode,
            @RequestParam @Parameter(example = "yyyy-MM-dd") String dateFrom,
            @RequestParam @Parameter(example = "yyyy-MM-dd") String dateTo,
            HttpServletRequest request) throws Exception {
        return gamePlacingManager.getGroupGameStats(groupId, campaignId, groupMode, dateFrom, dateTo);
    }
    
    @GetMapping("/api/report/campaign/placing/game")
    public Page<CampaignPlacing> getCampaingPlacingByGame(
            @RequestParam String campaignId,
            @RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateFrom,
            @RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateTo,
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) boolean groupByGroupId,
            @RequestParam(required = false) boolean filterByGroupId,
            @ParameterObject Pageable pageRequest,
            HttpServletRequest request) throws Exception {
        Page<CampaignPlacing> page = gamePlacingManager.getCampaignPlacingByGame(campaignId,  
                dateFrom, dateTo, groupId, groupByGroupId, filterByGroupId, pageRequest);
        return page;            
    }

    @GetMapping("/api/report/campaign/placing/player/game")
    public CampaignPlacing getPlayerCampaingPlacingByGame(
            @RequestParam String campaignId,
            @RequestParam String playerId,
            @RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateFrom,
            @RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateTo,
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) boolean filterByGroupId,
            HttpServletRequest request) throws Exception {
        CampaignPlacing placing = gamePlacingManager.getCampaignPlacingByGameAndOwner(playerId, campaignId, 
                dateFrom, dateTo, groupId, false, filterByGroupId);
        return placing;
    }

    @GetMapping("/api/report/campaign/placing/group/game")
    public CampaignPlacing getGroupCampaingPlacingByGame(
            @RequestParam String campaignId,
            @RequestParam String groupId,
            @RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateFrom,
            @RequestParam(required = false) @Parameter(example = "yyyy-MM-dd") String dateTo,
            @RequestParam(required = false) boolean filterByGroupId,
            HttpServletRequest request) throws Exception {
        CampaignPlacing placing = gamePlacingManager.getCampaignPlacingByGameAndOwner(groupId, campaignId, 
                dateFrom, dateTo, groupId, true, filterByGroupId);
        return placing;
    }
    
    @GetMapping("/api/report/player/transport/period")
    public List<CampaignPeriodStatsInfo> getPlayerCampaignPeriodStatsInfo(
            @RequestParam String campaignId,
            @RequestParam String playerId,
            HttpServletRequest request) throws Exception {
        return playerReportManager.getCampaignPeriodStatsInfo(campaignId, playerId);
    }
    
 }
