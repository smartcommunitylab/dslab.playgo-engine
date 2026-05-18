/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.playandgo.engine.validation;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import it.smartcommunitylab.playandgo.engine.geolocation.model.Geolocation;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationResult;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationResult.TravelValidity;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationStatus;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationStatus.ERROR_TYPE;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationStatus.MODE_TYPE;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.model.ValidationData;
import it.smartcommunitylab.playandgo.engine.util.Utils;

/**
 * @author raman
 *
 */
@Service
public class ValidationService {

	private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

	/**
	 * Validate free tracking instance for a given territory settings
	 * @param geolocations
	 * @param ttype
	 * @param territoryId
	 * @return
	 * @throws Exception
	 */
	public ValidationResult validateFreeTracking(Collection<Geolocation> geolocations, String ttype, String territoryId, ValidationData vd) throws Exception {
		if (geolocations == null || ttype == null) {
			return null;
		}
		ValidationResult vr = new ValidationResult();
		
		switch(ttype) {
		case "walk":
			vr.setValidationStatus(TrackValidator.validateFreeWalk(geolocations, territoryId, vd));
			break;
		case "bike": 
			vr.setValidationStatus(TrackValidator.validateFreeBike(geolocations, territoryId, vd));
			break;
		case "bus": 
			vr.setValidationStatus(TrackValidator.validateFreeBus(geolocations, territoryId, vd));
			break;
		case "train": 
			vr.setValidationStatus(TrackValidator.validateFreeTrain(geolocations, territoryId, vd));
			break;
		case "boat": 
			vr.setValidationStatus(TrackValidator.validateFreeBoat(geolocations, territoryId, vd));
			break;
		}
		// check mode type validity
		if(!Utils.checkMean(vd, ttype)) {
			vr.getValidationStatus().setValidationOutcome(TravelValidity.INVALID);
			vr.getValidationStatus().setError(ERROR_TYPE.DOES_NOT_MATCH);
		}
		if(TravelValidity.VALID.equals(vr.getValidationStatus().getValidationOutcome())) {
			vr.setValid(true);
		}		
		return vr;
	}
	
	/**
	 * @param passengerTrip
	 * @param driverTrip
	 * @param territoryId
	 * @return
	 */
	public ValidationResult validateSharedTripPassenger(TrackedInstance passengerTrack, TrackedInstance driverTrack, 
				ValidationData vd, boolean forceValidation) {
		if(forceValidation)
		    return passengerTrack.getValidationResult();
		
		Collection<Geolocation> driverTrip = passengerTrack.getGeolocationEvents();
	    if (driverTrip == null) {
			return null;
		}

		ValidationResult vr = new ValidationResult();
		vr.setValidationStatus(TrackValidator.validateSharedPassenger(passengerTrack.getGeolocationEvents(), driverTrack.getGeolocationEvents(), 
				passengerTrack.getTerritoryId(), vd));
        if(TravelValidity.VALID.equals(vr.getValidationStatus().getValidationOutcome())) {
            vr.setValid(true);
        }       		
		return vr;
	}
	
	/**
	 * @param driverTrip
	 * @param territoryId
	 * @return
	 */
	public ValidationResult validateSharedTripDriver(TrackedInstance track, ValidationData vd, boolean forceValidation) {
	    if(forceValidation) 
	        return track.getValidationResult();
	    
	    Collection<Geolocation> driverTrip = track.getGeolocationEvents();
		if (driverTrip == null) {
			return null;
		}
	
		ValidationResult vr = new ValidationResult();
		vr.setValidationStatus(TrackValidator.validateSharedDriver(driverTrip, track.getTerritoryId(), vd));
        if(TravelValidity.VALID.equals(vr.getValidationStatus().getValidationOutcome())) {
            vr.setValid(true);
        }       
		return vr;
	}

