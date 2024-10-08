package it.smartcommunitylab.playandgo.engine.manager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
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
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.dto.CampaignPeriodStatsInfo;
import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.manager.azienda.PgAziendaleManager;
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
	
	public static enum VirtualTrackOp {
	   add, sub, nothing 
	}
	
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
	
	@Autowired
	PgAziendaleManager pgAziendaleManager;
	
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");   
    DateTimeFormatter dftWeek = DateTimeFormatter.ofPattern("YYYY-ww", Locale.ITALY);
    DateTimeFormatter dftMonth = DateTimeFormatter.ofPattern("yyyy-MM");
	
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
	
	private String getDay(Campaign campaign, Date date) {
        ZoneId zoneId = null;
        Territory territory = territoryRepository.findById(campaign.getTerritoryId()).orElse(null);
        if(territory == null) {
            zoneId = ZoneId.systemDefault();
        } else {
            zoneId = ZoneId.of(territory.getTimezone());
        }
        ZonedDateTime zdt = ZonedDateTime.ofInstant(date.toInstant(), zoneId);
        return zdt.format(dtf);
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
			FindAndModifyOptions findAndModifyOptions = FindAndModifyOptions.options().upsert(true).returnNew(true);
			//transport daily placing
			ZonedDateTime trackDay = getTrackDay(campaign, pt);
			String day = trackDay.format(dtf);
			String weekOfYear = trackDay.format(dftWeek);
			String monthOfYear = trackDay.format(dftMonth);
			Query dayByModeQuery = new Query(new Criteria("playerId").is(pt.getPlayerId()).and("campaignId").is(pt.getCampaignId())
				.and("modeType").is(pt.getModeType()).and("global").is(Boolean.FALSE).and("day").is(day)); 
			Update dayByModeUpdate = upsertNewPlacing(pt.getPlayerId(), player.getNickname(), pt.getCampaignId(), pt.getModeType(), 
				pt.getGroupId(), Boolean.FALSE, day, weekOfYear, monthOfYear, pt);
			mongoTemplate.findAndModify(dayByModeQuery, dayByModeUpdate, findAndModifyOptions, PlayerStatsTransport.class);
			
			logger.info(String.format("updatePlayerCampaignPlacings: update player[%s] stats[%s] for campaign[%s]", 
				pt.getPlayerId(), pt.getModeType(), pt.getCampaignId()));
		}
	}
	
	public void removePlayerCampaignPlacings(CampaignPlayerTrack pt) {
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
            dayByMode.subVirtualScore(pt.getVirtualScore());
            if(pt.isVirtualTrack()) {
                dayByMode.subVirtualTrack();
            }			
			playerStatsTransportRepository.save(dayByMode);					
		}
	}
	
	public void updatePlayerCampaignPlacings(CampaignPlayerTrack pt, double deltaDistance, double deltaCo2, 
	        double deltaVirtualScore, VirtualTrackOp virtualTrackOp) {
		//transport daily placing
		Player player = playerRepository.findById(pt.getPlayerId()).orElse(null);
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
            if(deltaVirtualScore > 0) {
                dayByMode.addVirtualScore(deltaVirtualScore); 
            } else if(deltaVirtualScore < 0) {
                dayByMode.subVirtualScore(Math.abs(deltaVirtualScore));
            }
            if(virtualTrackOp != null) {
                if(virtualTrackOp.equals(VirtualTrackOp.add)) {
                    dayByMode.addVirtualTrack(); 
                } else if(virtualTrackOp.equals(VirtualTrackOp.sub)) {
                    dayByMode.subVirtualTrack();
                }
            }            
			playerStatsTransportRepository.save(dayByMode);			
		} else {
		    dayByMode = new PlayerStatsTransport();
		    dayByMode.setGlobal(Boolean.FALSE);
		    dayByMode.setModeType(pt.getModeType());
		    dayByMode.setCampaignId(pt.getCampaignId());
		    dayByMode.setPlayerId(pt.getPlayerId());
            if(player != null) {
                dayByMode.setNickname(player.getNickname());
                if(player.getGroup()) dayByMode.setGroupId(player.getPlayerId());
            }
            dayByMode.addDistance(deltaDistance);
            dayByMode.addDuration(pt.getDuration());
            dayByMode.addCo2(deltaCo2);
            dayByMode.addTrack();
            dayByMode.addVirtualScore(deltaVirtualScore);
            if(pt.isVirtualTrack()) dayByMode.addVirtualTrack();
            if(Utils.isNotEmpty(pt.getGroupId())) dayByMode.setGroupId(pt.getGroupId());
            String weekOfYear = trackDay.format(dftWeek);
            String monthOfYear = trackDay.format(dftMonth);
            dayByMode.setDay(day);
            dayByMode.setMonthOfYear(monthOfYear);
            dayByMode.setWeekOfYear(weekOfYear);
            playerStatsTransportRepository.save(dayByMode);
		}
	}
	
	private LocalDate getWeeklyDay(int startDayOfWeek, LocalDate trackDay) {
		LocalDate dayOfWeek = trackDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(startDayOfWeek)));
		return dayOfWeek;
	}
	
	private PlayerStatsTransport addNewPlacing(String playerId, String nickname, String campaignId, String modeType, 
			String groupId, Boolean global, String day, String weekOfYear, String monthOfYear) {
		PlayerStatsTransport pst = new PlayerStatsTransport();
		pst.setPlayerId(playerId);
		pst.setNickname(nickname);
		pst.setCampaignId(campaignId);
		pst.setModeType(modeType);
		pst.setGlobal(global);
		if(!global) {
			pst.setDay(day);
			pst.setWeekOfYear(weekOfYear);
			pst.setMonthOfYear(monthOfYear);			
		}
		if(Utils.isNotEmpty(groupId)) {
		    pst.setGroupId(groupId);
		}
		playerStatsTransportRepository.save(pst);
		return pst;
	}

	private Update upsertNewPlacing(String playerId, String nickname, String campaignId, String modeType, 
			String groupId, Boolean global, String day, String weekOfYear, String monthOfYear, CampaignPlayerTrack pt) {
		Update update = new Update();
		update.setOnInsert("playerId", playerId);
		update.setOnInsert("nickname", nickname);
		update.setOnInsert("campaignId", campaignId);
		update.setOnInsert("modeType", modeType);
		if(Utils.isNotEmpty(groupId)) update.setOnInsert("groupId", groupId);
		update.setOnInsert("global", global);
		if(Utils.isNotEmpty(day)) update.setOnInsert("day", day);
		if(Utils.isNotEmpty(monthOfYear)) update.setOnInsert("monthOfYear", monthOfYear);
		if(Utils.isNotEmpty(weekOfYear)) update.setOnInsert("weekOfYear", weekOfYear);
		update.inc("distance", pt.getDistance());
		update.inc("duration", pt.getDuration());
		update.inc("co2", pt.getCo2());
		update.inc("trackNumber", 1L);
		update.inc("virtualScore", pt.getVirtualScore());
		if(pt.isVirtualTrack()) {
			update.inc("virtualTrack", 1L);
		} else {
			update.inc("virtualTrack", 0L);
		}
		return update;
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
            MatchOperation matchOperationStats = Aggregation.match(new Criteria("playerId").is(player.getPlayerId())
                    .and("campaignId").is(campaign.getCampaignId()).and("global").is(Boolean.FALSE));         			
	        GroupOperation groupOperationStats = Aggregation.group("modeType").sum("distance").as("distance")
	                .sum("duration").as("duration").sum("co2").as("co2").sum("trackNumber").as("trackNumber");
	        Aggregation aggregationStats = Aggregation.newAggregation(matchOperationStats, groupOperationStats);
	        AggregationResults<Document> aggregationResultsStats = mongoTemplate.aggregate(aggregationStats, PlayerStatsTransport.class, Document.class);
            List<TransportStats> transportStatsList = new ArrayList<>();
            double co2 = 0.0;
            long tracks = 0;
	        for(Document doc : aggregationResultsStats.getMappedResults()) {
	            try {
	                TransportStats ts = new TransportStats();
	                ts.setModeType(doc.getString("_id"));
	                ts.setTotalDistance(doc.getDouble("distance"));
	                ts.setTotalDuration(doc.getLong("duration"));
	                ts.setTotalCo2(doc.getDouble("co2"));
	                ts.setTotalTravel(doc.getLong("trackNumber"));
	                transportStatsList.add(ts);
	                co2 += doc.getDouble("co2");
	                tracks += doc.getLong("trackNumber");
	            } catch (Exception e) {
	                logger.error(String.format("getPlayerStatus error[%s - %s]:%s - %s", player.getPlayerId(), campaign.getCampaignId(), 
	                        doc.toString(), e.getMessage()));
	            }
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
	
	private long countTransportDistincPlayers(Criteria criteria, String groupField) {
		MatchOperation matchOperation = Aggregation.match(criteria);
		GroupOperation groupOperation = Aggregation.group(groupField);
        CountOperation countOperation = Aggregation.count().as("totalNum");
        ProjectionOperation projectionOperation = Aggregation.project("totalNum");		
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, countOperation, projectionOperation);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, 
				PlayerStatsTransport.class, Document.class);
        List<Document> list = aggregationResults.getMappedResults();
        if(list.size() == 1) {
            return list.get(0).getInteger("totalNum");
        }
        return 0;
	}

	public Page<CampaignPlacing> getCampaignPlacing(String campaignId, String metric, String mean,  
			String dateFrom, String dateTo, Pageable pageRequest, String filterByGroupId, boolean groupByGroupId) {
	    List<CampaignPlacing> result = new ArrayList<>();
		Criteria criteria = new Criteria("campaignId").is(campaignId);
		if(Utils.isNotEmpty(mean)) {
			criteria = criteria.and("modeType").is(mean);
		}
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteria = criteria.and("global").is(Boolean.FALSE);
		}
		if(Utils.isNotEmpty(filterByGroupId)) {
		    criteria = criteria.and("groupId").is(filterByGroupId);
		}
		MatchOperation matchOperation = Aggregation.match(criteria);
		
        String groupField = "nickname";
        if(groupByGroupId) {
            groupField = "groupId";
        }
		String sumField = null;
		if(metric.equalsIgnoreCase("co2")) {
			sumField = "co2";
		} else if(metric.equalsIgnoreCase("tracks")) { 
			sumField = "trackNumber";
		} else if(metric.equalsIgnoreCase("time")) {
            sumField = "duration";
        } else if(metric.equalsIgnoreCase("virtualScore")) {
            sumField = "virtualScore";
        } else if(metric.equalsIgnoreCase("virtualTrack")) {
            sumField = "virtualTrack";
        } else {
			sumField = "distance";
		}
		GroupOperation groupOperation = Aggregation.group(groupField).sum(sumField).as("value");
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, "value").and(Direction.ASC, groupField);
		SkipOperation skipOperation = Aggregation.skip((long) (pageRequest.getPageNumber() * pageRequest.getPageSize()));
		LimitOperation limitOperation = Aggregation.limit(pageRequest.getPageSize());
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, sortOperation, 
				skipOperation, limitOperation);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, 
				PlayerStatsTransport.class, Document.class);
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
            if(metric.equalsIgnoreCase("tracks") || metric.equalsIgnoreCase("time") || metric.equalsIgnoreCase("virtualTrack")) {
				cp.setValue(getDoubleValue(doc, "value"));
            } else {
                cp.setValue(doc.getDouble("value"));
            }            
            cp.setPosition(index + 1);
            result.add(cp);
            index++;		    
		}
		return new PageImpl<>(result, pageRequest, countTransportDistincPlayers(criteria, groupField));
	}
	
    public CampaignPlacing getCampaignPlacingByPlayer(String playerId, String campaignId, 
            String metric, String mean, String dateFrom, String dateTo, String filterByGroupId) throws Exception {
        return getCampaignPlacingByOwner(playerId, campaignId, metric, mean, dateFrom, dateTo, filterByGroupId, false);
    }
    
    public CampaignPlacing getCampaignPlacingByGroup(String groupId, String campaignId, 
            String metric, String mean, String dateFrom, String dateTo) throws Exception {
        return getCampaignPlacingByOwner(groupId, campaignId, metric, mean, dateFrom, dateTo, null, true);
    }
    
	public CampaignPlacing getCampaignPlacingByOwner(String ownerId, String campaignId, 
			String metric, String mean, String dateFrom, String dateTo, String filterByGroupId, boolean group) throws Exception {
        String groupField = "groupId";
        String groupFieldValue =  ownerId;
        Player player = null;
        if(!group) {
            player = playerRepository.findById(ownerId).orElse(null);
            if(player == null) {
                throw new BadRequestException("player not found", ErrorCode.PLAYER_NOT_FOUND);
            }
            groupField = "nickname";
            groupFieldValue = player.getNickname();
        }
	    
		//get score
		Criteria criteria = new Criteria("campaignId").is(campaignId).and(groupField).is(groupFieldValue);
		if(Utils.isNotEmpty(mean)) {
			criteria = criteria.and("modeType").is(mean);
		}
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteria = criteria.and("global").is(Boolean.FALSE);
		}
        if(Utils.isNotEmpty(filterByGroupId)) {
            criteria = criteria.and("groupId").is(filterByGroupId);
        }
        
		MatchOperation matchOperation = Aggregation.match(criteria);
		String sumField = null;
		if(metric.equalsIgnoreCase("co2")) {
			sumField = "co2";
		} else if(metric.equalsIgnoreCase("tracks")) { 
			sumField = "trackNumber";
		} else if(metric.equalsIgnoreCase("time")) {
            sumField = "duration";
        } else if(metric.equalsIgnoreCase("virtualScore")) {
            sumField = "virtualScore";
        } else if(metric.equalsIgnoreCase("virtualTrack")) {
            sumField = "virtualTrack";
        } else {
			sumField = "distance";
		}
		
		GroupOperation groupOperation = Aggregation.group(groupField).sum(sumField).as("value");
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, 
				PlayerStatsTransport.class, Document.class);
		
        CampaignPlacing placing = new CampaignPlacing();
        if(!group) {
            placing.setPlayerId(player.getPlayerId());
            placing.setNickname(player.getNickname());            
        }
        if(aggregationResults.getMappedResults().size() > 0) {
            Document doc = aggregationResults.getMappedResults().get(0);
            placing.setValue(getDoubleValue(doc, "value"));
        }
		
		//get position
		Criteria criteriaPosition = new Criteria("campaignId").is(campaignId);
		if(Utils.isNotEmpty(mean)) {
			criteriaPosition = criteriaPosition.and("modeType").is(mean);
		}
		if((dateFrom != null) && (dateTo != null)) {
			criteriaPosition = criteriaPosition.and("global").is(Boolean.FALSE)
					.andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteriaPosition = criteriaPosition.and("global").is(Boolean.FALSE);
		}
        if(Utils.isNotEmpty(filterByGroupId)) {
            criteriaPosition = criteriaPosition.and("groupId").is(filterByGroupId);
        }
        
		MatchOperation matchModeAndTime = Aggregation.match(criteriaPosition);
		GroupOperation groupByPlayer = Aggregation.group(groupField).sum(sumField).as("value");
		MatchOperation filterByDistance = Aggregation.match(new Criteria("value").gt(placing.getValue()));
		Aggregation aggregationPosition = Aggregation.newAggregation(matchModeAndTime, groupByPlayer, filterByDistance);
		AggregationResults<Document> aggregationPositionResults = mongoTemplate.aggregate(aggregationPosition, 
				PlayerStatsTransport.class, Document.class);
		placing.setPosition(aggregationPositionResults.getMappedResults().size() + 1);
		
		return placing;
	}
	
