package it.smartcommunitylab.playandgo.engine.ge;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack.ScoreStatus;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsGame;
import it.smartcommunitylab.playandgo.engine.repository.CampaignPlayerTrackRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatsGameRepository;

@Component
public class PersonalCampaignGameStatusManager {
	@Autowired
	CampaignPlayerTrackRepository campaignPlayerTrackRepository;
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	PlayerStatsGameRepository statsGameRepository;

	public void updatePlayerGameStatus(Map<String, Object> msg) {
		@SuppressWarnings("unchecked")
		Map<String, Object> obj = (Map<String, Object>) msg.get("obj");
		if(obj != null) {
			String gameId = (String) obj.get("gameId");
			String playerId = (String) obj.get("playerId");
			double delta = (double) obj.get("delta");
			double score = (double) obj.get("score");
			String trackId = null;
			@SuppressWarnings("unchecked")
			Map<String, Object> dataPayLoad = (Map<String, Object>) obj.get("dataPayLoad");
			if(dataPayLoad != null) {
				trackId = (String) dataPayLoad.get("trackId");
			}
			Campaign campaign = campaignRepository.findByGameId(gameId);
			if(campaign != null) {
				String campaignId = campaign.getCampaignId();
				
				CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findByPlayerIdAndCampaignIdAndTrackedInstanceId(playerId, campaignId, trackId);
				if(playerTrack != null) {
					playerTrack.setScoreStatus(ScoreStatus.COMPUTED);
					playerTrack.setScore(delta);
					campaignPlayerTrackRepository.save(playerTrack);
				}
				
				PlayerStatsGame statsGame = statsGameRepository.findByPlayerIdAndCampaignId(playerId, campaignId);
				if(statsGame == null) {
					statsGame = new PlayerStatsGame();
					statsGame.setPlayerId(playerId);
					statsGame.setCampaignId(campaignId);
					statsGameRepository.save(statsGame);
				}
				statsGame.setUpdateTime(new Date());
				statsGame.setScore(score);
				statsGameRepository.save(statsGame);
			}
		}
	}
}
