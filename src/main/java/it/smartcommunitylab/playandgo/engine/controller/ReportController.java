package it.smartcommunitylab.playandgo.engine.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.report.CampaignPlacing;
import it.smartcommunitylab.playandgo.engine.report.PlayerReportManager;
import it.smartcommunitylab.playandgo.engine.report.PlayerStatus;

@RestController
public class ReportController extends PlayAndGoController {
	private static transient final Logger logger = LoggerFactory.getLogger(ReportController.class);
	
	@Autowired
	PlayerReportManager playerReportManager;
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	
	@GetMapping("/api/report/player/status")
	public PlayerStatus getPlayerStatsu(
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		PlayerStatus status = playerReportManager.getPlayerStatus(player);
		return status;
	}
	
	@GetMapping("/api/report/campaign/placing/transport")
	public List<CampaignPlacing> getCampaingPlacingByTransportMode(
			@RequestParam String campaignId,
			@RequestParam String modeType,
			@RequestParam String dateFrom,
			@RequestParam String dateTo,
			HttpServletRequest request) throws Exception {
		Date dateFromDate = sdf.parse(dateFrom);
		Date dateToDate = sdf.parse(dateTo);
		List<CampaignPlacing> list = playerReportManager.getCampaignPlacingByTransportMode(campaignId, modeType, dateFromDate, dateToDate);
		return list;			
	}
 }
