package ru.example.shardingtest;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.example.shardingtest.model.AcquiringOperationState;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@SpringBootTest
@Testcontainers
@DirtiesContext
@Slf4j
class TestcontainersParentTest {

    @Container
    static PostgreSQLContainer<?> postgresS1 = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("acq")
            .withUsername("test")
            .withPassword("test")
            .withExposedPorts(5432)
            .withCreateContainerCmdModifier(cmd ->
                    cmd.withHostConfig(new HostConfig().withPortBindings(
                            new PortBinding(Ports.Binding.bindPort(5433),
                                    new ExposedPort(5432))
                    ))
            )
            .withInitScript("db/changelog/v1/create_table.sql");


    @Container
    static PostgreSQLContainer<?> postgresS2 = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("acq")
            .withUsername("test")
            .withPassword("test")
            .withExposedPorts(5432)
            .withCreateContainerCmdModifier(cmd ->
                    cmd.withHostConfig(new HostConfig().withPortBindings(
                            new PortBinding(Ports.Binding.bindPort(5434),
                                    new ExposedPort(5432))
                    ))
            )
            .withInitScript("db/changelog/v1/create_table.sql");


    protected Connection getConnectionToShard(int shardIndex) throws SQLException {
        PostgreSQLContainer<?> container = (shardIndex == 0) ? postgresS1 : postgresS2;
        return DriverManager.getConnection(
                container.getJdbcUrl(),
                container.getUsername(),
                container.getPassword()
        );
    }


    @SneakyThrows
    protected void populateShardDirectly(PostgreSQLContainer<?> container, int shardIdx, long total, long batchSize) {
        String url = container.getJdbcUrl();
        String username = container.getUsername();
        String password = container.getPassword();

        Random random = new Random();
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            log.info("Populating shard{} via direct connection...", shardIdx);


            for (int batch = 0; batch < total / batchSize; batch++) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO acq_operation (id, client_id, state, created_at) VALUES (?, ?, ?, ?)")) {

                    for (int i = 0; i < batchSize; i++) {
                        //if shardIdx = 0 -> clientId will be odd (0,2,4..) else (1,3..)
                        long clientId = random.nextLong(10) * 2 + shardIdx;

                        ps.setObject(1, UUID.randomUUID());
                        ps.setLong(2, clientId);
                        ps.setString(3, AcquiringOperationState.values()[random.nextInt(AcquiringOperationState.values().length)].name());
                        ps.setTimestamp(4, Timestamp.valueOf(getRandomCreatedAt()));

                        ps.addBatch();
                    }

                    ps.executeBatch();
                }

                if (batch % 100 == 0) {
                    log.info("Shard{}: Processed {} records...", shardIdx, (batch + 1) * batchSize);
                }
            }
        }

        log.info("Completed populating shard{}", shardIdx);
    }



    private LocalDateTime getRandomCreatedAt() {
        return LocalDateTime.now()
                .minusDays((long) (Math.random() * 365))
                .minusHours((long) (Math.random() * 24))
                .minusMinutes((long) (Math.random() * 60));
    }
}
