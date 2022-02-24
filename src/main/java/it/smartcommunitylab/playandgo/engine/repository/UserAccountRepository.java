package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.UserAccount;

@Repository
public interface UserAccountRepository extends MongoRepository<UserAccount, String> {
	
	public UserAccount findByPlayerId(String playerId);
	
}
