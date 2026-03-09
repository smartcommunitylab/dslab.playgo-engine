package it.smartcommunitylab.playandgo.engine.manager.highschool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import it.smartcommunitylab.playandgo.engine.exception.ServiceException;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@Component
public class PgHighSchoolManager {
    private static transient final Logger logger = LoggerFactory.getLogger(PgHighSchoolManager.class);
    

    @Autowired
    @Qualifier("hscClient")
    RestTemplate hscRestTemplate;
    
    public String subscribeCampaign(String campaignId, String playerId, String nickname, String teamId)  throws ServiceException {
		try {
			String path = "/api/admin/initiatives/" + campaignId + "/player/subscribe";
	        
	        String url = UriComponentsBuilder.fromUriString(path)
	            .queryParam("nickname", nickname)
	            .queryParam("teamId", teamId)
	            .toUriString();
	        
	        logger.info(String.format("subscribeCampaign uri:%s", url));

			return hscRestTemplate.postForObject(url, null, String.class);
		} catch (Exception e) {
            throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_INVOCATION);   
		}
    }
    
    public void unsubscribeCampaign(String campaignId, String playerId, String nickname)  throws ServiceException {
		try {
	    	String path ="/api/admin/initiatives/" + campaignId + "/player/unsubscribe";
	    	
	    	String url = UriComponentsBuilder.fromUriString(path)
	            .queryParam("nickname", nickname)
	            .toUriString();
	    	
	    	logger.info(String.format("unsubscribeCampaign uri:%s", url));

			hscRestTemplate.postForObject(url, null, String.class);
		} catch (Exception e) {
            throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_INVOCATION);   
		}   
    }
    
    public void unregisterPlayer(String campaignId, String playerId, String nickname)  throws ServiceException {
        try {
            String path ="/api/admin/initiatives/" + campaignId + "/player/unregister";
            
            String url = UriComponentsBuilder.fromUriString(path)
                .queryParam("nickname", nickname)
                .queryParam("playerId", playerId)
                .toUriString();
            
            logger.info(String.format("unregisterPlayer uri:%s", url));

            hscRestTemplate.postForObject(url, null, String.class);
        } catch (Exception e) {
            throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_INVOCATION);   
        }   
    }
    
}
