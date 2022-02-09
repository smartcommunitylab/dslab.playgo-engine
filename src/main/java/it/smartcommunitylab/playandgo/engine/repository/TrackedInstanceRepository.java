package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;

@Repository
public interface TrackedInstanceRepository extends MongoRepository<TrackedInstance, String> {

}
