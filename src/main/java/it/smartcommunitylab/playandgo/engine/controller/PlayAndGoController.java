package it.smartcommunitylab.playandgo.engine.controller;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.exception.ErrorInfo;
import it.smartcommunitylab.playandgo.engine.exception.UnauthorizedException;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerRole;
import it.smartcommunitylab.playandgo.engine.model.PlayerRole.Role;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRoleRepository;

public class PlayAndGoController {

	static Log logger = LogFactory.getLog(PlayAndGoController.class);
	
	@Autowired
	private PlayerRepository playerRepository; 
	
	@Autowired
	private PlayerRoleRepository playerRoleRepository;
	
	public Player getCurrentPlayer(HttpServletRequest request) throws UnauthorizedException {
		Player player = playerRepository.findById("117").orElse(null);
		if(player == null) {
			throw new UnauthorizedException("user not found");
		}
		return player;
	}
	
	public void checkAdminRole(HttpServletRequest request) throws UnauthorizedException {
		Player player = getCurrentPlayer(request);
		PlayerRole r = playerRoleRepository.findByPlayerIdAndRole(player.getPlayerId(), Role.admin);
		if(r == null) {
			throw new UnauthorizedException("role not found");
		}
	}
	
	public void checkRole(HttpServletRequest request, Role role, String entityId) throws UnauthorizedException {
		Player player = getCurrentPlayer(request);
		PlayerRole r = playerRoleRepository.findByPlayerIdAndRoleAndEntityId(player.getPlayerId(), role, entityId);
		if(r == null) {
			throw new UnauthorizedException("role not found");
		}
	}
	
	public void checkId(Long... ids) throws BadRequestException {
		for (Long id : ids) {
			if (id == null) {
				throw new BadRequestException("Null id");
			}
		}
	}
	
	public String getUuid() {
		return UUID.randomUUID().toString();
	}

	public void checkNullId(Long... ids) throws BadRequestException {
		for (Long id : ids) {
			if (id != null) {
				throw new BadRequestException("Not null id");
			}
		}
	}

	@ExceptionHandler(BadRequestException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody ErrorInfo badRequest(HttpServletRequest req, Exception e) {
		logger.error("Bad request: " + e.getMessage());
		return new ErrorInfo(req.getRequestURL().toString(), e);
	}	
	
	@ExceptionHandler(UnauthorizedException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public @ResponseBody ErrorInfo unauthorized(HttpServletRequest req, Exception e) {
		logger.error("Unauthorized: " + e.getMessage());
		return new ErrorInfo(req.getRequestURL().toString(), e);
	}	
	
	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody ErrorInfo internalServerError(HttpServletRequest req, Exception e) {
		logger.error("Internal Server Error", e);
		return new ErrorInfo(req.getRequestURL().toString(), e);
	}		
	
	
}
