package it.smartcommunitylab.playandgo.engine.notification;

public class ChallengeFailedNotication extends NotificationGe {
	private String challengeName;

	public String getChallengeName() {
		return challengeName;
	}

	public void setChallengeName(String challengeName) {
		this.challengeName = challengeName;
	}

	@Override
	public String toString() {
		return String.format("[gameId=%s, playerId=%s, challengeName=%s]", getGameId(), getPlayerId(), challengeName);
	}

}
