package it.smartcommunitylab.playandgo.engine.manager;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.dto.ChallengeStatsInfo;
import it.smartcommunitylab.playandgo.engine.manager.PlayerCampaignPlacingManager.GroupMode;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatChallenge;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatChallengeRepository;
import it.smartcommunitylab.playandgo.engine.repository.TerritoryRepository;

@Component
public class ChallengeStatsManager {
	@Autowired
	MongoTemplate mongoTemplate;

	@Autowired
	PlayerStatChallengeRepository playerStatChallengeRepository;
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	TerritoryRepository territoryRepository;
	
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	private ZonedDateTime getDay(Campaign campaign, long timestamp) {
		ZoneId zoneId = null;
		Territory territory = territoryRepository.findById(campaign.getTerritoryId()).orElse(null);
		if(territory == null) {
			zoneId = ZoneId.systemDefault();
		} else {
			zoneId = ZoneId.of(territory.getTimezone());
		}
		return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneId);
	}

	public void updateChallengeStat(String playerId, String gameId, String type, 
			String challengeName, String counterName, long timestamp, boolean completed) {
		Campaign campaign = campaignRepository.findByGameId(gameId);
		
		ZonedDateTime trackDay = getDay(campaign, timestamp);
		String day = trackDay.format(dtf);
		int weekOfYear = trackDay.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
		int monthOfYear = trackDay.get(ChronoField.MONTH_OF_YEAR);
		int year = trackDay.get(ChronoField.YEAR);
		
		PlayerStatChallenge psc = playerStatChallengeRepository.findByPlayerIdAndCampaignIdAndTypeAndDay(playerId, 
				campaign.getCampaignId(), type, day);
		if(psc == null) {
			psc = new PlayerStatChallenge();
			psc.setCampaignId(campaign.getCampaignId());
			psc.setPlayerId(playerId);
			psc.setChallengeName(challengeName);
			psc.setType(type);
			psc.setCounterName(counterName);
			psc.setDay(day);
			psc.setWeekOfYear(year + "-" + weekOfYear);
			psc.setMonthOfYear(year + "-" + monthOfYear);
			playerStatChallengeRepository.save(psc);
		}
		
		if(completed) {
			psc.setCompleted(psc.getCompleted() + 1);
		} else {
			psc.setFailed(psc.getFailed() + 1);
		}
		playerStatChallengeRepository.save(psc);
	}
	
	public List<ChallengeStatsInfo> getPlayerChallengeStats(String playerId, String campaignId, String groupMode, String dateFrom, String dateTo) {
		List<ChallengeStatsInfo> result = new ArrayList<>();
		Criteria criteria = new Criteria("campaignId").is(campaignId).and("playerId").is(playerId)
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

		GroupOperation groupOperation = Aggregation.group("type", groupField).sum("completed").as("completed").sum("failed").as("failed");
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, groupField, "type");
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, sortOperation);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, PlayerStatChallenge.class, Document.class);
		for(Document doc : aggregationResults.getMappedResults()) {
			ChallengeStatsInfo stat = new ChallengeStatsInfo();			
			String type = ((Document)doc.get("_id")).getString("type");
			String period = ((Document)doc.get("_id")).getString(groupField);
			Integer completed = doc.getInteger("completed");
			Integer failed = doc.getInteger("failed");
			stat.setType(type);
			stat.setPeriod(period);
			stat.setCompleted(completed);
			stat.setFailed(failed);
			result.add(stat);
		}
		return result;
	}

}
