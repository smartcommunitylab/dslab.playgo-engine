package it.smartcommunitylab.playandgo.engine.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.Player;

@Repository
public interface PlayerRepository extends MongoRepository<Player, String> {
	
	public Player findByNicknameIgnoreCase(String nickname);
	
	@Query("{'nickname': {$regex: ?0, $options:'i'}}")
	public List<Player> findByNicknameRegex(String nickname);
}
