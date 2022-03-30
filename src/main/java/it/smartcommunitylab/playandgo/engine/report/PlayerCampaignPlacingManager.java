package it.smartcommunitylab.playandgo.engine.report;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.manager.CampaignManager;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsTransport;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatsTransportRepository;
import it.smartcommunitylab.playandgo.engine.repository.TerritoryRepository;

@Component
public class PlayerCampaignPlacingManager {
	private static Log logger = LogFactory.getLog(PlayerCampaignPlacingManager.class);
	
	@Autowired
	MongoTemplate mongoTemplate;
	
	@Autowired
	CampaignManager campaignManager;
	
	@Autowired
	CampaignSubscriptionRepository campaignSubscriptionRepository;
	
	@Autowired
	TerritoryRepository territoryRepository;
	
	@Autowired
	PlayerRepository playerRepository;
	
	@Autowired
	PlayerStatsTransportRepository playerStatsTransportRepository;
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	
	public void updatePlayerCampaignPlacings(CampaignPlayerTrack pt) {
		Campaign campaign = campaignManager.getCampaign(pt.getCampaignId());
		if(campaign != null) {
			LocalDate trackDay = pt.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			if(!campaign.getType().equals(Type.personal)) {
				if(trackDay.isBefore(campaign.getDateFrom()) || trackDay.isAfter(campaign.getDateTo())) {
					return;
				}
			}
			//transport global placing
			PlayerStatsTransport globalByMode = playerStatsTransportRepository.findByPlayerIdAndCampaignIdAndScoreTypeAndGlobal(
					pt.getPlayerId(), pt.getCampaignId(), pt.getModeType(), Boolean.TRUE);
			if(globalByMode == null) {
				globalByMode = addNewPlacing(pt.getPlayerId(), pt.getCampaignId(), pt.getModeType(), Boolean.TRUE, null);
			}
			globalByMode.setScore(globalByMode.getScore() + pt.getDistance());
			playerStatsTransportRepository.save(globalByMode);
			
			//transport weekly placing
			LocalDate weeklyDay = getWeeklyDay(campaign.getStartDayOfWeek(), trackDay);
			PlayerStatsTransport weeklyByMode = playerStatsTransportRepository.findByPlayerIdAndCampaignIdAndScoreTypeAndGlobalAndWeeklyDay(
					pt.getPlayerId(), pt.getCampaignId(), pt.getModeType(), Boolean.FALSE, weeklyDay);
			if(weeklyByMode == null) {
				weeklyByMode = addNewPlacing(pt.getPlayerId(), pt.getCampaignId(), pt.getModeType(), Boolean.FALSE, weeklyDay);
			}
			weeklyByMode.setScore(weeklyByMode.getScore() + pt.getDistance());
			playerStatsTransportRepository.save(weeklyByMode);
			
			//co2 global placing
			PlayerStatsTransport globalByCo2 = playerStatsTransportRepository.findByPlayerIdAndCampaignIdAndScoreTypeAndGlobal(
					pt.getPlayerId(), pt.getCampaignId(), "CO2", Boolean.TRUE);
			if(globalByCo2 == null) {
				globalByCo2 = addNewPlacing(pt.getPlayerId(), pt.getCampaignId(), "CO2", Boolean.TRUE, null);
			}
			globalByCo2.setScore(globalByCo2.getScore() + pt.getCo2());
			playerStatsTransportRepository.save(globalByCo2);
			
			//co2 weekly placing
			PlayerStatsTransport weeklyByCo2 = playerStatsTransportRepository.findByPlayerIdAndCampaignIdAndScoreTypeAndGlobalAndWeeklyDay(
					pt.getPlayerId(), pt.getCampaignId(), "CO2", Boolean.FALSE, weeklyDay);
			if(weeklyByCo2 == null) {
				weeklyByCo2 = addNewPlacing(pt.getPlayerId(), pt.getCampaignId(), "CO2", Boolean.FALSE, weeklyDay);
			}
			weeklyByCo2.setScore(weeklyByCo2.getScore() + pt.getCo2());
			playerStatsTransportRepository.save(weeklyByCo2);			
		}
	}
	
