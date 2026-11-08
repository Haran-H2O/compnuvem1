package br.edu.utfpr.compnuvem1.controller;

import br.edu.utfpr.compnuvem1.model.Leitura;
import br.edu.utfpr.compnuvem1.repository.LeituraRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LeituraController {

    private final LeituraRepository repository;

    public LeituraController(LeituraRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/sensores")
    public ResponseEntity<Leitura> criar(@RequestBody Leitura leitura) {
        if (leitura.getSensorId() == null || leitura.getSensorId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (leitura.getTimestamp() == null) {
            leitura.setTimestamp(Instant.now().getEpochSecond());
        }
        repository.salvar(leitura);
        return ResponseEntity.status(HttpStatus.CREATED).body(leitura);
    }

    @GetMapping("/sensores")
    public List<Leitura> listarTodos() {
        return repository.listarTodos();
    }

    @GetMapping("/sensores/{sensorId}")
    public List<Leitura> listarPorSensor(@PathVariable String sensorId) {
        return repository.listarPorSensor(sensorId);
    }

    @GetMapping("/sensores/{sensorId}/{timestamp}")
    public ResponseEntity<Leitura> buscar(@PathVariable String sensorId, @PathVariable long timestamp) {
        return ResponseEntity.of(repository.buscar(sensorId, timestamp));
    }

    @PutMapping("/sensores/{sensorId}/{timestamp}")
    public ResponseEntity<Leitura> atualizar(@PathVariable String sensorId, @PathVariable long timestamp,
            @RequestBody Leitura dados) {
        return ResponseEntity.of(repository.atualizar(sensorId, timestamp, dados));
    }

    @PostMapping("/sensores/{sensorId}/{timestamp}/leituras")
    public ResponseEntity<Leitura> incrementarLeituras(@PathVariable String sensorId, @PathVariable long timestamp) {
        return ResponseEntity.of(repository.incrementarLeituras(sensorId, timestamp));
    }

    @DeleteMapping("/sensores/{sensorId}/{timestamp}")
    public ResponseEntity<Void> remover(@PathVariable String sensorId, @PathVariable long timestamp) {
        return repository.remover(sensorId, timestamp)
                .<ResponseEntity<Void>>map(l -> ResponseEntity.noContent().build())
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/localizacoes/{localizacao}")
    public List<Leitura> listarPorLocalizacao(@PathVariable String localizacao) {
        return repository.listarPorLocalizacao(localizacao);
    }

    @GetMapping("/localizacoes/{localizacao}/sensores/{sensorId}")
    public List<Leitura> listarPorSensorELocalizacao(@PathVariable String localizacao, @PathVariable String sensorId) {
        return repository.listarPorSensorELocalizacao(sensorId, localizacao);
    }
}
