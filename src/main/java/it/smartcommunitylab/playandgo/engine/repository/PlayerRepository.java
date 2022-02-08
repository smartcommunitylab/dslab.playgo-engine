package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import it.smartcommunitylab.playandgo.engine.model.Player;

public interface PlayerRepository extends MongoRepository<Player, String> {

}
