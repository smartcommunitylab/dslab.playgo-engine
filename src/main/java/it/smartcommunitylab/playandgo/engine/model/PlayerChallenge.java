package it.smartcommunitylab.playandgo.engine.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengesData;

@Document(collection="playerChallenges")
public class PlayerChallenge {
	@Id
	private String id;
	@Indexed
	private String playerId;
	@Indexed
	private String campaignId;
	private String challangeId;
	private ChallengesData challengeData;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getPlayerId() {
		return playerId;
	}
	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}
	public String getCampaignId() {
		return campaignId;
	}
	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}
	public ChallengesData getChallengeData() {
		return challengeData;
	}
	public void setChallengeData(ChallengesData challengeData) {
		this.challengeData = challengeData;
	}
	public String getChallangeId() {
		return challangeId;
	}
	public void setChallangeId(String challangeId) {
		this.challangeId = challangeId;
	}
}
