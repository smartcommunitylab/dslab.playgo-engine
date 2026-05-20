package it.smartcommunitylab.playandgo.engine.model.conf;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.TypeAlias;

@TypeAlias("CampaignSchoolConf")
public class CampaignSchoolConf extends CampaignConf {
	Map<String, String> pointName = new HashMap<>();
	boolean showGeneralInfo;
	boolean showBadgeSection;
	boolean showUserBlacklist;
    String challengeProposed;
    String challengeAssigned;
    GamePlacingConf gamePlacing;
    TransportStatsConf transportStats;
    GameStatsConf gameStats;

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

    public GamePlacingConf getGamePlacing() {
        return gamePlacing;
    }

    public void setGamePlacing(GamePlacingConf gamePlacing) {
        this.gamePlacing = gamePlacing;
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
    
}
