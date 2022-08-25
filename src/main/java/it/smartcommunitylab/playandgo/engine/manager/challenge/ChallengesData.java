package it.smartcommunitylab.playandgo.engine.manager.challenge;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ChallengesData implements Comparable<ChallengesData> {
	
	private String challId = "";
	private Map<String, String> challDesc = new HashMap<>();
	private Map<String, String> challCompleteDesc = new HashMap<>();
	private int challTarget = 0;
	private int status = 0;
	private double row_status = 0L;
	private String unit;
	private String type = "";
	private Boolean active = false;
	private Boolean success = false;
	private long startDate = 0L;
	private long endDate = 0L;
	private int daysToEnd = 0;
	private int bonus = 0;
	private long challCompletedDate = 0;
	
	private String proposerId;
	private OtherAttendeeData otherAttendeeData;
	
	private String extUrl;
	
	public ChallengesData(){
		super();
	}

	public String getChallId() {
		return challId;
	}


	public int getChallTarget() {
		return challTarget;
	}

	public void setChallId(String challId) {
		this.challId = challId;
	}

	public void setChallTarget(int challTarget) {
		this.challTarget = challTarget;
	}
	
	public String getType() {
		return type;
	}

	public int getStatus() {
		return status;
	}

	public Boolean getActive() {
		return active;
	}

	public Boolean getSuccess() {
		return success;
	}

	public long getStartDate() {
		return startDate;
	}

	public long getEndDate() {
		return endDate;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public void setStartDate(long startDate) {
		this.startDate = startDate;
	}

	public void setEndDate(long endDate) {
		this.endDate = endDate;
	}

	public int getDaysToEnd() {
		return daysToEnd;
	}

	public void setDaysToEnd(int daysToEnd) {
		this.daysToEnd = daysToEnd;
	}

	public double getRow_status() {
		return row_status;
	}

	public void setRow_status(double row_status) {
		this.row_status = row_status;
	}
	
	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public int getBonus() {
		return bonus;
	}

	public void setBonus(int bonus) {
		this.bonus = bonus;
	}

	public long getChallCompletedDate() {
		return challCompletedDate;
	}

	public void setChallCompletedDate(long challCompletedDate) {
		this.challCompletedDate = challCompletedDate;
	}

	public String getProposerId() {
		return proposerId;
	}

	public void setProposerId(String creatorId) {
		this.proposerId = creatorId;
	}

	public OtherAttendeeData getOtherAttendeeData() {
		return otherAttendeeData;
	}

	public void setOtherAttendeeData(OtherAttendeeData competitionData) {
		this.otherAttendeeData = competitionData;
	}

	@Override
	public String toString() {
		ToStringBuilder tsb = new ToStringBuilder(this, ToStringStyle.JSON_STYLE);
		return tsb.build();
	}

	@Override
	public int compareTo(ChallengesData o) {
		int res = Long.valueOf(startDate).compareTo(Long.valueOf(o.startDate));
		if (res == 0) {
			res = challId.compareTo(o.challId);
		}
		return res;
	}

	public String getExtUrl() {
		return extUrl;
	}

	public void setExtUrl(String extUrl) {
		this.extUrl = extUrl;
	}

	public Map<String, String> getChallDesc() {
		return challDesc;
	}

	public void setChallDesc(Map<String, String> challDesc) {
		this.challDesc = challDesc;
	}

	public Map<String, String> getChallCompleteDesc() {
		return challCompleteDesc;
	}

	public void setChallCompleteDesc(Map<String, String> challCompleteDesc) {
		this.challCompleteDesc = challCompleteDesc;
	}

	
	
}
