package it.smartcommunitylab.playandgo.engine.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.PlayerRole;
import it.smartcommunitylab.playandgo.engine.model.PlayerRole.Role;

@Repository
public interface PlayerRoleRepository extends MongoRepository<PlayerRole, String> {
	public List<PlayerRole> findByPlayerId(String playerId);
	public PlayerRole findByPlayerIdAndRole(String playerId, Role role);
	public PlayerRole findByPlayerIdAndRoleAndEntityId(String playerId, Role role, String entityId);

}
