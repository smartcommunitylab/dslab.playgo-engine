package it.smartcommunitylab.playandgo.engine.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.PlayerRole;
import it.smartcommunitylab.playandgo.engine.model.PlayerRole.Role;

@Repository
public interface PlayerRoleRepository extends MongoRepository<PlayerRole, String> {
	public List<PlayerRole> findByPlayerId(String playerId);
	public List<PlayerRole> findByRoleAndEntityId(Role role, String entityId);
	public List<PlayerRole> findByPreferredUsername(String preferredUsername, Role role);
	
	public PlayerRole findFirstByPlayerIdAndRole(String playerId, Role role);
	public PlayerRole findByPlayerIdAndRoleAndEntityId(String playerId, Role role, String entityId);
	public PlayerRole findFirstByPreferredUsernameAndRole(String preferredUsername, Role role);
	public PlayerRole findByPreferredUsernameAndRoleAndEntityId(String preferredUsername, Role role, String entityId);

}
