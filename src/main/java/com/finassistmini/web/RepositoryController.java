package com.finassistmini.web;

import com.finassistmini.dto.*;
import com.finassistmini.service.RepositoryIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/repositories")
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryIngestionService ingestionService;

    @PostMapping("/index")
    public ResponseEntity<RepositoryResponse> index(
            @Valid @RequestBody IndexRepositoryRequest request) {
        log.info("Repository index request received: {}", request.url());
        RepositoryResponse response = ingestionService.submit(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RepositoryDetailsResponse>> list() {
        return ResponseEntity.ok(ingestionService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepositoryDetailsResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ingestionService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ingestionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/summary")
    public ResponseEntity<RepositorySummaryResponse> summary(@PathVariable Long id) {
        return ResponseEntity.ok(ingestionService.getSummary(id));
    }

    @PostMapping("/{id}/reindex")
    public ResponseEntity<RepositoryResponse> reindex(@PathVariable Long id) {
        return ResponseEntity.accepted().body(ingestionService.reindex(id));
    }
}