package it.smartcommunitylab.playandgo.engine.util;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.UUID;

import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationStatus;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;

public class Utils {

	public static boolean isNotEmpty(String value) {
		boolean result = false;
		if ((value != null) && (!value.isEmpty())) {
			result = true;
		}
		return result;
	}
	
	public static boolean isEmpty(String value) {
		boolean result = true;
		if ((value != null) && (!value.isEmpty())) {
			result = false;
		}
		return result;
	}

	public static boolean isWithinRange(Date testDate, Date startDate, Date endDate) {
		return !(testDate.before(startDate) || testDate.after(endDate));
	}

	public static String getUUID() {
		return UUID.randomUUID().toString();
	}
	
	public static <K, V> K getKey(Map<K, V> map, V value) {
	    for (Entry<K, V> entry : map.entrySet()) {
	        if (entry.getValue().equals(value)) {
	            return entry.getKey();
	        }
	    }
	    return null;
	}
	
	public static Date getUTCDate(long milli) {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.setTimeInMillis(milli);
		return calendar.getTime();
	}
	
	public static Date getEndTime(TrackedInstance t) {
		long starTime = t.getStartTime().getTime();
		long endTime = starTime + (t.getValidationResult().getValidationStatus().getDuration() * 1000);
		return new Date(endTime);
	}
	
	public static double getSavedCo2(String modeType, double distance) {
		if(modeType.equalsIgnoreCase("WALK")) {
			return (distance / 1000.0) * 0.24293;
		} else if(modeType.equalsIgnoreCase("BIKE")) {
			return (distance / 1000.0) * 0.24293;
		} 
		return 0.0;
	}
	
	public static double getTrackDistance(TrackedInstance track) {
	    ValidationStatus validationStatus = track.getValidationResult().getValidationStatus();
        if(validationStatus.getEffectiveDistances().containsKey(validationStatus.getModeType())) {
            return validationStatus.getEffectiveDistances().get(validationStatus.getModeType());
        }     
        return validationStatus.getDistance();
	}
 	
}
