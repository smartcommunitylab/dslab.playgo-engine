package it.smartcommunitylab.playandgo.engine.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.PlayerRole;

@Repository
public interface PlayerRoleRepository extends MongoRepository<PlayerRole, String> {
	public List<PlayerRole> findByPlayerId(String playerId);

}
