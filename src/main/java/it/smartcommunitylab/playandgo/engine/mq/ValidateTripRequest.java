package it.smartcommunitylab.playandgo.engine.mq;

public class ValidateTripRequest {
	private String playerId;
	private String trackedInstanceId;
	private String territoryId;
	
	public ValidateTripRequest() {}
	
	public ValidateTripRequest(String playerId, String territoryId, String trackedInstanceId) {
		this.playerId = playerId;
		this.territoryId = territoryId;
		this.trackedInstanceId = trackedInstanceId;
	}
	
	public String getPlayerId() {
		return playerId;
	}
	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}
	public String getTrackedInstanceId() {
		return trackedInstanceId;
	}
	public void setTrackedInstanceId(String trackedInstanceId) {
		this.trackedInstanceId = trackedInstanceId;
	}
	public String getTerritoryId() {
		return territoryId;
	}
	public void setTerritoryId(String territoryId) {
		this.territoryId = territoryId;
	}
}	
