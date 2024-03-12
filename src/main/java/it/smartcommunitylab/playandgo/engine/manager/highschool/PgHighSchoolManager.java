package it.smartcommunitylab.playandgo.engine.manager.highschool;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;

import it.smartcommunitylab.playandgo.engine.exception.ServiceException;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@Component
public class PgHighSchoolManager {
    private static transient final Logger logger = LoggerFactory.getLogger(PgHighSchoolManager.class);
    

    @Autowired
    @Qualifier("hscClient")
    WebClient hscWebClient;
    
    public String subscribeCampaign(String campaignId, String playerId, String nickname, String teamId)  throws ServiceException {
		try {
			String path = "/api/admin/initiatives/" + campaignId + "/player/subscribe";
	        String url = path + "?nickname=" + nickname + "&teamId=" + teamId;
	        logger.info(String.format("subscribeCampaign uri:%s", url));

			return 
				hscWebClient.post()
 				.uri(uriBuilder -> uriBuilder
					.path(path)
					.queryParam("nickname", UriUtils.encode(nickname, "UTF-8"))
					.queryParam("teamId", teamId)
					.build()) 
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
	    	String path ="/api/admin/initiatives/" + campaignId + "/player/unsubscribe";
			String url = path +	"?nickname=" + nickname;
	    	logger.info(String.format("unsubscribeCampaign uri:%s", url));

			hscWebClient.post()
			.uri(uriBuilder -> uriBuilder
				.path(path)
				.queryParam("nickname", UriUtils.encode(nickname, "UTF-8"))
				.build()) 
			.contentType(MediaType.APPLICATION_JSON)
			.attributes(clientRegistrationId("oauthprovider"))
			.retrieve()
			.bodyToMono(String.class)
			.block();
		} catch (Exception e) {
            throw new ServiceException(e.getMessage(), ErrorCode.EXT_SERVICE_INVOCATION);   
		}   
    }
    
    public void unregisterPlayer(String campaignId, String playerId, String nickname)  throws ServiceException {
        try {
            String path ="/api/admin/initiatives/" + campaignId + "/player/unregister";
			String url = path +	"?nickname=" + nickname + "&playerId=" + playerId;
            logger.info(String.format("unregisterPlayer uri:%s", url));

            hscWebClient.post()
			.uri(uriBuilder -> uriBuilder
				.path(path)
				.queryParam("nickname", UriUtils.encode(nickname, "UTF-8"))
				.queryParam("playerId", playerId)
				.build()) 
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
