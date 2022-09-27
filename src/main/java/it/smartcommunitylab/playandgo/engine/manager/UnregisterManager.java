package it.smartcommunitylab.playandgo.engine.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerGameStatus;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatChallenge;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsGame;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsTransport;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@Component
public class UnregisterManager {
	private static transient final Logger logger = LoggerFactory.getLogger(UnregisterManager.class);
	
	@Autowired
	PlayerRepository playerRepository;
	
	@Autowired
	MongoTemplate mongoTemplate;

	public void unregisterPlayer(Player player) throws Exception {
		Player playerDb = playerRepository.findById(player.getPlayerId()).orElse(null);
		if(playerDb == null) {
			throw new BadRequestException("player not found", ErrorCode.PLAYER_EXISTS);
		}
		Query query = new Query(new Criteria("userId").is(player.getPlayerId()));
		mongoTemplate.remove(query, TrackedInstance.class);
		query = new Query(new Criteria("playerId").is(player.getPlayerId()));
		mongoTemplate.remove(query, CampaignSubscription.class);
		mongoTemplate.remove(query, CampaignPlayerTrack.class);
		mongoTemplate.remove(query, PlayerGameStatus.class);
		mongoTemplate.remove(query, PlayerStatChallenge.class);
		mongoTemplate.remove(query, PlayerStatsGame.class);
		mongoTemplate.remove(query, PlayerStatsTransport.class);
		playerRepository.delete(playerDb);
	}
	
}
