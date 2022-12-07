package it.smartcommunitylab.playandgo.engine.manager.highschool;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import it.smartcommunitylab.playandgo.engine.exception.ServiceException;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@Component
public class PgHighSchoolManager {
    private static transient final Logger logger = LoggerFactory.getLogger(PgHighSchoolManager.class);
    

    @Autowired
    @Qualifier("hscClient")
    WebClient hscWebClient;
    
    public String subscribeCampaign(String campaignId, String playerId, String nickname)  throws ServiceException {
		try {
	        String url ="/api/admin/initiatives/" 
	                + URLEncoder.encode(campaignId, "UTF-8") 
	                + "/player/subscribe?nickname=" + URLEncoder.encode(nickname, "UTF-8");
	        logger.info(String.format("subscribeCampaign uri:%s", url));

			return 
				hscWebClient.post()
				.uri(url)
				.contentType(MediaType.APPLICATION_JSON)
				.attributes(clientRegistrationId("oauthprovider"))
				.retrieve()
				.bodyToMono(String.class)
				.block();
		} catch (Exception e) {
            throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_INVOCATION);   
		}
    }
    
    public void unsubscribeCampaign(String campaignId, String playerId, String nickname)  throws ServiceException {
		try {
	    	String url ="/api/admin/initiatives/" 
	    			+ URLEncoder.encode(campaignId, "UTF-8") 
	    			+ "/player/unsubscribe?nickname=" + URLEncoder.encode(nickname, "UTF-8");
	    	logger.info(String.format("unsubscribeCampaign uri:%s", url));

			hscWebClient.post()
			.uri(url)
			.contentType(MediaType.APPLICATION_JSON)
			.attributes(clientRegistrationId("oauthprovider"))
			.retrieve()
			.bodyToMono(String.class)
			.block();
		} catch (Exception e) {
            throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_INVOCATION);   
		}   
    }
}
