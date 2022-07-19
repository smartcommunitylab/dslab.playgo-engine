package it.smartcommunitylab.playandgo.engine.manager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
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
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsGame;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsTransport;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.report.CampaignGroupPlacing;
import it.smartcommunitylab.playandgo.engine.report.CampaignPlacing;
import it.smartcommunitylab.playandgo.engine.report.GameStats;
import it.smartcommunitylab.playandgo.engine.report.PlayerStatusReport;
import it.smartcommunitylab.playandgo.engine.report.TransportStat;
import it.smartcommunitylab.playandgo.engine.report.TransportStats;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatsGameRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatsTransportRepository;
import it.smartcommunitylab.playandgo.engine.repository.TerritoryRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;
import it.smartcommunitylab.playandgo.engine.util.Utils;

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
	AvatarManager avatarManager;
	
	@Autowired
	CampaignSubscriptionRepository campaignSubscriptionRepository;
	
	@Autowired
	TerritoryRepository territoryRepository;
	
	@Autowired
	PlayerRepository playerRepository;
	
	@Autowired
	PlayerStatsTransportRepository playerStatsTransportRepository;
	
	@Autowired
	PlayerStatsGameRepository playerStatsGameRepository;
	
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");   
	
	private ZonedDateTime getTrackDay(Campaign campaign, CampaignPlayerTrack pt) {		
		ZoneId zoneId = null;
		Territory territory = territoryRepository.findById(campaign.getTerritoryId()).orElse(null);
		if(territory == null) {
			zoneId = ZoneId.systemDefault();
		} else {
			zoneId = ZoneId.of(territory.getTimezone());
		}
		return ZonedDateTime.ofInstant(pt.getStartTime().toInstant(), zoneId);
	}
	
	public void updatePlayerCampaignPlacings(CampaignPlayerTrack pt) {
		Campaign campaign = campaignManager.getCampaign(pt.getCampaignId());
		if(campaign != null) {
			if(!campaign.getType().equals(Type.personal)) {
				if(pt.getStartTime().before(campaign.getDateFrom()) || pt.getStartTime().after(campaign.getDateTo())) {
					return;
				}
			}
			Player player = playerRepository.findById(pt.getPlayerId()).orElse(null);
			if(player == null) {
				return;
			}
			//transport global placing
			PlayerStatsTransport globalByMode = playerStatsTransportRepository.findByPlayerIdAndCampaignIdAndModeTypeAndGlobal(
					pt.getPlayerId(), pt.getCampaignId(), pt.getModeType(), Boolean.TRUE);
			if(globalByMode == null) {
				globalByMode = addNewPlacing(pt.getPlayerId(), player.getNickname(), pt.getCampaignId(), pt.getModeType(), 
						Boolean.TRUE, null, 0, 0, 0);
			}
			globalByMode.addDistance(pt.getDistance());
			globalByMode.addDuration(pt.getDuration());
			globalByMode.addCo2(pt.getCo2());
			globalByMode.addTrack();
			playerStatsTransportRepository.save(globalByMode);
			
			//transport daily placing
			ZonedDateTime trackDay = getTrackDay(campaign, pt);
			String day = trackDay.format(dtf);
			int weekOfYear = trackDay.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
			int monthOfYear = trackDay.get(ChronoField.MONTH_OF_YEAR);
			int year = trackDay.get(ChronoField.YEAR);
			PlayerStatsTransport dayByMode = playerStatsTransportRepository.findByPlayerIdAndCampaignIdAndModeTypeAndGlobalAndDay(
					pt.getPlayerId(), pt.getCampaignId(), pt.getModeType(), Boolean.FALSE, day);
			if(dayByMode == null) {
				dayByMode = addNewPlacing(pt.getPlayerId(), player.getNickname(), pt.getCampaignId(), pt.getModeType(), 
						Boolean.FALSE, day, weekOfYear, monthOfYear, year);
			}
			dayByMode.addDistance(pt.getDistance());
			dayByMode.addDuration(pt.getDuration());
			dayByMode.addCo2(pt.getCo2());
			dayByMode.addTrack();
			playerStatsTransportRepository.save(dayByMode);			
		}
	}
	
	public void removePlayerCampaignPlacings(CampaignPlayerTrack pt) {
		//transport global placing
		PlayerStatsTransport globalByMode = playerStatsTransportRepository.findByPlayerIdAndCampaignIdAndModeTypeAndGlobal(
				pt.getPlayerId(), pt.getCampaignId(), pt.getModeType(), Boolean.TRUE);
		if(globalByMode != null) {
			globalByMode.subDistance(pt.getDistance());
			globalByMode.subDuration(pt.getDuration());
			globalByMode.subCo2(pt.getCo2());
			globalByMode.subTrack();
			playerStatsTransportRepository.save(globalByMode);
		}
		
		//transport daily placing
		Campaign campaign = campaignManager.getCampaign(pt.getCampaignId());
		ZonedDateTime trackDay = getTrackDay(campaign, pt);
		String day = trackDay.format(dtf);
		PlayerStatsTransport dayByMode = playerStatsTransportRepository.findByPlayerIdAndCampaignIdAndModeTypeAndGlobalAndDay(
				pt.getPlayerId(), pt.getCampaignId(), pt.getModeType(), Boolean.FALSE, day);
		if(dayByMode != null) {
			dayByMode.subDistance(pt.getDistance());
			dayByMode.subDuration(pt.getDuration());
			dayByMode.subCo2(pt.getCo2());
			dayByMode.subTrack();
			playerStatsTransportRepository.save(dayByMode);					
		}
	}
	
	public void updatePlayerCampaignPlacings(CampaignPlayerTrack pt, double deltaDistance, double deltaCo2) {
		//transport global placing
		PlayerStatsTransport globalByMode = playerStatsTransportRepository.findByPlayerIdAndCampaignIdAndModeTypeAndGlobal(
				pt.getPlayerId(), pt.getCampaignId(), pt.getModeType(), Boolean.TRUE);
		if(globalByMode != null) {
			if(deltaDistance > 0) {
				globalByMode.addDistance(deltaDistance);
				globalByMode.addCo2(deltaCo2);
			} else if (deltaDistance < 0) {
				globalByMode.subDistance(Math.abs(deltaDistance));
				globalByMode.subCo2(Math.abs(deltaCo2));
			}
			playerStatsTransportRepository.save(globalByMode);
		}
		
		//transport daily placing
		Campaign campaign = campaignManager.getCampaign(pt.getCampaignId());
		ZonedDateTime trackDay = getTrackDay(campaign, pt);
		String day = trackDay.format(dtf);
		PlayerStatsTransport dayByMode = playerStatsTransportRepository.findByPlayerIdAndCampaignIdAndModeTypeAndGlobalAndDay(
				pt.getPlayerId(), pt.getCampaignId(), pt.getModeType(), Boolean.FALSE, day);
		if(dayByMode != null) {
			if(deltaDistance > 0) {
				dayByMode.addDistance(deltaDistance);
				dayByMode.addCo2(deltaCo2);
			} else if (deltaDistance < 0) {
				dayByMode.subDistance(Math.abs(deltaDistance));
				dayByMode.subCo2(Math.abs(deltaCo2));
			}
			playerStatsTransportRepository.save(dayByMode);			
		}
	}
	
	private LocalDate getWeeklyDay(int startDayOfWeek, LocalDate trackDay) {
		LocalDate dayOfWeek = trackDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(startDayOfWeek)));
		return dayOfWeek;
	}
	
	private PlayerStatsTransport addNewPlacing(String playerId, String nickname, String campaignId, String modeType, 
			Boolean global, String day, int weekOfYear, int monthOfYear, int year) {
		PlayerStatsTransport pst = new PlayerStatsTransport();
		pst.setPlayerId(playerId);
		pst.setNickname(nickname);
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
	
	public PlayerStatusReport getPlayerStatus(Player player) {
		PlayerStatusReport status = new PlayerStatusReport();
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
	
	private long countTransportDistincPlayers(Criteria criteria) {
		MatchOperation matchOperation = Aggregation.match(criteria);
		GroupOperation groupOperation = Aggregation.group("playerId");
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, 
				PlayerStatsTransport.class, Document.class);
		return aggregationResults.getMappedResults().size();
	}

	public Page<CampaignPlacing> getCampaignPlacing(String campaignId, String metric, String mean,  
			String dateFrom, String dateTo, Pageable pageRequest) {
		Criteria criteria = new Criteria("campaignId").is(campaignId);
		if(Utils.isNotEmpty(mean)) {
			criteria = criteria.and("modeType").is(mean);
		}
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteria = criteria.and("global").is(Boolean.TRUE);
		}
		MatchOperation matchOperation = Aggregation.match(criteria);
		String sumField = null;
		if(metric.equalsIgnoreCase("co2")) {
			sumField = "co2";
		} else if(metric.equalsIgnoreCase("tracks")) { 
			sumField = "trackNumber";
		} else {
			sumField = "distance";
		}
		GroupOperation groupOperation = Aggregation.group("nickname").sum(sumField).as("value");
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, "value").and(Direction.ASC, "nickname");
		SkipOperation skipOperation = Aggregation.skip((long) (pageRequest.getPageNumber() * pageRequest.getPageSize()));
		LimitOperation limitOperation = Aggregation.limit(pageRequest.getPageSize());
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, sortOperation, 
				skipOperation, limitOperation);
		AggregationResults<CampaignPlacing> aggregationResults = mongoTemplate.aggregate(aggregation, 
				PlayerStatsTransport.class, CampaignPlacing.class);
		List<CampaignPlacing> list = aggregationResults.getMappedResults();
		int index = pageRequest.getPageNumber() * pageRequest.getPageSize();
		for(CampaignPlacing cp : list) {
			Player player = playerRepository.findByNickname(cp.getNickname());
			if(player != null) {
				cp.setPlayerId(player.getPlayerId());
				cp.setAvatar(avatarManager.getPlayerSmallAvatar(player.getPlayerId()));
			}
			cp.setPosition(index + 1);
			index++;
		}
		return new PageImpl<>(list, pageRequest, countTransportDistincPlayers(criteria));
	}
	
	public CampaignPlacing getCampaignPlacingByPlayer(String playerId, String campaignId, 
			String metric, String mean, String dateFrom, String dateTo) throws Exception {
		Player player = playerRepository.findById(playerId).orElse(null);
		if(player == null) {
			throw new BadRequestException("player not found", ErrorCode.PLAYER_NOT_FOUND);
		}
		//get player score
		Criteria criteria = new Criteria("campaignId").is(campaignId).and("nickname").is(player.getNickname());
		if(Utils.isNotEmpty(mean)) {
			criteria = criteria.and("modeType").is(mean);
		}
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteria = criteria.and("global").is(Boolean.TRUE);
		}
		MatchOperation matchOperation = Aggregation.match(criteria);
		String sumField = null;
		if(metric.equalsIgnoreCase("co2")) {
			sumField = "co2";
		} else if(metric.equalsIgnoreCase("tracks")) { 
			sumField = "trackNumber";
		} else {
			sumField = "distance";
		}
		GroupOperation groupOperation = Aggregation.group("nickname").sum(sumField).as("value");
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
			placing.setPlayerId(player.getPlayerId());
		}
		
		//get player position
		Criteria criteriaPosition = new Criteria("campaignId").is(campaignId);
		if(Utils.isNotEmpty(mean)) {
			criteriaPosition = criteriaPosition.and("modeType").is(mean);
		}
		if((dateFrom != null) && (dateTo != null)) {
			criteriaPosition = criteriaPosition.and("global").is(Boolean.FALSE)
					.andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteriaPosition = criteriaPosition.and("global").is(Boolean.TRUE);
		}
		MatchOperation matchModeAndTime = Aggregation.match(criteriaPosition);
		GroupOperation groupByPlayer = Aggregation.group("nickname").sum(sumField).as("value");
		MatchOperation filterByDistance = Aggregation.match(new Criteria("value").gt(placing.getValue()));
		Aggregation aggregationPosition = Aggregation.newAggregation(matchModeAndTime, groupByPlayer, filterByDistance);
		AggregationResults<CampaignPlacing> aggregationPositionResults = mongoTemplate.aggregate(aggregationPosition, 
				PlayerStatsTransport.class, CampaignPlacing.class);
		placing.setPosition(aggregationPositionResults.getMappedResults().size() + 1);
		
		return placing;
	}
	
	public List<TransportStat> getPlayerTransportStats(String playerId, String campaignId, String groupMode, String metric,
			String mean, String dateFrom, String dateTo) {
		List<TransportStat> result = new ArrayList<>();
		
		//Campaign campaign = campaignManager.getDefaultCampaignByTerritory(player.getTerritoryId());
		Criteria criteria = new Criteria("campaignId").is(campaignId).and("playerId").is(playerId)
				.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		if(Utils.isNotEmpty(mean)) {
			criteria = criteria.and("modeType").is(mean);
		}
		MatchOperation matchOperation = Aggregation.match(criteria);
		
		String groupField = null;
		if(GroupMode.day.toString().equals(groupMode)) {
			groupField = "day";
		} else if(GroupMode.week.toString().equals(groupMode)) {
			groupField = "weekOfYear";
		} else {
			groupField = "monthOfYear";
		}
		String sumField = null;
		if(metric.equalsIgnoreCase("co2")) {
			sumField = "co2";
		} else if(metric.equalsIgnoreCase("tracks")) { 
			sumField = "trackNumber";
		} else {
			sumField = "distance";
		}		
		GroupOperation groupOperation = Aggregation.group(groupField).sum(sumField).as("value");
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, groupField);
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, sortOperation);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, PlayerStatsTransport.class, Document.class);
		for(Document doc : aggregationResults.getMappedResults()) {
			TransportStat stat = new TransportStat();
			stat.setPeriod(doc.getString("_id"));
			if(metric.equalsIgnoreCase("tracks")) {
				Long l = doc.getLong("value");
				stat.setValue(l.doubleValue());
			} else {
				stat.setValue(doc.getDouble("value"));
			}
			result.add(stat);
		}
		return result;
	}
	
	public List<TransportStat> getPlayerTransportStats(String playerId, String campaignId, String metric, String mean, 
			String dateFrom, String dateTo) {
		List<TransportStat> result = new ArrayList<>();
		
		//Campaign campaign = campaignManager.getDefaultCampaignByTerritory(player.getTerritoryId());		
		Criteria criteria = new Criteria("campaignId").is(campaignId).and("playerId").is(playerId);
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteria = criteria.and("global").is(Boolean.TRUE);
		}
		if(Utils.isNotEmpty(mean)) {
			criteria = criteria.and("modeType").is(mean);
		}		
		MatchOperation matchOperation = Aggregation.match(criteria);
		
		String sumField = null;
		if(metric.equalsIgnoreCase("co2")) {
			sumField = "co2";
		} else if(metric.equalsIgnoreCase("tracks")) { 
			sumField = "trackNumber";
		} else {
			sumField = "distance";
		}		
		GroupOperation groupOperation = Aggregation.group("playerId").sum(sumField).as("value");
		
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, PlayerStatsTransport.class, Document.class);
		for(Document doc : aggregationResults.getMappedResults()) {
			TransportStat stat = new TransportStat();
			if(metric.equalsIgnoreCase("tracks")) {
				Long l = doc.getLong("value");
				stat.setValue(l.doubleValue());
			} else {
				stat.setValue(doc.getDouble("value"));
			}
			result.add(stat);
		}
		return result;
	}
	
	public List<TransportStat> getPlayerTransportRecord(String playerId, String campaignId, String groupMode, 
			String metric, String mean) {
		List<TransportStat> result = new ArrayList<>();
		
		//Campaign campaign = campaignManager.getDefaultCampaignByTerritory(player.getTerritoryId());
		Criteria criteria = new Criteria("campaignId").is(campaignId).and("playerId").is(playerId)
				.and("global").is(Boolean.FALSE);
		if(Utils.isNotEmpty(mean)) {
			criteria = criteria.and("modeType").is(mean);
		}
		MatchOperation matchOperation = Aggregation.match(criteria);
		
		String groupField = null;
		if(GroupMode.day.toString().equals(groupMode)) {
			groupField = "day";
		} else if(GroupMode.week.toString().equals(groupMode)) {
			groupField = "weekOfYear";
		} else {
			groupField = "monthOfYear";
		}
		String sumField = null;
		if(metric.equalsIgnoreCase("co2")) {
			sumField = "co2";
		} else if(metric.equalsIgnoreCase("tracks")) { 
			sumField = "trackNumber";
		} else {
			sumField = "distance";
		}		
		GroupOperation groupOperation = Aggregation.group(groupField).sum(sumField).as("value");
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, "value");
		LimitOperation limitOperation = Aggregation.limit(1);
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, sortOperation, limitOperation);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, PlayerStatsTransport.class, Document.class);
		for(Document doc : aggregationResults.getMappedResults()) {
			TransportStat stat = new TransportStat();
			stat.setPeriod(doc.getString("_id"));
			if(metric.equalsIgnoreCase("tracks")) {
				Long l = doc.getLong("value");
				stat.setValue(l.doubleValue());
			} else {
				stat.setValue(doc.getDouble("value"));
			}
			result.add(stat);
		}
		return result;
	}
	
	public Page<CampaignPlacing> getCampaignPlacingByGame(String campaignId,  
			String dateFrom, String dateTo, Pageable pageRequest) {
		Criteria criteria = new Criteria("campaignId").is(campaignId);
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteria = criteria.and("global").is(Boolean.TRUE);
		}
		MatchOperation matchOperation = Aggregation.match(criteria);
		GroupOperation groupOperation = Aggregation.group("nickname").sum("score").as("value");
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, "value").and(Direction.ASC, "nickname");
		SkipOperation skipOperation = Aggregation.skip((long) (pageRequest.getPageNumber() * pageRequest.getPageSize()));
		LimitOperation limitOperation = Aggregation.limit(pageRequest.getPageSize());
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, sortOperation, 
				skipOperation, limitOperation);
		AggregationResults<CampaignPlacing> aggregationResults = mongoTemplate.aggregate(aggregation, 
				PlayerStatsGame.class, CampaignPlacing.class);
		List<CampaignPlacing> list = aggregationResults.getMappedResults();
		int index = pageRequest.getPageNumber() * pageRequest.getPageSize();
		for(CampaignPlacing cp : list) {
			Player player = playerRepository.findByNickname(cp.getNickname());
			if(player != null) {
				cp.setPlayerId(player.getPlayerId());
				cp.setAvatar(avatarManager.getPlayerSmallAvatar(player.getPlayerId()));
			}
			cp.setPosition(index + 1);
			index++;
		}
		return new PageImpl<>(list, pageRequest, countGameDistincPlayers(criteria));
	}
	
	public CampaignPlacing getCampaignPlacingByGameAndPlayer(String playerId, String campaignId,
			String dateFrom, String dateTo) throws Exception {
		Player player = playerRepository.findById(playerId).orElse(null);
		if(player == null) {
			throw new BadRequestException("player not found", ErrorCode.PLAYER_NOT_FOUND);
		}
		//get player score
		Criteria criteria = new Criteria("campaignId").is(campaignId).and("nickname").is(player.getNickname());
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteria = criteria.and("global").is(Boolean.TRUE);
		}
		MatchOperation matchOperation = Aggregation.match(criteria);
		GroupOperation groupOperation = Aggregation.group("nickname").sum("score").as("value");
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
		AggregationResults<CampaignPlacing> aggregationResults = mongoTemplate.aggregate(aggregation, 
				PlayerStatsGame.class, CampaignPlacing.class);
		
		CampaignPlacing placing = null;
		if(aggregationResults.getMappedResults().size() == 0) {
			placing = new CampaignPlacing();
			placing.setPlayerId(playerId);
			placing.setNickname(player.getNickname());
		} else {
			placing = aggregationResults.getMappedResults().get(0);
			placing.setPlayerId(playerId);
		}
		
		//get player position
		Criteria criteriaPosition = new Criteria("campaignId").is(campaignId);
		if((dateFrom != null) && (dateTo != null)) {
			criteriaPosition = criteriaPosition.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteriaPosition = criteriaPosition.and("global").is(Boolean.TRUE);
		}
		MatchOperation matchModeAndTime = Aggregation.match(criteriaPosition);
		GroupOperation groupByPlayer = Aggregation.group("nickname").sum("score").as("value");
		MatchOperation filterByDistance = Aggregation.match(new Criteria("value").gt(placing.getValue()));
		Aggregation aggregationPosition = Aggregation.newAggregation(matchModeAndTime, groupByPlayer, filterByDistance);
		AggregationResults<CampaignPlacing> aggregationPositionResults = mongoTemplate.aggregate(aggregationPosition, 
				PlayerStatsGame.class, CampaignPlacing.class);
		placing.setPosition(aggregationPositionResults.getMappedResults().size() + 1);
		
		return placing;
	}
	
	private long countGameDistincPlayers(Criteria criteria) {
		MatchOperation matchOperation = Aggregation.match(criteria);
		GroupOperation groupOperation = Aggregation.group("playerId");
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, 
				PlayerStatsGame.class, Document.class);
		return aggregationResults.getMappedResults().size();
	}
	
	public List<GameStats> getPlayerGameStats(String playerId, String campaignId, String groupMode, 
			String dateFrom, String dateTo) {
		List<GameStats> result = new ArrayList<>();
		//Campaign campaign = campaignManager.getDefaultCampaignByTerritory(player.getTerritoryId());
		Criteria criteria = new Criteria("campaignId").is(campaignId).and("playerId").is(playerId)
				.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		MatchOperation matchOperation = Aggregation.match(criteria);
		String groupField = null;
		if(GroupMode.day.toString().equals(groupMode)) {
			groupField = "day";
		} else if(GroupMode.week.toString().equals(groupMode)) {
			groupField = "weekOfYear";
		} else {
			groupField = "monthOfYear";
		}
		GroupOperation groupOperation = Aggregation.group(groupField).sum("score").as("totalScore");
		SortOperation sortOperation = Aggregation.sort(Sort.by(Direction.DESC, "_id"));
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, sortOperation);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, PlayerStatsGame.class, Document.class);
		for(Document doc : aggregationResults.getMappedResults()) {
			GameStats stats = new GameStats();
			stats.setPeriod(doc.getString("_id"));
			stats.setTotalScore(doc.getDouble("totalScore"));
			result.add(stats);
		}
		return result;
	}
	
	public double getPlayerGameTotalScore(String playerId, String campaignId) {
		PlayerStatsGame statsGame = playerStatsGameRepository.findGlobalByPlayerIdAndCampaignId(playerId, campaignId);
		if(statsGame != null) {
			return statsGame.getScore();
		}
		return 0.0;
	}
	
	private long countGameDistincGroups(Criteria criteria) {
		MatchOperation matchOperation = Aggregation.match(criteria);
		GroupOperation groupOperation = Aggregation.group("groupId");
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, 
				PlayerStatsGame.class, Document.class);
		return aggregationResults.getMappedResults().size();
	}
	
	public List<CampaignGroupPlacing> getCampaignGroupPlacingByGame(String campaignId,  
			String dateFrom, String dateTo) {
		Criteria criteria = new Criteria("campaignId").is(campaignId);
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteria = criteria.and("global").is(Boolean.TRUE);
		}
		MatchOperation matchOperation = Aggregation.match(criteria);
		GroupOperation groupOperation = Aggregation.group("groupId").sum("score").as("value");
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, "value").and(Direction.ASC, "groupId");
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, sortOperation);
		AggregationResults<CampaignGroupPlacing> aggregationResults = mongoTemplate.aggregate(aggregation, 
				PlayerStatsGame.class, CampaignGroupPlacing.class);
		List<CampaignGroupPlacing> list = aggregationResults.getMappedResults();
		int index = 0;
		for(CampaignGroupPlacing cp : list) {
			cp.setPosition(index + 1);
			index++;
		}
		return list;
	}

	public CampaignGroupPlacing getCampaignGroupPlacingByGameAndPlayer(String groupId, String campaignId,
			String dateFrom, String dateTo) throws Exception {
		//get group score
		Criteria criteria = new Criteria("campaignId").is(campaignId).and("groupId").is(groupId);
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteria = criteria.and("global").is(Boolean.TRUE);
		}
		MatchOperation matchOperation = Aggregation.match(criteria);
		GroupOperation groupOperation = Aggregation.group("groupId").sum("score").as("value");
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
		AggregationResults<CampaignGroupPlacing> aggregationResults = mongoTemplate.aggregate(aggregation, 
				PlayerStatsGame.class, CampaignGroupPlacing.class);
		
		CampaignGroupPlacing placing = null;
		if(aggregationResults.getMappedResults().size() == 0) {
			placing = new CampaignGroupPlacing();
			placing.setGroupId(groupId);
		} else {
			placing = aggregationResults.getMappedResults().get(0);
		}
		
		//get group position
		Criteria criteriaPosition = new Criteria("campaignId").is(campaignId);
		if((dateFrom != null) && (dateTo != null)) {
			criteriaPosition = criteriaPosition.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteriaPosition = criteriaPosition.and("global").is(Boolean.TRUE);
		}
		MatchOperation matchModeAndTime = Aggregation.match(criteriaPosition);
		GroupOperation groupByPlayer = Aggregation.group("groupId").sum("score").as("value");
		MatchOperation filterByDistance = Aggregation.match(new Criteria("value").gt(placing.getValue()));
		Aggregation aggregationPosition = Aggregation.newAggregation(matchModeAndTime, groupByPlayer, filterByDistance);
		AggregationResults<CampaignGroupPlacing> aggregationPositionResults = mongoTemplate.aggregate(aggregationPosition, 
				PlayerStatsGame.class, CampaignGroupPlacing.class);
		placing.setPosition(aggregationPositionResults.getMappedResults().size() + 1);
		
		return placing;
	}

	public List<GameStats> getGroupGameStats(String groupId, String campaignId, String groupMode, 
			String dateFrom, String dateTo) {
		List<GameStats> result = new ArrayList<>();
		//Campaign campaign = campaignManager.getDefaultCampaignByTerritory(player.getTerritoryId());
		Criteria criteria = new Criteria("campaignId").is(campaignId).and("groupId").is(groupId)
				.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		MatchOperation matchOperation = Aggregation.match(criteria);
		String groupField = null;
		if(GroupMode.day.toString().equals(groupMode)) {
			groupField = "day";
		} else if(GroupMode.week.toString().equals(groupMode)) {
			groupField = "weekOfYear";
		} else {
			groupField = "monthOfYear";
		}
		GroupOperation groupOperation = Aggregation.group(groupField).sum("score").as("totalScore");
		SortOperation sortOperation = Aggregation.sort(Sort.by(Direction.DESC, "_id"));
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, sortOperation);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, PlayerStatsGame.class, Document.class);
		for(Document doc : aggregationResults.getMappedResults()) {
			GameStats stats = new GameStats();
			stats.setPeriod(doc.getString("_id"));
			stats.setTotalScore(doc.getDouble("totalScore"));
			result.add(stats);
		}
		return result;
	}

}

