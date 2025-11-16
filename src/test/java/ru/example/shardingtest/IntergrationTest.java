package ru.example.shardingtest;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ru.example.shardingtest.model.AcquiringOperationEntity;
import ru.example.shardingtest.model.AcquiringOperationState;
import ru.example.shardingtest.repository.AcqOperationRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class IntergrationTest extends TestcontainersParentTest {

    @Autowired
    AcqOperationRepository repository;

    @Test
    public void simpleSave_Test1(){
        var e = repository.save(new AcquiringOperationEntity(1L, AcquiringOperationState.COMPLETED));
        Assertions.assertTrue(this.entityExistsInShard(1, e.getId()));
        Assertions.assertFalse(this.entityExistsInShard(0, e.getId()));
    }

    @Test
    public void simpleSave_Test2(){
        var e = repository.save(new AcquiringOperationEntity(10L, AcquiringOperationState.COMPLETED));
        Assertions.assertFalse(this.entityExistsInShard(1, e.getId()));
        Assertions.assertTrue(this.entityExistsInShard(0, e.getId()));
    }

    //Problem: search data by all shards -> sharding shpere handle it
    @Test
    public void searchAcrossShards_Tes1(){
        var e1 = repository.save(new AcquiringOperationEntity(1L, AcquiringOperationState.COMPLETED));
        repository.save(new AcquiringOperationEntity(1L, AcquiringOperationState.REGISTERED));
        var e2 = repository.save(new AcquiringOperationEntity(2L, AcquiringOperationState.COMPLETED));
        repository.save(new AcquiringOperationEntity(2L, AcquiringOperationState.REGISTERED));

        List<AcquiringOperationEntity> l = repository.findAllByState(AcquiringOperationState.COMPLETED);
        Assertions.assertEquals(2, l.size());
        Assertions.assertTrue(l.stream().anyMatch(e -> e.getId().equals(e1.getId())));
        Assertions.assertTrue(l.stream().anyMatch(e -> e.getId().equals(e2.getId())));
    }

    //Problem: resharding. how to solve it?

    @BeforeEach
    void cleanUp(){
        repository.deleteAll();
    }

//    @Test
//    public void gettingDataFromAllShards(){
////        super.populateShardDirectly(postgresS1, 0, 1_000_000, 100_000);
////        super.populateShardDirectly(postgresS2, 1,1_000_000, 100_000);
//        List<AcquiringOperationEntity> l = repository.findAllByClientIdOrderByCreatedAtDesc(2L);
//        Assertions.assertFalse(l.isEmpty());
//        List<AcquiringOperationEntity> l2 = repository.findAllByClientIdOrderByCreatedAtDesc(1L);
//        Assertions.assertTrue(l2.isEmpty());
//
//
//    }

    @SneakyThrows
    protected boolean entityExistsInShard(int shardIndex, UUID entityId) {
        try (Connection conn = super.getConnectionToShard(shardIndex);
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM acq_operation WHERE id = ?")) {
            stmt.setObject(1, entityId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

}
