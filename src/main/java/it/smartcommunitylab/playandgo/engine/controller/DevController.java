package it.smartcommunitylab.playandgo.engine.controller;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.engine.campaign.company.CompanyCampaignTripValidator;
import it.smartcommunitylab.playandgo.engine.campaign.school.SchoolCampaignSubscription;
import it.smartcommunitylab.playandgo.engine.ge.GamificationEngineManager;
import it.smartcommunitylab.playandgo.engine.manager.PlayerCampaignPlacingManager;
import it.smartcommunitylab.playandgo.engine.manager.azienda.PgAziendaleManager;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsTransport;
import it.smartcommunitylab.playandgo.engine.mq.ValidateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.report.CampaignPlacing;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatsTransportRepository;

@RestController
public class DevController extends PlayAndGoController {
	private static transient final Logger logger = LoggerFactory.getLogger(DevController.class);
			
	int players = 200;
	int tracks = 25000;
	double rangeMin = 52.0;
	double rangeMax = 1520.0;
	String weekOfYear = "2022-34";
	String monthOfYear = "2022-8";
	
	String[] modeTypes = new String[] {"WALK", "BIKE", "BUS", "TRAIN"}; 
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH.mm");
	
	@Autowired
	PlayerRepository playerRepository;
	
	@Autowired
	PlayerStatsTransportRepository playerStatsTransportRepository;
	
	@Autowired
	PlayerCampaignPlacingManager playerReportManager;
	
	@Autowired
	PgAziendaleManager aziendaleManager;
	
	@Autowired
	CompanyCampaignTripValidator companyCampaignTripValidator;
	
	@Autowired
	GamificationEngineManager gamificationEngineManager;
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	CampaignSubscriptionRepository campaignSubscriptionRepository;
	
	static final Random RANDOM = new Random();
	
	@GetMapping("/api/dev/survey/url")
	public String getSurveyUrl(
			@RequestParam String campaignId,
			@RequestParam String playerId,
			@RequestParam String surveyName,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign != null) {
			return gamificationEngineManager.createSurveyUrl(playerId, campaign.getGameId(), surveyName, "it");
		}
		return null;
	}
	
	@GetMapping("/api/dev/azienda/subscribe")
	public void subscribeAziendale(
			@RequestParam String campaignId,
			@RequestParam String playerId,
			@RequestParam String companyKey,
			@RequestParam String code,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		aziendaleManager.subscribeCampaign(campaignId, playerId, companyKey, code);
	}
	
	@GetMapping("/api/dev/azienda/unsubscribe")
	public void unsubscribeAziendale(
			@RequestParam String campaignId,
			@RequestParam String playerId,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		aziendaleManager.unsubscribeCampaign(campaignId, playerId);
	}
	
	@GetMapping("/api/dev/azienda/validate")
	public void validateAziendale(
			@RequestParam String campaignId,
			@RequestParam String playerId,
			@RequestParam String trackedInstanceId,
			@RequestParam String campaignPlayerTrackId,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		ValidateCampaignTripRequest msg = new ValidateCampaignTripRequest();
		msg.setCampaignId(campaignId);
		msg.setPlayerId(playerId);
		msg.setTrackedInstanceId(trackedInstanceId);
		msg.setCampaignPlayerTrackId(campaignPlayerTrackId);
		companyCampaignTripValidator.validateTripRequest(msg);
	}
	
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
				ps.setDay("2022-03-28");
				list.add(ps);
				logger.info("save track " + trackId);
			}
			playerStatsTransportRepository.saveAll(list);
		}
	}
	
	@PostMapping("/api/dev/stats")
	public void addStats(
			@RequestParam Integer tracksNum,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate today =  LocalDate.now();
		today.minusDays(5);
		String playerId = "u_fe939cab-1638-45b3-a604-80a3fb018e54";
		List<PlayerStatsTransport> list = new ArrayList<>();
		for(int j = 0; j < tracksNum; j++) {				
			PlayerStatsTransport ps = new PlayerStatsTransport();
			ps.setPlayerId(playerId);
			ps.setCampaignId("TAA.test1");
			ps.setNickname("test");
			String modeType = modeTypes[RANDOM.nextInt(modeTypes.length)];
			String trackId = playerId + "_" + modeType + "_" + j;
			ps.setModeType(modeType);
			ps.setDistance(rangeMin + (rangeMax - rangeMin) * RANDOM.nextDouble());
			ps.setCo2(0.32);
			ps.setDuration(4800);
			ps.setTrackNumber(1);
			ps.setGlobal(false);
			LocalDate day = today.plusDays(RANDOM.nextInt(7));
			ps.setDay(day.format(dtf));
			ps.setWeekOfYear("2022-34");
			ps.setMonthOfYear("2022-08");
			list.add(ps);
			logger.info("save track " + trackId);
		}
		playerStatsTransportRepository.saveAll(list);
	}
	
	@GetMapping("/api/dev/test/campaign/placing")
	public void testCampaignPlacingByTransportMode(HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		for(String modeType : modeTypes) {
			long startTime = System.currentTimeMillis();
			Page<CampaignPlacing> page = playerReportManager.getCampaignPlacing("TAA.test1", "distance", modeType, "2022-03-21", "2022-03-31", PageRequest.of(10, 10), false);
			long endTime = System.currentTimeMillis();
			logger.info(String.format("query2 [%s]: %s - %s", modeType, page.getSize(), (endTime - startTime)));
		}	
	}
	
	@GetMapping("/api/dev/test/campaign/sub/meta")
	public List<CampaignSubscription> testFindByMetaData(HttpServletRequest request) throws Exception {
	    checkAdminRole(request);
	    return campaignSubscriptionRepository.findByMetaData("TAA.school", SchoolCampaignSubscription.groupIdKey, "06d8f1a3-611d-4068-ad58-02f2a72f66db");
	}
}
