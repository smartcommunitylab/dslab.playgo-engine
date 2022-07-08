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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.ge.GameDataConverter;
import it.smartcommunitylab.playandgo.engine.ge.GamificationCache;
import it.smartcommunitylab.playandgo.engine.ge.GamificationEngineManager;
import it.smartcommunitylab.playandgo.engine.ge.model.GameStatistics;
import it.smartcommunitylab.playandgo.engine.ge.model.PlayerStatus;
import it.smartcommunitylab.playandgo.engine.ge.model.PointConcept;
import it.smartcommunitylab.playandgo.engine.ge.model.PointConceptPeriod;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeConceptInfo.ChallengeDataType;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeInvitation.ChallengePlayer;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeInvitation.PointConceptRef;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeInvitation.Reward;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.notification.CampaignNotificationManager;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
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
	private GamificationEngineManager gamificationEngineManager;
	
	@Autowired
	private CampaignNotificationManager campaignNotificationManager;
	
	@Autowired
	private GameDataConverter gameDataConverter;
	
	@Autowired
	private GamificationCache gamificationCache;
	
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
		PlayerStatus playerStatus = gameDataConverter.convertPlayerData(json, playerId, campaign.getGameId(), player.getNickname(), 
				1, player.getLanguage());
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
		Map<String, String> extraData = Maps.newTreeMap();
		extraData.put("opponent", player.getNickname());
		campaignNotificationManager.sendDirectNotification(invitation.getAttendeeId(), campaignId, "INVITATION", extraData);					
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
		
		String descr = gameDataConverter.fillDescription(invitation.getChallengeModelName().toString(), invitation.getChallengePointConcept(), pars, player.getLanguage());
		String longDescr = gameDataConverter.fillLongDescription(invitation.getChallengeModelName().toString(), invitation.getChallengePointConcept(), pars, player.getLanguage());
		
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
	
	

	
	
	private void fillMissingFields(ChallengeConcept challenge, String gameId) {
		List otherAttendeeScoresList = (List)challenge.getFields().getOrDefault(GameDataConverter.CHAL_FIELDS_OTHER_ATTENDEE_SCORES, Collections.EMPTY_LIST);
		Map<String, Object> otherAttendeeScores = null;
		
		if (!otherAttendeeScoresList.isEmpty()) {
			otherAttendeeScores = (Map)otherAttendeeScoresList.get(0);
		} else {
			return;
		}

		String otherPlayerId = (String)otherAttendeeScores.get(GameDataConverter.CHAL_FIELDS_PLAYER_ID); 
		Player otherPlayer = playerRepository.findById(otherPlayerId).orElse(null);		
		
		switch (challenge.getModelName()) {
		case GameDataConverter.CHAL_MODEL_GROUP_COMPETITIVE_PERFORMANCE : {
			if (otherPlayer != null) {
				String nickname = otherPlayer.getNickname();
				challenge.getFields().put("opponent", nickname);
			}			
			break;
		}
		case GameDataConverter.CHAL_MODEL_GROUP_COMPETITIVE_TIME : {
			if (otherPlayer != null) {
				String nickname = otherPlayer.getNickname();
				challenge.getFields().put("opponent", nickname);
			}			
			break;
		}
		case GameDataConverter.CHAL_MODEL_GROUP_COOPERATIVE : {
			Double reward = (Double) challenge.getFields().getOrDefault(GameDataConverter.CHAL_FIELDS_CHALLENGE_REWARD, "");
			Double target = (Double) challenge.getFields().get(GameDataConverter.CHAL_FIELDS_CHALLENGE_TARGET);
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
	
	private ClassificationData correctPlayerClassificationData(String profile, String playerId, String nickName, Long timestamp, String type) throws Exception {
		ClassificationData playerClass = new ClassificationData();
		if (profile != null && !profile.isEmpty()) {

			int score = 0;

			JsonNode profileData = mapper.readTree(profile);
			JsonNode stateData = (!profileData.has(GameDataConverter.STATE)) ? profileData.get(GameDataConverter.STATE) : null;
			JsonNode pointConceptData = null;
			if (stateData != null) {
				pointConceptData = (stateData.has(GameDataConverter.POINT_CONCEPT) && stateData.get(GameDataConverter.POINT_CONCEPT).isArray()) ? stateData.get(GameDataConverter.POINT_CONCEPT) : null;
				if (pointConceptData != null) {
					for (JsonNode point : pointConceptData) {
						String pc_name = point.has(GameDataConverter.PC_NAME) ? point.get(GameDataConverter.PC_NAME).asText() : null;
						if (timestamp == null || timestamp.longValue() == 0L) { // global
							if (pc_name != null && pc_name.compareTo(GameDataConverter.PC_GREEN_LEAVES) == 0) {
								score = point.has(GameDataConverter.PC_SCORE) ? point.get(GameDataConverter.PC_SCORE).asInt() : null;
							}
						} else { // specific week
							if (pc_name != null && pc_name.compareTo(GameDataConverter.PC_GREEN_LEAVES) == 0) {
								JsonNode pc_period = point.has(GameDataConverter.PC_PERIODS) ? point.get(GameDataConverter.PC_PERIODS) : null;
								if (pc_period != null) {
									Iterator<String> keys = pc_period.fieldNames();
									while (keys.hasNext()) {
										String key = keys.next();
										JsonNode pc_weekly = pc_period.get(key);
										if (pc_weekly != null) {
											JsonNode pc_instances = pc_weekly.get(GameDataConverter.PC_INSTANCES);

											if (pc_instances != null) {
												Iterator<String> instancesKeys = pc_instances.fieldNames();
												while (instancesKeys.hasNext()) {
													JsonNode pc_instance = pc_instances.get(instancesKeys.next());
													int instance_score = pc_instance.has(GameDataConverter.PC_SCORE) ? pc_instance.get(GameDataConverter.PC_SCORE).asInt() : 0;
													long instance_start = pc_instance.has(GameDataConverter.PC_START) ? pc_instance.get(GameDataConverter.PC_START).asLong() : 0L;
													long instance_end = pc_instance.has(GameDataConverter.PC_END) ? pc_instance.get(GameDataConverter.PC_END).asLong() : 0L;
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
			JsonNode stateData = profileData.has(GameDataConverter.STATE) ? profileData.get(GameDataConverter.STATE) : null;
			JsonNode pointConceptData = null;
			if (stateData != null) {
				pointConceptData = (stateData.has(GameDataConverter.POINT_CONCEPT) && stateData.get(GameDataConverter.POINT_CONCEPT).isArray()) ? stateData.get(GameDataConverter.POINT_CONCEPT) : null;
				if (pointConceptData != null) {
					for (JsonNode point : pointConceptData) {
						String pc_name = point.has(GameDataConverter.PC_NAME) ? point.get(GameDataConverter.PC_NAME).asText() : null;
						if (timestamp == null || timestamp.longValue() == 0L) { // global
							if (pc_name != null && pc_name.compareTo(GameDataConverter.PC_GREEN_LEAVES) == 0) {
								score = point.has(GameDataConverter.PC_SCORE) ? point.get(GameDataConverter.PC_SCORE).asInt() : null;
							}
						} else { // specific week
							if (pc_name != null && pc_name.compareTo(GameDataConverter.PC_GREEN_LEAVES) == 0) {
								JsonNode pc_period = point.has(GameDataConverter.PC_PERIODS) ? point.get(GameDataConverter.PC_PERIODS) : null;
								if (pc_period != null) {
									JsonNode pc_weekly = pc_period.get(GameDataConverter.PC_WEEKLY);
									if (pc_weekly != null) {
										JsonNode pc_instances = pc_weekly.get(GameDataConverter.PC_INSTANCES);
										if (pc_instances != null) {
											Iterator<String> instancesKeys = pc_instances.fieldNames();
											while (instancesKeys.hasNext()) {
												JsonNode pc_instance = pc_instances.get(instancesKeys.next());
												int instance_score = pc_instance.has(GameDataConverter.PC_SCORE) ? pc_instance.get(GameDataConverter.PC_SCORE).asInt() : 0;
												long instance_start = pc_instance.has(GameDataConverter.PC_START) ? pc_instance.get(GameDataConverter.PC_START).asLong() : 0L;
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
					String playerId = profileData.has(GameDataConverter.PLAYER_ID) ? profileData.get(GameDataConverter.PLAYER_ID).asText() : "0";
					score = 0; // here I reset the score value to avoid classification problem
					JsonNode stateData = profileData.has(GameDataConverter.STATE) ? profileData.get(GameDataConverter.STATE) : null;
					JsonNode pointConceptData = null;
					if (stateData != null) {
						pointConceptData = stateData.has(GameDataConverter.POINT_CONCEPT) ? stateData.get(GameDataConverter.POINT_CONCEPT) : null;
						if (pointConceptData != null) {
							for (JsonNode point : pointConceptData) {
								String pc_name = point.has(GameDataConverter.PC_NAME) ? point.get(GameDataConverter.PC_NAME).asText() : null;
								if (timestamp == null || timestamp.longValue() == 0L) { // global
									if (pc_name != null && pc_name.compareTo(GameDataConverter.PC_GREEN_LEAVES) == 0) {
										score = point.has(GameDataConverter.PC_SCORE) ? point.get(GameDataConverter.PC_SCORE).asInt() : null;
									}
								} else { // specific week
									if (pc_name != null && pc_name.compareTo(GameDataConverter.PC_GREEN_LEAVES) == 0) {
										JsonNode pc_period = point.has(GameDataConverter.PC_PERIODS) ? point.get(GameDataConverter.PC_PERIODS) : null;
										if (pc_period != null) {
											Iterator<String> keys = pc_period.fieldNames();
											while (keys.hasNext()) {
												String key = keys.next();
												JsonNode pc_weekly = pc_period.get(key);
												if (pc_weekly != null) {
													JsonNode pc_instances = pc_weekly.get(GameDataConverter.PC_INSTANCES);
													if (pc_instances != null) {
														Iterator<String> instancesKeys = pc_instances.fieldNames();
														while (instancesKeys.hasNext()) {
															JsonNode pc_instance = pc_instances.get(instancesKeys.next());
															int instance_score = pc_instance.has(GameDataConverter.PC_SCORE) ? pc_instance.get(GameDataConverter.PC_SCORE).asInt() : 0;
															long instance_start = pc_instance.has(GameDataConverter.PC_START) ? pc_instance.get(GameDataConverter.PC_START).asLong() : 0L;
															long instance_end = pc_instance.has(GameDataConverter.PC_END) ? pc_instance.get(GameDataConverter.PC_END).asLong() : 0L;
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
					String playerId = profileData.has(GameDataConverter.PLAYER_ID) ? profileData.get(GameDataConverter.PLAYER_ID).asText() : "0";
					score = 0; // here I reset the score value to avoid classification problem
					JsonNode stateData = profileData.has(GameDataConverter.STATE) ? profileData.get(GameDataConverter.STATE) : null;
					JsonNode pointConceptData = null;
					if (stateData != null) {
						pointConceptData = (stateData.has(GameDataConverter.POINT_CONCEPT) && stateData.get(GameDataConverter.POINT_CONCEPT).isArray()) ? stateData.get(GameDataConverter.POINT_CONCEPT) : null;
						if (pointConceptData != null) {
							for (JsonNode point : pointConceptData) {
								String pc_name = point.has(GameDataConverter.PC_NAME) ? point.get(GameDataConverter.PC_NAME).asText() : null;
								if (pc_name != null && pc_name.compareTo(GameDataConverter.PC_GREEN_LEAVES) == 0) {
									score = point.has(GameDataConverter.PC_SCORE) ? point.get(GameDataConverter.PC_SCORE).asInt() : null;
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
						String playerId = profileData.has(GameDataConverter.PLAYER_ID) ? profileData.get(GameDataConverter.PLAYER_ID).asText() : "0";
						Integer playerScore = profileData.has(GameDataConverter.PC_SCORE) ? profileData.get(GameDataConverter.PC_SCORE).asInt() : 0;
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

	private List<GameStatistics> getStatistics(String appId, String counter) throws Exception {
		List<GameStatistics> stats = gamificationCache.getStatistics(appId);
		return stats.stream().filter(x -> counter.equals(x.getPointConceptName())).collect(Collectors.toList());
	}
	
}
