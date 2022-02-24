package it.smartcommunitylab.playandgo.engine.notification;

public class ChallengeInvitationCanceledNotification extends NotificationGe {
    private String challengeName;
    private String proposerId;

    @Override
    public String toString() {
        return String.format("[gameId=%s, playerId=%s, proposerId=%s, challengeName=%s]", getGameId(),
                getPlayerId(), proposerId, challengeName);
    }

    public String getChallengeName() {
        return challengeName;
    }

    public void setChallengeName(String challengeName) {
        this.challengeName = challengeName;
    }

    public String getProposerId() {
        return proposerId;
    }

    public void setProposerId(String proposerId) {
        this.proposerId = proposerId;
    }

}
