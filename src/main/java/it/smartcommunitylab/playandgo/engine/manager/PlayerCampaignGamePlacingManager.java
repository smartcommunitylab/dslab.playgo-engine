package it.smartcommunitylab.playandgo.engine.manager;

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
import org.springframework.data.mongodb.core.aggregation.CountOperation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.manager.PlayerCampaignPlacingManager.GroupMode;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsGame;
import it.smartcommunitylab.playandgo.engine.report.CampaignGroupPlacing;
import it.smartcommunitylab.playandgo.engine.report.CampaignPlacing;
import it.smartcommunitylab.playandgo.engine.report.GameStats;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@Component
public class PlayerCampaignGamePlacingManager {
    private static Log logger = LogFactory.getLog(PlayerCampaignGamePlacingManager.class);

   	@Autowired
	MongoTemplate mongoTemplate;
	@Autowired
	AvatarManager avatarManager;
	@Autowired
	PlayerRepository playerRepository;

	public Page<CampaignPlacing> getCampaignPlacingByGame(String campaignId, String dateFrom, String dateTo, 
	        String groupId, boolean groupByGroupId, boolean filterByGroupId, Pageable pageRequest) {
	    List<CampaignPlacing> result = new ArrayList<>();
	    
		Criteria criteria = new Criteria("campaignId").is(campaignId);
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteria = criteria.and("global").is(Boolean.FALSE);
		}
        if(filterByGroupId && (groupId != null)) {
            criteria = criteria.and("groupId").is(groupId);
        }
		if(groupByGroupId) {
		    criteria = criteria.and("groupId").ne(null);
		} 
		MatchOperation matchOperation = Aggregation.match(criteria);
		
