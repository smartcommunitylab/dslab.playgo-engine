package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.Avatar;

@Repository
public interface AvatarRepository extends MongoRepository<Avatar, String> {
	
	public Avatar findByPlayerId(String playerId);

}
