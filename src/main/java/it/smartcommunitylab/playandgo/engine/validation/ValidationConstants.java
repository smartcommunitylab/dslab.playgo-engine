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

import java.util.HashMap;
import java.util.Map;

import it.smartcommunitylab.playandgo.engine.model.Territory;

/**
 * @author raman
 *
 */
public class ValidationConstants {

	public static final String PARAM_WALK_SPEED_THRESHOLD 				 	= "WALK_SPEED_THRESHOLD"; // km/h
	public static final String PARAM_WALK_AVG_SPEED_THRESHOLD 			 	= "WALK_AVG_SPEED_THRESHOLD"; // km/h
	public static final String PARAM_WALK_GUARANTEED_AVG_SPEED_THRESHOLD 	= "WALK_GUARANTEED_AVG_SPEED_THRESHOLD"; // km/h

	public static final String PARAM_BIKE_SPEED_THRESHOLD   			 	= "BIKE_SPEED_THRESHOLD"; // km/h
	public static final String PARAM_BIKE_AVG_SPEED_THRESHOLD 			 	= "BIKE_AVG_SPEED_THRESHOLD"; // km/h
	public static final String PARAM_BIKE_GUARANTEED_AVG_SPEED_THRESHOLD 	= "BIKE_GUARANTEED_AVG_SPEED_THRESHOLD"; // km/h

	public static final String PARAM_VALIDITY_THRESHOLD 				 	= "VALIDITY_THRESHOLD"; // %
	public static final String PARAM_ACCURACY_THRESHOLD 				 	= "ACCURACY_THRESHOLD"; // meters
	

	public static final String PARAM_MIN_COVERAGE_THRESHOLD 			 	= "MIN_COVERAGE_THRESHOLD"; // %

	public static final String PARAM_SHARED_TRIP_DISTANCE_THRESHOLD 	 	= "SHARED_TRIP_DISTANCE_THRESHOLD"; // meters 

	public static final String PARAM_DISTANCE_THRESHOLD 				 	= "DISTANCE_THRESHOLD"; // meters 
	public static final String DATA_HOLE_THRESHOLD 					     	= "HOLE_THRESHOLD"; // seconds
	public static final String PARAM_BIKE_DISTANCE_THRESHOLD 			 	= "BIKE_DISTANCE_THRESHOLD";// meters 
	public static final String PARAM_MAX_AVG_SPEED_THRESHOLD 			 	= "MAX_AVG_SPEED_THRESHOLD"; // km/h
	
	public static final String PARAM_PENDING_COVERAGE_THRESHOLD 		 	= "PENDING_COVERAGE_THRESHOLD";
	public static final String PARAM_COVERAGE_THRESHOLD 				 	= "COVERAGE_THRESHOLD"; // %
	public static final String PARAM_CERTIFIED_COVERAGE_THRESHOLD_VALID 	= "CERTIFIED_COVERAGE_THRESHOLD_VALID";
	public static final String PARAM_CERTIFIED_COVERAGE_THRESHOLD_PENDING	= "CERTIFIED_COVERAGE_THRESHOLD_PENDING";	
	public static final String PARAM_GUARANTEED_COVERAGE_THRESHOLD_VALID 	= "GUARANTEED_COVERAGE_THRESHOLD_VALID"; // %
	public static final String PARAM_GUARANTEED_COVERAGE_THRESHOLD_PENDING 	= "GUARANTEED_COVERAGE_THRESHOLD_PENDING"; // %
	
	/**
	 * @param t
	 * @param param
	 * @return territory property of type long
	 */
	public static long getLong(Territory t, String param) {
		return (Long) t.getTerritoryData().getOrDefault(param, defaultValues.getOrDefault(param, 0l));
	} 
	/**
	 * 
	 * @param t
	 * @param param
	 * @return territory property of type int
	 */
	public static int getInt(Territory t, String param) {
		return (Integer) t.getTerritoryData().getOrDefault(param, defaultValues.getOrDefault(param, 0));
	} 
	/**
	 * 
	 * @param t
	 * @param param
	 * @return territory property of type double
	 */
	public static double getDouble(Territory t, String param) {
		return (Double) t.getTerritoryData().getOrDefault(param, defaultValues.getOrDefault(param, 0d));
	} 
	
	/**
	 * @param sharedId
	 * @return
	 */
	public static String getDriverTravelId(String sharedId) {
		return "D"+sharedId.substring(1);
	}

	/**
	 * @param sharedId
	 * @return
	 */
	public static String getPassengerTravelId(String sharedId) {
		return "P"+sharedId.substring(1);
	}

	/**
	 * @param sharedId
	 * @return
	 */
	public static boolean isDriver(String sharedId) {
		return sharedId.charAt(0) == 'D';
	}
	
	
	private static final Map<String, Object> defaultValues = new HashMap<>();
	static {
		defaultValues.put(PARAM_WALK_SPEED_THRESHOLD, 7d);
		defaultValues.put(PARAM_WALK_AVG_SPEED_THRESHOLD, 8d);
		defaultValues.put(PARAM_WALK_GUARANTEED_AVG_SPEED_THRESHOLD, 5d);
		
		defaultValues.put(PARAM_BIKE_SPEED_THRESHOLD, 34d);
		defaultValues.put(PARAM_BIKE_AVG_SPEED_THRESHOLD, 27d);
		defaultValues.put(PARAM_BIKE_GUARANTEED_AVG_SPEED_THRESHOLD, 18d);
		
		defaultValues.put(PARAM_VALIDITY_THRESHOLD, 80d);
		defaultValues.put(PARAM_ACCURACY_THRESHOLD, 150d);
		
		defaultValues.put(PARAM_MIN_COVERAGE_THRESHOLD, 30d);
		
		defaultValues.put(PARAM_SHARED_TRIP_DISTANCE_THRESHOLD, 1000d);
		
		defaultValues.put(PARAM_DISTANCE_THRESHOLD, 250d);
		defaultValues.put(DATA_HOLE_THRESHOLD, 10*60);
		defaultValues.put(PARAM_BIKE_DISTANCE_THRESHOLD, 100d);
		defaultValues.put(PARAM_MAX_AVG_SPEED_THRESHOLD, 200d);
		
		defaultValues.put(PARAM_PENDING_COVERAGE_THRESHOLD, 60);
		defaultValues.put(PARAM_COVERAGE_THRESHOLD, 80d);
		defaultValues.put(PARAM_CERTIFIED_COVERAGE_THRESHOLD_VALID, 70);
		defaultValues.put(PARAM_CERTIFIED_COVERAGE_THRESHOLD_PENDING, 50);
		defaultValues.put(PARAM_GUARANTEED_COVERAGE_THRESHOLD_VALID, 90);
		defaultValues.put(PARAM_GUARANTEED_COVERAGE_THRESHOLD_PENDING, 80);
	}
}
