package it.smartcommunitylab.playandgo.engine.manager.challenge;

import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stringtemplate.v4.ST;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Range;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.ge.GamificationEngineManager;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeConceptInfo.ChallengeDataType;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeInvitation.ChallengePlayer;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeInvitation.PointConceptRef;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeInvitation.Reward;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@Component
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ChallengeManager {
	private static final Logger logger = LoggerFactory.getLogger(ChallengeManager.class);
	
	@Value("${challengeDir}")
	private String challengeDir;
	
	// point concept fields
	private static final String STATE = "state";
	private static final String PLAYER_ID = "playerId";
	private static final String POINT_CONCEPT = "PointConcept";
	private static final String PC_GREEN_LEAVES = "green leaves";
	private static final String PC_NAME = "name";
	private static final String PC_SCORE = "score";
	private static final String PC_PERIOD = "period";
	private static final String PC_PERIODS = "periods";
	private static final String PC_START = "start";
	private static final String PC_WEEKLY = "weekly";	
	private static final String PC_IDENTIFIER = "identifier";
	private static final String PC_INSTANCES = "instances";
	private static final String PC_END = "end";
	
	// challange fields
	private static final String CHAL_FIELDS_PERIOD_NAME = "periodName";
	private static final String CHAL_FIELDS_COUNTER_NAME = "counterName";
	private static final String CHAL_FIELDS_BONUS_SCORE = "bonusScore";
	private static final String CHAL_FIELDS_TARGET = "target";
	private static final String CHAL_FIELDS_PERIOD_TARGET = "periodTarget";
	private static final String CHAL_FIELDS_INITIAL_BADGE_NUM = "initialBadgeNum";
	private static final String CHAL_FIELDS_OTHER_ATTENDEE_SCORES = "otherAttendeeScores";
	private static final String CHAL_FIELDS_CHALLENGE_SCORE = "challengeScore";
	private static final String CHAL_FIELDS_CHALLENGE_TARGET = "challengeTarget";
	private static final String CHAL_FIELDS_PLAYER_ID = "playerId";
	private static final String CHAL_FIELDS_PROPOSER = "proposer";
	private static final String CHAL_FIELDS_CHALLENGE_SCORE_NAME = "challengeScoreName";
	private static final String CHAL_FIELDS_CHALLENGE_REWARD = "rewardBonusScore";
	
	// new challenge types
	private static final String CHAL_MODEL_PERCENTAGE_INC = "percentageIncrement";
	private static final String CHAL_MODEL_ABSOLUTE_INC = "absoluteIncrement";
	private static final String CHAL_MODEL_REPETITIVE_BEAV = "repetitiveBehaviour";
	private static final String CHAL_MODEL_NEXT_BADGE = "nextBadge";
	private static final String CHAL_MODEL_COMPLETE_BADGE_COLL = "completeBadgeCollection";
	private static final String CHAL_MODEL_SURVEY = "survey";
	private static final String CHAL_MODEL_POICHECKIN = "poiCheckin";
	private static final String CHAL_MODEL_CHECKIN = "checkin";
	private static final String CHAL_MODEL_CLASSPOSITION = "leaderboardPosition";
	private static final String CHAL_MODEL_GROUP_COMPETITIVE_PERFORMANCE = "groupCompetitivePerformance";
	private static final String CHAL_MODEL_GROUP_COMPETITIVE_TIME = "groupCompetitiveTime";
	private static final String CHAL_MODEL_GROUP_COOPERATIVE = "groupCooperative";
	private static final String CHAL_MODEL_INCENTIVE_GROUP = "incentiveGroupChallengeReward";
	
	public static final String TARGET = "target";
	public static final String PLAYER1_PRZ = "player1_prz";
	public static final String PLAYER2_PRZ = "player2_prz";
	
	public static final int MILLIS_IN_DAY = 1000 * 60 * 60 * 24;
	public static final long MILLIS_IN_WEEK = 1000 * 60 * 60 * 24 * 7;
		
	private LocalDate lastMonday = LocalDate.now().minusDays(7).with(ChronoField.DAY_OF_WEEK, 1);
	
	private GameStatistics gs;
	
	private GamificationCache gamificationCache;
	
	private BadgesCache badgeCache;

	private DifficultyCalculator dc = new DifficultyCalculator();

	private ObjectMapper mapper = new ObjectMapper();
	
	private Map<String, Reward> rewards;
	
	@Autowired
	private PlayerRepository playerRepository;
	
	@Autowired
	private CampaignRepository campaignRepository;
	
	@Autowired
	private GamificationEngineManager gamificationEngineManager;
	
	private Map<String, ChallengeStructure> challengeStructureMap;
	private Map<String, ChallengeLongDescrStructure> challengeLongStructureMap;

	private Map<String, List> challengeDictionaryMap;
	private Map<String, String> challengeReplacements;

	private static final Map<String, String> UNIT_MAPPING = Stream.of(new String[][] {
		  { "daily", "days" }, 
		  { "weekly", "weeks" }, 
		}).collect(Collectors.toMap(data -> data[0], data -> data[1]));
	
	@PostConstruct
	private void init() throws Exception {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		challengeStructureMap = Maps.newTreeMap();
		challengeLongStructureMap = Maps.newTreeMap();

		List list = mapper.readValue(Paths.get(challengeDir + "/challenges.json").toFile(), List.class);
		for (Object o : list) {
			ChallengeStructure challenge = mapper.convertValue(o, ChallengeStructure.class);

			String key = challenge.getName() + (challenge.getFilter() != null ? ("#" + challenge.getFilter()) : "");
			challengeStructureMap.put(key, challenge);
		}
		
		list = mapper.readValue(Paths.get(challengeDir + "/challenges_descriptions.json").toFile(), List.class);
		for (Object o : list) {
			ChallengeLongDescrStructure challenge = mapper.convertValue(o, ChallengeLongDescrStructure.class);

			String key = challenge.getModelName() + (challenge.getFilter() != null ? ("#" + challenge.getFilter()) : "");
			challengeLongStructureMap.put(key, challenge);
		}

		challengeDictionaryMap = mapper.readValue(Paths.get(challengeDir + "/challenges_dictionary.json").toFile(), Map.class);
		challengeReplacements = mapper.readValue(Paths.get(challengeDir + "/challenges_replacements.json").toFile(), Map.class);
		
		rewards = mapper.readValue(Paths.get(challengeDir + "/rewards.json").toFile(), new TypeReference<Map<String, Reward>>() {});
		
		gamificationCache = new GamificationCache(gamificationEngineManager);
		
		badgeCache = new BadgesCache(challengeDir + "/badges.json");
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
		gamificationCache.invalidatePlayer(playerId, campaign.getGameId());
		Inventory inventory = mapper.readValue(json , Inventory.class);
		return inventory.getChallengeChoices();
	}
	
	public ChallengeConceptInfo getChallenges(String playerId, String campaignId, ChallengeDataType filter) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		Player player = playerRepository.findById(playerId).orElse(null);
		if(player == null) {
			throw new BadRequestException("player not found", ErrorCode.PLAYER_NOT_FOUND);
		}
		String json = gamificationEngineManager.getGameStatus(playerId, campaign.getGameId());
		if(json == null) {
			throw new BadRequestException("error in GE invocation", ErrorCode.EXT_SERVICE_INVOCATION);
		}
		PlayerStatus playerStatus = convertPlayerData(json, playerId, campaign.getGameId(), player.getNickname(), 
				gamificationEngineManager.getPlaygoURL(), 1, player.getLanguage());
		if (filter != null) {
			playerStatus.getChallengeConcept().getChallengeData().entrySet().removeIf(x -> !filter.equals(x.getKey()));
		}
		return playerStatus.getChallengeConcept();
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
		gamificationCache.invalidatePlayer(playerId, campaign.getGameId());
	}
	
	public void sendInvitation(String playerId, String campaignId, Invitation invitation) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		ChallengeInvitation ci = new ChallengeInvitation();
		ci.setGameId(campaign.getGameId());
		ci.setProposer(new ChallengePlayer(playerId));
		ci.getGuests().add(new ChallengePlayer(invitation.getAttendeeId()));
		ci.setChallengeModelName(invitation.getChallengeModelName().toString()); // "groupCompetitivePerformance"
		
		LocalDateTime day = LocalDateTime.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).truncatedTo(ChronoUnit.DAYS);
		ci.setChallengeStart(new Date(day.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli())); // next saturday
		day = day.plusWeeks(1).minusSeconds(1);
		ci.setChallengeEnd(new Date(day.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli())); // 2 fridays
		
		ci.setChallengePointConcept(new PointConceptRef(invitation.getChallengePointConcept(), "weekly")); // "Walk_Km"
		
		Reward reward = rewards.get(ci.getChallengeModelName());
		
		if (invitation.getChallengeModelName().isCustomPrizes()) {
			Map<String, Double> prizes = targetPrizeChallengesCompute(playerId, invitation.getAttendeeId(), campaign.getGameId(), 
					invitation.getChallengePointConcept(), invitation.getChallengeModelName().toString());
			Map<String, Double> bonusScore = Maps.newTreeMap();
			bonusScore.put(playerId, prizes.get(PLAYER1_PRZ));
			bonusScore.put(invitation.getAttendeeId(), prizes.get(PLAYER2_PRZ));
			reward.setBonusScore(bonusScore);
			ci.setChallengeTarget(prizes.get(TARGET));
		}

		ci.setReward(reward); // from body		
		boolean result = gamificationEngineManager.sendChallengeInvitation(playerId, campaign.getGameId(), ci);
		if(result) {
			throw new BadRequestException("error in GE invocation", ErrorCode.EXT_SERVICE_INVOCATION);
		}
		gamificationCache.invalidatePlayer(playerId, campaign.getGameId());
		//TODO send direct notification