		String groupField = "nickname";
		if(groupByGroupId) {
		    groupField = "groupId";
		}
		GroupOperation groupOperation = Aggregation.group(groupField).sum("score").as("value");
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, "value").and(Direction.ASC, groupField);
		SkipOperation skipOperation = Aggregation.skip((long) (pageRequest.getPageNumber() * pageRequest.getPageSize()));
		LimitOperation limitOperation = Aggregation.limit(pageRequest.getPageSize());
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, sortOperation, 
				skipOperation, limitOperation);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, 
				PlayerStatsGame.class, Document.class);
        int index = pageRequest.getPageNumber() * pageRequest.getPageSize();
		for(Document doc : aggregationResults.getMappedResults()) {
		    CampaignPlacing cp = new CampaignPlacing();
		    if(groupByGroupId) {
		        cp.setGroupId(doc.getString("_id"));
		    } else {
		        cp.setNickname(doc.getString("_id"));
		        Player player = playerRepository.findByNickname(cp.getNickname());
	            if(player != null) {
	                cp.setPlayerId(player.getPlayerId());
	                cp.setAvatar(avatarManager.getPlayerSmallAvatar(player.getPlayerId()));
	            }		        
		    }
		    cp.setValue(doc.getDouble("value"));
		    cp.setPosition(index + 1);
		    result.add(cp);
		    index++;
		}		
		return new PageImpl<>(result, pageRequest, countGameDistincPlayers(criteria, groupField));
	}
	
    private long countGameDistincPlayers(Criteria criteria, String groupField) {
        MatchOperation matchOperation = Aggregation.match(criteria);
        GroupOperation groupOperation = Aggregation.group(groupField);
        CountOperation countOperation = Aggregation.count().as("totalNum");
        ProjectionOperation projectionOperation = Aggregation.project("totalNum");
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, countOperation, projectionOperation);
        AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, 
                PlayerStatsGame.class, Document.class);
        List<Document> list = aggregationResults.getMappedResults();
        if(list.size() == 1) {
            return list.get(0).getInteger("totalNum");
        }
        return 0;
    }
	
    public CampaignPlacing getCampaignPlacingByGameAndOwner(String ownerId, String campaignId,
            String dateFrom, String dateTo, String groupId, boolean groupByGroupId, boolean filterByGroupId) throws Exception {
        String groupField = "groupId";
        String groupFieldValue =  ownerId;
        Player player = null;
        if(!groupByGroupId) {
            player = playerRepository.findById(ownerId).orElse(null);
            if(player == null) {
                throw new BadRequestException("player not found", ErrorCode.PLAYER_NOT_FOUND);
            }
            groupField = "nickname";
            groupFieldValue = player.getNickname();
        }
        
        //get score
        Criteria criteria = new Criteria("campaignId").is(campaignId).and(groupField).is(groupFieldValue);
        if((dateFrom != null) && (dateTo != null)) {
            criteria = criteria.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
        } else {
            criteria = criteria.and("global").is(Boolean.FALSE);
        }
        if(filterByGroupId && (groupId != null)) {
            criteria = criteria.and("groupId").is(groupId);
        }
        MatchOperation matchOperation = Aggregation.match(criteria);
        GroupOperation groupOperation = Aggregation.group(groupField).sum("score").as("value");
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
        AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, 
                PlayerStatsGame.class, Document.class);
        
        CampaignPlacing placing = new CampaignPlacing();
        if(!groupByGroupId) {
            placing.setPlayerId(player.getPlayerId());
            placing.setNickname(player.getNickname());            
        } else {
            placing.setGroupId(groupFieldValue);
        }
        if(aggregationResults.getMappedResults().size() > 0) {
            Document doc = aggregationResults.getMappedResults().get(0);
            placing.setValue(doc.getDouble("value"));
        }
        
        //get position
        Criteria criteriaPosition = new Criteria("campaignId").is(campaignId);
        if((dateFrom != null) && (dateTo != null)) {
            criteriaPosition = criteriaPosition.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
        } else {
            criteriaPosition = criteriaPosition.and("global").is(Boolean.FALSE);
        }
        if(filterByGroupId && (groupId != null)) {
            criteriaPosition = criteriaPosition.and("groupId").is(groupId);
        }
		if(groupByGroupId) {
		    criteriaPosition = criteriaPosition.and("groupId").ne(null);
		} 
        MatchOperation matchModeAndTime = Aggregation.match(criteriaPosition);
        GroupOperation groupByPlayer = Aggregation.group(groupField).sum("score").as("value");
        MatchOperation filterByDistance = Aggregation.match(new Criteria("value").gt(placing.getValue()));
        Aggregation aggregationPosition = Aggregation.newAggregation(matchModeAndTime, groupByPlayer, filterByDistance);
        AggregationResults<Document> aggregationPositionResults = mongoTemplate.aggregate(aggregationPosition, 
                PlayerStatsGame.class, Document.class);
        placing.setPosition(aggregationPositionResults.getMappedResults().size() + 1);
        
        return placing;       
    }
    
    public List<GameStats> getPlayerGameStats(String playerId, String campaignId, String groupMode, 
            String dateFrom, String dateTo) {
        return getGameStatsByOwner(playerId, campaignId, groupMode, dateFrom, dateTo, false);
    }
    
    public List<GameStats> getGroupGameStats(String groupId, String campaignId, String groupMode, 
            String dateFrom, String dateTo) {
        return getGameStatsByOwner(groupId, campaignId, groupMode, dateFrom, dateTo, true);
    }
    
    private List<GameStats> getGameStatsByOwner(String ownerId, String campaignId, String groupMode, 
			String dateFrom, String dateTo, boolean group) {
		List<GameStats> result = new ArrayList<>();
		String ownerField = "playerId";
		if(group) {
		    ownerField = "groupId"; 
		}
		Criteria criteria = new Criteria("campaignId").is(campaignId).and(ownerField).is(ownerId)
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
	    Criteria criteria = new Criteria("campaignId").is(campaignId).and("playerId").is(playerId)
	            .and("global").is(Boolean.FALSE);
	    MatchOperation matchOperation = Aggregation.match(criteria);
	    GroupOperation groupOperation = Aggregation.group("playerId").sum("score").as("totalScore");
	    Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
	    AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, PlayerStatsGame.class, Document.class);
	    if(aggregationResults.getMappedResults().size() > 0) {
	        Document doc = aggregationResults.getMappedResults().get(0);
	        return doc.getDouble("totalScore");
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

	public CampaignGroupPlacing getCampaignGroupPlacingByGameAndPlayer(String groupId, String campaignId,
			String dateFrom, String dateTo) throws Exception {
		//get group score
		Criteria criteria = new Criteria("campaignId").is(campaignId).and("groupId").is(groupId);
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteria = criteria.and("global").is(Boolean.FALSE);
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
			criteriaPosition = criteriaPosition.and("global").is(Boolean.FALSE);
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

}
