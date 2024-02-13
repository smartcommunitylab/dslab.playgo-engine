package it.smartcommunitylab.playandgo.engine.manager.azienda;

import java.util.ArrayList;
import java.util.List;

public class TrackData {
	private Long startTime;
	private String multimodalId;
	private List<LegData> legs = new ArrayList<>();
	private String firstTrackId;
	
	public Long getStartTime() {
		return startTime;
	}
	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}
	public List<LegData> getLegs() {
		return legs;
	}
	public void setLegs(List<LegData> legs) {
		this.legs = legs;
	}
    public String getMultimodalId() {
        return multimodalId;
    }
    public void setMultimodalId(String multimodalId) {
        this.multimodalId = multimodalId;
    }
    public String getFirstTrackId() {
        return firstTrackId;
    }
    public void setFirstTrackId(String firstTrackId) {
        this.firstTrackId = firstTrackId;
    }
    
    @Override
    public String toString() {
        return startTime + "_" + multimodalId + "_" + firstTrackId;
    }
}