//		Map<String, String> extraData = Maps.newTreeMap();
//		extraData.put("opponent", player.getNickname());
//		notificationsManager.sendDirectNotification(appId, attendee, "INVITATION", extraData);					
	}
	
	public Map<String, String> getGroupChallengePreview(String playerId, String campaignId, Invitation invitation) throws Exception {
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
			Map<String, Double> prizes = targetPrizeChallengesCompute(player.getPlayerId(), invitation.getAttendeeId(), campaign.getGameId(), 
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
		
		String descr = fillDescription(invitation.getChallengeModelName().toString(), invitation.getChallengePointConcept(), pars, player.getLanguage());
		String longDescr = fillLongDescription(invitation.getChallengeModelName().toString(), invitation.getChallengePointConcept(), pars, player.getLanguage());
		
		Map<String, String> result = Maps.newTreeMap();
		result.put("description", descr);
		result.put("longDescription", longDescr);
		return result;
	}
	
	public void changeInvitationStatus(String playerId, String campaignId, String challengeName, String status) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		boolean result = gamificationEngineManager.changeChallengeInvitationStatus(playerId, campaign.getGameId(), challengeName, status);
		if(result) {
			throw new BadRequestException("error in GE invocation", ErrorCode.EXT_SERVICE_INVOCATION);
		}
		gamificationCache.invalidatePlayer(playerId, campaign.getGameId());
	}
	
	public List<Map<String, String>> getChallengables(String playerId, String campaignId) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		String json = gamificationEngineManager.getChallengables(playerId, campaignId);
		List<String> ps = mapper.readValue(json, List.class);
		
		List<Map<String, String>> res = Lists.newArrayList();
		ps.forEach(x -> {
			Player p =  playerRepository.findById(x).orElse(null);
			if (p != null) {
				Map<String, String> pd = Maps.newTreeMap();
				pd.put("id", x);
				pd.put("nickname", p.getNickname());
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
	
	public List<Map<String, String>> getBlackList(String playerId, String campaignId) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		String json = gamificationEngineManager.getBlackList(playerId, campaign.getGameId());
		PlayerBlackList pbl = mapper.readValue(json, PlayerBlackList.class);
		List<Map<String, String>> res = Lists.newArrayList();
		pbl.getBlockedPlayers().forEach(x -> {
			Player p = playerRepository.findById(x).orElse(null);
			if (p != null) {
				Map<String, String> pd = Maps.newTreeMap();
				pd.put("id", x);
				pd.put("nickname", p.getNickname());
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
		if(result) {
			throw new BadRequestException("error in GE invocation", ErrorCode.EXT_SERVICE_INVOCATION);
		}
		gamificationCache.invalidatePlayer(playerId, campaign.getGameId());		
	}

	public void deleteFromBlackList(String playerId, String campaignId, String blockedPlayerId) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		boolean result = gamificationEngineManager.deleteFromBlackList(playerId, campaign.getGameId(), blockedPlayerId);
		if(result) {
			throw new BadRequestException("error in GE invocation", ErrorCode.EXT_SERVICE_INVOCATION);
		}
		gamificationCache.invalidatePlayer(playerId, campaign.getGameId());
	}
	
	private List<ChallengeConcept> parse(String data) throws Exception {
		List<ChallengeConcept> concepts = Lists.newArrayList();

		Map playerMap = mapper.readValue(data, Map.class);
		if (playerMap.containsKey("state")) {
			Map stateMap = mapper.convertValue(playerMap.get("state"), Map.class);
			if (stateMap.containsKey("ChallengeConcept")) {
				List conceptList = mapper.convertValue(stateMap.get("ChallengeConcept"), List.class);
				for (Object o : conceptList) {
					ChallengeConcept concept = mapper.convertValue(o, ChallengeConcept.class);
					concepts.add(concept);
				}
			}
		}
		return concepts;
	}
	
	private List<PointConcept> convertGEPointConcept(List<Map> gePointsMap) {
		List<PointConcept> result = Lists.newArrayList();
		
		for (Map gePointMap: gePointsMap) {
//			PointConcept pc = new PointConcept();
//			pc.setName((String)gePointMap.get(PC_NAME));
//			pc.setScore(((Double)gePointMap.get(PC_SCORE)));
//			pc.setPeriodType(PC_WEEKLY);
			Map<String, Object> periods = (Map)gePointMap.get(PC_PERIODS);
			for (String period : periods.keySet()) {
				PointConcept pc = new PointConcept();
				pc.setName((String)gePointMap.get(PC_NAME));
				pc.setScore(((Double)gePointMap.get(PC_SCORE)));
				pc.setPeriodType(period);
				Map pMap = (Map)periods.get(period);
				if (pMap != null) {
					pc.setStart((Long)pMap.get(PC_START));
					pc.setPeriodDuration((Integer)pMap.get(PC_PERIOD));
					pc.setPeriodIdentifier((String)pMap.get(PC_IDENTIFIER));
					if (pMap.containsKey(PC_INSTANCES)) {
						Map<Object, Map> instances = (Map<Object, Map>)pMap.get(PC_INSTANCES);
						for (Map inst: instances.values()) {
							PointConceptPeriod pcp = mapper.convertValue(inst, PointConceptPeriod.class);
							pc.getInstances().add(pcp);
						}
					}
				}
				result.add(pc);
			}
		}
		return result;
	}
	
	// Method correctChallengeData: used to retrieve the challenge data objects from the user profile data
	private ChallengeConceptInfo convertChallengeData(String playerId, String gameId, String profile, int type, String language, List<PointConcept> pointConcept, List<BadgeCollectionConcept> bcc_list) throws Exception {
    	ListMultimap<ChallengeDataType, ChallengesData> challengesMap = ArrayListMultimap.create();
    	
    	ChallengeConceptInfo result = new ChallengeConceptInfo();
    	if(profile != null && !profile.isEmpty()){
    		
    		List<ChallengeConcept> challengeList = parse(profile);
    		
    		if(challengeList != null){
				for(ChallengeConcept challenge: challengeList){
					String name = challenge.getName();
					String modelName = challenge.getModelName();
					long start = challenge.getStart().getTime();
					long end = challenge.getEnd().getTime();
					Boolean completed = challenge.isCompleted();
					String state = challenge.getState();
					long dateCompleted = challenge.getDateCompleted() != null ? challenge.getDateCompleted().getTime() : 0L;
					int bonusScore = 0;
					String periodName = "";
					String counterName = "";
					double target = 0;
					double periodTarget = 0;
					String badgeCollectionName = "";
					int initialBadgeNum = 0;
					Map<String, Object> otherAttendeeScores = null;
					
					if(challenge.getFields() != null){
						bonusScore = ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_BONUS_SCORE, 0)).intValue();
						periodName = (String)challenge.getFields().getOrDefault(CHAL_FIELDS_PERIOD_NAME,"");
						counterName = (String)challenge.getFields().getOrDefault(CHAL_FIELDS_COUNTER_NAME,"");
						target =  ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_TARGET,0)).doubleValue(); 
						badgeCollectionName = (String)challenge.getFields().getOrDefault(CHAL_FIELDS_COUNTER_NAME,"");
						initialBadgeNum = ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_INITIAL_BADGE_NUM,0)).intValue();
						periodTarget = ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_PERIOD_TARGET,0)).doubleValue();
						List otherAttendeeScoresList = (List)challenge.getFields().getOrDefault(CHAL_FIELDS_OTHER_ATTENDEE_SCORES, Collections.EMPTY_LIST);
						if (!otherAttendeeScoresList.isEmpty()) {
							otherAttendeeScores = (Map)otherAttendeeScoresList.get(0);
						}
					}

					if (target == 0) {
						target = 1;
					}
					
					// Convert data to old challenges models
