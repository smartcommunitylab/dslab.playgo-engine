package it.smartcommunitylab.playandgo.engine.manager.survey;

import java.util.Map;

public class ChallengeRequestDTO {
	private String surveyName;
	private Map<String, Object> data;
	private long start;
	private long end;
	
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

}