	/**
	 * 
	 * @param territoryId
	 * @param geolocationEvents
	 * @param ttype
	 * @param vs
	 * @param overriddenDistances
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> computeFreeTrackingDistances(String territoryId, ValidationData vd, Collection<Geolocation> geolocationEvents, 
			String ttype, ValidationStatus vs, Map<String, Double> overriddenDistances) throws Exception {
		Map<String, Object> result = Maps.newTreeMap();
		double distance = 0; 		

		boolean isOverridden = overriddenDistances != null && !overriddenDistances.isEmpty();
		
		if (((geolocationEvents != null) && (geolocationEvents.size() >= 2)) || isOverridden) {
			if (vs == null) {
				vs = validateFreeTracking(geolocationEvents, ttype, territoryId, vd).getValidationStatus();
			}
			
			if (!isOverridden) {
				overriddenDistances = Maps.newTreeMap();
			}

			if ("walk".equals(ttype)) {
				if (overriddenDistances.containsKey("walk")) {
					distance = overriddenDistances.get("walk") / 1000.0;
					logger.info("Overridden walk distance: " + distance);
				} else if (vs.getEffectiveDistances().containsKey(MODE_TYPE.walk)) {
					distance = vs.getEffectiveDistances().get(MODE_TYPE.walk) / 1000.0; 
				}
				result.put("walkDistance", distance);
			}
			if ("bike".equals(ttype)) {
				if (overriddenDistances.containsKey("bike")) {
					distance = overriddenDistances.get("bike") / 1000.0;
					logger.info("Overridden bike distance: " + distance);
				} else if (vs.getEffectiveDistances().containsKey(MODE_TYPE.bike)) {				
					distance = vs.getEffectiveDistances().get(MODE_TYPE.bike) / 1000.0;
				}
				result.put("bikeDistance", distance);
			} if ("bus".equals(ttype)) {
				if (overriddenDistances.containsKey("bus")) {
					distance = overriddenDistances.get("bus") / 1000.0;
					logger.info("Overridden bus distance: " + distance);
				} else if (vs.getEffectiveDistances().containsKey(MODE_TYPE.bus)) {				
					distance = vs.getEffectiveDistances().get(MODE_TYPE.bus) / 1000.0;
				}
				result.put("busDistance", distance);
			} if ("train".equals(ttype)) {
				if (overriddenDistances.containsKey("train")) {
					distance = overriddenDistances.get("train") / 1000.0;
					logger.info("Overridden train distance: " + distance);
				} else if (vs.getEffectiveDistances().containsKey(MODE_TYPE.train)) {						
					distance = vs.getEffectiveDistances().get(MODE_TYPE.train) / 1000.0;
				}
				result.put("trainDistance", distance);
			} if ("boat".equals(ttype)) {
				if (overriddenDistances.containsKey("boat")) {
					distance = overriddenDistances.get("boat") / 1000.0;
					logger.info("Overridden boat distance: " + distance);
				} else if (vs.getEffectiveDistances().containsKey(MODE_TYPE.boat)) {						
					distance = vs.getEffectiveDistances().get(MODE_TYPE.boat) / 1000.0;
				}
				result.put("boatDistance", distance);
			}
		} else {
			logger.info("Skipping");
		}

		return result;
	}	
	

	/**
	 * @param territoryId
	 * @param geolocationEvents
	 * @param validationStatus
	 * @param overriddenDistances
	 * @return
	 */
	public Map<String, Object> computeSharedTravelDistanceForPassenger(String territoryId, Collection<Geolocation> geolocationEvents, ValidationStatus validationStatus, Map<String, Double> overriddenDistances) {
		Map<String, Object> results = Maps.newTreeMap();
		results.put("driverTrip", false);
		double distance = 0d;
		if (overriddenDistances == null) overriddenDistances = Collections.emptyMap();
		if (overriddenDistances.containsKey("car")) {
			distance = overriddenDistances.get("car") / 1000.0;
		} else if (validationStatus.getEffectiveDistances().containsKey(MODE_TYPE.car)) {
			distance = validationStatus.getEffectiveDistances().get(MODE_TYPE.car) / 1000.0; 
		} else {
			distance = validationStatus.getDistance() / 1000.0; 
		}
		results.put("carpoolingDistance", distance);
		return results;
	}
	/**
	 * @param territoryId
	 * @param geolocationEvents
	 * @param validationStatus
	 * @param overriddenDistances
	 * @param firstPair 
	 * @return
	 */
	public Map<String, Object> computeSharedTravelDistanceForDriver(String territoryId, Collection<Geolocation> geolocationEvents, ValidationStatus validationStatus, Map<String, Double> overriddenDistances, boolean firstPair) {
		Map<String, Object> results = Maps.newTreeMap();
		results.put("driverTrip", true);
		results.put("firstPair", firstPair);
		
		double distance = 0d;
		if (overriddenDistances == null) overriddenDistances = Collections.emptyMap();
		if (overriddenDistances.containsKey("car")) {
			distance = overriddenDistances.get("car") / 1000.0;
		} else if (validationStatus.getEffectiveDistances().containsKey(MODE_TYPE.car)) {
			distance = validationStatus.getEffectiveDistances().get(MODE_TYPE.car) / 1000.0; 
		} else {
			distance = validationStatus.getDistance() / 1000.0; 
		}
		results.put("carpoolingDistance", distance);
		return results;
	}
}
