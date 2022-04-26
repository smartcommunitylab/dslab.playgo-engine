package it.smartcommunitylab.playandgo.engine.report;

import org.springframework.data.annotation.Id;

import it.smartcommunitylab.playandgo.engine.model.Image;

public class CampaignPlacing {
	@Id
	private String playerId;
	private String nickname;
	private double value;
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
	
}
