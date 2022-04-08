package it.smartcommunitylab.playandgo.engine.validation;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.FastDateFormat;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import it.smartcommunitylab.playandgo.engine.geolocation.model.Activity;
import it.smartcommunitylab.playandgo.engine.geolocation.model.Battery;
import it.smartcommunitylab.playandgo.engine.geolocation.model.Coords;
import it.smartcommunitylab.playandgo.engine.geolocation.model.Geolocation;
import it.smartcommunitylab.playandgo.engine.geolocation.model.GeolocationsEvent;
import it.smartcommunitylab.playandgo.engine.geolocation.model.Location;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.repository.TrackedInstanceRepository;

@Component
public class GeolocationsProcessor {
	private static Logger logger = LoggerFactory.getLogger(GeolocationsProcessor.class);
	
	@Autowired
	private TrackedInstanceRepository trackedInstanceRepository;
	
	private static final int LOCATION_STORE_INTERVAL = 2 * 24 * 3600 * 1000;
	private static FastDateFormat shortSdf = FastDateFormat.getInstance("yyyy/MM/dd");
	private static FastDateFormat timeSdf = FastDateFormat.getInstance("HH:mm");
	private static FastDateFormat fullSdf = FastDateFormat.getInstance("yyyy/MM/dd HH:mm");

	private static final String TRAVEL_ID = "travelId";
	public static final String START_TIME = "startTime";
	private static final int MAX_LOCATIONS = 10000;
	
	public List<TrackedInstance> storeGeolocationEvents(GeolocationsEvent geolocationsEvent, String userId, String territoryId) throws Exception {
		List<TrackedInstance> instances = Lists.newArrayList();
		
		ObjectMapper mapper = new ObjectMapper();

		int pointCount = 0;
		if (geolocationsEvent.getLocation() != null) {
			pointCount = geolocationsEvent.getLocation().size();
		}
		logger.info("Received " + pointCount + " geolocations for " + userId + ", " + geolocationsEvent.getDevice());

		boolean virtual = false;
		
		if (geolocationsEvent.getDevice() != null) {
			virtual = (boolean) geolocationsEvent.getDevice().getOrDefault("isVirtual", false);
		}

		if (!virtual) {
			checkEventsOrder(geolocationsEvent, userId);

			Multimap<String, Geolocation> geolocationsByItinerary = ArrayListMultimap.create();
			Map<String, String> freeTracks = new HashMap<String, String>();
			Map<String, Long> freeTrackStarts = new HashMap<String, Long>();
			String deviceInfo = mapper.writeValueAsString(geolocationsEvent.getDevice());

			groupByItinerary(geolocationsEvent, userId, geolocationsByItinerary, freeTracks, freeTrackStarts);

			for (String key : geolocationsByItinerary.keySet()) {
				TrackedInstance ti = preSaveTrackedInstance(key, userId, deviceInfo, geolocationsByItinerary, freeTracks, freeTrackStarts, territoryId);
				if (ti != null) {
					instances.add(ti);
					logger.info("Saved geolocation events, user: " + userId + ", travel: " + ti.getId() + ", " + ti.getGeolocationEvents().size() + " events.");
				}
			}
		} else {
			logger.info("Device of user " + userId + " is virtual: " + geolocationsEvent.getDevice());
		}
		return instances;
	}

	private void checkEventsOrder(GeolocationsEvent geolocationsEvent, String userId) {
		if (geolocationsEvent.getLocation() != null && !geolocationsEvent.getLocation().isEmpty()) {
			Location lastOk = geolocationsEvent.getLocation().get(geolocationsEvent.getLocation().size() - 1);
			adjustTimestamp(lastOk);
			
			ArrayList<Location> toKeep = Lists.newArrayList();
			toKeep.add(lastOk);
			for (int i = geolocationsEvent.getLocation().size() - 2; i >= 0; i--) {
				Location l1 = geolocationsEvent.getLocation().get(i);
				adjustTimestamp(l1);

				Date dOk = lastOk.getTimestamp();
				Date d1 = l1.getTimestamp();
				if (d1 == null) {
					logger.debug("Missing timestamp in location object: " + l1.toString());
					continue;
				}

				int comp = d1.compareTo(dOk);
				if (comp < 0) {
					lastOk = l1;
					toKeep.add(l1);
				} else {
					String tidOk = null;
					String tid1 = null;

					if (lastOk.getExtras() != null && lastOk.getExtras().containsKey("idTrip")) {
						tidOk = (String) lastOk.getExtras().get("idTrip");
					}
					if (l1.getExtras() != null && l1.getExtras().containsKey("idTrip")) {
						tid1 = (String) l1.getExtras().get("idTrip");
					}
					logger.debug("'Unordered' events for user: " + userId + ", tripId: " + tid1 + " / " + tidOk + ", times: " + d1 + " / " + dOk + ", coordinates: " + l1.getCoords() + " / "
							+ lastOk.getCoords());
				}
			}

			geolocationsEvent.setLocation(toKeep);

			Collections.sort(geolocationsEvent.getLocation());
		} else {
			logger.debug("No geolocations found.");
		}
	}

