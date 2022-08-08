package it.smartcommunitylab.playandgo.engine.ge;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import it.smartcommunitylab.playandgo.engine.ge.model.BadgesData;

@Component
public class BadgeManager {
	
	@Value("${challengeDir}")
	private String challengeDir;
	
	private Map<String, BadgesData> badges;
	
	private ObjectMapper mapper = new ObjectMapper();

	@PostConstruct
	public void init() throws Exception {
		badges = Maps.newTreeMap();
		List<BadgesData> badgeList = mapper.readValue(Paths.get(challengeDir + "/badges.json").toFile(), new TypeReference<List<BadgesData>>() {});
		for (BadgesData badge: badgeList) {
			
			URL resource = getClass().getResource("/static/web/" + badge.getPath());
			byte b[] = Resources.asByteSource(resource).read();

			badge.setImageByte(b);
			badges.put(badge.getTextId(), badge);
		}		
	}
	
	public Map<String, BadgesData> getAllBadges() {
		return Collections.unmodifiableMap(badges);
	}
}
