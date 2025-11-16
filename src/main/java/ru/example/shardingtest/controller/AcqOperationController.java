package ru.example.shardingtest.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.example.shardingtest.model.AcquiringOperationEntity;
import ru.example.shardingtest.model.AcquiringOperationState;
import ru.example.shardingtest.repository.AcqOperationRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/acq-operation")
@RequiredArgsConstructor
public class AcqOperationController {

    private final AcqOperationRepository repo;

    @PostMapping
    public AcqOperationDto create(@RequestBody AcqOperationDto dto) {
        log.info("saved operation {}", dto);
        var res = repo.save(new AcquiringOperationEntity(dto.clientId, dto.state));
        return new AcqOperationDto(res.getId(), res.getClientId(), res.getState());
    }

    @GetMapping("/{clientId}")
    public List<AcqOperationDto> findForClientId(@PathVariable Long clientId) {
        log.info("Getting operations for client {}", clientId);
        var res = repo.findAllByClientIdOrderByCreatedAtDesc(clientId)
                .stream()
                .map(e -> new AcqOperationDto(e.getId(), e.getClientId(), e.getState()))
                .toList();
        log.info("Found {} operations for client {}", res.size(), clientId);
        return res;
    }


    public record AcqOperationDto(UUID id, Long clientId, AcquiringOperationState state) {}
}
