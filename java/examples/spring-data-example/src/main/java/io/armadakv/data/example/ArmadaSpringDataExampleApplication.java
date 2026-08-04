/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.data.example;

import io.armadakv.core.Client;
import io.armadakv.core.ssl.PemSSLFactoryRefresher;
import io.armadakv.data.configuration.EnableArmadaRepositories;
import io.armadakv.data.example.entity.AdditionalInfo;
import io.armadakv.data.example.entity.TestEntity;
import io.armadakv.data.example.repository.TestRepository;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import nl.altindag.ssl.SSLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import javax.net.ssl.SSLException;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableScheduling
@EnableArmadaRepositories
public class ArmadaSpringDataExampleApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmadaSpringDataExampleApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ArmadaSpringDataExampleApplication.class, args);
    }

    @Bean
    public SSLFactory sslFactory() {
        return SSLFactory.builder()
                .withDummyTrustMaterial()
                .withDummyIdentityMaterial()
                .withSwappableIdentityMaterial()
                .withSwappableTrustMaterial()
                .build();
    }

    @Bean
    public PemSSLFactoryRefresher refreshableSSLFactory(SSLFactory sslFactory) throws FileNotFoundException {
        return new PemSSLFactoryRefresher(
                sslFactory,
                Path.of(ResourceUtils.getURL("classpath:ca.crt").getPath()),
                Path.of(ResourceUtils.getURL("classpath:tls.crt").getPath()),
                Path.of(ResourceUtils.getURL("classpath:tls.key").getPath())
        );
    }

    @Bean
    public Client armadaClient(SSLFactory factory) throws SSLException {
        return Client.builder()
                .target("armada-api:8443")
                .negotiationType(NegotiationType.TLS)
                .sslFactory(factory)
                .build();
    }

    @Service
    public static class Scheduler {
        private final PemSSLFactoryRefresher refreshableSSLFactory;

        public Scheduler(PemSSLFactoryRefresher refreshableSSLFactory) {
            this.refreshableSSLFactory = refreshableSSLFactory;
        }

        @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
        public void checkCertificates() {
            refreshableSSLFactory.tryRefresh();
        }
    }

    @Bean
    public CommandLineRunner example(TestRepository testRepository) {
        return args -> {
            LOGGER.info("Repo count: {}", testRepository.count());
            testRepository.save(new TestEntity("foo", "label", List.of()));
            testRepository.findById("foo")
                    .ifPresent(entity -> LOGGER.info("Entity fetched: {}", entity));
            testRepository.save(new TestEntity("foo", "label", List.of(new AdditionalInfo("foo", "bar"))));
            testRepository.findAll().forEach(
                    entity -> LOGGER.info("Entity fetched: {}", entity)
            );
            testRepository.findByLabel("label").forEach(
                    entity -> LOGGER.info("Entity fetched: {}", entity)
            );
            LOGGER.info("Repo count: {}", testRepository.count());
            testRepository.deleteAll();
        };
    }

}
