package it.smartcommunitylab.playandgo.engine.report;

import org.springframework.data.annotation.Id;

public class CampaignGroupPlacing {
	@Id
	private String groupId;
	private double value;
	private int position;
	
	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
	public double getValue() {
		return value;
	}
	public void setValue(double value) {
		this.value = value;
	}
	public String getGroupId() {
		return groupId;
	}
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	
}
