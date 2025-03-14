package it.smartcommunitylab.playandgo.engine.manager.azienda;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.playandgo.engine.exception.ServiceException;
import it.smartcommunitylab.playandgo.engine.report.TransportStat;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class PgAziendaleManager {
	private static transient final Logger logger = LoggerFactory.getLogger(PgAziendaleManager.class);
	
	public static final String useMultiLocationKey = "useMultiLocation";
	public static final String useEmployeeLocationKey = "useEmployeeLocation";
	
	@Value("${aziende.endpoint}")
	private String endpoint;
	
	@Value("${aziende.user}")
	private String user;
	
	@Value("${aziende.password}")
	private String password;
    
	@Autowired
    RestTemplate restTemplate;
	
	private String jwt = null;
	private long expiration;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	private String getJwt() throws Exception {
		if((jwt == null) || (System.currentTimeMillis() > (expiration - 5000))) {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			AuthRequest authRequest = new AuthRequest(user, password);
			HttpEntity<AuthRequest> request = new HttpEntity<>(authRequest, headers);
			String url = endpoint.endsWith("/") ? endpoint : endpoint + "/";
			url = url + "api/authenticate";
			String response = restTemplate.postForObject(url, request, String.class);
			JsonNode root = mapper.readTree(response);
			jwt = root.get("id_token").asText();
			String[] chunks = jwt.split("\\.");
			Base64.Decoder decoder = Base64.getUrlDecoder();
			String payload = new String(decoder.decode(chunks[1]));
			JsonNode rootNode = mapper.readTree(payload);
			expiration = rootNode.get("exp").asLong() * 1000;			
		}
		return jwt;
	}
	
	public void subscribeCampaign(String campaignId, String playerId, String companyKey, String code)  throws ServiceException {
		HttpHeaders headers = new HttpHeaders();
		try {
			headers.setBearerAuth(getJwt());
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_AUTH);
		}
		HttpEntity<Object> request = new HttpEntity<>(headers);
		
		ResponseEntity<String> response = null;
		try {
			String url = endpoint.endsWith("/") ? endpoint : endpoint + "/";
			url = url + "api/admin/subscribe/" 
					+ campaignId + "/" + playerId 
					+ "/" + companyKey + "/" + code;
			response = restTemplate.postForEntity(url, request, String.class);
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_INVOCATION);	
		}
		if(!response.getStatusCode().is2xxSuccessful()) {
		    try {
                Map<String, Object> map = mapper.readValue(response.getBody(), new TypeReference<>() {});
                throw new ServiceException((String)map.get("message"), (String)map.get("type"));
            } catch (JsonProcessingException e) {
                throw new ServiceException(response.getBody(), response.getBody()); 
            }
		}
	}
	
	public void unsubscribeCampaign(String campaignId, String playerId)  throws ServiceException {
		HttpHeaders headers = new HttpHeaders();
		try {
			headers.setBearerAuth(getJwt());
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_AUTH);
		}
		HttpEntity<Object> request = new HttpEntity<>(headers);
		
		ResponseEntity<String> response = null;
		try {
			String url = endpoint.endsWith("/") ? endpoint : endpoint + "/";
			url = url + "api/admin/unsubscribe/" + campaignId + "/" + playerId;
			response = restTemplate.postForEntity(url, request, String.class);
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_INVOCATION);	
		}
		if(!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceException("External Service invocation result:" + response.getStatusCodeValue(), 
					ErrorCode.EXT_SERVICE_INVOCATION);
		}
	}
	
	public TrackResult validateTrack(String campaignId, String playerId, TrackData trackData) throws ServiceException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		try {
			headers.setBearerAuth(getJwt());
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_AUTH);
		}
		HttpEntity<TrackData> request = new HttpEntity<>(trackData, headers);
		ResponseEntity<String> response = null;		
		try {
			String url = endpoint.endsWith("/") ? endpoint : endpoint + "/";
			url = url + "api/admin/validate/" + campaignId + "/" + playerId;
			response = restTemplate.postForEntity(url, request, String.class);
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_INVOCATION);	
		}
		if(!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceException("External Service invocation result:" + response.getStatusCodeValue(), 
					ErrorCode.EXT_SERVICE_INVOCATION);
		}
		try {
		    logger.info(String.format("validateTrack response:%s - %s - %s", playerId, trackData.toString(), response.getBody()));
			TrackResult trackResult = mapper.readValue(response.getBody(), TrackResult.class);
			return trackResult;
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_INVOCATION);
		}
	}
	
	public TrackResult updateTrack(String campaignId, String playerId, String trackId, double increment) throws ServiceException {
		HttpHeaders headers = new HttpHeaders();
		try {
			headers.setBearerAuth(getJwt());
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_AUTH);
		}
		HttpEntity<Object> request = new HttpEntity<>(headers);
		
		ResponseEntity<String> response = null;
		try {
			String url = endpoint.endsWith("/") ? endpoint : endpoint + "/";
			url = url + "api/admin/update/"	+ campaignId + "/" + playerId 
			        + "/" + trackId	+ "/" + String.valueOf(increment);
			response = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_INVOCATION);	
		}
		if(!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceException("External Service invocation result:" + response.getStatusCodeValue(), 
					ErrorCode.EXT_SERVICE_INVOCATION);
		}
		try {
			TrackResult trackResult = mapper.readValue(response.getBody(), TrackResult.class);
			return trackResult;
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_INVOCATION);
		}
	}

	public TrackResult invalidateTrack(String campaignId, String playerId, String trackId) throws ServiceException {
		HttpHeaders headers = new HttpHeaders();
		try {
			headers.setBearerAuth(getJwt());
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_AUTH);
		}
		HttpEntity<Object> request = new HttpEntity<>(headers);
		
		ResponseEntity<String> response = null;
		try {
			String url = endpoint.endsWith("/") ? endpoint : endpoint + "/";
			url = url + "api/admin/invalidate/" 
					+ campaignId + "/" + playerId + "/" + trackId;
			response = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_INVOCATION);	
		}
		if(!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceException("External Service invocation result:" + response.getStatusCodeValue(), 
					ErrorCode.EXT_SERVICE_INVOCATION);
		}
		try {
			TrackResult trackResult = mapper.readValue(response.getBody(), TrackResult.class);
			return trackResult;
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_INVOCATION);
		}
	}
	
	public List<TransportStat> getPlayerTransportStats(String playerId, String campaignId, String groupMode, String metric,
			String mean, String dateFrom, String dateTo) throws ServiceException {
		HttpHeaders headers = new HttpHeaders();
		try {
			headers.setBearerAuth(getJwt());
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_AUTH);
		}
		HttpEntity<Object> request = new HttpEntity<>(headers);
		
		ResponseEntity<String> response = null;
		try {
			String url = endpoint.endsWith("/") ? endpoint : endpoint + "/";
			url = url + "api/admin/report/player/transport/stats?campaignId=" + campaignId 
					+ "&playerId=" + playerId;
			if(Utils.isNotEmpty(groupMode)) {
			    url = url + "&groupMode=" + groupMode;  
			}
			if(Utils.isNotEmpty(mean)) {
			    url = url + "&mean=" + mean;
			}
			if(Utils.isNotEmpty(dateFrom) && Utils.isNotEmpty(dateTo)) {
			    url = url + "&dateFrom=" + dateFrom
			            + "&dateTo=" + dateTo;
			}		
			response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_INVOCATION);	
		}
		if(!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceException("External Service invocation result:" + response.getStatusCodeValue(), 
					ErrorCode.EXT_SERVICE_INVOCATION);
		}
		try {
			List<TransportStat> result = mapper.readValue(response.getBody(), new TypeReference<List<TransportStat>>() {});
			return result;
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_INVOCATION);
		}
	}

    public void unregisterPlayer(String playerId) throws ServiceException {
        HttpHeaders headers = new HttpHeaders();
        try {
            headers.setBearerAuth(getJwt());
        } catch (Exception e) {
            throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_AUTH);
        }
        HttpEntity<Object> request = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = null;
        try {
            String url = endpoint.endsWith("/") ? endpoint : endpoint + "/";
            url = url + "api/admin/unregister/player/" + playerId; 
            response = restTemplate.postForEntity(url, request, String.class);
        } catch (Exception e) {
            throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_INVOCATION);   
        }
        if(!response.getStatusCode().is2xxSuccessful()) {
            throw new ServiceException("External Service invocation result:" + response.getStatusCodeValue(), 
                    ErrorCode.EXT_SERVICE_INVOCATION);
        } 
    }

}
