package it.smartcommunitylab.playandgo.engine.report;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import it.smartcommunitylab.playandgo.engine.util.LocalDateDeserializer;

public class PlayerStatus {
	private String playerId;
	private String nickname;
	private String mail;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	@JsonDeserialize(using = LocalDateDeserializer.class)	
	private LocalDate registrationDate;
	private int activityDays;
	private int travels;
	private List<TransportStats> transportStatsList = new ArrayList<>();
	
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
	public String getMail() {
		return mail;
	}
	public void setMail(String mail) {
		this.mail = mail;
	}
	public LocalDate getRegistrationDate() {
		return registrationDate;
	}
	public void setRegistrationDate(LocalDate registrationDate) {
		this.registrationDate = registrationDate;
	}
	public int getActivityDays() {
		return activityDays;
	}
	public void setActivityDays(int activityDays) {
		this.activityDays = activityDays;
	}
	public int getTravels() {
		return travels;
	}
	public void setTravels(int travels) {
		this.travels = travels;
	}
	public List<TransportStats> getTransportStatsList() {
		return transportStatsList;
	}
	public void setTransportStatsList(List<TransportStats> transportStatsList) {
		this.transportStatsList = transportStatsList;
	}
}
