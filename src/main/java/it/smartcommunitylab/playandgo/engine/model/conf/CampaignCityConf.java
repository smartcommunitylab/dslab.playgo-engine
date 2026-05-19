package it.smartcommunitylab.playandgo.engine.model.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CampaignCityConf {
	Map<String, String> pointName = new HashMap<>();
	boolean showGeneralInfo;
	boolean showBadgeSection;
	boolean showUserBlacklist;
    String challengeProposed;
    String challengeAssigned;
    GamePlacingConf generalPlacing; 
    GamePlacingConf groupPlacing; 
    GamePlacingConf topTenPlacing;
    TransportStatsConf transportStats;
    GameStatsConf gameStats;
    boolean userGroups;
    List<Group> groups = new ArrayList<>();  
    boolean useExtAuth;
    ExtProviderConf extProvider;

    public Map<String, String> getPointName() {
        return pointName;
    }

    public void setPointName(Map<String, String> pointName) {
        this.pointName = pointName;
    }

    public boolean isShowGeneralInfo() {
        return showGeneralInfo;
    }

    public void setShowGeneralInfo(boolean showGeneralInfo) {
        this.showGeneralInfo = showGeneralInfo;
    }

    public boolean isShowBadgeSection() {
        return showBadgeSection;
    }

    public void setShowBadgeSection(boolean showBadgeSection) {
        this.showBadgeSection = showBadgeSection;
    }

    public boolean isShowUserBlacklist() {
        return showUserBlacklist;
    }

    public void setShowUserBlacklist(boolean showUserBlacklist) {
        this.showUserBlacklist = showUserBlacklist;
    }

    public String getChallengeProposed() {
        return challengeProposed;
    }

    public void setChallengeProposed(String challengeProposed) {
        this.challengeProposed = challengeProposed;
    }

    public String getChallengeAssigned() {
        return challengeAssigned;
    }

    public void setChallengeAssigned(String challengeAssigned) {
        this.challengeAssigned = challengeAssigned;
    }

    public GamePlacingConf getGeneralPlacing() {
        return generalPlacing;
    }

    public void setGeneralPlacing(GamePlacingConf generalPlacing) {
        this.generalPlacing = generalPlacing;
    }

    public GamePlacingConf getGroupPlacing() {
        return groupPlacing;
    }

    public void setGroupPlacing(GamePlacingConf groupPlacing) {
        this.groupPlacing = groupPlacing;
    }

    public GamePlacingConf getTopTenPlacing() {
        return topTenPlacing;
    }

    public void setTopTenPlacing(GamePlacingConf topTenPlacing) {
        this.topTenPlacing = topTenPlacing;
    }

    public TransportStatsConf getTransportStats() {
        return transportStats;
    }

    public void setTransportStats(TransportStatsConf transportStats) {
        this.transportStats = transportStats;
    }

    public GameStatsConf getGameStats() {
        return gameStats;
    }

    public void setGameStats(GameStatsConf gameStats) {
        this.gameStats = gameStats;
    }

    public boolean isUserGroups() {
        return userGroups;
    }

    public void setUserGroups(boolean userGroups) {
        this.userGroups = userGroups;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public boolean isUseExtAuth() {
        return useExtAuth;
    }

    public void setUseExtAuth(boolean useExtAuth) {
        this.useExtAuth = useExtAuth;
    }

    public ExtProviderConf getExtProvider() {
        return extProvider;
    }

    public void setExtProvider(ExtProviderConf extProvider) {
        this.extProvider = extProvider;
    }
}
