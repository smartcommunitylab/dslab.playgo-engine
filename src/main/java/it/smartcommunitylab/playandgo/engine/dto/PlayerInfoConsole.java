package it.smartcommunitylab.playandgo.engine.dto;

import java.util.ArrayList;
import java.util.List;

import it.smartcommunitylab.playandgo.engine.model.Player;

public class PlayerInfoConsole {
	private Player player;
	private List<PlayerCampaign> campaigns = new ArrayList<>();
	
	public Player getPlayer() {
		return player;
	}
	public void setPlayer(Player player) {
		this.player = player;
	}
	public List<PlayerCampaign> getCampaigns() {
		return campaigns;
	}
	public void setCampaigns(List<PlayerCampaign> campaigns) {
		this.campaigns = campaigns;
	}
}
