package it.smartcommunitylab.playandgo.engine.model;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="playerStatsGames")
public class PlayerStatsGame {
	@Id
	private String id;
	@Indexed
	private String playerId;
	@Indexed
	private String nickname;	
	@Indexed
	private String campaignId;
	@Indexed
	private LocalDate day;
	@Indexed
	private Boolean global = Boolean.FALSE; 	
	private double score;
	private String weekOfYear;
	private String monthOfYear;	
	
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
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public LocalDate getDay() {
		return day;
	}
	public void setDay(LocalDate day) {
		this.day = day;
	}
	public String getWeekOfYear() {
		return weekOfYear;
	}
	public void setWeekOfYear(String weekOfYear) {
		this.weekOfYear = weekOfYear;
	}
	public String getMonthOfYear() {
		return monthOfYear;
	}
	public void setMonthOfYear(String monthOfYear) {
		this.monthOfYear = monthOfYear;
	}
	public Boolean getGlobal() {
		return global;
	}
	public void setGlobal(Boolean global) {
		this.global = global;
	}
	public String getNickname() {
		return nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}	
}