//	private List<TransportStat> getPlayerTransportStatsCompanyCampaign(String playerId, String campaignId, String groupMode, String metric,
//			String mean, String dateFrom, String dateTo) {
//		List<TransportStat> result = new ArrayList<>();
//		try {
//			result = pgAziendaleManager.getPlayerTransportStats(playerId, campaignId, groupMode, metric, mean, dateFrom, dateTo);
//			if(metric.equalsIgnoreCase("co2")) {
//				result.forEach(ts -> {
//					ts.setValue(Utils.getSavedCo2(mean, ts.getValue()));
//				});
//			}
//		} catch (Exception e) {
//			logger.error(String.format("getPlayerTransportStatsCompanyCampaign[%s][%s]:%s", playerId, campaignId, e.getMessage()));
//		}
//		return result;
//	}

   public List<TransportStat> getPlayerTransportStats(String playerId, String campaignId, String groupMode, String metric,
           String mean, String dateFrom, String dateTo) {
     return getOwnerTransportStats(playerId, campaignId, groupMode, metric, mean, dateFrom, dateTo, false);  
   }
   
   public List<TransportStat> getGroupTransportStats(String groupId, String campaignId, String groupMode, String metric, 
           String mean, String dateFrom, String dateTo) {
      return getOwnerTransportStats(groupId, campaignId, groupMode, metric, mean, dateFrom, dateTo, true);
   }

   private double getDoubleValue(Document doc, String field) {
		if(doc.get(field) instanceof Long) {
			return doc.getLong(field).doubleValue();
		}
		if(doc.get(field) instanceof Integer) {
			return doc.getInteger(field).doubleValue();
		}
		if(doc.get(field) instanceof Double) {
		   return doc.getDouble(field); 
		}
		return 0.0;
   }
   
   private List<TransportStat> getOwnerTransportStats(String ownerId, String campaignId, String groupMode, String metric,
			String mean, String dateFrom, String dateTo, boolean group) {
		List<TransportStat> result = new ArrayList<>();
		
//		Campaign campaign = campaignManager.getCampaign(campaignId);
//		if((campaign != null) && (Type.company.equals(campaign.getType()))) {
//			return getPlayerTransportStatsCompanyCampaign(ownerId, campaignId, Utils.isEmpty(groupMode) ? "total" : groupMode, 
//			        metric, mean, dateFrom, dateTo);
//		}
		
		String selectId = "playerId";
		if(group) {
		    selectId = "groupId";  
		}
		Criteria criteria = new Criteria("campaignId").is(campaignId).and(selectId).is(ownerId);
        if((dateFrom != null) && (dateTo != null)) {
            criteria = criteria.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
        } else {
            criteria = criteria.and("global").is(Boolean.FALSE);
        }		
		if(Utils.isNotEmpty(mean)) {
			criteria = criteria.and("modeType").is(mean);
		}
		MatchOperation matchOperation = Aggregation.match(criteria);
		
		String groupField = null;
		if(Utils.isEmpty(groupMode)) {
		    groupField = selectId;
		} else {
	        if(GroupMode.day.toString().equals(groupMode)) {
	            groupField = "day";
	        } else if(GroupMode.week.toString().equals(groupMode)) {
	            groupField = "weekOfYear";
	        } else {
	            groupField = "monthOfYear";
	        }		    
		}
		String sumField = null;
		if(metric.equalsIgnoreCase("co2")) {
			sumField = "co2";
		} else if(metric.equalsIgnoreCase("tracks")) { 
			sumField = "trackNumber";
		} else if(metric.equalsIgnoreCase("time")) {
		    sumField = "duration";
		} else if(metric.equalsIgnoreCase("virtualScore")) {
            sumField = "virtualScore";
        } else if(metric.equalsIgnoreCase("virtualTrack")) {
            sumField = "virtualTrack";
        } else {
			sumField = "distance";
		}		
		GroupOperation groupOperation = Aggregation.group(groupField).sum(sumField).as("value");
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, groupField);
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, sortOperation);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, PlayerStatsTransport.class, Document.class);
		for(Document doc : aggregationResults.getMappedResults()) {
		    try {
	            TransportStat stat = new TransportStat();
	            if(Utils.isNotEmpty(groupMode)) {
	                stat.setPeriod(doc.getString("_id"));
	            }
	            if(metric.equalsIgnoreCase("tracks") || metric.equalsIgnoreCase("time") || metric.equalsIgnoreCase("virtualTrack")) {
	                stat.setValue(getDoubleValue(doc, "value"));
	            } else {
	                stat.setValue(doc.getDouble("value"));
	            }
	            result.add(stat);                
            } catch (Exception e) {
                logger.error(String.format("getOwnerTransportStats error[%s - %s]:%s - %s", ownerId, campaignId, 
                        doc.toString(), e.getMessage()));
            }
		}
		return result;
	}
	
   public List<TransportStat> getPlayerTransportStatsGroupByMean(String playerId, String campaignId, String metric,
           String dateFrom, String dateTo) {
       return getOwnerTransportStatsGroupByMean(playerId, campaignId, metric, dateFrom, dateTo, false);
   }
   
   public List<TransportStat> getGroupTransportStatsGroupByMean(String groupId, String campaignId, String metric,
           String dateFrom, String dateTo) {
       return getOwnerTransportStatsGroupByMean(groupId, campaignId, metric, dateFrom, dateTo, true);
   }
   
   private List<TransportStat> getOwnerTransportStatsGroupByMean(String ownerId, String campaignId, String metric,
			String dateFrom, String dateTo, boolean group) {
		List<TransportStat> result = new ArrayList<>();

        String selectId = "playerId";
        if(group) {
            selectId = "groupId";  
        }
		Criteria criteria = new Criteria("campaignId").is(campaignId).and(selectId).is(ownerId);
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteria = criteria.and("global").is(Boolean.FALSE);
		}
		MatchOperation matchOperation = Aggregation.match(criteria);
		
		String sumField = null;
		if(metric.equalsIgnoreCase("co2")) {
			sumField = "co2";
		} else if(metric.equalsIgnoreCase("tracks")) { 
			sumField = "trackNumber";
		} else if(metric.equalsIgnoreCase("time")) {
            sumField = "duration";
        }  else if(metric.equalsIgnoreCase("virtualScore")) {
            sumField = "virtualScore";
        } else if(metric.equalsIgnoreCase("virtualTrack")) {
            sumField = "virtualTrack";
        } else {
			sumField = "distance";
		}		
		GroupOperation groupOperation = Aggregation.group("modeType").sum(sumField).as("value");
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, "modeType");
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, sortOperation);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, PlayerStatsTransport.class, Document.class);
		for(Document doc : aggregationResults.getMappedResults()) {
			TransportStat stat = new TransportStat();
			stat.setMean(doc.getString("_id"));
			if(metric.equalsIgnoreCase("tracks") || metric.equalsIgnoreCase("time") || metric.equalsIgnoreCase("virtualTrack")) {
				stat.setValue(getDoubleValue(doc, "value"));
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
		} else if(metric.equalsIgnoreCase("time")) {
            sumField = "duration";
        }  else if(metric.equalsIgnoreCase("virtualScore")) {
            sumField = "virtualScore";
        } else if(metric.equalsIgnoreCase("virtualTrack")) {
            sumField = "virtualTrack";
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
			if(metric.equalsIgnoreCase("tracks") || metric.equalsIgnoreCase("time") || metric.equalsIgnoreCase("virtualTrack")) {
				stat.setValue(getDoubleValue(doc, "value"));
			} else {
				stat.setValue(doc.getDouble("value"));
			}
			result.add(stat);
		}
		return result;
	}
	
	public Page<CampaignPlacing> getCampaignPlacingByGame(String campaignId, String dateFrom, String dateTo, 
	        Pageable pageRequest, boolean groupByGroupId) {
	    List<CampaignPlacing> result = new ArrayList<>();
	    
		Criteria criteria = new Criteria("campaignId").is(campaignId);
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
		} else {
			criteria = criteria.and("global").is(Boolean.FALSE);
		}
		if(groupByGroupId) {
		    criteria = criteria.and("groupId").ne(null);
		} else {
		    criteria = criteria.and("groupId").isNull();
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
	
	public CampaignPlacing getCampaignPlacingByGameAndPlayer(String playerId, String campaignId,
			String dateFrom, String dateTo) throws Exception {		
		return getCampaignPlacingByGameAndOwner(playerId, campaignId, dateFrom, dateTo, false);
	}
	
    public CampaignPlacing getCampaignPlacingByGameAndGroup(String groupId, String campaignId,
            String dateFrom, String dateTo) throws Exception {        
        return getCampaignPlacingByGameAndOwner(groupId, campaignId, dateFrom, dateTo, true);
    }
    
    private CampaignPlacing getCampaignPlacingByGameAndOwner(String ownerId, String campaignId,
            String dateFrom, String dateTo, boolean group) throws Exception {
        String groupField = "groupId";
        String groupFieldValue =  ownerId;
        Player player = null;
        if(!group) {
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
        if(!group) {
            criteria = criteria.and("groupId").isNull();
        }
        MatchOperation matchOperation = Aggregation.match(criteria);
        GroupOperation groupOperation = Aggregation.group(groupField).sum("score").as("value");
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
        AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, 
                PlayerStatsGame.class, Document.class);
        
        CampaignPlacing placing = new CampaignPlacing();
        if(!group) {
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
        if(group) {
            criteriaPosition = criteriaPosition.and("groupId").ne(null);
        } else {
            criteriaPosition = criteriaPosition.and("groupId").isNull();
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
	
//	public List<CampaignGroupPlacing> getCampaignGroupPlacingByGame(String campaignId,  
//			String dateFrom, String dateTo) {
//		Criteria criteria = new Criteria("campaignId").is(campaignId);
//		if((dateFrom != null) && (dateTo != null)) {
//			criteria = criteria.and("global").is(Boolean.FALSE).andOperator(Criteria.where("day").gte(dateFrom), Criteria.where("day").lte(dateTo));
//		} else {
//			criteria = criteria.and("global").is(Boolean.TRUE);
//		}
//		MatchOperation matchOperation = Aggregation.match(criteria);
//		GroupOperation groupOperation = Aggregation.group("groupId").sum("score").as("value");
//		SortOperation sortOperation = Aggregation.sort(Direction.DESC, "value").and(Direction.ASC, "groupId");
//		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, sortOperation);
//		AggregationResults<CampaignGroupPlacing> aggregationResults = mongoTemplate.aggregate(aggregation, 
//				PlayerStatsGame.class, CampaignGroupPlacing.class);
//		List<CampaignGroupPlacing> list = aggregationResults.getMappedResults();
//		int index = 0;
//		for(CampaignGroupPlacing cp : list) {
//			cp.setPosition(index + 1);
//			index++;
//		}
//		return list;
//	}

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
	
	@SuppressWarnings("unchecked")
    public List<CampaignPeriodStatsInfo> getCampaignPeriodStatsInfo(String campaignId, String playerId) {
	    List<CampaignPeriodStatsInfo> result = new ArrayList<>();
	    Campaign campaign = campaignManager.getCampaign(campaignId);
	    if((campaign != null) && (campaign.getSpecificData().get("periods") != null)) {
	        try {
	            List<Map<String, Object>> periods = (List<Map<String, Object>>) campaign.getSpecificData().get("periods");
	            periods.forEach(p -> {
	                Long start = 0L;
	                Long end = 0L;
	                if(p.get("start") instanceof Long) {
	                    start = (Long)p.get("start");
	                } else if(p.get("start") instanceof String) {
	                    start = Long.valueOf((String)p.get("start"));
	                }
                    if(p.get("end") instanceof Long) {
                        end = (Long)p.get("end");
                    } else if(p.get("end") instanceof String) {
                        end = Long.valueOf((String)p.get("end"));
                    }
	                CampaignPeriodStatsInfo info = new CampaignPeriodStatsInfo();
	                info.setDateFrom(start);
	                info.setDateTo(end);
	                info.setDateFromS(getDay(campaign, new Date(start)));
	                info.setDateToS(getDay(campaign, new Date(end)));
	                result.add(info);
	            });
	            String dateFrom = getDay(campaign, campaign.getDateFrom());
	            String dateTo = getDay(campaign, campaign.getDateTo());
	            List<TransportStat> stats = getOwnerTransportStats(playerId, campaignId, "day", "virtualScore", null, dateFrom, dateTo, false);
                stats.forEach(s -> {
                    for(CampaignPeriodStatsInfo info : result) {
                        if((s.getPeriod().compareTo(info.getDateFromS()) >= 0) &&
                                (s.getPeriod().compareTo(info.getDateToS()) <= 0)) {
                            info.addValue(s.getValue());
                        }
                    }
                });
            } catch (Exception e) {
                logger.error(String.format("getCampaignPeriodStatsInfo[%s,%s]:%s", campaignId, playerId, e.getMessage()));
            }  
	    }
	    return result;
	}

}

