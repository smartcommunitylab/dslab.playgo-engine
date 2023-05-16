package it.smartcommunitylab.playandgo.engine.campaign.city;

import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.support.CronExpression;
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

import it.smartcommunitylab.playandgo.engine.ge.BadgeManager;
import it.smartcommunitylab.playandgo.engine.ge.GamificationEngineManager;
import it.smartcommunitylab.playandgo.engine.ge.model.BadgeCollectionConcept;
import it.smartcommunitylab.playandgo.engine.ge.model.BadgesData;
import it.smartcommunitylab.playandgo.engine.ge.model.PlayerLevel;
import it.smartcommunitylab.playandgo.engine.ge.model.PlayerStatus;
import it.smartcommunitylab.playandgo.engine.ge.model.PointConcept;
import it.smartcommunitylab.playandgo.engine.ge.model.PointConceptPeriod;
import it.smartcommunitylab.playandgo.engine.manager.AvatarManager;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeConcept;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeConceptInfo;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeConceptInfo.ChallengeDataType;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeLongDescrStructure;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeStructure;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengesData;
import it.smartcommunitylab.playandgo.engine.manager.challenge.Inventory;
import it.smartcommunitylab.playandgo.engine.manager.challenge.OtherAttendeeData;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.repository.TerritoryRepository;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CityGameDataConverter {
	private static final Logger logger = LoggerFactory.getLogger(CityGameDataConverter.class);
    
	@Value("${playgoURL}")
    private String playgoURL;
	
	// point concept fields
	public static final String STATE = "state";
	public static final String PLAYER_ID = "playerId";
	public static final String POINT_CONCEPT = "PointConcept";
	public static final String PC_GREEN_LEAVES = "green leaves";
	public static final String PC_NAME = "name";
	public static final String PC_SCORE = "score";
	public static final String PC_PERIOD = "period";
	public static final String PC_PERIODS = "periods";
	public static final String PC_START = "start";
	public static final String PC_WEEKLY = "weekly";	
	public static final String PC_IDENTIFIER = "identifier";
	public static final String PC_INSTANCES = "instances";
	public static final String PC_END = "end";
	
	// challange fields
	public static final String CHAL_FIELDS_PERIOD_NAME = "periodName";
	public static final String CHAL_FIELDS_COUNTER_NAME = "counterName";
	public static final String CHAL_FIELDS_BONUS_SCORE = "bonusScore";
	public static final String CHAL_FIELDS_TARGET = "target";
	public static final String CHAL_FIELDS_PERIOD_TARGET = "periodTarget";
	public static final String CHAL_FIELDS_INITIAL_BADGE_NUM = "initialBadgeNum";
	public static final String CHAL_FIELDS_OTHER_ATTENDEE_SCORES = "otherAttendeeScores";
	public static final String CHAL_FIELDS_CHALLENGE_SCORE = "challengeScore";
	public static final String CHAL_FIELDS_CHALLENGE_TARGET = "challengeTarget";
	public static final String CHAL_FIELDS_PLAYER_ID = "playerId";
	public static final String CHAL_FIELDS_PROPOSER = "proposer";
	public static final String CHAL_FIELDS_CHALLENGE_SCORE_NAME = "challengeScoreName";
	public static final String CHAL_FIELDS_CHALLENGE_REWARD = "rewardBonusScore";
	
	// new challenge types
	public static final String CHAL_MODEL_PERCENTAGE_INC = "percentageIncrement";
	public static final String CHAL_MODEL_ABSOLUTE_INC = "absoluteIncrement";
	public static final String CHAL_MODEL_REPETITIVE_BEAV = "repetitiveBehaviour";
	public static final String CHAL_MODEL_NEXT_BADGE = "nextBadge";
	public static final String CHAL_MODEL_COMPLETE_BADGE_COLL = "completeBadgeCollection";
	public static final String CHAL_MODEL_SURVEY = "survey";
	public static final String CHAL_MODEL_POICHECKIN = "poiCheckin";
	public static final String CHAL_MODEL_CHECKIN = "checkin";
	public static final String CHAL_MODEL_CLASSPOSITION = "leaderboardPosition";
	public static final String CHAL_MODEL_GROUP_COMPETITIVE_PERFORMANCE = "groupCompetitivePerformance";
	public static final String CHAL_MODEL_GROUP_COMPETITIVE_TIME = "groupCompetitiveTime";
	public static final String CHAL_MODEL_GROUP_COOPERATIVE = "groupCooperative";
	public static final String CHAL_MODEL_INCENTIVE_GROUP = "incentiveGroupChallengeReward";
	public static final String CHAL_MODEL_VISIT_POINT = "visitPointInterest";
	
	public static final int MILLIS_IN_DAY = 1000 * 60 * 60 * 24;
	public static final long MILLIS_IN_WEEK = 1000 * 60 * 60 * 24 * 7;
	
	private static final Map<String, String> UNIT_MAPPING = Stream.of(new String[][] {
		  { "daily", "days" }, 
		  { "weekly", "weeks" }, 
		}).collect(Collectors.toMap(data -> data[0], data -> data[1]));
	
	private static final List<String> languages = Arrays.asList("it", "en");
	
	@Value("${challengeDir}")
	private String challengeDir;
	
	@Autowired
	MongoTemplate mongoTemplate;
	
	@Autowired
	private PlayerRepository playerRepository;
    
	@Autowired
    TerritoryRepository territoryRepository;
	
	@Autowired
	private GamificationEngineManager gamificationEngineManager;
	
	@Autowired
	private BadgeManager badgeManager;
	
	@Autowired
	AvatarManager avatarManager;

	private Map<String, ChallengeStructure> challengeStructureMap;
	private Map<String, ChallengeLongDescrStructure> challengeLongStructureMap;

	private Map<String, List> challengeDictionaryMap;
	private Map<String, String> challengeReplacements;

	private Map<String, BadgesData> badges;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	@PostConstruct
	public void init() throws Exception {
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
		
		badges = badgeManager.getAllBadges();
	}
	
	public String encryptIdentity(String playerId, String gameId) throws Exception {
		return gamificationEngineManager.encryptIdentity(playerId, gameId);
	}
	
	public List<BadgesData> getAllBadges() {
		return Lists.newArrayList(badges.values());
	}
	
	public List<BadgeCollectionConcept> convertBadgeCollection(JsonNode rootNode) {
		if(rootNode.isMissingNode()) {
		    return new ArrayList<>();
		}
	    List<BadgeCollectionConcept> badges = mapper.convertValue(rootNode, new TypeReference<List<BadgeCollectionConcept>>() {});
		return filterBadges(badges);
	}
	
	private List<BadgeCollectionConcept> filterBadges(List<BadgeCollectionConcept> badges) {
        if(badges == null) {
            return new ArrayList<BadgeCollectionConcept>();
        }
	    List<BadgeCollectionConcept> filteredBadges = badges.stream().filter(x -> !x.isHidden()).map(x -> {
            x.getBadgeEarned().forEach(y -> {
                y.setUrl(getUrlFromBadgeName(playgoURL, y.getName()));
            });
            return x;
        }).collect(Collectors.toList());
        return filteredBadges;	    
	}
	
	public List<PlayerLevel> convertLevels(JsonNode rootNode) {
		List<PlayerLevel> levels = mapper.convertValue(rootNode, new TypeReference<List<PlayerLevel>>() {});
		return levels;
	}
	
	public PlayerStatus convertPlayerData(String profile, String playerId, String gameId, String nickName, int challType, String language)
			throws Exception {

		try {
			PlayerStatus ps = new PlayerStatus();
			
			Map<String, Object> stateMap = mapper.readValue(profile, Map.class);
			
			Map<String, Object> state = (Map<String, Object>)stateMap.get("state");
			List<BadgeCollectionConcept> badges = mapper.convertValue(state.get("BadgeCollectionConcept"), new TypeReference<List<BadgeCollectionConcept>>() {});
			ps.setBadgeCollectionConcept(filterBadges(badges));
			
			List<Map> gePointsMap = mapper.convertValue(state.get("PointConcept"), new TypeReference<List<Map>>() {});
			List<PointConcept> points = convertGEPointConcept(gePointsMap);
			
			List<ChallengeConcept> challengeList = mapper.convertValue(state.get("ChallengeConcept"), new TypeReference<List<ChallengeConcept>>() {});
			
			ChallengeConceptInfo challenges = convertChallengeData(playerId, gameId, challType, language, points, badges, challengeList);
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
			Calendar to = Calendar.getInstance(); to.setTime(from.getTime()); to.add(Calendar.DAY_OF_MONTH, 2);
			ps.setCanInvite(c.before(to) && c.after(from));
			
			return ps;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw e;
		}
	}
	
    private ZonedDateTime getZonedDateTime(Campaign campaign) {
        ZoneId zoneId = null;
        Territory territory = territoryRepository.findById(campaign.getTerritoryId()).orElse(null);
        if(territory == null) {
            zoneId = ZoneId.systemDefault();
        } else {
            zoneId = ZoneId.of(territory.getTimezone());
        }
        return ZonedDateTime.now(zoneId);        
    }
	
	public ChallengeConceptInfo convertPlayerChallengesData(String jsonChallenges, String playerStatus, Player player, Campaign campaign, int challType) throws Exception {
		try {
			Map playerMap = mapper.readValue(playerStatus, Map.class);
			Map stateMap = mapper.convertValue(playerMap.get("state"), Map.class);
			List<BadgeCollectionConcept> badges = mapper.convertValue(stateMap.get("BadgeCollectionConcept"), new TypeReference<List<BadgeCollectionConcept>>() {});
			
			List<Map> gePointsMap = mapper.convertValue(stateMap.get("PointConcept"), new TypeReference<List<Map>>() {});
			List<PointConcept> points = convertGEPointConcept(gePointsMap);
			
			List<ChallengeConcept> challengeList = mapper.readValue(jsonChallenges, new TypeReference<List<ChallengeConcept>>() {});
					
			ChallengeConceptInfo challenges = convertChallengeData(player.getPlayerId(), campaign.getGameId(), challType, player.getLanguage(),
					points, filterBadges(badges), challengeList);

			challenges.setCanInvite(false);
			
			String expressionFromString = Utils.getCronExp(campaign, Campaign.challengePlayerProposed);
			String expressionToString = Utils.getCronExp(campaign, Campaign.challengePlayerAssigned);
			
			if(Utils.isNotEmpty(expressionFromString) && Utils.isNotEmpty(expressionToString)) {
	            CronExpression expressionFrom = CronExpression.parse(expressionFromString);
	            CronExpression expressionTo = CronExpression.parse(expressionToString);
	            
	            ZonedDateTime nowZoned = getZonedDateTime(campaign);
	            ZonedDateTime truncatedTime = null;
	            if(nowZoned.getDayOfWeek().getValue() == 1) {
	                truncatedTime = nowZoned.truncatedTo(ChronoUnit.DAYS);
	            } else {
	                truncatedTime = nowZoned.with(TemporalAdjusters.previous(DayOfWeek.MONDAY))
	                        .truncatedTo(ChronoUnit.DAYS);	                
	            }
	            
	            ZonedDateTime from = expressionFrom.next(truncatedTime);
	            ZonedDateTime to = expressionTo.next(truncatedTime); 
	            
	            if(nowZoned.isAfter(from) && nowZoned.isBefore(to)) {
	                if(playerMap.containsKey("inventory")) {
	                    Map inventory = (Map) playerMap.get("inventory");
	                    List challengeChoices = (List) inventory.get("challengeChoices");
	                    if((challengeChoices != null) && challengeChoices.size() > 0) {
	                        challenges.setCanInvite(true);
	                    }
	                }                
	            }			    
			}
            
			return challenges;	
		} catch (Exception e) {
			logger.error("convertPlayerChallengesData:" + e.getMessage());
			throw e;
		}
	}
	
	public ChallengesData convertPlayerChallengesDataByName(String jsonChallenge, String playerStatus, Player player, Campaign campaign, 
			int challType) throws Exception {
		try {
			Map playerMap = mapper.readValue(playerStatus, Map.class);
			Map stateMap = mapper.convertValue(playerMap.get("state"), Map.class);
			List<BadgeCollectionConcept> badges = mapper.convertValue(stateMap.get("BadgeCollectionConcept"), new TypeReference<List<BadgeCollectionConcept>>() {});
			
			List<Map> gePointsMap = mapper.convertValue(stateMap.get("PointConcept"), new TypeReference<List<Map>>() {});
			List<PointConcept> points = convertGEPointConcept(gePointsMap);
			
			ChallengeConcept challenge = mapper.readValue(jsonChallenge, new TypeReference<ChallengeConcept>() {});
					
			long start = challenge.getStart().getTime();
			long end = challenge.getEnd().getTime();
			
			final long now = System.currentTimeMillis();
			
			ChallengesData challengeData = extractChallengeData(player.getPlayerId(), campaign.getGameId(), player.getLanguage(), points, filterBadges(badges),
					challenge, start, end, now);

			return challengeData;	
		} catch (Exception e) {
			logger.error("convertPlayerChallengesDataByName:" + e.getMessage());
			throw e;
		}
	}
	
	private String getUrlFromBadgeName(String gamificationUrl, String b_name) {
		BadgesData badge = badges.get(b_name);
		if (badge != null) {
			return gamificationUrl + "/" + badge.getPath();
		}
		return null;
	}
	
	public List<PointConcept> convertGEPointConcept(List<Map> gePointsMap) {
		List<PointConcept> result = Lists.newArrayList();
		if(gePointsMap != null) {
			for (Map gePointMap: gePointsMap) {
//				PointConcept pc = new PointConcept();
//				pc.setName((String)gePointMap.get(PC_NAME));
//				pc.setScore(((Double)gePointMap.get(PC_SCORE)));
//				pc.setPeriodType(PC_WEEKLY);
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
		}
		return result;
	}
	
	private Map<String, Object> buildPlayerData(String playerId, String gameId, String nickName) {
		Map<String, Object> map = Maps.newTreeMap();
		map.put("playerId", playerId);
		map.put("gameId", gameId);
		map.put("nickName", nickName);
		return map;
	}
	
//	private List<ChallengeConcept> parse(String data) throws Exception {
//		List<ChallengeConcept> concepts = Lists.newArrayList();
//
//		Map playerMap = mapper.readValue(data, Map.class);
//		if (playerMap.containsKey("state")) {
//			Map stateMap = mapper.convertValue(playerMap.get("state"), Map.class);
//			if (stateMap.containsKey("ChallengeConcept")) {
//				List conceptList = mapper.convertValue(stateMap.get("ChallengeConcept"), List.class);
//				for (Object o : conceptList) {
//					ChallengeConcept concept = mapper.convertValue(o, ChallengeConcept.class);
//					concepts.add(concept);
//				}
//			}
//		}
//		return concepts;
//	}
	
	private int calculateRemainingDays(long endTime, long now){
    	int remainingDays = 0;
    	if(now < endTime){
    		long tmpMillis = endTime - now;
    		remainingDays = (int) Math.ceil((float)tmpMillis / MILLIS_IN_DAY);
    	}
    	return remainingDays;
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
	
	private double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    long factor = (long) Math.pow(10, places);
	    value = value * factor;
	    long tmp = Math.round(value);
	    return (double) tmp / factor;
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
	
	public Map<String, String> fillDescription(String name, String filterField, Map<String, Object> params) {
		Map<String, String> result = new HashMap<>();
		ChallengeStructure challengeStructure = challengeStructureMap.getOrDefault(name + "#" + filterField, null);
		if (challengeStructure != null) {
			for(String lang : languages) {
				String description = "";
				ST st = new ST(challengeStructure.getDescription().get(lang));
				
				for (String field : params.keySet()) {
					Object o = params.get(field);
					st.add(field, o instanceof Number ? ((Number) o).intValue() : (o instanceof String ? instantiateWord(o.toString(), false, lang) : o));
				}			
				
				for (String key: challengeReplacements.keySet()) {
					description = description.replaceAll(key, challengeReplacements.get(key));
				}		
				result.put(lang, st.render());				
			}
		} else {
			logger.error("Cannot find structure for challenge preview: '" + name + "', " + filterField);
		}
		return result;
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
	
	private String fillLongDescription(ChallengeLongDescrStructure structure, String counterName, ChallengeConcept challenge, String lang)  {
		ST st = new ST(structure.getDescription().get(lang));

		for (String field : challenge.getFields().keySet()) {
			Object o = challenge.getFields().get(field);
			st.add(field, o instanceof Number ? ((Number) o).intValue() : (o instanceof String ? instantiateWord(o.toString(), false, lang) : o));
		}

		return st.render();
	}
	
	public Map<String, String> fillLongDescription(String name, String filterField, Map<String, Object> params) {
		Map<String, String> result = new HashMap<>();
		ChallengeLongDescrStructure challengeStructure = challengeLongStructureMap.getOrDefault(name + "#" + filterField, null);
		
		if (challengeStructure != null) {
			for(String lang : languages) {
				String description = "";
				ST st = new ST(challengeStructure.getDescription().get(lang));
				
				for (String field : params.keySet()) {
					Object o = params.get(field);
					st.add(field, o instanceof Number ? ((Number) o).intValue() : (o instanceof String ? instantiateWord(o.toString(), false, lang) : o));
				}			
				
				for (String key: challengeReplacements.keySet()) {
					description = description.replaceAll(key, challengeReplacements.get(key));
				}
				result.put(lang, st.render());
			}
		} else {
			logger.error("Cannot find structure for challenge preview: '" + name + "', " + filterField);
		}
		return result;
	}	

	// Method correctChallengeData: used to retrieve the challenge data objects from the user profile data
	private ChallengeConceptInfo convertChallengeData(String playerId, String gameId, int type, String language, 
			List<PointConcept> pointConcept, List<BadgeCollectionConcept> bcc_list, List<ChallengeConcept> challengeList) throws Exception {
    	ListMultimap<ChallengeDataType, ChallengesData> challengesMap = ArrayListMultimap.create();
    	
    	ChallengeConceptInfo result = new ChallengeConceptInfo();    		
    		
		if(challengeList != null){
			for(ChallengeConcept challenge: challengeList) {
				String state = challenge.getState();
				long start = challenge.getStart().getTime();
				long end = challenge.getEnd().getTime();
				
				// Convert data to old challenges models
//						final String ch_point_type = challData.getBonusPointType();
				final long now = System.currentTimeMillis();
				
    			ChallengesData challengeData = extractChallengeData(playerId, gameId, language, pointConcept, bcc_list,
						challenge, start, end, now);
				
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

	public ChallengesData extractChallengeData(String playerId, String gameId, String language,
			List<PointConcept> pointConcept, List<BadgeCollectionConcept> bcc_list, ChallengeConcept challenge,
			final long start, final long end, final long now) throws Exception {
		
		String name = challenge.getName();
		String modelName = challenge.getModelName();
		Boolean completed = challenge.isCompleted();
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
				challengeData.setExtUrl(link);
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
				otherAttendeeData.setAvatar(avatarManager.getPlayerSmallAvatar(otherPlayerId));
				
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
				otherAttendeeData.setAvatar(avatarManager.getPlayerSmallAvatar(otherPlayerId));

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
				otherAttendeeData.setAvatar(avatarManager.getPlayerSmallAvatar(otherPlayerId));

				challengeData.setOtherAttendeeData(otherAttendeeData);
				
				bonusScore = ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_CHALLENGE_REWARD, 0)).intValue();

				break;
			}		
			case CHAL_MODEL_VISIT_POINT: {
				String poiType = (String)challenge.getFields().getOrDefault("typePoi","");
				int count = getEarnedBadgesFromList(bcc_list, poiType, 0);
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
		for(String lang : languages) {
			challengeData.getChallDesc().put(lang, fillDescription(challenge, lang));
			challengeData.getChallCompleteDesc().put(lang, fillLongDescription(challenge, getFilterByType(challengeData.getType()), lang));
		}

		challengeData.setBonus(bonusScore);
		challengeData.setStatus(status);
		challengeData.setRow_status(round(row_status, 2));
		return challengeData;
	}

}
