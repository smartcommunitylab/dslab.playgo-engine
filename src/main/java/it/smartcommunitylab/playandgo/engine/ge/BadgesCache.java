package it.smartcommunitylab.playandgo.engine.ge;

import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import it.smartcommunitylab.playandgo.engine.ge.model.BadgesData;

public class BadgesCache {

	private Map<String, BadgesData> badges;
	
	public BadgesCache(String path) throws Exception {
		badges = Maps.newTreeMap();
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		List<BadgesData> list = mapper.readValue(Paths.get(path).toFile(), new TypeReference<List<BadgesData>>() {
		});
		for (BadgesData badge: list) {
			
			URL resource = getClass().getResource("/static/web/" + badge.getPath());
			byte b[] = Resources.asByteSource(resource).read();

			badge.setImageByte(b);					
			badges.put(badge.getTextId(), badge);
		}
	}
	
	public BadgesData getBadge(String name) {
		return badges.get(name);
	}
	
	public List<BadgesData> getAllBadges() {
		return Lists.newArrayList(badges.values());
	}	
	
}
