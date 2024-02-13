package it.smartcommunitylab.playandgo.engine.manager.challenge;

import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import it.smartcommunitylab.playandgo.engine.campaign.city.CityGameDataConverter;
import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.ge.GamificationEngineManager;
import it.smartcommunitylab.playandgo.engine.ge.model.GameStatistics;
import it.smartcommunitylab.playandgo.engine.ge.model.PointConcept;
import it.smartcommunitylab.playandgo.engine.ge.model.PointConceptPeriod;
import it.smartcommunitylab.playandgo.engine.manager.AvatarManager;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeConceptInfo.ChallengeDataType;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeInvitation.ChallengePlayer;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeInvitation.PointConceptRef;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeInvitation.Reward;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerChallenge;
import it.smartcommunitylab.playandgo.engine.notification.CampaignNotificationManager;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerChallengeRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatChallengeRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@Component
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ChallengeManager {
	private static final Logger logger = LoggerFactory.getLogger(ChallengeManager.class);
	
	@Value("${challengeDir}")
	private String challengeDir;
	
	public static final String TARGET = "target";
	public static final String PLAYER1_PRZ = "player1_prz";
	public static final String PLAYER2_PRZ = "player2_prz";
	
	public static final int MILLIS_IN_DAY = 1000 * 60 * 60 * 24;
	public static final long MILLIS_IN_WEEK = 1000 * 60 * 60 * 24 * 7;
		
	private LocalDate lastMonday = LocalDate.now().minusDays(7).with(ChronoField.DAY_OF_WEEK, 1);
	
	private GameStatistics gs;
	
	private DifficultyCalculator dc = new DifficultyCalculator();

	private ObjectMapper mapper = new ObjectMapper();
	
	private Map<String, Reward> rewards;
	
	@Autowired
	private PlayerRepository playerRepository;
	
	@Autowired
	private CampaignRepository campaignRepository;
	
	@Autowired
	PlayerStatChallengeRepository playerStatChallengeRepository;
	
	@Autowired
	PlayerChallengeRepository playerChallengeRepository;
	
	@Autowired
	private GamificationEngineManager gamificationEngineManager;
	
	@Autowired
	private CampaignNotificationManager campaignNotificationManager;
	
	@Autowired
	AvatarManager avatarManager;

	@Autowired
	private CityGameDataConverter gameDataConverter;
	
	@PostConstruct
	private void init() throws Exception {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		rewards = mapper.readValue(Paths.get(challengeDir + "/rewards.json").toFile(), new TypeReference<Map<String, Reward>>() {});
	}
	
	public List<ChallengeChoice> getChallengeStatus(String playerId, String campaignId) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		String json = gamificationEngineManager.getChallengeStatus(playerId, campaign.getGameId());
		if(json == null) {
			throw new BadRequestException("error in GE invocation", ErrorCode.EXT_SERVICE_INVOCATION);
		}
		Inventory inventory = mapper.readValue(json , Inventory.class);
		return inventory.getChallengeChoices();
	}
	
	public List<ChallengeChoice> activateChallengeType(String playerId, String campaignId, String challengeName) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		String json = gamificationEngineManager.activateChallengeByType(playerId, campaign.getGameId(), challengeName, "CHALLENGE_MODEL");
		if(json == null) {
			throw new BadRequestException("error in GE invocation", ErrorCode.EXT_SERVICE_INVOCATION);
		}
		Inventory inventory = mapper.readValue(json , Inventory.class);
		return inventory.getChallengeChoices();
	}
	
	public ChallengeConceptInfo getChallenges(String playerId, String campaignId, ChallengeDataType filter) throws Exception {
		logger.debug(String.format("getChallenges[%s][%s]", playerId, campaignId));
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		Player player = playerRepository.findById(playerId).orElse(null);
		if(player == null) {
			throw new BadRequestException("player not found", ErrorCode.PLAYER_NOT_FOUND);
		}
		String playerStatus = gamificationEngineManager.getPlayerStatus(playerId, campaign.getGameId());
		logger.debug(String.format("getChallenges[%s][%s] status response: %s", playerId, campaignId, playerStatus));
		if(playerStatus == null) {
			throw new BadRequestException("error in GE invocation", ErrorCode.EXT_SERVICE_INVOCATION);
		}
		String jsonChallenges = gamificationEngineManager.getChallenges(playerId, campaign.getGameId(), true);
		logger.debug(String.format("getChallenges[%s][%s] challenges response: %s", playerId, campaignId, jsonChallenges));
		if(jsonChallenges == null) {
			throw new BadRequestException("error in GE invocation", ErrorCode.EXT_SERVICE_INVOCATION);
		}
		ChallengeConceptInfo result = gameDataConverter.convertPlayerChallengesData(jsonChallenges, playerStatus, player, campaign, 1);
		if (filter != null) {
			result.getChallengeData().entrySet().removeIf(x -> !filter.equals(x.getKey()));
		}
		return result;
	}
	
	public ChallengesData getChallangeData(String playerId, String campaignId, String challengeName) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		Player player = playerRepository.findById(playerId).orElse(null);
		if(player == null) {
			throw new BadRequestException("player not found", ErrorCode.PLAYER_NOT_FOUND);
		}
		String playerStatus = gamificationEngineManager.getPlayerStatus(playerId, campaign.getGameId());
		if(playerStatus == null) {
			throw new BadRequestException("error in GE invocation", ErrorCode.EXT_SERVICE_INVOCATION);
		}
		String jsonChallenge = gamificationEngineManager.getChallenge(playerId, campaign.getGameId(), challengeName);
		if(jsonChallenge == null) {
			throw new BadRequestException("error in GE invocation", ErrorCode.EXT_SERVICE_INVOCATION);
		}
		return gameDataConverter.convertPlayerChallengesDataByName(jsonChallenge, playerStatus, player, campaign, 1);
	}
	
	public void chooseChallenge(String playerId, String campaignId, String challengeId) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		boolean result = gamificationEngineManager.chooseChallenge(playerId, campaign.getGameId(), challengeId);
		if(!result) {
			throw new BadRequestException("error in GE invocation", ErrorCode.EXT_SERVICE_INVOCATION);
		}
	}
	
	public void sendInvitation(String playerId, String campaignId, Invitation invitation) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		Player player = playerRepository.findById(playerId).orElse(null);
		if(player == null) {
			throw new BadRequestException("player doesn't exist", ErrorCode.PLAYER_NOT_FOUND);
		}
		ChallengeInvitation ci = new ChallengeInvitation();
		ci.setGameId(campaign.getGameId());
		ci.setProposer(new ChallengePlayer(playerId));
		ci.getGuests().add(new ChallengePlayer(invitation.getAttendeeId()));
		ci.setChallengeModelName(invitation.getChallengeModelName().toString()); // "groupCompetitivePerformance"
		
		LocalDateTime day = LocalDateTime.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS);
		ci.setChallengeStart(new Date(day.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli())); // next saturday
		day = day.plusWeeks(1).minusSeconds(1);
		ci.setChallengeEnd(new Date(day.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli())); // 2 fridays
		
		ci.setChallengePointConcept(new PointConceptRef(invitation.getChallengePointConcept(), "weekly")); // "Walk_Km"
		
		Reward reward = rewards.get(ci.getChallengeModelName());
		
		if (invitation.getChallengeModelName().isCustomPrizes()) {
			Map<String, Double> prizes = targetPrizeChallengesCompute(playerId, invitation.getAttendeeId(), campaign, 
					invitation.getChallengePointConcept(), invitation.getChallengeModelName().toString());
			Map<String, Double> bonusScore = Maps.newTreeMap();
			bonusScore.put(playerId, prizes.get(PLAYER1_PRZ));
			bonusScore.put(invitation.getAttendeeId(), prizes.get(PLAYER2_PRZ));
			reward.setBonusScore(bonusScore);
			ci.setChallengeTarget(prizes.get(TARGET));
		}

		ci.setReward(reward); // from body		
		boolean result = gamificationEngineManager.sendChallengeInvitation(playerId, campaign.getGameId(), ci);
		if(!result) {
			throw new BadRequestException("error in GE invocation", ErrorCode.EXT_SERVICE_INVOCATION);
		}
		Map<String, String> extraData = Maps.newTreeMap();
		extraData.put("opponent", player.getNickname());
		campaignNotificationManager.sendDirectNotification(invitation.getAttendeeId(), campaignId, "INVITATION", extraData);					
	}
	
	public Map<String, Object> getGroupChallengePreview(String playerId, String campaignId, Invitation invitation) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		Player player = playerRepository.findById(playerId).orElse(null);
		if(player == null) {
			throw new BadRequestException("player not found", ErrorCode.PLAYER_NOT_FOUND);
		}
		Player attendee = playerRepository.findById(invitation.getAttendeeId()).orElse(null);
		if(attendee == null) {
			throw new BadRequestException("attendee not found", ErrorCode.PLAYER_NOT_FOUND);
		}
		if (attendee.getPlayerId().equals(player.getPlayerId())) {
			throw new BadRequestException("attendee not allowed", ErrorCode.PARAM_NOT_CORRECT);
		}
		Reward reward = rewards.get(invitation.getChallengeModelName().toString());
		
		Map<String, Object> pars = Maps.newTreeMap();
		pars.put("opponent", attendee.getNickname());
		
		if (invitation.getChallengeModelName().isCustomPrizes()) {
			Map<String, Double> prizes = targetPrizeChallengesCompute(player.getPlayerId(), invitation.getAttendeeId(), campaign, 
					invitation.getChallengePointConcept(), invitation.getChallengeModelName().toString());
			pars.put("rewardBonusScore", prizes.get(PLAYER1_PRZ));
			pars.put("reward", prizes.get(PLAYER1_PRZ));
			pars.put("challengerBonusScore", prizes.get(PLAYER2_PRZ));
			pars.put("challengeTarget", prizes.get(TARGET));
			pars.put("target", prizes.get(TARGET));
		} else {
			pars.put("rewardPercentage", reward.getPercentage());
			pars.put("rewardThreshold", reward.getThreshold());
		}
		
		Map<String, String> descr = gameDataConverter.fillDescription(invitation.getChallengeModelName().toString(), invitation.getChallengePointConcept(), pars);
		Map<String, String> longDescr = gameDataConverter.fillLongDescription(invitation.getChallengeModelName().toString(), invitation.getChallengePointConcept(), pars);
		
		Map<String, Object> result = Maps.newTreeMap();
		result.put("description", descr);
		result.put("longDescription", longDescr);
		result.put("params", pars);
		return result;
	}
	
	public void changeInvitationStatus(String playerId, String campaignId, String challengeId, String status) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		boolean result = gamificationEngineManager.changeChallengeInvitationStatus(playerId, campaign.getGameId(), challengeId, status);
		if(!result) {
			throw new BadRequestException("error in GE invocation", ErrorCode.EXT_SERVICE_INVOCATION);
		}
	}
	
	public List<Map<String, Object>> getChallengeables(String playerId, String campaignId) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		String json = gamificationEngineManager.getChallengables(playerId, campaign.getGameId());
		List<String> ps = mapper.readValue(json, List.class);
		
		List<Map<String, Object>> res = Lists.newArrayList();
		ps.forEach(x -> {
			Player p =  playerRepository.findById(x).orElse(null);
			if (p != null) {
				Map<String, Object> pd = Maps.newTreeMap();
				pd.put("id", x);
				pd.put("nickname", p.getNickname());
				pd.put("avatar", avatarManager.getPlayerSmallAvatar(x));
				res.add(pd);
			}
		});
		
		Collections.sort(res, new Comparator<Map>() {
			@Override
			public int compare(Map o1, Map o2) {
				return ((String)o1.get("nickname")).compareToIgnoreCase((String)o2.get("nickname"));
			}
			
		});
		
		return res;		
	}
	
	public Map<String, Reward> getRewards() {
		return rewards;
	}
	
	public List<Map<String, Object>> getBlackList(String playerId, String campaignId) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		String json = gamificationEngineManager.getBlackList(playerId, campaign.getGameId());
		PlayerBlackList pbl = mapper.readValue(json, PlayerBlackList.class);
		List<Map<String, Object>> res = Lists.newArrayList();
		pbl.getBlockedPlayers().forEach(x -> {
			Player p = playerRepository.findById(x).orElse(null);
			if (p != null) {
				Map<String, Object> pd = Maps.newTreeMap();
				pd.put("id", x);
				pd.put("nickname", p.getNickname());
				pd.put("avatar", avatarManager.getPlayerSmallAvatar(x));
				res.add(pd);
			}
		});
		return res;
	}
	
	public void addToBlackList(String playerId, String campaignId, String blockedPlayerId) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		boolean result = gamificationEngineManager.addToBlackList(playerId, campaign.getGameId(), blockedPlayerId);
		if(!result) {
			throw new BadRequestException("error in GE invocation", ErrorCode.EXT_SERVICE_INVOCATION);
		}
	}

	public void deleteFromBlackList(String playerId, String campaignId, String blockedPlayerId) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		boolean result = gamificationEngineManager.deleteFromBlackList(playerId, campaign.getGameId(), blockedPlayerId);
		if(!result) {
			throw new BadRequestException("error in GE invocation", ErrorCode.EXT_SERVICE_INVOCATION);
		}
	}
	
	public PlayerChallenge storePlayerChallenge(String playerId, String gameId,	String challengeName) throws Exception {
		Campaign campaign = campaignRepository.findByGameId(gameId);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		Player player = playerRepository.findById(playerId).orElse(null);
		if(player == null) {
			throw new BadRequestException("player not found", ErrorCode.PLAYER_NOT_FOUND);
		}
		PlayerChallenge pc = playerChallengeRepository.findByPlayerIdAndCampaignIdAndChallangeId(playerId, 
				campaign.getCampaignId(), challengeName);
		if(pc == null) {
			ChallengesData challangeData = getChallangeData(playerId, campaign.getCampaignId(), challengeName);
			pc = new PlayerChallenge();
			pc.setPlayerId(playerId);
			pc.setCampaignId(campaign.getCampaignId());
			pc.setChallangeId(challengeName);
			pc.setChallengeData(challangeData);
			playerChallengeRepository.save(pc);
		}
		return pc;
	}
	
	public List<PlayerChallenge> getCompletedChallanges(String playerId, String campaignId, long start, long end) throws Exception {
		return playerChallengeRepository.findByDate(playerId, campaignId, start, end, Sort.by(Sort.Direction.DESC, "challengeData.status"));
	}
	
	private Map<String, Double> targetPrizeChallengesCompute(String pId_1, String pId_2, Campaign campaign, String counter, String type) throws Exception {

		prepare();

		Map<Integer, Double> quantiles = getQuantiles(campaign.getGameId(), counter);

		Map<String, Double> res = Maps.newTreeMap();

		String player1 = gamificationEngineManager.getPlayerStatus(pId_1, campaign.getGameId());
        Pair<Double, Double> res1 = getForecast("player1", res, player1, counter);
        double player1_tgt = res1.getFirst();
        double player1_bas = res1.getSecond();
        res.put("player1_tgt", player1_tgt);

        String player2 = gamificationEngineManager.getPlayerStatus(pId_2, campaign.getGameId());
        Pair<Double, Double> res2 = getForecast("player2", res, player2, counter);
        double player2_tgt = res2.getFirst();
        double player2_bas = res2.getSecond();
        res.put("player2_tgt", player2_tgt);		

		double target;
        if (type.equals("groupCompetitiveTime")) {
            target = ChallengesConfig.roundTarget(counter,(player1_tgt + player2_tgt) / 2.0);

            target = checkMaxTargetCompetitive(counter, target);

            res.put(TARGET, target);
            res.put(PLAYER1_PRZ, evaluate(target, player1_bas, counter, quantiles));
            res.put(PLAYER2_PRZ,  evaluate(target, player2_bas, counter, quantiles));
        }
        else if (type.equals("groupCooperative")) {
            target = ChallengesConfig.roundTarget(counter, player1_tgt + player2_tgt);

            target = checkMaxTargetCooperative(counter, target);

            double player1_prz = evaluate(player1_tgt, player1_bas, counter, quantiles);
            double player2_prz = evaluate(player2_tgt, player2_bas, counter, quantiles);
            double prz = Math.max(player1_prz, player2_prz);

            res.put(TARGET, target);
            res.put(PLAYER1_PRZ, prz);
            res.put(PLAYER2_PRZ, prz);
        }  
        return res;
    }
	
    private Pair<Double, Double> getForecast(String nm, Map<String, Double> res, String state, String counter) throws Exception {
        Pair<Double, Double> forecast = forecastMode(state, counter);

        double tgt = forecast.getFirst();
        double bas = forecast.getSecond();

        tgt = checkMinTarget(counter, tgt);

        tgt = ChallengesConfig.roundTarget(counter, tgt);
        
        res.put(nm + "_tgt", tgt);
        res.put(nm + "_bas", bas);

        return new Pair<>(tgt, bas);
    }	

    private double checkMaxTargetCompetitive(String counter, double v) {
	    if ("Walk_Km".equals(counter))
	        return Math.min(70, v);
	    if ("Bike_Km".equals(counter))
	        return Math.min(210, v);
	    if ("green leaves".equals(counter))
	        return Math.min(3000, v);
	
	    return 0.0;
	}

    private double checkMaxTargetCooperative(String counter, double v) {
	    if ("Walk_Km".equals(counter))
	        return Math.min(140, v);
	    if ("Bike_Km".equals(counter))
	        return Math.min(420, v);
	    if ("green leaves".equals(counter))
	        return Math.min(6000, v);
	
	    return 0.0;
    }

	private Double checkMinTarget(String counter, Double v) {
		if ("Walk_Km".equals(counter))
			return Math.max(1, v);
		if ("Bike_Km".equals(counter))
			return Math.max(5, v);
		if ("green leaves".equals(counter))
			return Math.max(50, v);

		// p("WRONG COUNTER");
		return 0.0;
	}

	private Map<Integer, Double> getQuantiles(String appId, String counter) throws Exception {
		// Da sistemare richiesta per dati della settimana precedente, al momento non presenti
		List<GameStatistics> stats = getStatistics(appId, counter);
		if (stats == null || stats.isEmpty()) {
			return null;
		}

		gs = stats.iterator().next();
		return gs.getQuantiles();
	}

	private void prepare() {
		dc = new DifficultyCalculator();
	}

	private Pair<Double, Double> forecastMode(String state, String counter) throws Exception {

        // Check date of registration, decide which method to use
        int week_playing = getWeekPlaying(state, counter);

        if (week_playing == 1) {
            Double baseline = getWeeklyContentMode(state, counter, lastMonday);
            return new Pair<Double, Double>(baseline*ChallengesConfig.booster, baseline);
        } else if (week_playing == 2) {
            return forecastModeSimple(state, counter);
        }

        return forecastWMA(Math.min(ChallengesConfig.week_n, week_playing), state, counter);
    }

    // Weighted moving average
    private Pair<Double, Double> forecastWMA(int v, String state, String counter) throws Exception {

    	LocalDate date = lastMonday;

        double den = 0;
        double num = 0;
        for (int ix = 0; ix < v; ix++) {
            // weight * value
            Double c = getWeeklyContentMode(state, counter, date);
            den += (v -ix) * c;
            num += (v -ix);

            date = date.minusDays(7);
        }

        double baseline = den / num;

        double pv = baseline * ChallengesConfig.booster;

        return new Pair<Double, Double>(pv, baseline);
    }

    private int getWeekPlaying(String state, String counter) throws Exception {
    	LocalDate date = lastMonday;
        int i = 0;
        while (i < 100) {
            // weight * value
            Double c = getWeeklyContentMode(state, counter, date);
            if (c.equals(-1.0))
                break;
            i++;
            date = date.minusDays(7);
        }

        return i;
    }

    private Pair<Double, Double> forecastModeSimple(String state, String counter) throws Exception {

    	LocalDate date = lastMonday;
        Double currentValue = getWeeklyContentMode(state, counter, date);
        date = date.minusDays(7);
        Double lastValue = getWeeklyContentMode(state, counter, date);

        double slope = (lastValue - currentValue) / lastValue;
        slope = Math.abs(slope) * 0.8;
        if (slope > 0.3)
            slope = 0.3;

        double value = currentValue * (1 + slope);
        if (value == 0 || Double.isNaN(value))
            value = 1;


        return new Pair<Double, Double>(value, currentValue);
    }

	private Double evaluate(Double target, Double baseline, String counter, Map<Integer, Double> quantiles) {
		if (baseline == 0) {
			return 100.0;
		}

		Integer difficulty = DifficultyCalculator.computeDifficulty(quantiles, baseline, target);

		double d = (target / Math.max(1, baseline)) - 1;

		int prize = dc.calculatePrize(difficulty, d, counter);

        double bonus =  Math.ceil(prize * ChallengesConfig.competitiveChallengesBooster / 10.0) * 10;

        return Math.min(bonus, 300);
	}
	
	private Double getWeeklyContentMode(String status, String mode, LocalDate execDate) throws Exception {
		Map<String, Object> stateMap = mapper.readValue(status, Map.class);
		Map<String, Object> state = (Map<String, Object>) stateMap.get("state");
		List<Map> gePointsMap = mapper.convertValue(state.get("PointConcept"), new TypeReference<List<Map>>() {
		});

		long time = LocalDate.now().atStartOfDay().atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli();

		List<PointConcept> points = gameDataConverter.convertGEPointConcept(gePointsMap);

		for (PointConcept concept : points) {
			// System.err.println(concept.getName() + " / " + concept.getPeriodType() + " => " + concept.getInstances().size());
			if (mode.equals(concept.getName()) && "weekly".equals(concept.getPeriodType())) {
				for (PointConceptPeriod pcd : concept.getInstances()) {
					if (pcd.getStart() <= time && pcd.getEnd() > time) {
						return pcd.getScore();
					}
				}
			}
		}

		return 0.0;
	}

	private List<GameStatistics> getStatistics(String gameId, String counter) throws Exception {
		String json = gamificationEngineManager.getStatistics(gameId);
		List<GameStatistics> stats = mapper.readValue(json,  new TypeReference<List<GameStatistics>>() {});
		return stats.stream().filter(x -> counter.equals(x.getPointConceptName())).collect(Collectors.toList());
	}

}
