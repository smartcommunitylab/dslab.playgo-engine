package it.smartcommunitylab.playandgo.engine.manager;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.dto.ChallengeStatsInfo;
import it.smartcommunitylab.playandgo.engine.manager.PlayerCampaignPlacingManager.GroupMode;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatChallenge;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatChallengeRepository;
import it.smartcommunitylab.playandgo.engine.repository.TerritoryRepository;
import it.smartcommunitylab.playandgo.engine.util.Utils;

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
    DateTimeFormatter dftWeek = DateTimeFormatter.ofPattern("YYYY-ww", Locale.ITALY);
    DateTimeFormatter dftMonth = DateTimeFormatter.ofPattern("yyyy-MM");
	
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
		String weekOfYear = trackDay.format(dftWeek);
		String monthOfYear = trackDay.format(dftMonth);

		FindAndModifyOptions findAndModifyOptions = FindAndModifyOptions.options().upsert(true).returnNew(true);
		Query dayByModeQuery = new Query(new Criteria("playerId").is(playerId).and("campaignId").is(campaign.getCampaignId())
				.and("type").is(type).and("day").is(day)); 
		Update dayByModeUpdate = upsertNewPlacing(playerId, campaign.getCampaignId(), type, counterName,
				day, weekOfYear, monthOfYear, completed);
		mongoTemplate.findAndModify(dayByModeQuery, dayByModeUpdate, findAndModifyOptions, PlayerStatChallenge.class);	
	}

	private Update upsertNewPlacing(String playerId, String campaignId, String type, 
			String counterName, String day, String weekOfYear, String monthOfYear, boolean completed) {
		Update update = new Update();
		update.setOnInsert("playerId", playerId);
		update.setOnInsert("campaignId", campaignId);
		update.setOnInsert("type", type);
		if(Utils.isNotEmpty(counterName)) update.setOnInsert("counterName", counterName);
		if(Utils.isNotEmpty(day)) update.setOnInsert("day", day);
		if(Utils.isNotEmpty(monthOfYear)) update.setOnInsert("monthOfYear", monthOfYear);
		if(Utils.isNotEmpty(weekOfYear)) update.setOnInsert("weekOfYear", weekOfYear);
		if(completed) {
			update.inc("completed", 1);
		} else {
			update.inc("failed", 1);
		}
		return update;
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
