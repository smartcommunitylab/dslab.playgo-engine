package it.smartcommunitylab.playandgo.engine.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="playerStatsGames")
public class PlayerStatsGame {
	@Id
	private String id;
	private String playerId;
	private String campaignId;
	private Date updateTime;
	private int greenLeaves;
	private int greenLeavesWeekly;
	private int level;
	private int pointToNextLevel;
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
	public int getGreenLeaves() {
		return greenLeaves;
	}
	public void setGreenLeaves(int greenLeaves) {
		this.greenLeaves = greenLeaves;
	}
	
}
