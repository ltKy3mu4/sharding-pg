package ru.example.shardingtest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.example.shardingtest.model.AcquiringOperationEntity;
import ru.example.shardingtest.model.AcquiringOperationState;

import java.util.List;
import java.util.UUID;

@Repository
public interface AcqOperationRepository extends JpaRepository<AcquiringOperationEntity, UUID> {

    List<AcquiringOperationEntity> findAllByClientIdOrderByCreatedAtDesc(Long clientId);

    @Query(value = "SELECT a FROM AcquiringOperationEntity a WHERE a.state = :state ORDER BY a.createdAt DESC")
    List<AcquiringOperationEntity> findAllByState(@Param("state") AcquiringOperationState state);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM AcquiringOperationEntity a")
    void deleteAllData();
}

