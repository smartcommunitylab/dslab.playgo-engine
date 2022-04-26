package it.smartcommunitylab.playandgo.engine.dto;

import it.smartcommunitylab.playandgo.engine.model.Logo;

public class PlayerInfo {
	private String playerId;
	private String nickname;
	private Logo avatar;
	
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
	public Logo getAvatar() {
		return avatar;
	}
	public void setAvatar(Logo avatar) {
		this.avatar = avatar;
	}
}
