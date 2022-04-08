package it.smartcommunitylab.playandgo.engine.report;

import org.springframework.data.annotation.Id;

public class GameStats {
	@Id
	private String period;
	private double totalScore;
	
	public String getPeriod() {
		return period;
	}
	public void setPeriod(String period) {
		this.period = period;
	}
	public double getTotalScore() {
		return totalScore;
	}
	public void setTotalScore(double totalScore) {
		this.totalScore = totalScore;
	}
}
