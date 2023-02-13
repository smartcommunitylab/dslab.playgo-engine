package it.smartcommunitylab.playandgo.engine.ge.model;

import java.util.List;

public class BadgeCollectionConcept {

	private String id;
	private String name;
	private List<BadgeConcept> badgeEarned;
	private boolean hidden = false;

	public String getName() {
		return name;
	}

	public List<BadgeConcept> getBadgeEarned() {
		return badgeEarned;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setBadgeEarned(List<BadgeConcept> badgeEarned) {
		this.badgeEarned = badgeEarned;
	}
	
	public BadgeCollectionConcept() {
		super();
	}

	public BadgeCollectionConcept(String name, List<BadgeConcept> badgeEarned) {
		super();
		this.name = name;
		this.badgeEarned = badgeEarned;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

}
