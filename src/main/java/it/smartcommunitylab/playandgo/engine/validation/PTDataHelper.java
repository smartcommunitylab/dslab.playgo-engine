/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import it.smartcommunitylab.playandgo.engine.geolocation.model.Geolocation;
import it.smartcommunitylab.playandgo.engine.geolocation.model.TTDescriptor;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.util.GamificationHelper;

@Component
public class PTDataHelper {
	
	private static final Logger logger = LoggerFactory.getLogger(PTDataHelper.class);
	
	private static PTDataHelper _instance;

	public Map<String, List<List<Geolocation>>> TRAIN_SHAPES_MAP = new HashMap<>();
	public Map<String, List<List<Geolocation>>> BOAT_SHAPES_MAP = new HashMap<>();

	public Map<String, List<String>> TRAIN_POLYLINES_MAP = new HashMap<>();
	public Map<String, List<String>> BOAT_POLYLINES_MAP = new HashMap<>();

	public Map<String, TTDescriptor> BUS_DESCRIPTORS = new HashMap<>();

	@Autowired
	@Value("${validation.shapefolder}")
	private String shapeFolder;	
	
	@PostConstruct
	public void init() {
		_instance = this;
		// TODO async init for all territories ?
	}
	
	/**
	 * @param territoryId
	 * @return
	 */
	public static List<List<Geolocation>> getTrainTracksForTerritory(String territoryId) {
		if (_instance.TRAIN_SHAPES_MAP.get(territoryId) == null) {
			try {
				_instance.initValidationData(territoryId);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return _instance.TRAIN_SHAPES_MAP.get(territoryId);
	}

	/**
	 * @param territoryId
	 * @return
	 */
	public static List<List<Geolocation>> getBoatTracksForTerritory(String territoryId) {
		if (_instance.BOAT_SHAPES_MAP.get(territoryId) == null) {
			try {
				_instance.initValidationData(territoryId);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return _instance.BOAT_SHAPES_MAP.get(territoryId);
	}

	/**
	 * @param territoryId
	 * @return
	 */
	public static List<List<Geolocation>> getBusTracksForTerritory(String territoryId, Collection<Geolocation> geolocations) {
		if (_instance.BUS_DESCRIPTORS.get(territoryId) == null) {
			try {
				_instance.initValidationData(territoryId);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return _instance.BUS_DESCRIPTORS.get(territoryId).filterShapes(geolocations);
	}	
	
	public static Map<String, Object> getPolylines(TrackedInstance instance, String territoryId) throws Exception {
		if (instance.getGeolocationEvents() == null || instance.getGeolocationEvents().size() < 2 || instance.getFreeTrackingTransport() == null) {
			return null;
		}
		Map<String, Object> polys = Maps.newTreeMap();
		switch(instance.getFreeTrackingTransport()) {
		case "bus": 
			polys.put("bus", _instance.BUS_DESCRIPTORS.get(territoryId).filteredPolylines(instance.getGeolocationEvents()));
			break;
		case "train": 
			polys.put("train", _instance.TRAIN_POLYLINES_MAP.get(territoryId));
			break;
		case "boat": 
			polys.put("boat", _instance.BOAT_POLYLINES_MAP.get(territoryId));
			break;
		}
		return polys;
	}	
	
	private void initValidationData(String territoryId) throws Exception{
		List<List<Geolocation>> trainShapes = new ArrayList<>();
		TRAIN_SHAPES_MAP.put(territoryId, trainShapes);
		List<List<Geolocation>> boatShapes = new ArrayList<>();
		BOAT_SHAPES_MAP.put(territoryId, boatShapes);
		List<String> trainPoly = new ArrayList<>();
		TRAIN_POLYLINES_MAP.put(territoryId, trainPoly);
		List<String> boatPoly = new ArrayList<>();
		BOAT_POLYLINES_MAP.put(territoryId, boatPoly);
		
		final File[] trainFiles = (new File(shapeFolder+"/train/" + territoryId)).listFiles();
		if (trainFiles != null) {
			for (File f : trainFiles) {
				trainShapes.addAll(TrackValidator.parseShape(new FileInputStream(f)));
			}
			trainPoly = trainShapes.stream().map(x -> GamificationHelper.encodePoly(x)).collect(Collectors.toList());
		}
		
		final File[] boatFiles = (new File(shapeFolder+"/boat/" + territoryId)).listFiles();
		if (boatFiles != null) {
			for (File f : boatFiles) {
				boatShapes.addAll(TrackValidator.parseShape(new FileInputStream(f)));
			}
			boatPoly = boatShapes.stream().map(x -> GamificationHelper.encodePoly(x)).collect(Collectors.toList());
		}
		
		TTDescriptor bus = new TTDescriptor();
		BUS_DESCRIPTORS.put(territoryId, bus);
		loadBusFolder(new File(shapeFolder+"/bus/" + territoryId), bus);
		bus.build(100);
	}
	
	
	
	/**
	 * @param file
	 * @throws FileNotFoundException 
	 */
	private void loadBusFolder(File file, TTDescriptor bus) throws Exception {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			InputStream shapes = null, stops = null, trips = null, stopTimes = null;
			
			for (File f : files) {
				if (f.isDirectory()) {
					loadBusFolder(f, bus);
				} else {
					if ("stops.txt".equals(f.getName())) stops = new FileInputStream(f);
					if ("shapes.txt".equals(f.getName())) shapes = new FileInputStream(f);
					if ("stop_times.txt".equals(f.getName())) stopTimes = new FileInputStream(f);
					if ("trips.txt".equals(f.getName())) trips = new FileInputStream(f);
				}
			}
			if (shapes != null && stops != null && stopTimes != null && trips != null) {
				bus.load(stops, trips, stopTimes, shapes);
			}
		}
	}

}
