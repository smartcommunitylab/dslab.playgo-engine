package dslab.playandgo.engine.tests;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import it.smartcommunitylab.playandgo.engine.lock.UserCampaignLock;

public class TestLock {
    
    @Test
    public void testUserCampaignLock() throws InterruptedException {
        UserCampaignLock campaignLock = new UserCampaignLock();
        int executionTimes = 10;
        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (int i = 0; i < executionTimes; i++) {
            executor.submit(() -> {
                String key = campaignLock.getKey("playerId", "campaignId");
                try {
                    System.out.println("Thread " + Thread.currentThread().getName() + " trying to acquire lock for key: " + key);
                    campaignLock.lock(key);
                    System.out.println("Thread " + Thread.currentThread().getName() + " acquired lock for key: " + key);
                    // Simulate some work with the locked resource
                    Thread.sleep(2000);
                } catch (Exception e) {
                    System.out.println("Thread " + Thread.currentThread().getName() + e.getMessage());
                } finally {
                    campaignLock.unlock(key);
                    System.out.println("Thread " + Thread.currentThread().getName() + " released lock for key: " + key);
                }
            });
        } 
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.MINUTES);
    }
}
