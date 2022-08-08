package it.smartcommunitylab.playandgo.engine.notification;

public class BadgeNotification extends NotificationGe {
	private String badge;
	private String collectionName;



	@Override
	public String toString() {
		return String.format("[gameId=%s, playerId=%s, badge=%s, collection=%s]", 
				getGameId(), getPlayerId(), getBadge(), getCollectionName());
	}



	public String getBadge() {
		return badge;
	}



	public void setBadge(String badge) {
		this.badge = badge;
	}



	public String getCollectionName() {
		return collectionName;
	}



	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

}