	/**
	 * Happens in strange situations: the timestamp of GPS is far in the past. Need to adjust
	 * @param lastOk
	 */
	private void adjustTimestamp(Location l) {
		Date lastDate = l.getTimestamp();
		Long startTs = null; 
		if (l.getExtras() != null && l.getExtras().containsKey("start")) {
			startTs = Long.parseLong(l.getExtras().get("start").toString());
		} else {
			// should not happen
			startTs = System.currentTimeMillis();
		}
		if (lastDate.getTime() < startTs) {
			Calendar tsc = Calendar.getInstance();
			tsc.setTime(l.getTimestamp());
			Calendar sc = Calendar.getInstance();
			sc.setTimeInMillis(startTs);
			tsc.set(Calendar.YEAR, sc.get(Calendar.YEAR));
			tsc.set(Calendar.MONTH, sc.get(Calendar.MONTH));
			tsc.set(Calendar.DATE, sc.get(Calendar.DATE));
			logger.debug("Adjusting time point: " + l.getTimestamp().getTime());
			l.setTimestamp(tsc.getTime());
		}
	}

	private void groupByItinerary(GeolocationsEvent geolocationsEvent, String userId, Multimap<String, Geolocation> geolocationsByItinerary, Map<String, String> freeTracks, Map<String, Long> freeTrackStarts) throws Exception {
		Long now = System.currentTimeMillis();
		Map<String, Object> device = geolocationsEvent.getDevice();

		Multimap<String, Long> freeTrackStartsByKey = ArrayListMultimap.create();
		
		if (geolocationsEvent.getLocation() != null) {
			int skippedOld = 0;
			int skippedNoId = 0;
			for (Location location : geolocationsEvent.getLocation()) {
				String locationTravelId = null;
				Long locationTs = null;
				if (location.getExtras() != null && location.getExtras().containsKey("idTrip")) {
					locationTravelId = (String) location.getExtras().get("idTrip");
					locationTs = location.getExtras().get("start") != null ? Long.parseLong("" + location.getExtras().get("start")) : null;
				} else {
					// now the plugin supports correctly the extras for each
					// location.
					// locations with empty idTrip are possible only upon
					// initialization/synchronization.
					// we skip them here
					skippedNoId++;
//					logger.warn("location without idTrip, user: " + userId + ", extras = " + location.getExtras());
					continue;
				}

				if (location.getTimestamp() == null) {
					logger.debug("Missing timestamp in location object: " + location.toString());
					continue;
				}

				if (locationTs == null) {
					locationTs = location.getTimestamp().getTime();
				}

				// discard event older than 2 days
				if (now - LOCATION_STORE_INTERVAL > location.getTimestamp().getTime()) {
					logger.debug("Skipped point at time " + location.getTimestamp().getTime());
					skippedOld++;
//					continue;
				}

				Geolocation geolocation = buildGeolocation(location, userId, locationTravelId, device, now);
				
				
				String key = geolocation.getTravelId() + (geolocation.getMultimodalId() != null ? ("#" + geolocation.getMultimodalId()):""); // + "@" + day;
				geolocationsByItinerary.put(key, geolocation);
				if (StringUtils.hasText((String) location.getExtras().get("transportType"))) {
					freeTracks.put(key, (String) location.getExtras().get("transportType"));
				}

				freeTrackStartsByKey.put(key, locationTs);

				// storage.saveGeolocation(geolocation);
			}
			
			for (String key: freeTrackStartsByKey.keySet()) {
				Long min = freeTrackStartsByKey.get(key).stream().min(Long::compare).orElse(0L);
				freeTrackStarts.put(key, min);
			}
			
			if (skippedOld > 0) {
//				logger.warn("Timestamps too old, skipped " + skippedOld + " locations.");
				logger.debug("Found " + skippedOld + " locations to old.");
			}
			if (skippedNoId > 0) {
				logger.debug("Locations without idTrip, skipped " + skippedNoId + " locations.");
			}
		}

		logger.debug("Group keys: " + geolocationsByItinerary.keySet());
		if (geolocationsByItinerary.keySet() == null || geolocationsByItinerary.keySet().isEmpty()) {
			logger.debug("No geolocationsByItinerary set.");
		}
	}
	
