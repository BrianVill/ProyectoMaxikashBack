package com.loki.variablesCreditoPorDia.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.loki.tasks.CloudTasksService;
import com.loki.tasks.EmptyRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


import com.loki.variablesCreditoPorDia.Models.ClientesCumpleBono;
import com.loki.variablesCreditoPorDia.Service.ClientesCumpleBonoService;

@RestController
@RequestMapping("/clientesbonos")
public class ClientesCumpleBonoController {

    @Autowired private ClientesCumpleBonoService svc;
    @Autowired private CloudTasksService       tasksSvc;
    @Autowired private ObjectMapper            objectMapper;

    @Value("${tasks.worker.base-url}")
    private String baseUrl;

    private String resolveBaseUrl() {
        return (baseUrl == null || baseUrl.isBlank())
                ? ServletUriComponentsBuilder.fromCurrentContextPath()
                        .build().toUriString()
                : baseUrl;
    }

    @PostMapping("/evaluar")
    public ResponseEntity<String> evaluar() {
        try {
            String payload = objectMapper.writeValueAsString(new EmptyRequest());
            tasksSvc.enqueueTask(resolveBaseUrl() + "/clientesbonos/evaluar/worker", payload);

            return ResponseEntity.ok("✅ Evaluación de bonos encolada.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    @GetMapping               public List<ClientesCumpleBono> getAll()              { return svc.getAll();       }
    @GetMapping("/{credito}") public List<ClientesCumpleBono> getPorCredito(@PathVariable String credito){
        return svc.getByCredito(credito);
    }

    @PostMapping("/evaluar/worker")
    public ResponseEntity<String> worker() {
        try {
            svc.evaluarClientesCumpleBonoSync();
            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }
}
