package it.smartcommunitylab.playandgo.engine.model.conf;

public class CampaignPersonalConf {
    boolean showGeneralInfo;
    TransportPlacingConf transportPlacing;
    TransportStatsConf transportStats;

    public boolean isShowGeneralInfo() {
        return showGeneralInfo;
    }

    public void setShowGeneralInfo(boolean showGeneralInfo) {
        this.showGeneralInfo = showGeneralInfo;
    }

    public TransportPlacingConf getTransportPlacing() {
        return transportPlacing;
    }

    public void setTransportPlacing(TransportPlacingConf transportPlacing) {
        this.transportPlacing = transportPlacing;
    }

    public TransportStatsConf getTransportStats() {
        return transportStats;
    }

    public void setTransportStats(TransportStatsConf transportStats) {
        this.transportStats = transportStats;
    }
}
