package it.smartcommunitylab.playandgo.engine.manager;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.exception.StorageException;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.TerritoryRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@Component
public class TerritoryManager {
	@SuppressWarnings("unused")
	private static transient final Logger logger = LoggerFactory.getLogger(TerritoryManager.class);
	
	@Autowired
	TerritoryRepository territoryRepository;
	
	@Autowired
	CampaignRepository campaignRepository;
	
	public void saveTerritory(Territory territory) throws Exception {
		try {
			territoryRepository.save(territory);
			Campaign campaign = new Campaign();
			campaign.setCampaignId(territory.getTerritoryId() + ".personal");
			campaign.setTerritoryId(territory.getTerritoryId());
			campaign.setType(Type.personal);
			campaign.getName().put("it", "Il mio Play&Go");
			campaign.getName().put("en", "My Play&Go");
			campaign.getValidationData().put("means", territory.getTerritoryData().get("means"));
			//TODO compile other fields
			campaignRepository.save(campaign);
		} catch (Exception e) {
			throw new StorageException("territory save error", ErrorCode.ENTITY_SAVE_ERROR);
		}
	}
	
	public void updateTerritory(Territory territory) throws Exception {
		Territory territoryDb = getTerritory(territory.getTerritoryId());
		if(territoryDb == null) {
			throw new BadRequestException("territory doesn't exist", ErrorCode.TERRITORY_NOT_FOUND);
		}
		territoryDb.setName(territory.getName());
		territoryDb.setDescription(territory.getDescription());
		territoryDb.setTerritoryData(territory.getTerritoryData());
		territoryDb.setTimezone(territory.getTimezone());
		territoryRepository.save(territoryDb);
	}
	
	public Territory getTerritory(String territoryId) {
		return territoryRepository.findById(territoryId).orElse(null);
	}
	
	public List<Territory> getTerritories() {
		return territoryRepository.findAll();
	}
	
	public Territory deleteTerritory(String territoryId) throws Exception {
		Territory territory = territoryRepository.findById(territoryId).orElse(null);
		if(territory == null) {
			throw new BadRequestException("territory doesn't exist", ErrorCode.TERRITORY_NOT_FOUND);
		}
		Long count = campaignRepository.countByTerritoryId(territoryId);
		if(count > 0) {
			throw new BadRequestException("territory in use", ErrorCode.TERRITORY_IN_USE);
		}
		territoryRepository.deleteById(territoryId);
		return territory;
	}
}
