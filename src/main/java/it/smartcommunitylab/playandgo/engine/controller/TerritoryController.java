package it.smartcommunitylab.playandgo.engine.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.engine.manager.TerritoryManager;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.model.PlayerRole.Role;

@RestController
public class TerritoryController extends PlayAndGoController {
	@SuppressWarnings("unused")
	private static transient final Logger logger = LoggerFactory.getLogger(TerritoryController.class);
	
	@Autowired
	private TerritoryManager territoryManager;
	
	@PostMapping("/api/territory")
	public void saveTerritory(
			@RequestBody Territory territory,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		territoryManager.saveTerritory(territory);
	}
	
	@PutMapping("/api/territory")
	public void updateTerritory(
			@RequestBody Territory territory,
			HttpServletRequest request) throws Exception {
		checkRole(request, Role.territory, territory.getTerritoryId());
		territoryManager.updateTerritory(territory);
	}
	
	@GetMapping("/api/territory/{territoryId}")
	public Territory getTerritory(
			@PathVariable String territoryId,
			HttpServletRequest request) throws Exception {
		return territoryManager.getTerritory(territoryId);
	}
	
	@GetMapping("/api/territory")
	public List<Territory> getTerritories(HttpServletRequest request) throws Exception {
		return territoryManager.getTerritories();
	}
	
    @GetMapping("/publicapi/territory")
    public List<Territory> getPublicTerritories(HttpServletRequest request) throws Exception {
        return territoryManager.getTerritories();
    }
    
	@DeleteMapping("/api/territory/{territoryId}")
	public Territory deleteTerritory(
			@PathVariable String territoryId,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		return territoryManager.deleteTerritory(territoryId);
	}
}
