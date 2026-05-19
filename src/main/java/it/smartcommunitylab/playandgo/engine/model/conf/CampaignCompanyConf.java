package it.smartcommunitylab.playandgo.engine.model.conf;

import java.util.HashMap;
import java.util.Map;

public class CampaignCompanyConf {
	boolean showGeneralInfo;
	boolean hideCompanyDesc;
	Map<String, String> registrationCompanyDesc = new HashMap<>();
	boolean useMultiLocation;
	boolean useEmployeeLocation;
	VirtualScoreConf virtualScoreConf;
    TransportPlacingConf transportPlacing;
    TransportStatsConf transportStats;

    public boolean isShowGeneralInfo() {
        return showGeneralInfo;
    }

    public void setShowGeneralInfo(boolean showGeneralInfo) {
        this.showGeneralInfo = showGeneralInfo;
    }

    public boolean isHideCompanyDesc() {
        return hideCompanyDesc;
    }

    public void setHideCompanyDesc(boolean hideCompanyDesc) {
        this.hideCompanyDesc = hideCompanyDesc;
    }

    public Map<String, String> getRegistrationCompanyDesc() {
        return registrationCompanyDesc;
    }

    public void setRegistrationCompanyDesc(Map<String, String> registrationCompanyDesc) {
        this.registrationCompanyDesc = registrationCompanyDesc;
    }

    public boolean isUseMultiLocation() {
        return useMultiLocation;
    }

    public void setUseMultiLocation(boolean useMultiLocation) {
        this.useMultiLocation = useMultiLocation;
    }

    public boolean isUseEmployeeLocation() {
        return useEmployeeLocation;
    }

    public void setUseEmployeeLocation(boolean useEmployeeLocation) {
        this.useEmployeeLocation = useEmployeeLocation;
    }

    public VirtualScoreConf getVirtualScoreConf() {
        return virtualScoreConf;
    }

    public void setVirtualScoreConf(VirtualScoreConf virtualScoreConf) {
        this.virtualScoreConf = virtualScoreConf;
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
