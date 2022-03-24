package it.smartcommunitylab.playandgo.engine.report;

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
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
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
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsTrack;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.repository.TerritoryRepository;

@Component
public class PlayerReportManager {
	private static Log logger = LogFactory.getLog(PlayerReportManager.class);
	
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
			MatchOperation matchOperation = Aggregation.match(new Criteria("playerId").is(player.getPlayerId()).and("campaignId").is(campaign.getCampaignId()));
			GroupOperation groupOperation = Aggregation.group("modeType").sum("distance").as("totalDistance").sum("duration").as("totalDuration").sum("co2").as("totalCo2");
			Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
			AggregationResults<TransportStats> aggregationResults = mongoTemplate.aggregate(aggregation, PlayerStatsTrack.class, TransportStats.class);
			status.setTransportStatsList(aggregationResults.getMappedResults());
			
			//co2
			status.setCo2(getSavedCo2(status.getTransportStatsList()));
			
			//total travels
			Query query = new Query(new Criteria("playerId").is(player.getPlayerId()).and("campaignId").is(campaign.getCampaignId()));
			long count = mongoTemplate.count(query, PlayerStatsTrack.class);
			status.setTravels((int) count);
			
			//activityDays
			ProjectionOperation projectionOperation = Aggregation.project("startTime").and(DateOperators.DayOfYear.dayOfYear("startTime")).as("dayOfYear");
			groupOperation = Aggregation.group("dayOfYear").count().as("total");
			aggregation = Aggregation.newAggregation(matchOperation, projectionOperation, groupOperation);
			AggregationResults<Document> aggregationResults2 = mongoTemplate.aggregate(aggregation, PlayerStatsTrack.class, Document.class);
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
	
	public Page<CampaignPlacing> getCampaignPlacingByTransportMode(String campaignId, String modeType, Date dateFrom, Date dateTo, Pageable pageRequest) {
		LookupOperation lookupOperation = Aggregation.lookup("playerStatsTracks", "_id", "playerId", "pst");
		UnwindOperation unwindOperation = Aggregation.unwind("pst", true);
		MatchOperation matchOperation = Aggregation.match(new Criteria().orOperator(
				Criteria.where("pst.campaignId").is(campaignId)
				.and("pst.modeType").is(modeType)
				.and("pst.startTime").gt(dateFrom).and("pst.endTime").lt(dateTo),
				Criteria.where("pst").exists(false)
		));
		GroupOperation groupOperation = Aggregation.group("playerId").sum("pst.distance").as("value");
		SortOperation sortByPopDesc = Aggregation.sort(Sort.by(Direction.DESC, "value"));
		SkipOperation skipOperation = Aggregation.skip((long) (pageRequest.getPageNumber() * pageRequest.getPageSize()));
		LimitOperation limitOperation = Aggregation.limit(pageRequest.getPageSize());
		Aggregation aggregation = Aggregation.newAggregation(lookupOperation, unwindOperation, matchOperation, groupOperation, sortByPopDesc, 
				skipOperation, limitOperation);
		AggregationResults<CampaignPlacing> aggregationResults = mongoTemplate.aggregate(aggregation, Player.class, CampaignPlacing.class);
		List<CampaignPlacing> list = aggregationResults.getMappedResults();
		int index = 0;
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
//		GroupOperation groupOperation = Aggregation.group("playerId");
//		CountOperation countOperation = Aggregation.count().as("total");
//		Aggregation aggregation = Aggregation.newAggregation(groupOperation, countOperation);
//		Document document = mongoTemplate.aggregate(aggregation, Player.class, Document.class).getUniqueMappedResult();
//		return document.getInteger("total");
	}
	
	public CampaignPlacing getCampaignPlacingByPlayerAndTransportMode(String playerId, String campaignId, String modeType, Date dateFrom, Date dateTo) {
		//get player score
		MatchOperation matchOperation = Aggregation.match(new Criteria("campaignId").is(campaignId).and("modeType").is(modeType)
				.and("playerId").is(playerId).and("startTime").gt(dateFrom).and("endTime").lt(dateTo));
		GroupOperation groupOperation = Aggregation.group("playerId").sum("distance").as("value");
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
		AggregationResults<CampaignPlacing> aggregationResults = mongoTemplate.aggregate(aggregation, PlayerStatsTrack.class, CampaignPlacing.class);
		CampaignPlacing placing = aggregationResults.getMappedResults().get(0);
		Player player = playerRepository.findById(playerId).orElse(null);
		if(player != null) {
			placing.setNickname(player.getNickname());
		}
		//get player position
		MatchOperation matchModeAndTime = Aggregation.match(new Criteria("campaignId").is(campaignId).and("modeType").is(modeType)
				.and("startTime").gt(dateFrom).and("endTime").lt(dateTo));
		GroupOperation groupByPlayer = Aggregation.group("playerId").sum("distance").as("value");
		MatchOperation filterByDistance = Aggregation.match(new Criteria("value").gt(placing.getValue()));
		Aggregation aggregationPosition = Aggregation.newAggregation(matchModeAndTime, groupByPlayer, filterByDistance);
		AggregationResults<CampaignPlacing> aggregationPositionResults = mongoTemplate.aggregate(aggregationPosition, PlayerStatsTrack.class, CampaignPlacing.class);
		placing.setPosition(aggregationPositionResults.getMappedResults().size() + 1);
		
		return placing;
	}

}

