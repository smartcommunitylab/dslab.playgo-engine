package it.smartcommunitylab.playandgo.engine.manager.highschool;

import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.playandgo.engine.exception.ServiceException;
import it.smartcommunitylab.playandgo.engine.manager.UnregisterManager;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@Component
public class PgHighSchoolManager {
    private static transient final Logger logger = LoggerFactory.getLogger(PgHighSchoolManager.class);
    
    @Value("${hsc.endpoint}")
    private String endpoint;
    
    @Autowired
    UnregisterManager unregisterManager;
    
    private ObjectMapper mapper = new ObjectMapper();
    
    public String subscribeCampaign(String campaignId, String playerId, String nickname)  throws ServiceException {
        HttpHeaders headers = new HttpHeaders();
        try {
            headers.setBearerAuth(unregisterManager.getBearerToken());
        } catch (Exception e) {
            throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_AUTH);
        }
        HttpEntity<Object> request = new HttpEntity<>(headers);
        
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = null;
        try {
            String url = endpoint.endsWith("/") ? endpoint : endpoint + "/";
            url = url + "api/initiatives/" 
                    + URLEncoder.encode(campaignId, "UTF-8") 
                    + "/player/subscribe?nickname=" + URLEncoder.encode(nickname, "UTF-8");
             response = restTemplate.postForEntity(url, request, String.class);
        } catch (Exception e) {
            throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_INVOCATION);   
        }
        if(!response.getStatusCode().is2xxSuccessful()) {
            throw new ServiceException("External Service invocation result:" + response.getStatusCodeValue(), 
                    ErrorCode.EXT_SERVICE_INVOCATION);
        }
        return response.getBody();
    }
    
    public void unsubscribeCampaign(String campaignId, String playerId)  throws ServiceException {
        //TODO
        HttpHeaders headers = new HttpHeaders();
        try {
            headers.setBearerAuth(unregisterManager.getBearerToken());
        } catch (Exception e) {
            throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_AUTH);
        }
        HttpEntity<Object> request = new HttpEntity<>(headers);
        
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = null;
        try {
            String url = endpoint.endsWith("/") ? endpoint : endpoint + "/";
            url = url + "api/admin/unsubscribe/" 
                    + URLEncoder.encode(campaignId, "UTF-8") 
                    + "/" + URLEncoder.encode(playerId, "UTF-8");
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
