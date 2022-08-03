package it.smartcommunitylab.playandgo.engine.dto;

public class ChallengeStatsInfo {
	private String type;
	private int completed;
	private int failed;
	
	public void addCompleted() {
		this.completed++;
	}
	
	public void addFailed() {
		this.failed++;
	}
	
	public void addState(boolean complete) {
		if(complete) {
			this.addCompleted();	
		} else {
			this.addFailed();
		}
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public int getCompleted() {
		return completed;
	}
	public void setCompleted(int completed) {
		this.completed = completed;
	}
	public int getFailed() {
		return failed;
	}
	public void setFailed(int failed) {
		this.failed = failed;
	}
	
}
