package it.smartcommunitylab.playandgo.engine.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsTrack;
import it.smartcommunitylab.playandgo.engine.report.CampaignPlacing;
import it.smartcommunitylab.playandgo.engine.report.PlayerReportManager;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatsTrackRepository;

@RestController
public class DevController extends PlayAndGoController {
	private static transient final Logger logger = LoggerFactory.getLogger(DevController.class);
			
	int players = 200;
	int tracks = 2000;
	double rangeMin = 52.0;
	double rangeMax = 1520.0;
	
	String[] modeTypes = new String[] {"WALK", "BIKE", "BUS", "TRAIN"}; 
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH.mm");
	
	@Autowired
	PlayerRepository playerRepository;
	
	@Autowired
	PlayerStatsTrackRepository playerStatsTrackRepository;
	
	@Autowired
	PlayerReportManager playerReportManager;
	
	static final Random RANDOM = new Random();
	
	@PostMapping("/api/dev/players")
	public void addPlayers(HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		for(int i = 0; i < players; i++) {
			Player p = new Player();
			p.setPlayerId("p_" + i);
			p.setNickname("p_" + i);
			p.setTerritoryId("TAA");
			playerRepository.save(p);
			logger.info("save player " + p.getPlayerId());
		}
	}
	
	@PostMapping("/api/dev/tracks")
	public void addTracks(HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		Date fromDate = sdf.parse("2022-02-05 13.20");
		Date toDate = sdf.parse("2022-02-05 13.35");
		for(int i = 0; i < players; i++) {
			String playerId = "p_" + i;
			for(int j = 0; j < tracks; j++) {
				PlayerStatsTrack ps = new PlayerStatsTrack();
				ps.setPlayerId(playerId);
				ps.setCampaignId("TAA.test1");
				String modeType = modeTypes[RANDOM.nextInt(modeTypes.length)];
				ps.setModeType(modeType);
				String trackId = playerId + "_" + modeType + "_" + j;
				ps.setTrackedInstanceId(trackId);
				ps.setDistance(rangeMin + (rangeMax - rangeMin) * RANDOM.nextDouble());
				ps.setStartTime(fromDate);
				ps.setEndTime(toDate);
				playerStatsTrackRepository.save(ps);
				logger.info("save track " + trackId);
			}
		}
	}
	
	@GetMapping("/api/dev/test/campaign/placing")
	public void testCampaignPlacingByTransportMode(HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		Date dateFrom = sdf.parse("2022-01-01 00.00");
		Date dateTo = sdf.parse("2023-01-01 00.00");
		for(String modeType : modeTypes) {
			long startTime = System.currentTimeMillis();
			Page<CampaignPlacing> page = playerReportManager.getCampaignPlacingByTransportModeFull("TAA.test1", modeType, dateFrom, dateTo, PageRequest.of(5, 10));
			long endTime = System.currentTimeMillis();
			logger.info(String.format("query1 [%s]: %s - %s", modeType, page.getSize(), (endTime - startTime)));
		}
		for(String modeType : modeTypes) {
			long startTime = System.currentTimeMillis();
			Page<CampaignPlacing> page = playerReportManager.getCampaignPlacingByTransportMode("TAA.test1", modeType, dateFrom, dateTo, PageRequest.of(5, 10));
			long endTime = System.currentTimeMillis();
			logger.info(String.format("query2 [%s]: %s - %s", modeType, page.getSize(), (endTime - startTime)));
		}
	}
}
