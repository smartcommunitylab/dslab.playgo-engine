package it.smartcommunitylab.playandgo.engine.report;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
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
			GroupOperation groupOperation = Aggregation.group("modeType").sum("distance").as("totalDistance").sum("duration").as("totalDuration");
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
		// TODO calculate co2
		return 0.0;
	}
	
	public List<CampaignPlacing> getCampaignPlacingByTransportMode(String campaignId, String modeType, Date dateFrom, Date dateTo) {
		MatchOperation matchOperation = Aggregation.match(new Criteria("campaignId").is(campaignId).and("modeType").is(modeType)
				.and("startTime").gt(dateFrom).and("endTime").lt(dateTo));
		GroupOperation groupOperation = Aggregation.group("playerId").sum("distance").as("totalDistance");
		SortOperation sortByPopDesc = Aggregation.sort(Sort.by(Direction.DESC, "totalDistance"));
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, sortByPopDesc);
		AggregationResults<CampaignPlacing> aggregationResults = mongoTemplate.aggregate(aggregation, PlayerStatsTrack.class, CampaignPlacing.class);
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
		return list;
	}
	
}

