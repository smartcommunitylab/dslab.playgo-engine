package it.smartcommunitylab.playandgo.engine.ge;

import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import it.smartcommunitylab.playandgo.engine.config.Const;
import it.smartcommunitylab.playandgo.engine.ge.model.BadgesData;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class BadgeManager {
	
	@Value("${challengeDir}")
	private String challengeDir;
	
	private Map<String, BadgesData> badges;
	
	private ObjectMapper mapper = new ObjectMapper();

	@PostConstruct
	public void init() throws Exception {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		badges = Maps.newTreeMap();
		List<BadgesData> badgeList = mapper.readValue(Paths.get(challengeDir + "/badges.json").toFile(), new TypeReference<List<BadgesData>>() {});
		for (BadgesData badge: badgeList) {
			
			URL resource = getClass().getResource("/static/web/" + badge.getPath());
			byte b[] = Resources.asByteSource(resource).read();

			badge.setImageByte(b);
			badges.put(badge.getTextId(), badge);
		}		
	}
	
	//public Map<String, BadgesData> getAllBadges() {
	//	return getBadges(null);
	//}

	public Map<String, BadgesData> getAllBadges(Campaign campaign) {
		Map<String, BadgesData> result = new HashMap<>();
		for(BadgesData badge: badges.values()) {
			// duplicate badge to avoid modifying original one
			BadgesData newBadge = badge.clone();
			for(String lang: Const.languages) {
				newBadge.getText().put(lang, getBadgeContent(campaign, badge.getText().get(lang), lang));
			}
			result.put(newBadge.getTextId(), newBadge);
		}
		return result;		
	}

	private String getBadgeContent(Campaign campaign, String content, String lang) {
		String pointName = Utils.getPointNameByCampaign(campaign, lang);
		content = content.replace("{ecoLeaves}", pointName);
		return content;
	}
}
