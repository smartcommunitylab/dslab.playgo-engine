/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.playandgo.engine.security;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import it.smartcommunitylab.playandgo.engine.exception.UnauthorizedException;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerRole;
import it.smartcommunitylab.playandgo.engine.model.PlayerRole.Role;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRoleRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;
import it.smartcommunitylab.playandgo.engine.util.Utils;

/**
 * @author raman
 *
 */
@Service
public class SecurityHelper {

	static Log logger = LogFactory.getLog(SecurityHelper.class);
	
    @Value("${spring.security.oauth2.resourceserver.jwt.client-id}")
    private String jwtAudience;

	@Autowired
	private PlayerRepository playerRepository; 
	
	@Autowired
	private PlayerRoleRepository playerRoleRepository;
	
	LoadingCache<String, List<PlayerRole>> roleCache = 
			CacheBuilder.newBuilder()
			.expireAfterWrite(30, TimeUnit.MINUTES)
			.build(new CacheLoader<String, List<PlayerRole>>() {
				@Override
				public List<PlayerRole> load(String key) throws Exception {
					List<PlayerRole> list = playerRoleRepository.findByPreferredUsername(key);
					if(list.isEmpty()) {
						list = playerRoleRepository.findByPlayerId(key);
					}
					return list; 
				}
	});
	LoadingCache<String, Player> playerCache = 
			CacheBuilder.newBuilder()
			.expireAfterWrite(30, TimeUnit.MINUTES)
			.build(new CacheLoader<String, Player>() {
				@Override
				public Player load(String key) throws Exception {
					return playerRepository.findById(key).orElse(null);
				}
	});
	
	public String getCurrentSubject() throws UnauthorizedException {
		JwtAuthenticationToken authentication = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		Jwt principal = (Jwt) authentication.getPrincipal();
		String subject = principal.getSubject();
		if(Utils.isEmpty(subject)) {
			throw new UnauthorizedException("subject not found", ErrorCode.SUBJECT_NOT_FOUND);
		}
		return subject;
	}
	
	public String getGivenName() {
		JwtAuthenticationToken authentication = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		Jwt principal = (Jwt) authentication.getPrincipal();
		return principal.getClaimAsString("given_name");		
	}
	
	public String getFamilyName() {
		JwtAuthenticationToken authentication = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		Jwt principal = (Jwt) authentication.getPrincipal();
		return principal.getClaimAsString("family_name");		
	}
	
	public String getCurrentPreferredUsername() throws UnauthorizedException {
		JwtAuthenticationToken authentication = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		Jwt principal = (Jwt) authentication.getPrincipal();
		String subject = principal.getClaimAsString("preferred_username");
		if(Utils.isEmpty(subject)) {
			throw new UnauthorizedException("preferred_username not found", ErrorCode.SUBJECT_NOT_FOUND);
		}
		return subject.toLowerCase();
	}
	
	public Player getCurrentPlayer() throws UnauthorizedException {
		String subject = getCurrentSubject();		
		Player player = readPlayer(subject);
		if(player == null) {
			throw new UnauthorizedException("user not found", ErrorCode.PLAYER_NOT_FOUND);
		}
		return player;
	}

	public Player getCurrentPlayerOrNUll() throws UnauthorizedException {
		String subject = getCurrentSubject();		
		Player player = playerRepository.findById(subject).orElse(null);
		return player;
	}	
	
	public void checkAdminRole() throws UnauthorizedException {
		String username = getCurrentPreferredUsername();
		PlayerRole r = readRoleByUsernameAndRole(username, Role.admin);
		if(r == null) {
			throw new UnauthorizedException("role not found", ErrorCode.ROLE_NOT_FOUND);
		}
	}
	public void checkAPIRole() throws UnauthorizedException {
		JwtAuthenticationToken authentication = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		Jwt principal = (Jwt) authentication.getPrincipal();
		String identity = principal.getClaimAsString("preferred_username");
		if(Utils.isEmpty(identity)) {
			if (principal.getAudience().contains(jwtAudience)) return;
			identity = principal.getSubject();
		}
		
		PlayerRole r = readRoleByUsernameAndRole(identity, Role.admin);
		if(r == null) {
			throw new UnauthorizedException("role not found", ErrorCode.ROLE_NOT_FOUND);
		}
	}
	
	public void checkRole(Role role, String entityId) throws UnauthorizedException {
		String username = getCurrentPreferredUsername();
		PlayerRole r = readRoleByUsernameAndRole(username, Role.admin);
		if(r == null) {
			r = readRoleByUsernameAndRoleAndEntity(username, role, entityId);
			if(r == null) {
				throw new UnauthorizedException("role not found", ErrorCode.ROLE_NOT_FOUND);
			}
		}
	}
	
	public void checkRole(String terriotryId, String campaignId) throws UnauthorizedException {
		String username = getCurrentPreferredUsername();
		PlayerRole r = readRoleByUsernameAndRole(username, Role.admin);
		if(r != null) {
			return;
		}
		if(Utils.isNotEmpty(terriotryId)) {
			r = readRoleByUsernameAndRoleAndEntity(username, Role.territory, terriotryId);
			if(r != null) {
				return;
			}
		}
		if(Utils.isNotEmpty(campaignId)) {
			r = readRoleByUsernameAndRoleAndEntity(username, Role.campaign, campaignId);
			if(r != null) {
				return;
			}
		}
		throw new UnauthorizedException("role not found", ErrorCode.ROLE_NOT_FOUND);
 	}

	private Player readPlayer(String subject) {
		try {
			return playerCache.get(subject);
		} catch (Exception e) {
			return null;
		}
		// return playerRepository.findById(subject).orElse(null);
	}
	
	private PlayerRole readRoleByUsernameAndRole(String username, Role role) {
		try {
			return roleCache.get(username).stream().filter(r -> role.equals(r.getRole())).findAny().orElse(null);
		} catch (ExecutionException e) {
			return null;
		}
		// playerRoleRepository.findFirstByPreferredUsernameAndRole(username, role)
	}
	private PlayerRole readRoleByUsernameAndRoleAndEntity(String username, Role role, String entityId) {
		try {
			return roleCache.get(username).stream().filter(r -> role.equals(r.getRole()) && entityId.equals(r.getEntityId())).findAny().orElse(null);
		} catch (ExecutionException e) {
			return null;
		}
		// playerRoleRepository.findFirstByPreferredUsernameAndRole(username, role)
	}

	public void invalidateUserRoleCache(String username) {
		roleCache.invalidate(username);
	}
	
	public void invalidatePlayerCache(String subject) {
		playerCache.invalidate(subject);
	}
	
}
