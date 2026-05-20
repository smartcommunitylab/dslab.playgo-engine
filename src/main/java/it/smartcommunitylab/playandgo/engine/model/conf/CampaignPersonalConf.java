package it.smartcommunitylab.playandgo.engine.model.conf;

import org.springframework.data.annotation.TypeAlias;

@TypeAlias("CampaignPersonalConf")
public class CampaignPersonalConf extends CampaignConf {
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
