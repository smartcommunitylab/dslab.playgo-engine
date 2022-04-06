package it.smartcommunitylab.playandgo.engine.manager;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
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
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsTransport;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.report.CampaignPlacing;
import it.smartcommunitylab.playandgo.engine.report.PlayerStatus;
import it.smartcommunitylab.playandgo.engine.report.TransportStats;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatsTransportRepository;
import it.smartcommunitylab.playandgo.engine.repository.TerritoryRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@Component
public class PlayerCampaignPlacingManager {
	private static Log logger = LogFactory.getLog(PlayerCampaignPlacingManager.class);
	
	public static enum GroupMode {
		day, week, month
	};
	
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
			PlayerStatsTransport globalByMode = playerStatsTransportRepository.findByPlayerIdAndCampaignIdAndModeTypeAndGlobal(
					pt.getPlayerId(), pt.getCampaignId(), pt.getModeType(), Boolean.TRUE);
			if(globalByMode == null) {
				globalByMode = addNewPlacing(pt.getPlayerId(), pt.getCampaignId(), pt.getModeType(), Boolean.TRUE, 
						null, 0, 0, 0);
			}
			globalByMode.addDistance(pt.getDistance());
			globalByMode.addDuration(pt.getDuration());
			globalByMode.addCo2(pt.getCo2());
			globalByMode.addTrack();
			playerStatsTransportRepository.save(globalByMode);
			
