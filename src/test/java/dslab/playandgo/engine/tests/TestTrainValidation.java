package dslab.playandgo.engine.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.mongodb.client.MongoClients;

import it.smartcommunitylab.playandgo.engine.geolocation.model.Geolocation;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationResult.TravelValidity;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationStatus;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.util.GamificationHelper;
import it.smartcommunitylab.playandgo.engine.validation.PTDataHelper;
import it.smartcommunitylab.playandgo.engine.validation.TrackValidator;


public class TestTrainValidation {

	private static final String DB_NAME = "playngo-engine";
	private static final String CONN_URI = "";
	private static final String shapeFolder = "src/test/resources/validation";
	
//	@Test
	public void convertShapes() {
		String s1 = "";
		List<Geolocation> l1 = GamificationHelper.decodePoly(s1);
//		Collections.reverse(l1);
		List<Geolocation> list = new ArrayList<>(l1);
		String s = GamificationHelper.encodePoly(list);
		String res = "";
		int i = 1;
		for (Geolocation g : list) {
			res += "XX," + g.getLatitude()+","+g.getLongitude()+","+i+"\n";
			i++;
		}
		i = 1;
		Collections.reverse(list);
		for (Geolocation g : list) {
			res += "XX," + g.getLatitude()+","+g.getLongitude()+","+i+"\n";
			i++;
		}
		System.err.println(res);
	}
	
//	@Test
	public void testTrainTrip() throws JsonParseException, JsonMappingException, IOException {
		
		Territory t = new Territory();
		t.setTerritoryId("test");
		PTDataHelper helper = new PTDataHelper();
		helper.setShapeFolder(shapeFolder);
		helper.init();
		
		MongoTemplate template = new MongoTemplate(MongoClients.create(CONN_URI), DB_NAME);
		List<TrackedInstance> tracks = template.find(Query.query(
				Criteria.where("territoryId").is("L")
				.and("freeTrackingTransport").is("train")
				.and("validationResult.valid").is(false)
//				.and("_id").in("63fe023b8649a64421ad4417")
				), TrackedInstance.class);
		int invalid = 0, valid = 0;
		for (TrackedInstance instance: tracks) {
			if (instance.getValidationResult().getValidationStatus().getError() != null) {
				continue;
			}
			Collection<Geolocation> track = instance.getGeolocationEvents();
			System.err.println("track ID = " + instance.getId());
			ValidationStatus stat = TrackValidator.validateFreeTrain(track, t);
			System.err.println(stat);
			if (stat.getValidationOutcome().equals(TravelValidity.INVALID)) invalid++;
			else valid++;
		};
		
		System.err.println("INVALID / VALID " +invalid + " / " + valid);
		
		
	}
}
