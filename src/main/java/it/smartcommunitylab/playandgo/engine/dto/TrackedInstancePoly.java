package it.smartcommunitylab.playandgo.engine.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;

public class TrackedInstancePoly {
	private TrackedInstance trackedInstance;
	private String trackPolyline;
	private Map<String, Object> routesPolylines;
	private PlayerInfo playerInfo;
	private List<CampaignTripInfo> campaigns = new ArrayList<>();
	
	public TrackedInstance getTrackedInstance() {
		return trackedInstance;
	}
	public void setTrackedInstance(TrackedInstance trackedInstance) {
		this.trackedInstance = trackedInstance;
	}
	public String getTrackPolyline() {
		return trackPolyline;
	}
	public void setTrackPolyline(String trackPolyline) {
		this.trackPolyline = trackPolyline;
	}
	public Map<String, Object> getRoutesPolylines() {
		return routesPolylines;
	}
	public void setRoutesPolylines(Map<String, Object> routesPolylines) {
		this.routesPolylines = routesPolylines;
	}
	public PlayerInfo getPlayerInfo() {
		return playerInfo;
	}
	public void setPlayerInfo(PlayerInfo playerInfo) {
		this.playerInfo = playerInfo;
	}
	public List<CampaignTripInfo> getCampaigns() {
		return campaigns;
	}
	public void setCampaigns(List<CampaignTripInfo> campaigns) {
		this.campaigns = campaigns;
	}
	
}