	private LocalDate getWeeklyDay(int startDayOfWeek, LocalDate trackDay) {
		LocalDate dayOfWeek = trackDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(startDayOfWeek)));
		return dayOfWeek;
	}
	
	private PlayerStatsTransport addNewPlacing(String playerId, String campaignId, String scoreType, 
			Boolean global, LocalDate weeklyDay) {
		PlayerStatsTransport pst = new PlayerStatsTransport();
		pst.setPlayerId(playerId);
		pst.setCampaignId(campaignId);
		pst.setScoreType(scoreType);
		pst.setGlobal(global);
		pst.setWeeklyDay(weeklyDay);
		playerStatsTransportRepository.save(pst);
		return pst;
	}
	
	public PlayerStatus getPlayerStatus(Player player) {
		PlayerStatus status = new PlayerStatus();
		status.setPlayerId(player.getPlayerId());
		status.setNickname(player.getNickname());
		status.setMail(player.getMail());
		Territory territory = territoryRepository.findById(player.getTerritoryId()).orElse(null);
		if(territory != null) {
			status.setTerritory(territory);
		}
		Campaign campaign = campaignManager.getDefaultCampaignByTerritory(player.getTerritoryId());
		if(campaign != null) {
			CampaignSubscription campaignSubscription = campaignSubscriptionRepository.findByCampaignIdAndPlayerId(campaign.getCampaignId(), player.getPlayerId());
			if(campaignSubscription != null) {
				status.setRegistrationDate(campaignSubscription.getRegistrationDate());
			}
			
			//transport stats
			MatchOperation matchOperation = Aggregation.match(new Criteria("playerId").is(player.getPlayerId()).and("campaignId").is(campaign.getCampaignId())
					.and("valid").is(Boolean.TRUE));
			GroupOperation groupOperation = Aggregation.group("modeType").sum("distance").as("totalDistance").sum("duration").as("totalDuration").sum("co2").as("totalCo2");
			Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
			AggregationResults<TransportStats> aggregationResults = mongoTemplate.aggregate(aggregation, CampaignPlayerTrack.class, TransportStats.class);
			status.setTransportStatsList(aggregationResults.getMappedResults());
			
			//co2
			status.setCo2(getSavedCo2(status.getTransportStatsList()));
			
			//total travels
			Query query = new Query(new Criteria("playerId").is(player.getPlayerId()).and("campaignId").is(campaign.getCampaignId()).and("valid").is(Boolean.TRUE));
			long count = mongoTemplate.count(query, CampaignPlayerTrack.class);
			status.setTravels((int) count);
			
			//activityDays
			ProjectionOperation projectionOperation = Aggregation.project("startTime").and(DateOperators.DayOfYear.dayOfYear("startTime")).as("dayOfYear");
			groupOperation = Aggregation.group("dayOfYear").count().as("total");
			aggregation = Aggregation.newAggregation(matchOperation, projectionOperation, groupOperation);
			AggregationResults<Document> aggregationResults2 = mongoTemplate.aggregate(aggregation, CampaignPlayerTrack.class, Document.class);
			status.setActivityDays(aggregationResults2.getMappedResults().size());
		}
		return status;
	}
	
	private double getSavedCo2(List<TransportStats> transportStatsList) {
		double co2 = 0.0;
		for(TransportStats ts : transportStatsList) {
			co2 += ts.getTotalCo2();
		}
		return co2;
	}
	
	public Page<CampaignPlacing> getCampaignPlacingByTransportModeFull(String campaignId, String modeType, Date dateFrom, Date dateTo, Pageable pageRequest) {
		String lookupQuery = "{$lookup: {"
				+ " from: 'playerStatsTracks',"
				+ " localField: '_id',"
				+ " foreignField: 'playerId',"
				+ " pipeline: [{"
				+ "   $match: {"
				+ "    campaignId: '" + campaignId + "',"
				+ "    modeType: '" + modeType + "',"
				+ "    startTime: {"
				+ "      $gt: ISODate('" + sdf.format(dateFrom) + "'),"
				+ "      $lt: ISODate('" + sdf.format(dateTo) + "')" 
				+ "    }"
				+ "   }"
				+ " }],"
				+ " as: 'pst'"
				+ "}}";
		CustomAggregationOperation lookupOperation = new CustomAggregationOperation(lookupQuery);
		UnwindOperation unwindOperation = Aggregation.unwind("pst", true);
		GroupOperation groupOperation = Aggregation.group("playerId").sum("pst.distance").as("value");
		SortOperation sortOperation = Aggregation.sort(Sort.by(Direction.DESC, "value"));
		SkipOperation skipOperation = Aggregation.skip((long) (pageRequest.getPageNumber() * pageRequest.getPageSize()));
		LimitOperation limitOperation = Aggregation.limit(pageRequest.getPageSize());
		Aggregation aggregation = Aggregation.newAggregation(lookupOperation, unwindOperation, groupOperation, sortOperation, 
				skipOperation, limitOperation);
		AggregationResults<CampaignPlacing> aggregationResults = mongoTemplate.aggregate(aggregation, Player.class, CampaignPlacing.class);
		List<CampaignPlacing> list = aggregationResults.getMappedResults();
		int index = pageRequest.getPageNumber() * pageRequest.getPageSize();
		for(CampaignPlacing cp : list) {
			Player player = playerRepository.findById(cp.getPlayerId()).orElse(null);
			if(player != null) {
				cp.setNickname(player.getNickname());
			}
			cp.setPosition(index + 1);
			index++;
		}
		return new PageImpl<>(list, pageRequest, countDistincPlayers());
	}
	
	public Page<CampaignPlacing> getCampaignPlacingByTransportMode(String campaignId, String scoreType, boolean global, LocalDate weeklyDay, Pageable pageRequest) {
		Criteria criteria = new Criteria("campaignId").is(campaignId).and("scoreType").is(scoreType)
				.and("global").is(global);
		if(!global) {
			criteria = criteria.and("weeklyDay").is(weeklyDay);
		}
		MatchOperation matchOperation = Aggregation.match(criteria);
		GroupOperation groupOperation = Aggregation.group("playerId").sum("score").as("value");
		SortOperation sortOperation = Aggregation.sort(Sort.by(Direction.DESC, "value"));
		SkipOperation skipOperation = Aggregation.skip((long) (pageRequest.getPageNumber() * pageRequest.getPageSize()));
		LimitOperation limitOperation = Aggregation.limit(pageRequest.getPageSize());
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, sortOperation, 
				skipOperation, limitOperation);
		AggregationResults<CampaignPlacing> aggregationResults = mongoTemplate.aggregate(aggregation, PlayerStatsTransport.class, CampaignPlacing.class);
		List<CampaignPlacing> list = aggregationResults.getMappedResults();
		int index = pageRequest.getPageNumber() * pageRequest.getPageSize();
		for(CampaignPlacing cp : list) {
			Player player = playerRepository.findById(cp.getPlayerId()).orElse(null);
			if(player != null) {
				cp.setNickname(player.getNickname());
			}
			cp.setPosition(index + 1);
			index++;
		}
		return new PageImpl<>(list, pageRequest, countDistincPlayers());
	}
	
	public long countDistincPlayers() {
		return playerRepository.count();
	}
	
	public CampaignPlacing getCampaignPlacingByPlayerAndTransportMode(String playerId, String campaignId, String scoreType, boolean global, LocalDate weeklyDay) {
		//get player score
		Criteria criteria = new Criteria("campaignId").is(campaignId).and("scoreType").is(scoreType)
		.and("playerId").is(playerId).and("global").is(global);
		if(!global) {
			criteria = criteria.and("weeklyDay").is(weeklyDay);
		}		
		MatchOperation matchOperation = Aggregation.match(criteria);
		GroupOperation groupOperation = Aggregation.group("playerId").sum("score").as("value");
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
		AggregationResults<CampaignPlacing> aggregationResults = mongoTemplate.aggregate(aggregation, PlayerStatsTransport.class, CampaignPlacing.class);
		CampaignPlacing placing = aggregationResults.getMappedResults().get(0);
		Player player = playerRepository.findById(playerId).orElse(null);
		if(player != null) {
			placing.setNickname(player.getNickname());
		}
		//get player position
		Criteria criteriaPosition = new Criteria("campaignId").is(campaignId).and("scoreType").is(scoreType).and("global").is(global);
		if(!global) {
			criteriaPosition = criteriaPosition.and("weeklyDay").is(weeklyDay);
		}		
		MatchOperation matchModeAndTime = Aggregation.match(criteriaPosition);
		GroupOperation groupByPlayer = Aggregation.group("playerId").sum("score").as("value");
		MatchOperation filterByDistance = Aggregation.match(new Criteria("value").gt(placing.getValue()));
		Aggregation aggregationPosition = Aggregation.newAggregation(matchModeAndTime, groupByPlayer, filterByDistance);
		AggregationResults<CampaignPlacing> aggregationPositionResults = mongoTemplate.aggregate(aggregationPosition, PlayerStatsTransport.class, CampaignPlacing.class);
		placing.setPosition(aggregationPositionResults.getMappedResults().size() + 1);
		
		return placing;
	}

}

