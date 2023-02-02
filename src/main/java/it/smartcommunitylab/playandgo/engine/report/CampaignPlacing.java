package it.smartcommunitylab.playandgo.engine.report;

import it.smartcommunitylab.playandgo.engine.model.Image;

public class CampaignPlacing {
	private String nickname;
	private String playerId;
	private String groupId;
	private double value = 0.0;
	private int position;
	private Image avatar;
	
	public String getPlayerId() {
		return playerId;
	}
	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}
	public String getNickname() {
		return nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
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
	public Image getAvatar() {
		return avatar;
	}
	public void setAvatar(Image avatar) {
		this.avatar = avatar;
	}
    public String getGroupId() {
        return groupId;
    }
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
	
}