	private Geolocation buildGeolocation(Location location, String userId, String locationTravelId, Map<String, Object> device, Long now) {
		Coords coords = location.getCoords();
		Activity activity = location.getActivity();
		Battery battery = location.getBattery();

		Geolocation geolocation = new Geolocation();

		geolocation.setUserId(userId);

		geolocation.setTravelId(locationTravelId);

		geolocation.setUuid(location.getUuid());
		if (device != null) {
			geolocation.setDevice_id((String) device.get("uuid"));
			geolocation.setDevice_model((String) device.get("model"));
		} else {
			geolocation.setDevice_model("UNKNOWN");
		}
		if (coords != null) {
			geolocation.setLatitude(coords.getLatitude());
			geolocation.setLongitude(coords.getLongitude());
			double c[] = new double[2];
			c[0] = geolocation.getLongitude();
			c[1] = geolocation.getLatitude();
			geolocation.setGeocoding(c);
			geolocation.setAccuracy(coords.getAccuracy());
			geolocation.setAltitude(coords.getAltitude());
			geolocation.setSpeed(coords.getSpeed());
			geolocation.setHeading(coords.getHeading());
		}
		if (activity != null) {
			geolocation.setActivity_type(activity.getType());
			geolocation.setActivity_confidence(activity.getConfidence());
		}
		if (battery != null) {
			geolocation.setBattery_level(battery.getLevel());
			geolocation.setBattery_is_charging(battery.getIs_charging());
		}

		geolocation.setIs_moving(location.getIs_moving());

		geolocation.setRecorded_at(new Date(location.getTimestamp().getTime()));

		geolocation.setCreated_at(new Date(now++));

		geolocation.setGeofence(location.getGeofence());

		if (StringUtils.hasText((String) location.getExtras().get("btDeviceId"))) {
			geolocation.setCertificate((String) location.getExtras().get("btDeviceId"));
		}
		if (StringUtils.hasText((String) location.getExtras().get("multimodalId"))) {
			geolocation.setMultimodalId((String) location.getExtras().get("multimodalId"));
		}		
		if (StringUtils.hasText((String) location.getExtras().get("sharedTravelId"))) {
			geolocation.setSharedTravelId((String) location.getExtras().get("sharedTravelId"));
		}		

		return geolocation;
	}

	private TrackedInstance preSaveTrackedInstance(String key, String userId, String deviceInfo, Multimap<String, Geolocation> geolocationsByItinerary, Map<String, String> freeTracks,
			Map<String, Long> freeTrackStarts, String territoryId) throws Exception {
		String splitKey[] = key.split("@");
		String travelId = splitKey[0];
		String multimodalId = null;

		String splitId[] = travelId.split("#");
		if (splitId.length == 2) {
			travelId = splitId[0];
			multimodalId = splitId[1];
		}

		String day = shortSdf.format(freeTrackStarts.get(key));
		String time = timeSdf.format(freeTrackStarts.get(key));
		
		//check existing track
		TrackedInstance res = trackedInstanceRepository.findByDayAndUserIdAndClientId(day, userId, travelId);
		if (res == null) {
			logger.debug("No existing TrackedInstance found.");
			res = new TrackedInstance();
			res.setClientId(travelId);
			res.setDay(day);
			res.setTime(time);
			res.setStartTime(fullSdf.parse(day + " " + time));
			res.setUserId(userId);
			res.setId(ObjectId.get().toString());
			if (travelId.contains("_temporary_")) {
				logger.debug("Orphan temporary, skipping clientId: " + travelId);
				return null;
			}
			String ftt = freeTracks.get(key);
			if (ftt == null) {
				logger.debug("No freetracking transport found, extracting from clientId: " + travelId);
				String[] cid = travelId.split("_");
				if (cid != null && cid.length > 1) {
					ftt = cid[0];
				} else {
					logger.debug("Cannot find transport type for " + key);
				}
			}
			res.setFreeTrackingTransport(ftt);
			if (freeTrackStarts.containsKey(key)) {
				res.setTime(timeSdf.format(new Date(freeTrackStarts.get(key))));
			}
			if (geolocationsByItinerary.get(key) != null) {
				logger.debug("Adding " + geolocationsByItinerary.get(key).size() + " geolocations to existing " + res.getGeolocationEvents().size() + ".");
				res.getGeolocationEvents().addAll(geolocationsByItinerary.get(key));
				String sharedId = res.getGeolocationEvents().stream().filter(e -> e.getSharedTravelId() != null).findFirst().map(e -> e.getSharedTravelId()).orElse(null);
				res.setSharedTravelId(sharedId);
				logger.debug("Resulting events: " + res.getGeolocationEvents().size());
			}
			// limit number of points to avoid failure of saving data
			if (res.getGeolocationEvents() != null) {
				int mul = 1; 
				while (res.getGeolocationEvents().size() > (mul * MAX_LOCATIONS)) mul++;
				if (mul > 1) {
					logger.debug("TOO MANY GEOLOCATION EVENTS, user: " + userId + ", travel: " + res.getId() + ", " + res.getGeolocationEvents().size() + " events.");
					List<Geolocation> src = new LinkedList<>(res.getGeolocationEvents());
					List<Geolocation> list = new LinkedList<>();
					for (int i = 0; i < src.size(); i += mul) {
						list.add(src.get(i));
					}
					res.setGeolocationEvents(list);
				}
			}
			res.setDeviceInfo(deviceInfo);		
			res.setMultimodalId(multimodalId);
			res.setTerritoryId(territoryId);
			trackedInstanceRepository.save(res);
		} else {
			if (res.getComplete() != null && res.getComplete()) {
				logger.debug("Skipping complete trip " + res.getId());
				return null;				
			} else {
				logger.debug("Skipping already existing trip " + res.getId());
				return null;
			}
		}
		return res;
	}
	
}
