package it.smartcommunitylab.playandgo.engine.controller;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.exception.ErrorInfo;
import it.smartcommunitylab.playandgo.engine.exception.UnauthorizedException;

public interface PlayAndGoController {

	static Log logger = LogFactory.getLog(PlayAndGoController.class);	
	
	default void checkId(Long... ids) throws BadRequestException {
		for (Long id : ids) {
			if (id == null) {
				throw new BadRequestException("Null id");
			}
		}
	}
	
	default String getUuid() {
		return UUID.randomUUID().toString();
	}

	default void checkNullId(Long... ids) throws BadRequestException {
		for (Long id : ids) {
			if (id != null) {
				throw new BadRequestException("Not null id");
			}
		}
	}

	@ExceptionHandler(BadRequestException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	default @ResponseBody ErrorInfo badRequest(HttpServletRequest req, Exception e) {
		logger.error("Bad request: " + e.getMessage());
		return new ErrorInfo(req.getRequestURL().toString(), e);
	}	
	
	@ExceptionHandler(UnauthorizedException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	default @ResponseBody ErrorInfo unauthorized(HttpServletRequest req, Exception e) {
		logger.error("Unauthorized: " + e.getMessage());
		return new ErrorInfo(req.getRequestURL().toString(), e);
	}	
	
	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	default @ResponseBody ErrorInfo internalServerError(HttpServletRequest req, Exception e) {
		logger.error("Internal Server Error", e);
		return new ErrorInfo(req.getRequestURL().toString(), e);
	}		
	
	
}
