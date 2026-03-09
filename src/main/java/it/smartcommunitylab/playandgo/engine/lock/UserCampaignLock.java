package it.smartcommunitylab.playandgo.engine.lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.exception.ServiceException;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@Component
public class UserCampaignLock {

    private static class LockWrapper {
        private final Lock lock = new ReentrantLock();
        private final AtomicInteger numberOfThreadsInQueue = new AtomicInteger(1);    
        
        private LockWrapper addThreadInQueue() {
            numberOfThreadsInQueue.incrementAndGet(); 
            return this;
        }

        private int removeThreadFromQueue() {
            return numberOfThreadsInQueue.decrementAndGet(); 
        }    
    }

    public static final long LOCK_TIMEOUT_S = 60L; // 60 seconds
    private static ConcurrentHashMap<String, LockWrapper> locks = new ConcurrentHashMap<String, LockWrapper>();
    
    public void lock(String key) throws ServiceException {
        LockWrapper lockWrapper = locks.compute(key, (k, v) -> v == null ? new LockWrapper() : v.addThreadInQueue());
        try {
            boolean result = lockWrapper.lock.tryLock(LOCK_TIMEOUT_S, java.util.concurrent.TimeUnit.SECONDS); // avoid deadlock
            if (!result) {
                throw new ServiceException("Timeout acquiring lock for key: " + key, ErrorCode.CONCURRENT_TIMEOUT);
            }
        } catch (InterruptedException e) {
            throw new ServiceException(e.getMessage(), ErrorCode.CONCURRENT_TIMEOUT);
        }
    }
    
    public void unlock(String key) {
        LockWrapper lockWrapper = locks.get(key);
        if(lockWrapper != null) {
            lockWrapper.lock.unlock();
            if (lockWrapper.removeThreadFromQueue() == 0) { 
                // NB : We pass in the specific value to remove to handle the case where another thread would queue right before the removal
                locks.remove(key, lockWrapper);
            }
        }
    }
    
    public String getKey(String playerId, String campaignId) {
        return playerId + "__" + campaignId;
    }
}

