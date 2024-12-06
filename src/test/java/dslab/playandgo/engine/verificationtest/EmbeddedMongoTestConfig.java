package dslab.playandgo.engine.verificationtest;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.MongoTemplate;

import javax.annotation.PreDestroy;

@TestConfiguration
public class EmbeddedMongoTestConfig {

    private MongodProcess mongodProcess;

    @Bean
    public MongoClient mongoClient() throws Exception {
        MongodExecutable mongodExecutable = MongodStarter.getDefaultInstance()
                .prepare(new MongodConfigBuilder()
                        .version(Version.Main.PRODUCTION)
                        .net(new Net("localhost", 27017, false))
                        .build());

        mongodProcess = mongodExecutable.start();

        return MongoClients.create("mongodb://localhost:27017");
    }

    @Bean
    @Scope("prototype")
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(mongoClient(), "test");
    }

    // Cleanup the MongoDB process after tests are done
    @PreDestroy
    public void stopMongo() {
        if (mongodProcess != null) {
            mongodProcess.stop();
        }
    }
}
