package it.smartcommunitylab.playandgo.engine.mq;

public class ValidateTripRequest {
	private String playerId;
	private String multimodalId;
	private String territoryId;
	private boolean forceValidation = false;
	
	public ValidateTripRequest() {}
	
	public ValidateTripRequest(String playerId, String territoryId, String multimodalId, boolean forceValidation) {
		this.playerId = playerId;
		this.territoryId = territoryId;
		this.multimodalId = multimodalId;
		this.forceValidation = forceValidation;
	}
	
	public String getPlayerId() {
		return playerId;
	}
	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}
	public String getTerritoryId() {
		return territoryId;
	}
	public void setTerritoryId(String territoryId) {
		this.territoryId = territoryId;
	}

    public String getMultimodalId() {
        return multimodalId;
    }

    public void setMultimodalId(String multimodalId) {
        this.multimodalId = multimodalId;
    }

    public boolean isForceValidation() {
        return forceValidation;
    }

    public void setForceValidation(boolean forceValidation) {
        this.forceValidation = forceValidation;
    }
}	
