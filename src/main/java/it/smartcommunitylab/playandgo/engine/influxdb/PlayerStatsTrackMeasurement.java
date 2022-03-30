package it.smartcommunitylab.playandgo.engine.influxdb;

import java.time.Instant;

//@Measurement(name = "playerStatsTrack")
public class PlayerStatsTrackMeasurement {
//	@Column(tag = true)
	private String playerId;
//	@Column(tag = true)
	private String campaignId;
//	@Column(tag = true)
	private String trackedInstanceId;
//	@Column(tag = true)
	private String modeType;
//	@Column
	private double value;
//	@Column(timestamp = true)
	private Instant startTime;
	
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
	public String getModeType() {
		return modeType;
	}
	public void setModeType(String modeType) {
		this.modeType = modeType;
	}
	public Instant getStartTime() {
		return startTime;
	}
	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}
	public String getTrackedInstanceId() {
		return trackedInstanceId;
	}
	public void setTrackedInstanceId(String trackedInstanceId) {
		this.trackedInstanceId = trackedInstanceId;
	}
	public double getValue() {
		return value;
	}
	public void setValue(double value) {
		this.value = value;
	}
	 
}
