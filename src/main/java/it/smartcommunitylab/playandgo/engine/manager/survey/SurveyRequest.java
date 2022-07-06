package it.smartcommunitylab.playandgo.engine.manager.survey;

import java.util.Map;

public class SurveyRequest {
	private String surveyName;
	private String surveyLink;
	private Map<String, Object> data;
	private long start;
	private long end;
	private boolean defaultSurvey;
	
	public Map<String, Object> getData() {
		return data;
	}
	public void setData(Map<String, Object> data) {
		this.data = data;
	}
	public long getStart() {
		return start;
	}
	public void setStart(long start) {
		this.start = start;
	}
	public long getEnd() {
		return end;
	}
	public void setEnd(long end) {
		this.end = end;
	}
	public String getSurveyName() {
		return surveyName;
	}
	public void setSurveyName(String surveyName) {
		this.surveyName = surveyName;
	}
	public String getSurveyLink() {
		return surveyLink;
	}
	public void setSurveyLink(String surveyLink) {
		this.surveyLink = surveyLink;
	}
	public boolean isDefaultSurvey() {
		return defaultSurvey;
	}
	public void setDefaultSurvey(boolean defaultSurvey) {
		this.defaultSurvey = defaultSurvey;
	}

}
