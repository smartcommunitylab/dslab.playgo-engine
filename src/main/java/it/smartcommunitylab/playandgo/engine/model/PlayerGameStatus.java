package it.smartcommunitylab.playandgo.engine.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="playerGameStatus")
public class PlayerGameStatus {
	@Id
	private String id;
	@Indexed
	private String playerId;
	private String nickname;
	@Indexed
	private String campaignId;
	private Date updateTime;
	private double score;
	private Map<String, Object> level = new HashMap<>(); 
	private List<Object> challenges = new ArrayList<>();
	private List<Object> badges = new ArrayList<>();
	
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
	public Date getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public List<Object> getChallenges() {
		return challenges;
	}
	public void setChallenges(List<Object> challenges) {
		this.challenges = challenges;
	}
	public List<Object> getBadges() {
		return badges;
	}
	public void setBadges(List<Object> badges) {
		this.badges = badges;
	}
	public Map<String, Object> getLevel() {
		return level;
	}
	public void setLevel(Map<String, Object> level) {
		this.level = level;
	}
	public String getNickname() {
		return nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	
}
