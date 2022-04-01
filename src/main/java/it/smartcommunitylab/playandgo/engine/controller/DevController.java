package it.smartcommunitylab.playandgo.engine.controller;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsTransport;
import it.smartcommunitylab.playandgo.engine.report.CampaignPlacing;
import it.smartcommunitylab.playandgo.engine.report.PlayerCampaignPlacingManager;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatsTransportRepository;

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
	PlayerStatsTransportRepository playerStatsTransportRepository;
	
	@Autowired
	PlayerCampaignPlacingManager playerReportManager;
	
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
		LocalDate day = LocalDate.parse("2022-03-28");
		for(int i = 0; i < players; i++) {
			String playerId = "p_" + i;
			List<PlayerStatsTransport> list = new ArrayList<>();
			for(int j = 0; j < tracks; j++) {				
				PlayerStatsTransport ps = new PlayerStatsTransport();
				ps.setPlayerId(playerId);
				ps.setCampaignId("TAA.test1");
				String modeType = modeTypes[RANDOM.nextInt(modeTypes.length)];
				String trackId = playerId + "_" + modeType + "_" + j;
				ps.setModeType(modeType);
				ps.setDistance(rangeMin + (rangeMax - rangeMin) * RANDOM.nextDouble());
				ps.setGlobal(false);
				ps.setDay(day);
				list.add(ps);
				logger.info("save track " + trackId);
			}
			playerStatsTransportRepository.saveAll(list);
		}
	}
	
	@GetMapping("/api/dev/test/campaign/placing")
	public void testCampaignPlacingByTransportMode(HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		LocalDate dateFrom = LocalDate.parse("2022-03-21");
		LocalDate dateTo = LocalDate.parse("2022-03-31");
		for(String modeType : modeTypes) {
			long startTime = System.currentTimeMillis();
			Page<CampaignPlacing> page = playerReportManager.getCampaignPlacingByTransportMode("TAA.test1", modeType, dateFrom, dateTo, PageRequest.of(10, 10));
			long endTime = System.currentTimeMillis();
			logger.info(String.format("query2 [%s]: %s - %s", modeType, page.getSize(), (endTime - startTime)));
		}
		
	}
}
