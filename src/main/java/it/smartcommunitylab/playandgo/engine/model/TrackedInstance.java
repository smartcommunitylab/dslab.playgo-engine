package it.smartcommunitylab.playandgo.engine.model;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.google.common.collect.Sets;

import it.smartcommunitylab.playandgo.engine.geolocation.model.Geolocation;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationResult;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationResult.TravelValidity;

@Document(collection = "trackedInstances")
public class TrackedInstance {

	
	@Id
	private String id;

	private String clientId;
	
	@Indexed
	private String userId;
	
	@Indexed
	private String territoryId;
	
	private String multimodalId;
	private String sharedTravelId;
	
	private String freeTrackingTransport;
	
	private Collection<Geolocation> geolocationEvents;
	private Boolean started = Boolean.FALSE;
	private Boolean complete = Boolean.FALSE;
	private Boolean validating = Boolean.FALSE;
	
	private String time;
	
	private String deviceInfo;

	@Indexed
	private String day;
	
	@Indexed
	private Date startTime; 
	
	private ValidationResult validationResult;
	
	private TravelValidity changedValidity;
	private Boolean approved;
	private Boolean toCheck;
	
//	private int groupId;
	
	private Map<String, Double> overriddenDistances;
	
	private Boolean suspect;
	
//	private Map<String, String> busPolylines;
//	private List<String> trainPolylines;
	private Map<String, Object> routesPolylines;
	
	public TrackedInstance() {
		geolocationEvents = Sets.newConcurrentHashSet();
		validationResult = new ValidationResult();
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String travelId) {
		this.clientId = travelId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getTerritoryId() {
		return territoryId;
	}

	public void setTerritoryId(String territoryId) {
		this.territoryId = territoryId;
	}

	public String getMultimodalId() {
		return multimodalId;
	}

	public void setMultimodalId(String multimodalId) {
		this.multimodalId = multimodalId;
	}

	public Collection<Geolocation> getGeolocationEvents() {
		return geolocationEvents;
	}

	public void setGeolocationEvents(Collection<Geolocation> geolocationEvents) {
		this.geolocationEvents = geolocationEvents;
	}

	public Boolean getStarted() {
		return started;
	}

	public void setStarted(Boolean started) {
		this.started = started;
	}

	public Boolean getComplete() {
		return complete;
	}

	public void setComplete(Boolean complete) {
		this.complete = complete;
	}

	public String getDay() {
		return day;
	}

	public void setDay(String day) {
		this.day = day;
	}

	public ValidationResult getValidationResult() {
		return validationResult;
	}

	public void setValidationResult(ValidationResult validationResult) {
		this.validationResult = validationResult;
	}

	/**
	 * @return the deviceInfo
	 */
	public String getDeviceInfo() {
		return deviceInfo;
	}

	/**
	 * @param deviceInfo the deviceInfo to set
	 */
	public void setDeviceInfo(String deviceInfo) {
		this.deviceInfo = deviceInfo;
	}

	/**
	 * @return the time
	 */
	public String getTime() {
		return time;
	}

	/**
	 * @param time the time to set
	 */
	public void setTime(String time) {
		this.time = time;
	}

	/**
	 * @return the freeTrackingTransport
	 */
	public String getFreeTrackingTransport() {
		return freeTrackingTransport;
	}

	/**
	 * @param freeTrackingTransport the freeTrackingTransport to set
	 */
	public void setFreeTrackingTransport(String freeTrackingTransport) {
		this.freeTrackingTransport = freeTrackingTransport;
	}

	public TravelValidity getChangedValidity() {
		return changedValidity;
	}

	public void setChangedValidity(TravelValidity changedValidity) {
		this.changedValidity = changedValidity;
	}

	public Boolean getApproved() {
		return approved;
	}

	public void setApproved(Boolean approved) {
		this.approved = approved;
	}

	public Boolean getToCheck() {
		return toCheck;
	}

	public void setToCheck(Boolean toCheck) {
		this.toCheck = toCheck;
	}

//	public int getGroupId() {
//		return groupId;
//	}
//
//	public void setGroupId(int groupId) {
//		this.groupId = groupId;
//	}

	public Map<String, Double> getOverriddenDistances() {
		return overriddenDistances;
	}

	public void setOverriddenDistances(Map<String, Double> overriddenDistances) {
		this.overriddenDistances = overriddenDistances;
	}

	public Boolean getSuspect() {
		return suspect;
	}

	public void setSuspect(Boolean suspect) {
		this.suspect = suspect;
	}

	public Map<String, Object> getRoutesPolylines() {
		return routesPolylines;
	}

	public void setRoutesPolylines(Map<String, Object> routesPolylines) {
		this.routesPolylines = routesPolylines;
	}
	
	@Override
	public String toString() {
		return id;
	}

	public String getSharedTravelId() {
		return sharedTravelId;
	}

	public void setSharedTravelId(String sharedTravelId) {
		this.sharedTravelId = sharedTravelId;
	}

	public Boolean getValidating() {
		return validating;
	}

	public void setValidating(Boolean validating) {
		this.validating = validating;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	
}
