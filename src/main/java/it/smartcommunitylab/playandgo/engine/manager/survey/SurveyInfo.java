package it.smartcommunitylab.playandgo.engine.manager.survey;

public class SurveyInfo {
	private boolean completed = false;
	private String url;
	
	public boolean isCompleted() {
		return completed;
	}
	public void setCompleted(boolean completed) {
		this.completed = completed;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
}
