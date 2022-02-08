package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import it.smartcommunitylab.playandgo.engine.model.Territory;

public interface TerritoryRepository extends MongoRepository<Territory, String> {

}
