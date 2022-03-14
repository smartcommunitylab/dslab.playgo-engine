package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.Player;

@Repository
public interface PlayerRepository extends MongoRepository<Player, String> {
	public Player findByNicknameIgnoreCase(String nickname);
}
