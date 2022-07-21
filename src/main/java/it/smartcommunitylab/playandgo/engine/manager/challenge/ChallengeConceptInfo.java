package it.smartcommunitylab.playandgo.engine.manager.challenge;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

public class ChallengeConceptInfo {
	
	public enum ChallengeDataType {
		ACTIVE, OLD, PROPOSED, FUTURE
	}

	private Map<ChallengeDataType, List<ChallengesData>> challengeData = Maps.newHashMap();
	private boolean canInvite;
	
	public Map<ChallengeDataType, List<ChallengesData>> getChallengeData() {
		return challengeData;
	}

	public void setChallengeData(Map<ChallengeDataType, List<ChallengesData>> challengeData) {
		this.challengeData = challengeData;
	}

	public boolean isCanInvite() {
		return canInvite;
	}

	public void setCanInvite(boolean canInvite) {
		this.canInvite = canInvite;
	}

}