			//transport daily placing
			int weekOfYear = trackDay.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
			int monthOfYear = trackDay.get(ChronoField.MONTH_OF_YEAR);
			int year = trackDay.get(ChronoField.YEAR);
			PlayerStatsTransport dayByMode = playerStatsTransportRepository.findByPlayerIdAndCampaignIdAndModeTypeAndGlobalAndDay(
					pt.getPlayerId(), pt.getCampaignId(), pt.getModeType(), Boolean.FALSE, trackDay);
			if(dayByMode == null) {
				dayByMode = addNewPlacing(pt.getPlayerId(), pt.getCampaignId(), pt.getModeType(), Boolean.FALSE, 
						trackDay, weekOfYear, monthOfYear, year);
			}
			dayByMode.addDistance(pt.getDistance());
			dayByMode.addDuration(pt.getDuration());
			dayByMode.addCo2(pt.getCo2());
			dayByMode.addTrack();
			playerStatsTransportRepository.save(dayByMode);			
		}
	}
	
	private LocalDate getWeeklyDay(int startDayOfWeek, LocalDate trackDay) {
		LocalDate dayOfWeek = trackDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(startDayOfWeek)));
		return dayOfWeek;
	}
	
	private PlayerStatsTransport addNewPlacing(String playerId, String campaignId, String modeType, 
			Boolean global, LocalDate day, int weekOfYear, int monthOfYear, int year) {
		PlayerStatsTransport pst = new PlayerStatsTransport();
		pst.setPlayerId(playerId);
		pst.setCampaignId(campaignId);
		pst.setModeType(modeType);
		pst.setGlobal(global);
		if(!global) {
			pst.setDay(day);
			pst.setWeekOfYear(year + "-" + weekOfYear);
			pst.setMonthOfYear(year + "-" + monthOfYear);			
		}
		playerStatsTransportRepository.save(pst);
		return pst;
	}
	
	public PlayerStatus getPlayerStatus(Player player) {
		PlayerStatus status = new PlayerStatus();
		status.setPlayerId(player.getPlayerId());
		Territory territory = territoryRepository.findById(player.getTerritoryId()).orElse(null);
		if(territory != null) {
			status.setTerritory(territory);
		}
		Campaign campaign = campaignManager.getDefaultCampaignByTerritory(player.getTerritoryId());
		if(campaign != null) {
			CampaignSubscription campaignSubscription = campaignSubscriptionRepository.findByCampaignIdAndPlayerId(
					campaign.getCampaignId(), player.getPlayerId());
			if(campaignSubscription != null) {
				status.setRegistrationDate(campaignSubscription.getRegistrationDate());
			}
			
			//transport stats
			List<PlayerStatsTransport> playerStats = playerStatsTransportRepository.findByPlayerIdAndCampaignIdAndGlobal(
					player.getPlayerId(), campaign.getCampaignId(), Boolean.TRUE);
			List<TransportStats> transportStatsList = new ArrayList<>();
			double co2 = 0.0;
			long tracks = 0;
			for(PlayerStatsTransport pst : playerStats) {
				TransportStats ts = new TransportStats();
				ts.setModeType(pst.getModeType());
				ts.setTotalDistance(pst.getDistance());
				ts.setTotalDuration(pst.getDuration());
				ts.setTotalCo2(pst.getCo2());
				transportStatsList.add(ts);
				co2 += pst.getCo2();
				tracks += pst.getTrackNumber();
			}
			status.getTransportStatsList().addAll(transportStatsList);
			status.setCo2(co2);
			status.setTravels(tracks);
			
			//activityDays
			MatchOperation matchOperation = Aggregation.match(new Criteria("playerId").is(player.getPlayerId())
					.and("campaignId").is(campaign.getCampaignId()).and("valid").is(Boolean.TRUE));			
			ProjectionOperation projectionOperation = Aggregation.project("startTime")
					.and("startTime").extractYear().as("year")
					.and("startTime").extractDayOfYear().as("dayOfYear");
			GroupOperation groupOperation = Aggregation.group("year","dayOfYear").count().as("total");
			Aggregation aggregation = Aggregation.newAggregation(matchOperation, projectionOperation, groupOperation);
			AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, CampaignPlayerTrack.class, Document.class);
			status.setActivityDays(aggregationResults.getMappedResults().size());
		}
		return status;
	}
	
	private long countDistincPlayers(Criteria criteria) {
		MatchOperation matchOperation = Aggregation.match(criteria);
		GroupOperation groupOperation = Aggregation.group("playerId");
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, 
				PlayerStatsTransport.class, Document.class);
		return aggregationResults.getMappedResults().size();
	}

	private Page<CampaignPlacing> getCampaignPlacing(String campaignId, String groupMode, 
			LocalDate dateFrom, LocalDate dateTo, Pageable pageRequest) {
		Criteria criteria = new Criteria("campaignId").is(campaignId);
		if(!groupMode.equalsIgnoreCase("co2")) {
			criteria = criteria.and("modeType").is(groupMode);
		}
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteria = criteria.and("global").is(Boolean.TRUE);
		}
		MatchOperation matchOperation = Aggregation.match(criteria);
		String sumField = null;
		if(groupMode.equalsIgnoreCase("co2")) {
			sumField = "co2";
		} else {
			sumField = "distance";
		}
		GroupOperation groupOperation = Aggregation.group("playerId").sum(sumField).as("value");
		SortOperation sortOperation = Aggregation.sort(Sort.by(Direction.DESC, "value"));
		SkipOperation skipOperation = Aggregation.skip((long) (pageRequest.getPageNumber() * pageRequest.getPageSize()));
		LimitOperation limitOperation = Aggregation.limit(pageRequest.getPageSize());
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, sortOperation, 
				skipOperation, limitOperation);
		AggregationResults<CampaignPlacing> aggregationResults = mongoTemplate.aggregate(aggregation, 
				PlayerStatsTransport.class, CampaignPlacing.class);
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
		return new PageImpl<>(list, pageRequest, countDistincPlayers(criteria));
	}
	
	private CampaignPlacing getCampaignPlacingByPlayer(String playerId, String campaignId, 
			String groupMode, LocalDate dateFrom, LocalDate dateTo) throws Exception {
		Player player = playerRepository.findById(playerId).orElse(null);
		if(player == null) {
			throw new BadRequestException("player not found", ErrorCode.PLAYER_NOT_FOUND);
		}
		//get player score
		Criteria criteria = new Criteria("campaignId").is(campaignId).and("playerId").is(playerId);
		if(!groupMode.equalsIgnoreCase("co2")) {
			criteria = criteria.and("modeType").is(groupMode);
		}		
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteria = criteria.and("global").is(Boolean.TRUE);
		}
		MatchOperation matchOperation = Aggregation.match(criteria);
		String sumField = null;
		if(groupMode.equalsIgnoreCase("co2")) {
			sumField = "co2";
		} else {
			sumField = "distance";
		}		
		GroupOperation groupOperation = Aggregation.group("playerId").sum(sumField).as("value");
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
		AggregationResults<CampaignPlacing> aggregationResults = mongoTemplate.aggregate(aggregation, 
				PlayerStatsTransport.class, CampaignPlacing.class);
		
		CampaignPlacing placing = null;
		if(aggregationResults.getMappedResults().size() == 0) {
			placing = new CampaignPlacing();
			placing.setPlayerId(playerId);
			placing.setNickname(player.getNickname());
		} else {
			placing = aggregationResults.getMappedResults().get(0);
			placing.setNickname(player.getNickname());
		}
		
		//get player position
		Criteria criteriaPosition = new Criteria("campaignId").is(campaignId);
		if(!groupMode.equalsIgnoreCase("co2")) {
			criteriaPosition = criteriaPosition.and("modeType").is(groupMode);
		}				
		if((dateFrom != null) && (dateTo != null)) {
			criteriaPosition = criteriaPosition.and("global").is(Boolean.FALSE)
					.andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteria = criteria.and("global").is(Boolean.TRUE);
		}
		MatchOperation matchModeAndTime = Aggregation.match(criteriaPosition);
		GroupOperation groupByPlayer = Aggregation.group("playerId").sum(sumField).as("value");
		MatchOperation filterByDistance = Aggregation.match(new Criteria("value").gt(placing.getValue()));
		Aggregation aggregationPosition = Aggregation.newAggregation(matchModeAndTime, groupByPlayer, filterByDistance);
		AggregationResults<CampaignPlacing> aggregationPositionResults = mongoTemplate.aggregate(aggregationPosition, 
				PlayerStatsTransport.class, CampaignPlacing.class);
		placing.setPosition(aggregationPositionResults.getMappedResults().size() + 1);
		
		return placing;
	}
	
	public Page<CampaignPlacing> getCampaignPlacingByTransportMode(String campaignId, String modeType, 
			LocalDate dateFrom, LocalDate dateTo, Pageable pageRequest) {
		return getCampaignPlacing(campaignId, modeType, dateFrom, dateTo, pageRequest);
	}
	
	
	public CampaignPlacing getCampaignPlacingByPlayerAndTransportMode(String playerId, String campaignId, 
			String modeType, LocalDate dateFrom, LocalDate dateTo) throws Exception {
		return getCampaignPlacingByPlayer(playerId, campaignId, modeType, dateFrom, dateTo);
	}
	
	public Page<CampaignPlacing> getCampaignPlacingByCo2(String campaignId, LocalDate dateFrom, LocalDate dateTo, 
			Pageable pageRequest) {
		return getCampaignPlacing(campaignId, "co2", dateFrom, dateTo, pageRequest);
	}
	
	public CampaignPlacing getCampaignPlacingByPlayerAndCo2(String playerId, String campaignId, 
			LocalDate dateFrom, LocalDate dateTo) throws Exception {
		return getCampaignPlacingByPlayer(playerId, campaignId, "co2", dateFrom, dateTo);
	}

	public List<TransportStats> getPlayerTransportStats(Player player, LocalDate dateFrom, LocalDate dateTo, 
			String groupMode) {
		List<TransportStats> result = new ArrayList<>();
		Campaign campaign = campaignManager.getDefaultCampaignByTerritory(player.getTerritoryId());
		Criteria criteria = new Criteria("campaignId").is(campaign.getCampaignId())
				.and("playerId").is(player.getPlayerId()).and("global").is(Boolean.FALSE)
				.andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		MatchOperation matchOperation = Aggregation.match(criteria);
		String groupField = null;
		if(GroupMode.day.toString().equals(groupMode)) {
			groupField = "day";
		} else if(GroupMode.week.toString().equals(groupMode)) {
			groupField = "weekOfYear";
		} else {
			groupField = "monthOfYear";
		}
		GroupOperation groupOperation = Aggregation.group("modeType", groupField)
				.sum("distance").as("totalDistance")
				.sum("duration").as("totalDuration")
				.sum("co2").as("totalCo2")
				.sum("trackNumber").as("totalTravel");			
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, PlayerStatsTransport.class, Document.class);
		for(Document doc : aggregationResults.getMappedResults()) {
			TransportStats stats = new TransportStats();
			if(GroupMode.day.toString().equals(groupMode)) {
				Date date = ((Document)doc.get("_id")).getDate(groupField);
				stats.setPeriod(sdf.format(date));
			} else {
				stats.setPeriod(((Document)doc.get("_id")).getString(groupField));
			}
			stats.setModeType(((Document)doc.get("_id")).getString("modeType"));
			stats.setTotalDistance(doc.getDouble("totalDistance"));
			stats.setTotalDuration(doc.getLong("totalDuration"));
			stats.setTotalCo2(doc.getDouble("totalCo2"));
			stats.setTotalTravel(doc.getLong("totalTravel"));
			result.add(stats);
		}
		return result;
	}
	
}

