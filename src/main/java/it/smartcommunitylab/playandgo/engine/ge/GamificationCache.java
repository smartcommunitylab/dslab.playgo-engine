package it.smartcommunitylab.playandgo.engine.ge;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.ge.model.GameStatistics;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@Component
public class GamificationCache {
	private static transient final Logger logger = LoggerFactory.getLogger(GamificationCache.class);

	@Autowired
	private GamificationEngineManager gamificationEngineManager;
	
	private LoadingCache<String, String> playerState;
	private LoadingCache<String, String> playerNotifications;
	private LoadingCache<String, List<GameStatistics>> statistics;
	
	private ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);	
	
	@PostConstruct
	public void init() {
		playerState = CacheBuilder.newBuilder()
				.refreshAfterWrite(1, TimeUnit.MINUTES).build(new CacheLoader<String, String>() {
			@Override
			public String load(String id) throws Exception {
				try {
					String[] ids = id.split("@");
					String data = loadPlayerState(ids[0], ids[1]);
					logger.debug("Loaded player state: " + ids[0]);
					return data;
				} catch (Exception e) {
					logger.error("Error populating player state cache: ", e);
					throw e;
				}
			}
			
			@Override
			public ListenableFuture<String> reload(String key, String old) {
				ListenableFutureTask<String> task = ListenableFutureTask.create(new Callable<String>() {
					@Override
					public String call() throws Exception {
						try {
							return load(key);
						} catch (Exception e) {
							logger.error("Returning old value for player state: " + key);
							return old;
						}
					}
				});
				task.run();
				return task;
			}

		});	
		
		playerNotifications = CacheBuilder.newBuilder()
				.refreshAfterWrite(1, TimeUnit.MINUTES).build(new CacheLoader<String, String>() {
			@Override
			public String load(String id) throws Exception {
				try {
					String[] ids = id.split("@");
					String data = loadNotifications(ids[0], ids[1]);
					logger.debug("Loaded player notifications: " + ids[0]);
					return data;
				} catch (Exception e) {
					logger.error("Error populating player notifications cache:", e);
					throw e;
				}
			}
			
			@Override
			public ListenableFuture<String> reload(String key, String old) {
				ListenableFutureTask<String> task = ListenableFutureTask.create(new Callable<String>() {
					@Override
					public String call() throws Exception {
						try {
							return load(key);
						} catch (Exception e) {
							logger.error("Returning old value for notifications: " + key);
							return old;
						}
					}
				});
				task.run();
				return task;
			}

		});			
		
		statistics = CacheBuilder.newBuilder()
				.refreshAfterWrite(1, TimeUnit.MINUTES).build(new CacheLoader<String, List<GameStatistics>>() {
			@Override
			public List<GameStatistics> load(String id) throws Exception {
				try {
					List<GameStatistics> data = loadStatistics(id);
					logger.debug("Loaded statistics: " + id);
					return data;
				} catch (Exception e) {
					logger.error("Error populating statistics cache: ", e);
					throw e;
				}
			}
			
			@Override
			public ListenableFuture<List<GameStatistics>> reload(String key, List<GameStatistics> old) {
				ListenableFutureTask<List<GameStatistics>> task = ListenableFutureTask.create(new Callable<List<GameStatistics>>() {
					@Override
					public List<GameStatistics> call() throws Exception {
						try {
							return load(key);
						} catch (Exception e) {
							logger.error("Returning old value for notifications: " + key);
							return old;
						}
					}
				});
				task.run();
				return task;
			}
		});				
	}	
	
	public String getPlayerState(String playerId, String gameId) {
		try {
			return playerState.get(playerId + "@" + gameId);
		} catch (ExecutionException e) {
			logger.error(String.format("getPlayerState error: %s - %s", gameId, e.getMessage()));
			return null;
		}
	}
	
	public String getPlayerNotifications(String playerId, String gameId) {
		try {
			return playerNotifications.get(playerId + "@" + gameId);
		} catch (ExecutionException e) {
			logger.error(String.format("getPlayerNotifications error: %s - %s", gameId, e.getMessage()));
			return null;
		}
	}	
	
	public void invalidatePlayer(String playerId, String gameId) {
		playerState.invalidate(playerId + "@" + gameId);
		playerNotifications.invalidate(playerId + "@" + gameId);
	}
	
	public List<GameStatistics> getStatistics(String gameId) {
		try {
			return statistics.get(gameId);
		} catch (ExecutionException e) {
			logger.error(String.format("getStatistics error: %s - %s", gameId, e.getMessage()));
			return null;
		}
	}	
	
	private String loadPlayerState(String playerId, String gameId) {
		return gamificationEngineManager.getGameStatus(playerId, gameId);
	}

	private String loadNotifications(String playerId, String gameId) {
		return gamificationEngineManager.getNotifications(playerId, gameId);
	}	
	
	private List<GameStatistics> loadStatistics(String gameId) throws Exception {
		String json = gamificationEngineManager.getStatistics(gameId);
		if(json == null) {
			throw new BadRequestException("error in GE invocation", ErrorCode.EXT_SERVICE_INVOCATION);
		}
		List<GameStatistics> stats = mapper.readValue(json,  new TypeReference<List<GameStatistics>>() {});
		return stats;
	}	
	
	
}