//						final String ch_point_type = challData.getBonusPointType();
					final long now = System.currentTimeMillis();
					
	    			ChallengesData challengeData = new ChallengesData();
	    			challengeData.setChallId(name);

    				challengeData.setType(modelName);
    				challengeData.setActive(now < end);
    				challengeData.setSuccess(completed);
    				challengeData.setStartDate(start);
    				challengeData.setEndDate(end);
    				challengeData.setDaysToEnd(calculateRemainingDays(end, now));
    				challengeData.setChallCompletedDate(dateCompleted);
    				challengeData.setUnit(counterName);
	    			
    				double row_status = 0D;
    				int status = 0;
    				
    				switch (modelName) {
    					case CHAL_MODEL_REPETITIVE_BEAV:
		    				double successes = retrieveRepeatitiveStatusFromCounterName(counterName, periodName, pointConcept, start, end, target); 
		    				row_status = successes;
		    				status = Math.min(100, (int)(100.0 * successes / periodTarget));
		    				challengeData.setChallTarget((int)periodTarget);
		    				// update unit for repetitive behavior: correspond to the number of periods
		    				challengeData.setUnit(UNIT_MAPPING.get(periodName));
	    					break;
	    				case CHAL_MODEL_PERCENTAGE_INC:
	    				case CHAL_MODEL_ABSOLUTE_INC: {
		    				double earned = retrieveCorrectStatusFromCounterName(counterName, periodName, pointConcept, start, end); 
		    				row_status = earned;
		    				status = Math.min(100, (int)(100.0 * earned / target));
	    					break;
	    				}
	    				case CHAL_MODEL_NEXT_BADGE: {
		    				int count = getEarnedBadgesFromList(bcc_list, badgeCollectionName, initialBadgeNum);
		    				if(!challengeData.getActive()){	// NB: fix to avoid situation with challenge not win and count > target
		    					if(completed){
		    						count = (int)target;
		    					} else {
		    						count = (int)target - 1;
		    					}
		    				}
		    				row_status = count;
		    				status = Math.min(100, (int)(100.0 * count / target));
		    				break;
	    				}
	    				case CHAL_MODEL_SURVEY: {
		    				if(completed) {
	    						row_status = 1; 
	    						status = 100;
	    					}
		    				// survey link to be passed
		    				String link = gamificationEngineManager.createSurveyUrl(playerId, gameId, (String)challenge.getFields().get("surveyType"), language);
		    				challenge.getFields().put("surveylink", link);
		    				break;
	    				}
	    				case CHAL_MODEL_INCENTIVE_GROUP: {
		    				if(completed) {
	    						row_status = 1; 
	    						status = 100;
	    					}	    					
	    					break;
	    				}
	    				case CHAL_MODEL_GROUP_COMPETITIVE_PERFORMANCE : {
	    					row_status = (Double)challenge.getFields().get(CHAL_FIELDS_CHALLENGE_SCORE);
	    					double other_row_status = (Double)otherAttendeeScores.get(CHAL_FIELDS_CHALLENGE_SCORE);
	    					double total = row_status + other_row_status;
	    					int other_status = 0;
	    					if (total != 0) {
	    						status = (int)(100 * row_status / total);
	    						other_status = 100 - status;
	    					}
	    					
	    					String unit = (String)challenge.getFields().getOrDefault(CHAL_FIELDS_CHALLENGE_SCORE_NAME, "");
	    					challengeData.setUnit(unit);
	    					
	    					String proposer = (String)challenge.getFields().get(CHAL_FIELDS_PROPOSER);
	    					challengeData.setProposerId(proposer);
	    					
	    					String otherPlayerId = (String)otherAttendeeScores.get(CHAL_FIELDS_PLAYER_ID); 
	    					Player otherPlayer = playerRepository.findById(playerId).orElse(null);
	    					
	    					String nickname = null;
	    					if (otherPlayer != null) {
	    						nickname = otherPlayer.getNickname();
	    						challenge.getFields().put("opponent", nickname);
	    					}
	    					
	    					OtherAttendeeData otherAttendeeData = new OtherAttendeeData();
	    					otherAttendeeData.setRow_status(round(other_row_status, 2));
	    					otherAttendeeData.setStatus(other_status);
	    					otherAttendeeData.setPlayerId(otherPlayerId);
	    					otherAttendeeData.setNickname(nickname);
	    					
	    					challengeData.setOtherAttendeeData(otherAttendeeData);
	    					
//	    					bonusScore = ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_CHALLENGE_REWARD, 0)).intValue();
	    					
	    					break;
	    				}	    		
						case CHAL_MODEL_GROUP_COMPETITIVE_TIME: {
							row_status = (Double) challenge.getFields().get(CHAL_FIELDS_CHALLENGE_SCORE);
							double other_row_status = (Double) otherAttendeeScores.get(CHAL_FIELDS_CHALLENGE_SCORE);
							double challengeTarget = Math.ceil((Double) challenge.getFields().get(CHAL_FIELDS_CHALLENGE_TARGET));
							target = (int)challengeTarget;
							int other_status = 0;
							if (challengeTarget != 0) {
								status = (int) (100 * row_status / challengeTarget);
								other_status = (int) (100 * other_row_status / challengeTarget);
							}
							
							if (status + other_status > 100) {
								float coeff = (float)(status + other_status) / 100;
								status = Math.round(status / coeff);
								other_status = Math.round(other_status / coeff);
							}							
	
							String unit = (String)challenge.getFields().getOrDefault(CHAL_FIELDS_CHALLENGE_SCORE_NAME, "");
							challengeData.setUnit(unit);
	
							String proposer = (String)challenge.getFields().get(CHAL_FIELDS_PROPOSER);
							challengeData.setProposerId(proposer);
	
							String otherPlayerId = (String) otherAttendeeScores.get(CHAL_FIELDS_PLAYER_ID);
							Player otherPlayer = playerRepository.findById(otherPlayerId).orElse(null);
	
							String nickname = null;
							if (otherPlayer != null) {
								nickname = otherPlayer.getNickname();
								challenge.getFields().put("opponent", nickname);
							}
	
							OtherAttendeeData otherAttendeeData = new OtherAttendeeData();
							otherAttendeeData.setRow_status(round(other_row_status, 2));
							otherAttendeeData.setStatus(other_status);
							otherAttendeeData.setPlayerId(otherPlayerId);
							otherAttendeeData.setNickname(nickname);
	
							challengeData.setOtherAttendeeData(otherAttendeeData);
							
	    					bonusScore = ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_CHALLENGE_REWARD, 0)).intValue();
	
							break;
						}	
						case CHAL_MODEL_GROUP_COOPERATIVE: {
							row_status = (Double) challenge.getFields().get(CHAL_FIELDS_CHALLENGE_SCORE);
							double other_row_status = (Double) otherAttendeeScores.get(CHAL_FIELDS_CHALLENGE_SCORE);
							double challengeTarget = Math.ceil((Double) challenge.getFields().get(CHAL_FIELDS_CHALLENGE_TARGET));
							target = (int)challengeTarget;
							int other_status = 0;
							if (challengeTarget != 0) {
								status = (int) (100 * row_status / challengeTarget);
								other_status = (int) (100 * other_row_status / challengeTarget);
								
								if (status + other_status > 100) {
									float coeff = (float)(status + other_status) / 100;
									status = Math.round(status / coeff);
									other_status = Math.round(other_status / coeff);
								}
								
							}
	
							Double reward = (Double) challenge.getFields().getOrDefault(CHAL_FIELDS_CHALLENGE_REWARD, "");
							
							String unit = (String) challenge.getFields().getOrDefault(CHAL_FIELDS_CHALLENGE_SCORE_NAME, "");
							challengeData.setUnit(unit);
	
							String proposer = (String) challenge.getFields().get(CHAL_FIELDS_PROPOSER);
							challengeData.setProposerId(proposer);
	
							String otherPlayerId = (String) otherAttendeeScores.get(CHAL_FIELDS_PLAYER_ID);
							Player otherPlayer = playerRepository.findById(otherPlayerId).orElse(null);
	
							String nickname = null;
							if (otherPlayer != null) {
								nickname = otherPlayer.getNickname();
								challenge.getFields().put("opponent", nickname);
							}
							challenge.getFields().put("reward", reward);
							challenge.getFields().put("target", target);
	
							OtherAttendeeData otherAttendeeData = new OtherAttendeeData();
							otherAttendeeData.setRow_status(round(other_row_status, 2));
							otherAttendeeData.setStatus(other_status);
							otherAttendeeData.setPlayerId(otherPlayerId);
							otherAttendeeData.setNickname(nickname);
	
							challengeData.setOtherAttendeeData(otherAttendeeData);
							
	    					bonusScore = ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_CHALLENGE_REWARD, 0)).intValue();
	
							break;
						}						
	    				// boolean status: 100 or 0
	    				case CHAL_MODEL_COMPLETE_BADGE_COLL: 
	    				case CHAL_MODEL_POICHECKIN: 
	    				case CHAL_MODEL_CLASSPOSITION: 
	    				default: {
		    				if(completed){
	    						row_status = 1;
	    						status = 100;
	    					}
	    				}
    				}
    				
    				challengeData.setChallTarget((int)target);
    				challengeData.setChallDesc(fillDescription(challenge, language));
    				challengeData.setChallCompleteDesc(fillLongDescription(challenge, getFilterByType(challengeData.getType()), language));

    				challengeData.setBonus(bonusScore);
    				challengeData.setStatus(status);
    				challengeData.setRow_status(round(row_status, 2));
    				
					if (type == 0) {
						if ("ASSIGNED".equals(state)) {
							if (now >= start - MILLIS_IN_DAY) { // if challenge is started (with one day of offset for mail)
								if (now < end - MILLIS_IN_DAY) { // if challenge is not ended
									// challenges.add(challengeData);
									challengesMap.put(ChallengeDataType.ACTIVE, challengeData);
								} else if (now < end + MILLIS_IN_DAY) { // CHAL_TS_OFFSET
									// oldChallenges.add(challengeData); // last week challenges
									challengesMap.put(ChallengeDataType.OLD, challengeData);
								}
							}
						}
					} else {
						if ("PROPOSED".equals(state)) {
							challengesMap.put(ChallengeDataType.PROPOSED, challengeData);
						} else if (now < end) { // if challenge is not ended
							if (now >= start) {
								challengesMap.put(ChallengeDataType.ACTIVE, challengeData);
							} else {
								challengesMap.put(ChallengeDataType.FUTURE, challengeData);
							}
						} else { // CHAL_TS_OFFSET
							challengesMap.put(ChallengeDataType.OLD, challengeData);
						}
					}
				}

				result.setChallengeData(Multimaps.asMap(challengesMap));
			}
    		
		}
    	
		result.getChallengeData().values().forEach(x -> {
			Collections.sort(x);
			Collections.reverse(x);
		});	
		
		if (result.getChallengeData().containsKey(ChallengeDataType.PROPOSED)) {
			Collections.sort(result.getChallengeData().get(ChallengeDataType.PROPOSED), new Comparator<ChallengesData>() {

				@Override
				public int compare(ChallengesData o1, ChallengesData o2) {
					String isGroup1 = o1.getProposerId() == null ? "1" : "0";
					String isGroup2 = o2.getProposerId() == null ? "1" : "0";
					int res = new String(isGroup1 + o1.getStartDate()).compareTo(new String(isGroup2 + o2.getStartDate()));
					if (res == 0) {
						res = o1.getChallId().compareTo(o2.getChallId());
					}
					return res;
				}

			});
		}
		
    	return result;
    }
	
	private void fillMissingFields(ChallengeConcept challenge, String gameId) {
		List otherAttendeeScoresList = (List)challenge.getFields().getOrDefault(CHAL_FIELDS_OTHER_ATTENDEE_SCORES, Collections.EMPTY_LIST);
		Map<String, Object> otherAttendeeScores = null;
		
		if (!otherAttendeeScoresList.isEmpty()) {
			otherAttendeeScores = (Map)otherAttendeeScoresList.get(0);
		} else {
			return;
		}

		String otherPlayerId = (String)otherAttendeeScores.get(CHAL_FIELDS_PLAYER_ID); 
		Player otherPlayer = playerRepository.findById(otherPlayerId).orElse(null);		
		
		switch (challenge.getModelName()) {
		case CHAL_MODEL_GROUP_COMPETITIVE_PERFORMANCE : {
			if (otherPlayer != null) {
				String nickname = otherPlayer.getNickname();
				challenge.getFields().put("opponent", nickname);
			}			
			break;
		}
		case CHAL_MODEL_GROUP_COMPETITIVE_TIME : {
			if (otherPlayer != null) {
				String nickname = otherPlayer.getNickname();
				challenge.getFields().put("opponent", nickname);
			}			
			break;
		}
		case CHAL_MODEL_GROUP_COOPERATIVE : {
			Double reward = (Double) challenge.getFields().getOrDefault(CHAL_FIELDS_CHALLENGE_REWARD, "");
			Double target = (Double) challenge.getFields().get(CHAL_FIELDS_CHALLENGE_TARGET);
			if (target != null) target = Math.ceil(target);
			if (otherPlayer != null) {
				String nickname = otherPlayer.getNickname();
				challenge.getFields().put("opponent", nickname);
			}
			challenge.getFields().put("reward", reward);
			challenge.getFields().put("target", target);
			break;
			}			
		}		
	}
	
	private String getFilterByType(String type) {
		switch(type) {
			case CHAL_MODEL_PERCENTAGE_INC:
			case CHAL_MODEL_ABSOLUTE_INC: {
				return "counterName";
			}
			case CHAL_MODEL_REPETITIVE_BEAV: {
				return "counterName";
			}
			case CHAL_MODEL_COMPLETE_BADGE_COLL:
			case CHAL_MODEL_NEXT_BADGE: {
				return "badgeCollectionName";
			}
			case CHAL_MODEL_POICHECKIN: {
				return "eventName";
			}
			case CHAL_MODEL_CLASSPOSITION: {
				return null;
			}
			case CHAL_MODEL_SURVEY: {
				return "surveyType";
			}
			case CHAL_MODEL_CHECKIN: {
				return "checkinType";
			}
			case CHAL_MODEL_GROUP_COMPETITIVE_PERFORMANCE:
			case CHAL_MODEL_GROUP_COMPETITIVE_TIME:
			case CHAL_MODEL_GROUP_COOPERATIVE:
				return "challengePointConceptName";
			default: {
				return null;
			}
		
		}
	}
	
	// Method retrieveCorrectStatusFromCounterName: used to get the correct player status starting from counter name field
	private double retrieveCorrectStatusFromCounterName(String cName, String periodType, List<PointConcept> pointConcept, Long chalStart, Long chalEnd){
		if (chalEnd <= chalStart) return 0;
		
		Range<Long> challengeRange = Range.open(chalStart, chalEnd);
		double actualStatus = 0; // km or trips
		if(cName != null && !cName.isEmpty()){
			for(PointConcept pt : pointConcept){
				if(cName.equals(pt.getName()) && periodType.equals(pt.getPeriodType())){
					List<PointConceptPeriod> allPeriods = pt.getInstances();
					for(PointConceptPeriod pcp : allPeriods) {
						Range<Long> pcpRange = Range.open(pcp.getStart(), pcp.getEnd()); 
						if(chalStart != null && chalEnd != null) {
							if (pcpRange.isConnected(challengeRange)) {
								actualStatus += pcp.getScore();
							}
						} 
					}
					break;
				}
			}
		}
		return actualStatus;
	}
	
	private int retrieveRepeatitiveStatusFromCounterName(String cName, String periodType, List<PointConcept> pointConcept, Long chalStart, Long chalEnd, double target){
		if (chalEnd <= chalStart) return 0;

		Range<Long> challengeRange = Range.open(chalStart, chalEnd);
		int countSuccesses = 0; // km or trips
		if(cName != null && !cName.isEmpty()){
			for(PointConcept pt : pointConcept){
				if(cName.equals(pt.getName()) && periodType.equals(pt.getPeriodType())){
					List<PointConceptPeriod> allPeriods = pt.getInstances();
					for(PointConceptPeriod pcp : allPeriods){
						if(chalStart != null && chalEnd != null){
							Range<Long> pcpRange = Range.open(pcp.getStart(), pcp.getEnd()); 
							if(chalStart != null && chalEnd != null && pcpRange.isConnected(challengeRange)) {
								countSuccesses += pcp.getScore() >= target ? 1 : 0;
							}
						}
					}
					break;
				}
			}
		}
		
		return countSuccesses;
	}	
	
	// Method getEarnedBadgesFromList: used to get the earned badge number during challenge
	private int getEarnedBadgesFromList(List<BadgeCollectionConcept> bcc_list, String badgeCollName, int initial){
		int earnedBadges = 0;
		for(BadgeCollectionConcept bcc : bcc_list){
			if(bcc.getName().compareTo(badgeCollName) == 0){
				earnedBadges = bcc.getBadgeEarned().size() - initial;
				break;
			}
		}
		return earnedBadges;
	}
	
	private int calculateRemainingDays(long endTime, long now){
    	int remainingDays = 0;
    	if(now < endTime){
    		long tmpMillis = endTime - now;
    		remainingDays = (int) Math.ceil((float)tmpMillis / MILLIS_IN_DAY);
    	}
    	return remainingDays;
    }
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    long factor = (long) Math.pow(10, places);
	    value = value * factor;
	    long tmp = Math.round(value);
	    return (double) tmp / factor;
	}
	
	private String fillDescription(ChallengeConcept challenge, String lang) {
		String filter = getFilterByType(challenge.getModelName());
		String description = null;
		String name = challenge.getModelName();
		String filterField = (String) challenge.getFields().get(filter);

		String counterNameA = null;
		String counterNameB = null;
		if (filterField != null) {
			if (CHAL_FIELDS_COUNTER_NAME.equals(filter)) {
				String counterNames[] = filterField.split("_");
				counterNameA = counterNames[0];
				if (counterNames.length == 2) {
					counterNameB = counterNames[1];

					if (counterNameA.startsWith("No")) {
						counterNameA = counterNameA.replace("No", "");
						counterNameB = "No" + counterNameB;
					}

				}
			}
		}

		ChallengeStructure challengeStructure = challengeStructureMap.getOrDefault(name + "#" + filterField, null);

		if (challengeStructure == null) {
			challengeStructure = challengeStructureMap.getOrDefault(name + (counterNameB != null ? ("#_" + counterNameB) : ""), null);
		}

		if (challengeStructure != null) {
			description = fillDescription(challengeStructure, counterNameA, counterNameB, challenge, lang);
			
			for (String key: challengeReplacements.keySet()) {
				description = description.replaceAll(key, challengeReplacements.get(key));
			}			
		} else {
			logger.error("Cannot find structure for challenge: '" + name + "', " + filter + "=" + filterField);
			return "";
		}

		return description;
	}

	private String fillLongDescription(ChallengeConcept challenge, String filterField, String lang) {
		String description = null;
		String name = challenge.getModelName();
		String counterName = filterField != null ? (String) challenge.getFields().get(filterField) : null;

		ChallengeLongDescrStructure challengeStructure = challengeLongStructureMap.getOrDefault(name + "#" + counterName, challengeLongStructureMap.getOrDefault(name, null));

		if (challengeStructure != null) {
			description = fillLongDescription(challengeStructure, counterName, challenge, lang);
			
			for (String key: challengeReplacements.keySet()) {
				description = description.replaceAll(key, challengeReplacements.get(key));
			}			
		} else {
			return "";
		}
		return description;
	}
	
	private String fillDescription(ChallengeStructure structure, String counterNameA, String counterNameB, ChallengeConcept challenge, String lang) {
		ST st = new ST(structure.getDescription().get(lang));

		boolean negative = counterNameB != null && counterNameB.startsWith("No");

		for (String field : challenge.getFields().keySet()) {
			Object o = challenge.getFields().get(field);
			st.add(field, o instanceof Number ? ((Number) o).intValue() : (o instanceof String ? instantiateWord(o.toString(), negative, lang) : o));
		}

		st.add("counterNameA", instantiateWord(counterNameA, negative, lang));
		st.add("counterNameB", instantiateWord(counterNameB, negative, lang));

		return st.render();
	}

	private String fillLongDescription(ChallengeLongDescrStructure structure, String counterName, ChallengeConcept challenge, String lang)  {
		ST st = new ST(structure.getDescription().get(lang));

		for (String field : challenge.getFields().keySet()) {
			Object o = challenge.getFields().get(field);
			st.add(field, o instanceof Number ? ((Number) o).intValue() : (o instanceof String ? instantiateWord(o.toString(), false, lang) : o));
		}

		return st.render();
	}
	
	private String fillDescription(String name, String filterField, Map<String, Object> params, String lang) {
		ChallengeStructure challengeStructure = challengeStructureMap.getOrDefault(name + "#" + filterField, null);
		
		String description = "";
		if (challengeStructure != null) {
			ST st = new ST(challengeStructure.getDescription().get(lang));
			
			for (String field : params.keySet()) {
				Object o = params.get(field);
				st.add(field, o instanceof Number ? ((Number) o).intValue() : (o instanceof String ? instantiateWord(o.toString(), false, lang) : o));
			}			
			
			for (String key: challengeReplacements.keySet()) {
				description = description.replaceAll(key, challengeReplacements.get(key));
			}		
			
			return st.render();
		} else {
			logger.error("Cannot find structure for challenge preview: '" + name + "', " + filterField);
			return "";
		}		
	}
	
	private String fillLongDescription(String name, String filterField, Map<String, Object> params, String lang) {
		ChallengeLongDescrStructure challengeStructure = challengeLongStructureMap.getOrDefault(name + "#" + filterField, null);
		
		String description = "";
		if (challengeStructure != null) {
			ST st = new ST(challengeStructure.getDescription().get(lang));
			
			for (String field : params.keySet()) {
				Object o = params.get(field);
				st.add(field, o instanceof Number ? ((Number) o).intValue() : (o instanceof String ? instantiateWord(o.toString(), false, lang) : o));
			}			
			
			for (String key: challengeReplacements.keySet()) {
				description = description.replaceAll(key, challengeReplacements.get(key));
			}		
			
			return st.render();
		} else {
			logger.error("Cannot find structure for challenge preview: '" + name + "', " + filterField);
			return "";
		}		
	}	
	
	private String instantiateWord(String word, boolean negative, String lang) {
		if (word != null) {
			List versions = challengeDictionaryMap.get(word.toLowerCase());
			if (versions != null) {
				Optional<Map> result = versions.stream().filter(x -> negative == (Boolean) ((Map) x).get("negative")).findFirst();
				if (result.isPresent()) {
					return (String)((Map)((Map) result.get()).get("word")).get(lang);
				}
			}
		}
		return word;
	}
	
	private PlayerStatus convertPlayerData(String profile, String playerId, String gameId, String nickName, String gamificationUrl, int challType, String language)
			throws Exception {

		try {
			PlayerStatus ps = new PlayerStatus();
			
			Map<String, Object> stateMap = mapper.readValue(profile, Map.class);
			
			Map<String, Object> state = (Map<String, Object>)stateMap.get("state");
			List<BadgeCollectionConcept> badges = mapper.convertValue(state.get("BadgeCollectionConcept"), new TypeReference<List<BadgeCollectionConcept>>() {});
			badges.forEach(x -> {
				x.getBadgeEarned().forEach(y -> {
					y.setUrl(getUrlFromBadgeName(gamificationUrl, y.getName()));
				});
			});
			ps.setBadgeCollectionConcept(badges);
			
			List<Map> gePointsMap = mapper.convertValue(state.get("PointConcept"), new TypeReference<List<Map>>() {});
			List<PointConcept> points = convertGEPointConcept(gePointsMap);
			
			ChallengeConceptInfo challenges = convertChallengeData(playerId, gameId, profile, challType, language, points, badges);
			ps.setChallengeConcept(challenges);

			List<PlayerLevel> levels = mapper.convertValue((List)stateMap.get("levels"), new TypeReference<List<PlayerLevel>>() {});
			ps.setLevels(levels);		

			Inventory inventory = mapper.convertValue(stateMap.get("inventory"), Inventory.class);
			ps.setInventory(inventory);	
			
			Map<String, Object> playerData = buildPlayerData(playerId, gameId, nickName);
			ps.setPlayerData(playerData);
			
			points.removeIf(x -> !PC_GREEN_LEAVES.equals(x.getName()) || !PC_WEEKLY.equals(x.getPeriodType()));
			ps.setPointConcept(points);		
			
			Calendar c = Calendar.getInstance();
			Calendar from = Calendar.getInstance(); from.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY); from.set(Calendar.HOUR_OF_DAY, 12); from.set(Calendar.MINUTE, 0); from.set(Calendar.SECOND, 0);
			Calendar to = Calendar.getInstance(); to.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY); to.set(Calendar.HOUR_OF_DAY, 12); to.set(Calendar.MINUTE, 0); to.set(Calendar.SECOND, 0);
			ps.setCanInvite(c.before(to) && c.after(from));
			
			return ps;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	private ClassificationData correctPlayerClassificationData(String profile, String playerId, String nickName, Long timestamp, String type) throws Exception {
		ClassificationData playerClass = new ClassificationData();
		if (profile != null && !profile.isEmpty()) {

			int score = 0;

			JsonNode profileData = mapper.readTree(profile);
			JsonNode stateData = (!profileData.has(STATE)) ? profileData.get(STATE) : null;
			JsonNode pointConceptData = null;
			if (stateData != null) {
				pointConceptData = (stateData.has(POINT_CONCEPT) && stateData.get(POINT_CONCEPT).isArray()) ? stateData.get(POINT_CONCEPT) : null;
				if (pointConceptData != null) {
					for (JsonNode point : pointConceptData) {
						String pc_name = point.has(PC_NAME) ? point.get(PC_NAME).asText() : null;
						if (timestamp == null || timestamp.longValue() == 0L) { // global
							if (pc_name != null && pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
								score = point.has(PC_SCORE) ? point.get(PC_SCORE).asInt() : null;
							}
						} else { // specific week
							if (pc_name != null && pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
								JsonNode pc_period = point.has(PC_PERIODS) ? point.get(PC_PERIODS) : null;
								if (pc_period != null) {
									Iterator<String> keys = pc_period.fieldNames();
									while (keys.hasNext()) {
										String key = keys.next();
										JsonNode pc_weekly = pc_period.get(key);
										if (pc_weekly != null) {
											JsonNode pc_instances = pc_weekly.get(PC_INSTANCES);

											if (pc_instances != null) {
												Iterator<String> instancesKeys = pc_instances.fieldNames();
												while (instancesKeys.hasNext()) {
													JsonNode pc_instance = pc_instances.get(instancesKeys.next());
													int instance_score = pc_instance.has(PC_SCORE) ? pc_instance.get(PC_SCORE).asInt() : 0;
													long instance_start = pc_instance.has(PC_START) ? pc_instance.get(PC_START).asLong() : 0L;
													long instance_end = pc_instance.has(PC_END) ? pc_instance.get(PC_END).asLong() : 0L;
													if (timestamp >= instance_start && timestamp <= instance_end) {
														score = instance_score;
														break;
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
				playerClass.setNickName(nickName);
				playerClass.setPlayerId(playerId);
				playerClass.setScore(score);
				if (nickName == null || nickName.isEmpty()) {
					playerClass.setPosition(-1); // used for user without nickName
				}
			}

		}
		return playerClass;
	}
	
	private ClassificationData playerClassificationSince(String profile, String playerId, String nickName, Long timestamp) throws Exception {
		ClassificationData playerClass = new ClassificationData();
		if (profile != null && !profile.isEmpty()) {

			int score = 0;

			JsonNode profileData = mapper.readTree(profile);
			JsonNode stateData = profileData.has(STATE) ? profileData.get(STATE) : null;
			JsonNode pointConceptData = null;
			if (stateData != null) {
				pointConceptData = (stateData.has(POINT_CONCEPT) && stateData.get(POINT_CONCEPT).isArray()) ? stateData.get(POINT_CONCEPT) : null;
				if (pointConceptData != null) {
					for (JsonNode point : pointConceptData) {
						String pc_name = point.has(PC_NAME) ? point.get(PC_NAME).asText() : null;
						if (timestamp == null || timestamp.longValue() == 0L) { // global
							if (pc_name != null && pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
								score = point.has(PC_SCORE) ? point.get(PC_SCORE).asInt() : null;
							}
						} else { // specific week
							if (pc_name != null && pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
								JsonNode pc_period = point.has(PC_PERIODS) ? point.get(PC_PERIODS) : null;
								if (pc_period != null) {
									JsonNode pc_weekly = pc_period.get(PC_WEEKLY);
									if (pc_weekly != null) {
										JsonNode pc_instances = pc_weekly.get(PC_INSTANCES);
										if (pc_instances != null) {
											Iterator<String> instancesKeys = pc_instances.fieldNames();
											while (instancesKeys.hasNext()) {
												JsonNode pc_instance = pc_instances.get(instancesKeys.next());
												int instance_score = pc_instance.has(PC_SCORE) ? pc_instance.get(PC_SCORE).asInt() : 0;
												long instance_start = pc_instance.has(PC_START) ? pc_instance.get(PC_START).asLong() : 0L;
												if (timestamp <= instance_start) {
													score += instance_score;
												}
											}
										}
									}
								}
							}
						}
					}
				}
				playerClass.setNickName(nickName);
				playerClass.setPlayerId(playerId);
				playerClass.setScore(score);
			}

		}
		return playerClass;
	}

	private List<ClassificationData> correctClassificationData(String allStatus, Map<String, String> allNicks, Long timestamp, String type) throws Exception {
		List<ClassificationData> playerClassList = new ArrayList<ClassificationData>();
		if (allStatus != null && !allStatus.isEmpty()) {

			int score = 0;

			JsonNode allPlayersData = mapper.readTree(allStatus);
			JsonNode allPlayersDataList = (allPlayersData.has("content") && allPlayersData.get("content").isArray()) ? allPlayersData.get("content") : null;
			if (allPlayersDataList != null) {
				for (JsonNode profileData : allPlayersDataList) {
					String playerId = profileData.has(PLAYER_ID) ? profileData.get(PLAYER_ID).asText() : "0";
					score = 0; // here I reset the score value to avoid classification problem
					JsonNode stateData = profileData.has(STATE) ? profileData.get(STATE) : null;
					JsonNode pointConceptData = null;
					if (stateData != null) {
						pointConceptData = stateData.has(POINT_CONCEPT) ? stateData.get(POINT_CONCEPT) : null;
						if (pointConceptData != null) {
							for (JsonNode point : pointConceptData) {
								String pc_name = point.has(PC_NAME) ? point.get(PC_NAME).asText() : null;
								if (timestamp == null || timestamp.longValue() == 0L) { // global
									if (pc_name != null && pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
										score = point.has(PC_SCORE) ? point.get(PC_SCORE).asInt() : null;
									}
								} else { // specific week
									if (pc_name != null && pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
										JsonNode pc_period = point.has(PC_PERIODS) ? point.get(PC_PERIODS) : null;
										if (pc_period != null) {
											Iterator<String> keys = pc_period.fieldNames();
											while (keys.hasNext()) {
												String key = keys.next();
												JsonNode pc_weekly = pc_period.get(key);
												if (pc_weekly != null) {
													JsonNode pc_instances = pc_weekly.get(PC_INSTANCES);
													if (pc_instances != null) {
														Iterator<String> instancesKeys = pc_instances.fieldNames();
														while (instancesKeys.hasNext()) {
															JsonNode pc_instance = pc_instances.get(instancesKeys.next());
															int instance_score = pc_instance.has(PC_SCORE) ? pc_instance.get(PC_SCORE).asInt() : 0;
															long instance_start = pc_instance.has(PC_START) ? pc_instance.get(PC_START).asLong() : 0L;
															long instance_end = pc_instance.has(PC_END) ? pc_instance.get(PC_END).asLong() : 0L;
															if (timestamp >= instance_start && timestamp <= instance_end) {
																score = instance_score;
																break;
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
						String nickName = getPlayerNickNameById(allNicks, playerId); // getPlayerNameById(allNicks,
																						// playerId);
						ClassificationData playerClass = new ClassificationData();
						playerClass.setNickName(nickName);
						playerClass.setPlayerId(playerId);
						playerClass.setScore(score);
						if (nickName != null && !nickName.isEmpty()) { // if
																				// nickName
																				// present
																				// (user
																				// registered
																				// and
																				// active)
							playerClassList.add(playerClass);
						}
					}
				}
			}

		}
		return playerClassList;
	}

	// Method correctGlobalClassification: return a map 'playerId, score' of the
	// global classification
	private Map<String, Integer> correctGlobalClassification(String allStatus) throws Exception {
		Map classification = new HashMap<String, Integer>();
		if (allStatus != null && !allStatus.isEmpty()) {
			int score = 0;
			JsonNode allPlayersData = mapper.readTree(allStatus);
			JsonNode allPlayersDataList = (allPlayersData.has("content") && allPlayersData.get("content").isArray()) ? allPlayersData.get("content") : null;
			if (allPlayersDataList != null) {
				for (JsonNode profileData : allPlayersDataList) {
					String playerId = profileData.has(PLAYER_ID) ? profileData.get(PLAYER_ID).asText() : "0";
					score = 0; // here I reset the score value to avoid classification problem
					JsonNode stateData = profileData.has(STATE) ? profileData.get(STATE) : null;
					JsonNode pointConceptData = null;
					if (stateData != null) {
						pointConceptData = (stateData.has(POINT_CONCEPT) && stateData.get(POINT_CONCEPT).isArray()) ? stateData.get(POINT_CONCEPT) : null;
						if (pointConceptData != null) {
							for (JsonNode point : pointConceptData) {
								String pc_name = point.has(PC_NAME) ? point.get(PC_NAME).asText() : null;
								if (pc_name != null && pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
									score = point.has(PC_SCORE) ? point.get(PC_SCORE).asInt() : null;
								}
							}
						}
						classification.put(playerId, score);
					}
				}
			}

		}
		return classification;
	}

	private List<ClassificationData> correctClassificationIncData(String allStatus, Map<String, String> allNicks, Long timestamp, String type) throws Exception {
		List<ClassificationData> playerClassList = new ArrayList<ClassificationData>();

		/*
		 * allStatus = "{" + "\"pointConceptName\": \"green leaves\"," + "\"type\": \"INCREMENTAL\"," + "\"board\": [" + "{" + "\"score\": 12," + "\"playerId\": \"3\"" + "}," + "{" + "\"score\": 10," + "\"playerId\": \"16\"" + "}," + "{" + "\"score\": 4," + "\"playerId\": \"4\"" + "}" + "]" + "}";
		 */

		if (allStatus != null && !allStatus.isEmpty()) {
			JsonNode allIncClassData = mapper.readTree(allStatus);
			if (allIncClassData != null) {
				JsonNode allPlayersDataList = (allIncClassData.has("board") && allIncClassData.get("board").isArray()) ? allIncClassData.get("board") : null;
				if (allPlayersDataList != null) {
					for (JsonNode profileData : allPlayersDataList) {
						String playerId = profileData.has(PLAYER_ID) ? profileData.get(PLAYER_ID).asText() : "0";
						Integer playerScore = profileData.has(PC_SCORE) ? profileData.get(PC_SCORE).asInt() : 0;
						String nickName = getPlayerNickNameById(allNicks, playerId); // getPlayerNameById(allNicks, playerId);
						ClassificationData playerClass = new ClassificationData();
						playerClass.setNickName(nickName);
						playerClass.setPlayerId(playerId);
						playerClass.setScore(playerScore);
						if (nickName != null && !nickName.isEmpty()) { // if
																				// nickName
																				// present
																				// (user
																				// registered
																				// and
																				// active)
							playerClassList.add(playerClass);
						}
					}
				}
			}
		}
		return playerClassList;
	}

	private PlayerClassification completeClassificationPosition(List<ClassificationData> playersClass, ClassificationData actualPlayerClass, Integer from, Integer to) {
		List<ClassificationData> playersClassCorr = new ArrayList<ClassificationData>();
		int from_index = 0;
		PlayerClassification pc = new PlayerClassification();
		List<ClassificationData> cleanedList = new ArrayList<ClassificationData>();
		boolean myPosFind = false;
		if (playersClass != null && !playersClass.isEmpty()) {
			ClassificationData prec_pt = null;
			for (int i = 0; i < playersClass.size(); i++) {
				ClassificationData pt = playersClass.get(i);
				if (i > 0) {
					if (pt.getScore() < prec_pt.getScore()) {
						pt.setPosition(i + 1);
					} else {
						pt.setPosition(prec_pt.getPosition());
					}
				} else {
					pt.setPosition(i + 1);
				}
				prec_pt = pt;
				if (pt.getPlayerId().compareTo(actualPlayerClass.getPlayerId()) == 0) {
					myPosFind = true;
					actualPlayerClass.setPosition(pt.getPosition());
				}
				playersClassCorr.add(pt);
			}
			if (!myPosFind) {
				ClassificationData lastPlayer = playersClass.get(playersClass.size() - 1);
				if (lastPlayer.getScore() == actualPlayerClass.getScore()) {
					actualPlayerClass.setPosition(lastPlayer.getPosition());
				} else {
					actualPlayerClass.setPosition(lastPlayer.getPosition() + 1);
				}
				playersClassCorr.add(actualPlayerClass);
			}
			int to_index = playersClassCorr.size();
			if (from != null) {
				from_index = from.intValue();
			}
			if (to != null) {
				to_index = to.intValue();
			}
			if (from_index < 0)
				from_index = 0;
			if (from_index > playersClassCorr.size())
				from_index = playersClassCorr.size();
			if (to_index < 0)
				to_index = 0;
			if (to_index > playersClassCorr.size())
				to_index = playersClassCorr.size();
			if (from_index >= to_index)
				from_index = to_index;
			try {
				cleanedList = playersClassCorr.subList(from_index, to_index);
			} catch (Exception ex) {
				// do nothings
			}
			pc.setClassificationList(cleanedList);
		} else {
			pc.setClassificationList(playersClass);
			actualPlayerClass.setPosition(0);
		}
		pc.setActualUser(actualPlayerClass);
		return pc;
	}

	private String getPlayerNickNameById(Map<String, String> allNicks, String id) {
		String name = "";
		if (allNicks != null && !allNicks.isEmpty()) {
			name = allNicks.get(id);
		}
		return name;
	}

	private String getUrlFromBadgeName(String gamificationUrl, String b_name) {
		BadgesData badge = badgeCache.getBadge(b_name);
		if (badge != null) {
			return gamificationUrl + "/" + badge.getPath();
		}
		return null;
	}
	
	private Map<String, Object> buildPlayerData(String playerId, String gameId, String nickName) {
		Map<String, Object> map = Maps.newTreeMap();
		map.put("playerId", playerId);
		map.put("gameId", gameId);
		map.put("nickName", nickName);
		return map;
	}
	
	private Map<String, Double> targetPrizeChallengesCompute(String pId_1, String pId_2, String gameId, String counter, String type) throws Exception {

		prepare();

		Map<Integer, Double> quantiles = getQuantiles(gameId, counter);

		Map<String, Double> res = Maps.newTreeMap();

		String player1 = gamificationCache.getPlayerState(pId_1, gameId);
        Pair<Double, Double> res1 = getForecast("player1", res, player1, counter);
        double player1_tgt = res1.getFirst();
        double player1_bas = res1.getSecond();
        res.put("player1_tgt", player1_tgt);

        String player2 = gamificationCache.getPlayerState(pId_2, gameId);
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

	public Double evaluate(Double target, Double baseline, String counter, Map<Integer, Double> quantiles) {
		if (baseline == 0) {
			return 100.0;
		}

		Integer difficulty = DifficultyCalculator.computeDifficulty(quantiles, baseline, target);

		double d = (target / Math.max(1, baseline)) - 1;

		int prize = dc.calculatePrize(difficulty, d, counter);

        double bonus =  Math.ceil(prize * ChallengesConfig.competitiveChallengesBooster / 10.0) * 10;

        return Math.min(bonus, 300);
	}

	public Double getWeeklyContentMode(String status, String mode, LocalDate execDate) throws Exception {
		Map<String, Object> stateMap = mapper.readValue(status, Map.class);
		Map<String, Object> state = (Map<String, Object>) stateMap.get("state");
		List<Map> gePointsMap = mapper.convertValue(state.get("PointConcept"), new TypeReference<List<Map>>() {
		});

		long time = LocalDate.now().atStartOfDay().atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli();

		List<PointConcept> points = convertGEPointConcept(gePointsMap);

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

	private List<GameStatistics> getStatistics(String appId, String counter) throws Exception {
		List<GameStatistics> stats = gamificationCache.getStatistics(appId);
		return stats.stream().filter(x -> counter.equals(x.getPointConceptName())).collect(Collectors.toList());
	}
	
}
