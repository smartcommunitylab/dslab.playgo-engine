package dslab.playandgo.engine.verificationtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.playandgo.engine.PlayGoEngineApplication;
import it.smartcommunitylab.playandgo.engine.geolocation.model.Geolocation;
import it.smartcommunitylab.playandgo.engine.geolocation.model.GeolocationsEvent;
import it.smartcommunitylab.playandgo.engine.geolocation.model.Location;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationResult;
import it.smartcommunitylab.playandgo.engine.manager.TerritoryManager;
import it.smartcommunitylab.playandgo.engine.manager.TrackedInstanceManager;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.validation.GeolocationsProcessor;
import it.smartcommunitylab.playandgo.engine.validation.TrackValidator;
import it.smartcommunitylab.playandgo.engine.validation.ValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootApplication(exclude = EmbeddedMongoAutoConfiguration.class)
@SpringBootTest(classes = PlayGoEngineApplication.class)
@Import(EmbeddedMongoTestConfig.class)
public class MainVerificationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public GeolocationsProcessor geolocationsProcessor() {
            return new GeolocationsProcessor();
        }

        @Bean
        public ValidationService validationService() {
            return new ValidationService();
        }

        @Bean
        public TrackValidator trackValidator() {
            return new TrackValidator();
        }

    }

    @Autowired
    private GeolocationsProcessor geolocationsProcessor;

    @Autowired
    private ValidationService validationService;

    private TrackValidator trackValidator;

    @Autowired
    private TrackedInstanceManager trackedInstanceManager;

    @Autowired
    private TerritoryManager territoryManager;


    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void testValidationServices() throws Exception {

        Player player = createTestPlayer();
        Territory territory = createTestTerritory();
        territoryManager.saveTerritory(territory);
        ObjectMapper objectMapper = new ObjectMapper();
        int count = 0;

        Path folderPath = Paths.get("src/test/resources/testverificationfiles/");

        if (!Files.isDirectory(folderPath)) {
            throw new IllegalArgumentException("Folder path does not exist or is not a directory: " + folderPath);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath)) {
            for (Path filePath : stream) {

                String fileName = filePath.toString();
                String jsonContent = readJsonFromFile(fileName);
                GeolocationsEvent geolocationsEvent = objectMapper.readValue(jsonContent, GeolocationsEvent.class);

                List<TrackedInstance> trackedInstances = geolocationsProcessor.storeGeolocationEvents(geolocationsEvent, player);

                assertNotNull(trackedInstances, "Tracked instances should not be null");
                assertFalse(trackedInstances.isEmpty(), "Tracked instances should not be empty");

                List<ValidationResult> vrList = new ArrayList<>();
                for (TrackedInstance trackedInstance : trackedInstances) {
                    ValidationResult vr = validationService.validateFreeTracking(trackedInstance.getGeolocationEvents(), trackedInstance.getFreeTrackingTransport(), trackedInstance.getTerritoryId());
                    assertThat(vr.getValidationStatus().getValidationOutcome()).isEqualTo(ValidationResult.TravelValidity.VALID);
                    vrList.add(vr);
                }

                assertNotNull(vrList, "Validation results list should not be null");
                count++;
                System.out.println("Test done:" + count);

            }
        }

    }

    private static String readJsonFromFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    private static Player createTestPlayer() {
        Player player = new Player();
        player.setPlayerId("12345");
        player.setNickname("testPlayer");
        player.setTerritoryId("T12345");
        player.setGivenName("Mario");
        return player;
    }

    private static Territory createTestTerritory() {
        Territory territory = new Territory();
        territory.setTerritoryId("T12345");
        Map<String, String> name = new HashMap<>();
        name.put("it", "Territorio di prova");
        name.put("en", "Test territory");
        territory.setName(name);
        return territory;
    }


}
