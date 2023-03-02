package dslab.playandgo.engine.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class TestBatchRevalidate {

	private static final String base = "https://backend.playngo.it/playandgo";
	private static final String token = "";
	
//	@Test
	public void revalidate() throws IOException {
		RestTemplate template = new RestTemplate();
		List<String> list = Files.readAllLines(Path.of("src/test/resources/revalidate.csv"));
		list.subList(1, list.size()).stream().forEach(l -> {
			String[] array = l.split(",");
			if (array.length < 3) return;
			try {
				HttpHeaders headers = new HttpHeaders();
				headers.add("Authorization", "Bearer " + token);
				HttpEntity<?> entity = new HttpEntity<>(headers);
				template.exchange(base + "/api/dev/track/revalidate?territoryId={tId}&playerId={pId}&trackedInstanceId={iId}", HttpMethod.GET, entity, String.class, array[0], array[1], array[2]);
			} catch (RestClientException e) {
				e.printStackTrace();
			}
		});
		
	}
}
