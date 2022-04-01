package it.smartcommunitylab.playandgo.engine.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public class PlayerTransportStatsInfo {
	private String modeType;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate dateFrom;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate dateTo;
	private double distance = 0.0; //meters
	private long duration = 0L; // seconds
	private double co2 = 0.0;
	private long trackNumber = 0L;
	
	public String getModeType() {
		return modeType;
	}
	public void setModeType(String modeType) {
		this.modeType = modeType;
	}
	public LocalDate getDateFrom() {
		return dateFrom;
	}
	public void setDateFrom(LocalDate dateFrom) {
		this.dateFrom = dateFrom;
	}
	public LocalDate getDateTo() {
		return dateTo;
	}
	public void setDateTo(LocalDate dateTo) {
		this.dateTo = dateTo;
	}
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}
	public long getDuration() {
		return duration;
	}
	public void setDuration(long duration) {
		this.duration = duration;
	}
	public double getCo2() {
		return co2;
	}
	public void setCo2(double co2) {
		this.co2 = co2;
	}
	public long getTrackNumber() {
		return trackNumber;
	}
	public void setTrackNumber(long trackNumber) {
		this.trackNumber = trackNumber;
	}

}
