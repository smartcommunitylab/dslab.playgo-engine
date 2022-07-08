package it.smartcommunitylab.playandgo.engine.ge.model;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeConceptInfo;
import it.smartcommunitylab.playandgo.engine.manager.challenge.Inventory;

public class PlayerStatus {

	private Map<String, Object> playerData = Maps.newTreeMap();
	private List<PointConcept> pointConcept = Lists.newArrayList();
	private List<BadgeCollectionConcept> badgeCollectionConcept = Lists.newArrayList();
	private ChallengeConceptInfo challengeConcept = new ChallengeConceptInfo();
	private List<PlayerLevel> levels = Lists.newArrayList();
	private Inventory inventory;
	private Boolean canInvite;
	
	public PlayerStatus() {
		super();
	}

	public List<BadgeCollectionConcept> getBadgeCollectionConcept() {
		return badgeCollectionConcept;
	}

	public void setBadgeCollectionConcept(List<BadgeCollectionConcept> badgeCollectionConcept) {
		this.badgeCollectionConcept = badgeCollectionConcept;
	}

	public Map<String, Object> getPlayerData() {
		return playerData;
	}

	public ChallengeConceptInfo getChallengeConcept() {
		return challengeConcept;
	}

	public void setPlayerData(Map<String, Object> playerData) {
		this.playerData = playerData;
	}

	public void setChallengeConcept(ChallengeConceptInfo challengeConcept) {
		this.challengeConcept = challengeConcept;
	}

	public List<PointConcept> getPointConcept() {
		return pointConcept;
	}

	public void setPointConcept(List<PointConcept> pointConcept) {
		this.pointConcept = pointConcept;
	}

	public List<PlayerLevel> getLevels() {
		return levels;
	}

	public void setLevels(List<PlayerLevel> levels) {
		this.levels = levels;
	}

	public Inventory getInventory() {
		return inventory;
	}

	public void setInventory(Inventory inventory) {
		this.inventory = inventory;
	}

	public Boolean getCanInvite() {
		return canInvite;
	}

	public void setCanInvite(Boolean canInvite) {
		this.canInvite = canInvite;
	}
	
}
