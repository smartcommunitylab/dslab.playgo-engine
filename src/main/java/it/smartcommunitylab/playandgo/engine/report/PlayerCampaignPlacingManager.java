package it.smartcommunitylab.playandgo.engine.report;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
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
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
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
			PlayerStatsTransport globalByMode = playerStatsTransportRepository.findByPlayerIdAndCampaignIdAndModeTypeAndGlobal(
					pt.getPlayerId(), pt.getCampaignId(), pt.getModeType(), Boolean.TRUE);
			if(globalByMode == null) {
				globalByMode = addNewPlacing(pt.getPlayerId(), pt.getCampaignId(), pt.getModeType(), Boolean.TRUE, null);
			}
			globalByMode.addDistance(pt.getDistance());
			globalByMode.addDuration(pt.getDuration());
			globalByMode.addCo2(pt.getCo2());
			globalByMode.addTrack();
			playerStatsTransportRepository.save(globalByMode);
			
			//transport weekly placing
			LocalDate weeklyDay = getWeeklyDay(campaign.getStartDayOfWeek(), trackDay);
			PlayerStatsTransport weeklyByMode = playerStatsTransportRepository.findByPlayerIdAndCampaignIdAndModeTypeAndGlobalAndWeeklyDay(
					pt.getPlayerId(), pt.getCampaignId(), pt.getModeType(), Boolean.FALSE, weeklyDay);
			if(weeklyByMode == null) {
				weeklyByMode = addNewPlacing(pt.getPlayerId(), pt.getCampaignId(), pt.getModeType(), Boolean.FALSE, weeklyDay);
			}
			weeklyByMode.addDistance(pt.getDistance());
			weeklyByMode.addDuration(pt.getDuration());
			weeklyByMode.addCo2(pt.getCo2());
			weeklyByMode.addTrack();
			playerStatsTransportRepository.save(weeklyByMode);			
		}
	}
	
	private LocalDate getWeeklyDay(int startDayOfWeek, LocalDate trackDay) {
		LocalDate dayOfWeek = trackDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(startDayOfWeek)));
		return dayOfWeek;
	}
	
	private PlayerStatsTransport addNewPlacing(String playerId, String campaignId, String modeType, 
			Boolean global, LocalDate weeklyDay) {
		PlayerStatsTransport pst = new PlayerStatsTransport();
		pst.setPlayerId(playerId);
		pst.setCampaignId(campaignId);
		pst.setModeType(modeType);
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
				tracks++;
			}
			status.getTransportStatsList().addAll(transportStatsList);
			status.setCo2(co2);
			status.setTravels(tracks);
			
			//activityDays
			MatchOperation matchOperation = Aggregation.match(new Criteria("playerId").is(player.getPlayerId())
					.and("campaignId").is(campaign.getCampaignId()).and("valid").is(Boolean.TRUE));			
			ProjectionOperation projectionOperation = Aggregation.project("startTime")
					.and(DateOperators.DayOfYear.dayOfYear("startTime")).as("dayOfYear");
			GroupOperation groupOperation = Aggregation.group("dayOfYear").count().as("total");
			Aggregation aggregation = Aggregation.newAggregation(matchOperation, projectionOperation, groupOperation);
			AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, CampaignPlayerTrack.class, Document.class);
			status.setActivityDays(aggregationResults.getMappedResults().size());
		}
		return status;
	}
	
	public Page<CampaignPlacing> getCampaignPlacingByTransportMode(String campaignId, String modeType, 
			LocalDate weeklyDay, Pageable pageRequest) {
		Criteria criteria = new Criteria("campaignId").is(campaignId).and("modeType").is(modeType);
		if(weeklyDay != null) {
			criteria = criteria.and("weeklyDay").is(weeklyDay).and("global").is(Boolean.FALSE);
		} else {
			criteria = criteria.and("global").is(Boolean.TRUE);
		}
		MatchOperation matchOperation = Aggregation.match(criteria);
		GroupOperation groupOperation = Aggregation.group("playerId").sum("distance").as("value");
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
	
	private long countDistincPlayers(Criteria criteria) {
		MatchOperation matchOperation = Aggregation.match(criteria);
		GroupOperation groupOperation = Aggregation.group("playerId");
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, 
				PlayerStatsTransport.class, Document.class);
		return aggregationResults.getMappedResults().size();
	}
	
	public CampaignPlacing getCampaignPlacingByPlayerAndTransportMode(String playerId, String campaignId, 
			String modeType, LocalDate weeklyDay) {
		//get player score
		Criteria criteria = new Criteria("campaignId").is(campaignId).and("modeType").is(modeType).and("playerId").is(playerId);
		if(weeklyDay != null) {
			criteria = criteria.and("weeklyDay").is(weeklyDay).and("global").is(Boolean.FALSE);
		} else {
			criteria = criteria.and("global").is(Boolean.TRUE);
		}
		MatchOperation matchOperation = Aggregation.match(criteria);
		GroupOperation groupOperation = Aggregation.group("playerId").sum("distance").as("value");
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
		AggregationResults<CampaignPlacing> aggregationResults = mongoTemplate.aggregate(aggregation, 
				PlayerStatsTransport.class, CampaignPlacing.class);
		CampaignPlacing placing = aggregationResults.getMappedResults().get(0);
		Player player = playerRepository.findById(playerId).orElse(null);
		if(player != null) {
			placing.setNickname(player.getNickname());
		}
		//get player position
		Criteria criteriaPosition = new Criteria("campaignId").is(campaignId).and("modeType").is(modeType);
		if(weeklyDay != null) {
			criteriaPosition = criteriaPosition.and("weeklyDay").is(weeklyDay).and("global").is(Boolean.FALSE);
		} else {
			criteriaPosition = criteriaPosition.and("global").is(Boolean.TRUE);
		}
		MatchOperation matchModeAndTime = Aggregation.match(criteriaPosition);
		GroupOperation groupByPlayer = Aggregation.group("playerId").sum("distance").as("value");
		MatchOperation filterByDistance = Aggregation.match(new Criteria("value").gt(placing.getValue()));
		Aggregation aggregationPosition = Aggregation.newAggregation(matchModeAndTime, groupByPlayer, filterByDistance);
		AggregationResults<CampaignPlacing> aggregationPositionResults = mongoTemplate.aggregate(aggregationPosition, 
				PlayerStatsTransport.class, CampaignPlacing.class);
		placing.setPosition(aggregationPositionResults.getMappedResults().size() + 1);
		
		return placing;
	}

	public List<PlayerStatsTransport> getPlayerPersonalTransportStats(Player player, LocalDate weeklyDay) {
		Campaign campaign = campaignManager.getDefaultCampaignByTerritory(player.getTerritoryId());
		List<PlayerStatsTransport> list = null;
		if(weeklyDay == null) {
		list = playerStatsTransportRepository.findByPlayerIdAndCampaignIdAndGlobal(player.getPlayerId(), 
				campaign.getCampaignId(), Boolean.TRUE);
		} else {
			list = playerStatsTransportRepository.findByPlayerIdAndCampaignIdAndGlobalAndWeeklyDay(
					player.getPlayerId(), campaign.getCampaignId(), Boolean.FALSE, weeklyDay);
		}
		return list;
	}
	

}

