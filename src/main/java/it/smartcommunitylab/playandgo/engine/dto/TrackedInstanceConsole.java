package it.smartcommunitylab.playandgo.engine.dto;

import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;

public class TrackedInstanceConsole {
	private TrackedInstance trackedInstance;
	private PlayerInfo playerInfo;
	
	public TrackedInstance getTrackedInstance() {
		return trackedInstance;
	}
	public void setTrackedInstance(TrackedInstance trackedInstance) {
		this.trackedInstance = trackedInstance;
	}
	public PlayerInfo getPlayerInfo() {
		return playerInfo;
	}
	public void setPlayerInfo(PlayerInfo playerInfo) {
		this.playerInfo = playerInfo;
	}
	
}
