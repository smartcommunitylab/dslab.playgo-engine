package it.smartcommunitylab.playandgo.engine.util;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationStatus;
import it.smartcommunitylab.playandgo.engine.manager.CampaignManager;
import it.smartcommunitylab.playandgo.engine.manager.TrackedInstanceManager;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;

public class Utils {

	public static final DateTimeFormatter dtfDay = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter dftWeek = DateTimeFormatter.ofPattern("YYYY-ww", Locale.ITALY);
    public static final DateTimeFormatter dftMonth = DateTimeFormatter.ofPattern("yyyy-MM");

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
		if(modeType.equalsIgnoreCase("walk")) {
			return (distance / 1000.0) * 0.2319;
		} else if(modeType.equalsIgnoreCase("bike")) {
			return (distance / 1000.0) * 0.2319;
        } else if(modeType.equalsIgnoreCase("bus")) {
            return (distance / 1000.0) * 0.1233;
        } else if(modeType.equalsIgnoreCase("train")) {
            return (distance / 1000.0) * 0.1919;
        } else if(modeType.equalsIgnoreCase("car")) {
            return (distance / 1000.0) * 0.0773;
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
	
	public static String getCronExp(Campaign campaign, String cronKey) {
	    String exp = (String)campaign.getSpecificData().get(cronKey);
	    if(Utils.isNotEmpty(exp)) {
	        String[] items = exp.split(";");
	        if(items.length > 1) {
	            //0 0 14 * * WED
	            return "0 0 " + items[0].trim() + " * * " + items[1].trim().toUpperCase();
	        }
	    }
	    return null;
	}
	
	@SuppressWarnings("unchecked")
    public static boolean checkPlayerAlreadyRegistered(Player player, Campaign campaign) {
	    boolean result = false;
	    List<String> registeredIds = null;
	    Object object = player.getPersonalData().get(Campaign.registeredIds);
	    if(object == null) {
	        registeredIds = new ArrayList<>();
	        player.getPersonalData().put(Campaign.registeredIds, registeredIds);
	    } else {
	        registeredIds = (List<String>) object;
	    }
	    if(registeredIds.contains(campaign.getCampaignId())) {
	        result = true;
	    } else {
	        registeredIds.add(campaign.getCampaignId()); 
	    }
	    return result;
	}
	
	@SuppressWarnings("unchecked")
    public static boolean checkMean(Campaign campaign, String mean) {
        if((campaign.getValidationData() != null) && (campaign.getValidationData().get(TrackedInstanceManager.meansKey) != null)) {
            List<String> means = (List<String>) campaign.getValidationData().get(TrackedInstanceManager.meansKey);
            return means.contains(mean);
        }
        return false;	    
	}

    public static Set<String> getModeTypesFromPlayerTracks(List<CampaignPlayerTrack> playerTracks) {
		Set<String> modeTypes = new java.util.HashSet<>();
		for(CampaignPlayerTrack pt : playerTracks) {
			modeTypes.add(pt.getModeType());
		}
		return modeTypes;
    }

	@SuppressWarnings("unchecked")
	public static String getPointNameByCampaign(Campaign campaign, String lang) {
		String pointName = "eco-Leaves";
		if((campaign != null) && (campaign.getSpecificData() != null) && (campaign.getSpecificData().get(CampaignManager.CAMPAIGNPOINTNAME) != null)) {
			String name = ((Map<String, String>) campaign.getSpecificData().get(CampaignManager.CAMPAIGNPOINTNAME)).get(lang);
			if(Utils.isNotEmpty(name)) {
				pointName = name;
			}
		}
		return pointName;
	}
 	
}
