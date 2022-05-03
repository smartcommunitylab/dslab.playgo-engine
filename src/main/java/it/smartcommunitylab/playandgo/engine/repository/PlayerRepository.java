package it.smartcommunitylab.playandgo.engine.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.Player;

@Repository
public interface PlayerRepository extends MongoRepository<Player, String> {
	
	public Player findByNicknameIgnoreCase(String nickname);
	
	public Player findByNickname(String nickname);
	
	@Query("{'nickname': {$regex: ?0, $options:'i'}}")
	public List<Player> findByNicknameRegex(String nickname);
	
	public Page<Player> findByTerritoryId(String territoryId, Pageable pageRequest);
	
	@Query("{'territoryId': ?0, '$or': [{'nickname': {$regex: ?1, $options:'i'}}, {'playerId': {$regex: ?1, $options:'i'}}, {'mail': {$regex: ?1, $options:'i'}}, {'givenName': {$regex: ?1, $options:'i'}}, {'familyName': {$regex: ?1, $options:'i'}}]}")
	public Page<Player> findByTerritoryIdAndText(String territoryId, String text, Pageable pageRequest);
	
}
