package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;

public interface TrackedInstanceRepository extends MongoRepository<TrackedInstance, String> {

}
