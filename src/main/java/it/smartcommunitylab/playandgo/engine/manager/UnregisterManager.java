package it.smartcommunitylab.playandgo.engine.manager;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.exception.ServiceException;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerGameStatus;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsGame;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsTransport;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;
import it.smartcommunitylab.playandgo.engine.util.HTTPConnector;

@Component
public class UnregisterManager {
	private static transient final Logger logger = LoggerFactory.getLogger(UnregisterManager.class);
   
	@Value("${security.oauth2.resourceserver.jwt.issuer-uri}")
    private String endpoint;
    
	@Value("${aac.client-id}")
    private String clientId;
    
	@Value("${aac.client-secret}")
    private String clientSecret;
	
	@Value("${aac.realm}")
    private String realm;
	
	@Autowired
	PlayerRepository playerRepository;
	
	@Autowired
	AvatarManager avatarManager;
	
	@Autowired
	MongoTemplate mongoTemplate;
	
	ObjectMapper mapper = new ObjectMapper();

	public void unregisterPlayer(Player player) throws Exception {
		Player playerDb = playerRepository.findById(player.getPlayerId()).orElse(null);
		if(playerDb == null) {
			throw new BadRequestException("player not found", ErrorCode.PLAYER_EXISTS);
		}
		if(!playerDb.getDeleted()) {
			deleteAccount(player.getPlayerId());
			String nickname = playerDb.getNickname();
			playerDb.setDeleted(true);
			playerDb.setMail("undefined");
			playerDb.setGivenName("undefined");
			playerDb.setFamilyName("undefined");
			playerDb.setSendMail(false);
			playerDb.setNickname(generateNickname());
			playerRepository.save(playerDb);
			//update stats data
			Query query = new Query(new Criteria("nickname").is(nickname));
			Update update = new Update().set("nickname", playerDb.getNickname());
			mongoTemplate.updateMulti(query, update, PlayerGameStatus.class);
			mongoTemplate.updateMulti(query, update, PlayerStatsGame.class);
			mongoTemplate.updateMulti(query, update, PlayerStatsTransport.class);
			mongoTemplate.updateMulti(query, update, TrackedInstance.class);
			try {
				avatarManager.deleteAvatar(player.getPlayerId());
			} catch (Exception e) {
				logger.warn(String.format("unregisterPlayer[%s]:%s", player.getPlayerId(), e.getMessage()));
			}
		}
		logger.info(String.format("unregisterPlayer:%s", player.getPlayerId()));
	}

	private String generateNickname() {
	    int length = 10;
	    boolean useLetters = true;
	    boolean useNumbers = true;
	    String generatedString = RandomStringUtils.random(length, useLetters, useNumbers);
	    return generatedString;
	}
	
	private void deleteAccount(String userId) throws Exception {
		ResponseEntity<String> token = getToken();
		if(!token.getStatusCode().is2xxSuccessful()) {
			logger.warn(String.format("deleteAccount[%s]: error getting token - %s", userId, token.getStatusCodeValue()));
			throw new ServiceException("error getting token", ErrorCode.EXT_SERVICE_INVOCATION);
		}
		String json = token.getBody();
		JsonNode jsonNode = mapper.readTree(json);
		if(jsonNode.has("access_token")) {
			ResponseEntity<String> response = deleteAccountApi(userId, jsonNode.get("access_token").asText());
			if(!response.getStatusCode().is2xxSuccessful()) {
				logger.warn(String.format("deleteAccount[%s]: error calling api - %s", userId, response.getStatusCodeValue()));
				throw new ServiceException("error calling api", ErrorCode.EXT_SERVICE_INVOCATION);			
			}			
		}
	}
	
	private ResponseEntity<String> getToken() throws Exception {
		String url = new String(endpoint);
		if(!url.endsWith("/")) {
			url = url + "/";
		}
		url = url + "oauth/token";
		RestTemplate restTemplate = buildRestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
		map.add("grant_type", "client_credentials");
		map.add("client_id", clientId);
		map.add("client_secret", clientSecret);
		map.add("scope", "aac.api.users");

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

		ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);		
		return response;
	}
	
	private ResponseEntity<String> deleteAccountApi(String userId, String token)  throws Exception {
		String url = new String(endpoint);
		if(!url.endsWith("/")) {
			url = url + "/";
		}
		url = url + "api/users/" + realm + "/" + userId;
		RestTemplate restTemplate = buildRestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + token);
		HttpEntity<?> request = new HttpEntity<Object>(headers);
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);
		return response; 
	}
	
	private static RestTemplate buildRestTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(5000);
		factory.setReadTimeout(15000);
		return new RestTemplate(factory);
	}

}
