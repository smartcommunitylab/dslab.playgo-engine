package it.smartcommunitylab.playandgo.engine.manager.azienda;

import java.util.ArrayList;
import java.util.List;

public class TrackResult {
	private Boolean valid;
	private String errorCode;
	private List<LegResult> legs = new ArrayList<>();
	
	public Boolean getValid() {
		return valid;
	}
	public void setValid(Boolean valid) {
		this.valid = valid;
	}
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public List<LegResult> getLegs() {
		return legs;
	}
	public void setLegs(List<LegResult> legs) {
		this.legs = legs;
	}
}
