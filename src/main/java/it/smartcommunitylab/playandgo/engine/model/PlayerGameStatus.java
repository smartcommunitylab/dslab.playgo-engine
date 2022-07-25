package it.smartcommunitylab.playandgo.engine.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import it.smartcommunitylab.playandgo.engine.ge.model.BadgeCollectionConcept;
import it.smartcommunitylab.playandgo.engine.ge.model.PlayerLevel;

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
	private List<PlayerLevel> levels = new ArrayList<>(); 
	private List<BadgeCollectionConcept> badges = new ArrayList<>();
	
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
	public String getNickname() {
		return nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	public List<BadgeCollectionConcept> getBadges() {
		return badges;
	}
	public void setBadges(List<BadgeCollectionConcept> badges) {
		this.badges = badges;
	}
	public List<PlayerLevel> getLevels() {
		return levels;
	}
	public void setLevels(List<PlayerLevel> levels) {
		this.levels = levels;
	}
	
}
