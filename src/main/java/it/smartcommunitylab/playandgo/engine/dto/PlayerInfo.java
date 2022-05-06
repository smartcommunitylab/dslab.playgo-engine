package it.smartcommunitylab.playandgo.engine.dto;

import it.smartcommunitylab.playandgo.engine.model.Image;

public class PlayerInfo {
	private String playerId;
	private String nickname;
	private String email;
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
	public Image getAvatar() {
		return avatar;
	}
	public void setAvatar(Image avatar) {
		this.avatar = avatar;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
}
