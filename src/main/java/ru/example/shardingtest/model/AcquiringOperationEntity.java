package ru.example.shardingtest.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "acq_operation")
@Getter
@Setter
@NoArgsConstructor
public class AcquiringOperationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    private AcquiringOperationState state;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AcquiringOperationEntity(Long clientId, AcquiringOperationState state) {
        this.clientId = clientId;
        this.state = state;
        this.createdAt = LocalDateTime.now();
    }
}
