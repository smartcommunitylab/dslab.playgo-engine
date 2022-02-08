package it.smartcommunitylab.playandgo.engine.manager;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.repository.TerritoryRepository;

@Component
public class TerritoryManager {
	private static transient final Logger logger = LoggerFactory.getLogger(TerritoryManager.class);
	
	@Autowired
	private TerritoryRepository territoryRepository;
	
	public void saveTerritory(Territory territory) {
		territoryRepository.save(territory);
	}
	
	public Territory getTerritory(String territoryId) {
		return territoryRepository.findById(territoryId).orElse(null);
	}
	
	public List<Territory> getTerritories() {
		return territoryRepository.findAll();
	}
	
	public Territory deleteTerritory(String territoryId) {
		Territory territory = territoryRepository.findById(territoryId).orElse(null);
		if(territory != null) {
			territoryRepository.deleteById(territoryId);
		}
		return territory;
	}
}
