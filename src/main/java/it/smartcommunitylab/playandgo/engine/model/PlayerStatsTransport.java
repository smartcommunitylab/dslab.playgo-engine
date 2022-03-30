package it.smartcommunitylab.playandgo.engine.model;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="playerStatsTransports")
public class PlayerStatsTransport {
	@Id
	private String id;
	@Indexed
	private String playerId;
	@Indexed
	private String campaignId;
	@Indexed
	private Boolean global = Boolean.FALSE; 
	@Indexed
	private String scoreType;
	@Indexed
	private LocalDate weeklyDay;
	private double score = 0.0;
	
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
	public Boolean getGlobal() {
		return global;
	}
	public void setGlobal(Boolean global) {
		this.global = global;
	}
	public String getScoreType() {
		return scoreType;
	}
	public void setScoreType(String scoreType) {
		this.scoreType = scoreType;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public LocalDate getWeeklyDay() {
		return weeklyDay;
	}
	public void setWeeklyDay(LocalDate weeklyDay) {
		this.weeklyDay = weeklyDay;
	}
	
	
}
