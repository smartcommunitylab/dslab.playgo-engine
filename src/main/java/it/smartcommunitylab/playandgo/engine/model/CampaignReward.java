package it.smartcommunitylab.playandgo.engine.model;

import java.util.HashMap;
import java.util.Map;

public class CampaignReward {
	private Map<String, String> desc = new HashMap<>();
	private int position;
	
	public Map<String, String> getDesc() {
		return desc;
	}
	public void setDesc(Map<String, String> desc) {
		this.desc = desc;
	}
	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
}
