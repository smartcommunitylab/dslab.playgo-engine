package it.smartcommunitylab.playandgo.engine.model.conf;

import java.util.ArrayList;
import java.util.List;

public class ExtProviderConf {
	String jwksEndpoint;
	String claimName;
	String authUrl;
	String clientId;
    List<String> optionalClaims = new ArrayList<>();   

    public String getJwksEndpoint() {
        return jwksEndpoint;
    }

    public void setJwksEndpoint(String jwksEndpoint) {
        this.jwksEndpoint = jwksEndpoint;
    }

    public String getClaimName() {
        return claimName;
    }

    public void setClaimName(String claimName) {
        this.claimName = claimName;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public List<String> getOptionalClaims() {
        return optionalClaims;
    }

    public void setOptionalClaims(List<String> optionalClaims) {
        this.optionalClaims = optionalClaims;
    }

}
