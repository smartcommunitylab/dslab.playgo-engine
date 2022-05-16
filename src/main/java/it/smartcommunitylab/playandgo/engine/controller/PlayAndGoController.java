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
import it.smartcommunitylab.playandgo.engine.exception.PlayAndGoException;
import it.smartcommunitylab.playandgo.engine.exception.UnauthorizedException;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerRole.Role;
import it.smartcommunitylab.playandgo.engine.security.SecurityHelper;

public class PlayAndGoController {

	static Log logger = LogFactory.getLog(PlayAndGoController.class);
	
	@Autowired
	private SecurityHelper securityHelper;
	
	public String getCurrentSubject(HttpServletRequest request) throws UnauthorizedException {
		return securityHelper.getCurrentSubject();
	}
	
	public String getGivenName(HttpServletRequest request) {
		return securityHelper.getGivenName();		
	}
	
	public String getFamilyName(HttpServletRequest request) {
		return securityHelper.getFamilyName();		
	}
	
	public String getCurrentPreferredUsername(HttpServletRequest request) throws UnauthorizedException {
		return securityHelper.getCurrentPreferredUsername();
	}
	
	public Player getCurrentPlayer(HttpServletRequest request) throws UnauthorizedException {
		return securityHelper.getCurrentPlayer();
	}
	
	public Player getCurrentPlayerOrNUll(HttpServletRequest request) throws UnauthorizedException {
		return securityHelper.getCurrentPlayerOrNUll();
	}	
	
	public void checkAdminRole(HttpServletRequest request) throws UnauthorizedException {
		securityHelper.checkAdminRole();
	}
	public void checkAPIRole(HttpServletRequest request) throws UnauthorizedException {
		securityHelper.checkAPIRole();
	}
	
	public void checkRole(HttpServletRequest request, Role role, String entityId) throws UnauthorizedException {
		securityHelper.checkRole(role, entityId);
	}
	
	public void checkRole(HttpServletRequest request, String terriotryId, String campaignId) throws UnauthorizedException {
		securityHelper.checkRole(terriotryId, campaignId);
 	}
	
	public void invalidateUserRoleCache(String username) {
		securityHelper.invalidateUserRoleCache(username);
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
	public @ResponseBody ErrorInfo badRequest(HttpServletRequest req, PlayAndGoException e) {
		logger.error(String.format("Bad request: %s [%s]", req.getRequestURL().toString(), e.getMessage()));
		return new ErrorInfo(req.getRequestURL().toString(), e);
	}	
	
	@ExceptionHandler(UnauthorizedException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public @ResponseBody ErrorInfo unauthorized(HttpServletRequest req, PlayAndGoException e) {
		logger.error(String.format("Unauthorized: %s [%s]", req.getRequestURL().toString(), e.getMessage()));
		return new ErrorInfo(req.getRequestURL().toString(), e);
	}	
	
	@ExceptionHandler(PlayAndGoException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody ErrorInfo genericError(HttpServletRequest req, PlayAndGoException e) {
		logger.error(String.format("Internal Server Error PG: %s [%s]", req.getRequestURL().toString(), e.getMessage()));
		return new ErrorInfo(req.getRequestURL().toString(), e);
	}
	
	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody ErrorInfo internalServerError(HttpServletRequest req, Exception e) {
		logger.error(String.format("Internal Server Error: %s [%s]", req.getRequestURL().toString(), e.getMessage()));
		return new ErrorInfo(req.getRequestURL().toString(), null, e);
	}
	
	
	
}
