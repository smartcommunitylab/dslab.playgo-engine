package it.smartcommunitylab.playandgo.engine.campaign.company;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsGame;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatsGameRepository;
import it.smartcommunitylab.playandgo.engine.repository.TerritoryRepository;

@Component
public class CompanyCampaignGameStatusManager {
	private static transient final Logger logger = LoggerFactory.getLogger(CompanyCampaignGameStatusManager.class);
	
	@Autowired
	TerritoryRepository territoryRepository;
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	PlayerRepository playerRepository;
	
	@Autowired
	PlayerStatsGameRepository statsGameRepository;
	
	protected DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	protected DateTimeFormatter dftWeek = DateTimeFormatter.ofPattern("YYYY-ww", Locale.ITALY);
	protected DateTimeFormatter dftMonth = DateTimeFormatter.ofPattern("yyyy-MM");

    public void updatePlayerGameStatus(CampaignPlayerTrack playerTrack) {
	    Campaign campaign = campaignRepository.findById(playerTrack.getCampaignId()).orElse(null);
	    Player p = playerRepository.findById(playerTrack.getPlayerId()).orElse(null);
	    
	    //update daily points
        try {
            ZonedDateTime trackDay = getTrackDay(campaign, playerTrack);
            String day = trackDay.format(dtf);
            PlayerStatsGame statsGame = statsGameRepository.findByPlayerIdAndCampaignIdAndDayAndGlobal(playerTrack.getPlayerId(), 
                    playerTrack.getCampaignId(), day, Boolean.FALSE);
            if(statsGame == null) {
                statsGame = new PlayerStatsGame();
                statsGame.setPlayerId(playerTrack.getPlayerId());
                statsGame.setNickname(p.getNickname());
                statsGame.setCampaignId(playerTrack.getCampaignId());
                statsGame.setGlobal(Boolean.FALSE);
                statsGame.setDay(day);
                statsGame.setWeekOfYear(trackDay.format(dftWeek));
                statsGame.setMonthOfYear(trackDay.format(dftMonth));
                statsGameRepository.save(statsGame);
                logger.debug("add statsGame " + statsGame.getId());
            }
            statsGame.setScore(statsGame.getScore() + playerTrack.getScore());
            statsGameRepository.save(statsGame);
            logger.debug("update statsGame " + statsGame.getId());
        } catch (Exception e) {
            logger.warn("updatePlayerState error:" + e.getMessage());
        }        

        //update generale
        PlayerStatsGame statsGlobal = statsGameRepository.findGlobalByPlayerIdAndCampaignId(
                playerTrack.getPlayerId(), playerTrack.getCampaignId());
        if(statsGlobal == null) {
            statsGlobal = new PlayerStatsGame();
            statsGlobal.setPlayerId(playerTrack.getPlayerId());
            statsGlobal.setNickname(p.getNickname());
            statsGlobal.setCampaignId(playerTrack.getCampaignId());
            statsGlobal.setGlobal(Boolean.TRUE);
            statsGameRepository.save(statsGlobal);
        }
        statsGlobal.setScore(statsGlobal.getScore() + playerTrack.getScore());
        statsGameRepository.save(statsGlobal);
	}
	
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
	
	public void removePlayerGameStatus(CampaignPlayerTrack playerTrack) {
        //update generale
        PlayerStatsGame statsGlobal = statsGameRepository.findGlobalByPlayerIdAndCampaignId(
                playerTrack.getPlayerId(), playerTrack.getCampaignId());
        if(statsGlobal != null) {
            statsGlobal.setScore(statsGlobal.getScore() - playerTrack.getScore());
            statsGameRepository.save(statsGlobal);
        }
        
        //update daily points
        Campaign campaign = campaignRepository.findById(playerTrack.getCampaignId()).orElse(null);
        try {
            ZonedDateTime trackDay = getTrackDay(campaign, playerTrack);
            String day = trackDay.format(dtf);
            PlayerStatsGame statsGame = statsGameRepository.findByPlayerIdAndCampaignIdAndDayAndGlobal(playerTrack.getPlayerId(), 
                    playerTrack.getCampaignId(), day, Boolean.FALSE);
            if(statsGame != null) {
                statsGame.setScore(statsGame.getScore() - playerTrack.getScore());
                statsGameRepository.save(statsGame);
            }
        } catch (Exception e) {
            logger.warn("removePlayerGameStatus error:" + e.getMessage());
        }         
	}
	
	public void updatePlayerGameStatus(CampaignPlayerTrack playerTrack, double deltaScore) {
        Campaign campaign = campaignRepository.findById(playerTrack.getCampaignId()).orElse(null);
        
        //update daily points
        try {
            ZonedDateTime trackDay = getTrackDay(campaign, playerTrack);
            String day = trackDay.format(dtf);
            PlayerStatsGame statsGame = statsGameRepository.findByPlayerIdAndCampaignIdAndDayAndGlobal(playerTrack.getPlayerId(), 
                    playerTrack.getCampaignId(), day, Boolean.FALSE);
            if(statsGame != null) {
                if(deltaScore > 0) {
                    statsGame.setScore(statsGame.getScore() + deltaScore);
                } else if (deltaScore < 0) {
                    statsGame.setScore(statsGame.getScore() - Math.abs(deltaScore));
                }
                statsGameRepository.save(statsGame);
            }
        } catch (Exception e) {
            logger.warn("updatePlayerState error:" + e.getMessage());
        }        

        //update generale
        PlayerStatsGame statsGlobal = statsGameRepository.findGlobalByPlayerIdAndCampaignId(
                playerTrack.getPlayerId(), playerTrack.getCampaignId());
        if(statsGlobal != null) {
            if(deltaScore > 0) {
                statsGlobal.setScore(statsGlobal.getScore() + deltaScore);
            } else if (deltaScore < 0) {
                statsGlobal.setScore(statsGlobal.getScore() - Math.abs(deltaScore));
            }
            statsGameRepository.save(statsGlobal);      
        }
	}
	
}
