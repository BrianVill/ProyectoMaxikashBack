package com.loki.variablesCreditoPorDia.Controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.loki.tasks.CloudTasksService;
import com.loki.tasks.EmptyRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


import com.loki.variablesCreditoPorDia.Models.PuestoCumpleCondicion;
import com.loki.variablesCreditoPorDia.Service.PuestoCumpleCondicionService;

@RestController
@RequestMapping("/puestocumplecondicion")
public class PuestoCumpleCondicionController {

    @Autowired private PuestoCumpleCondicionService svc;
    @Autowired private CloudTasksService           tasksSvc;
    @Autowired private ObjectMapper                objectMapper;

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
            tasksSvc.enqueueTask(resolveBaseUrl() + "/puestocumplecondicion/evaluar/worker", payload);

            return ResponseEntity.ok("✅ Evaluación de condiciones encolada.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    @GetMapping                   public List<PuestoCumpleCondicion> getAll() { return svc.getAll(); }
    @GetMapping("/{puestoId}")    public List<PuestoCumpleCondicion> getPorPuesto(@PathVariable UUID puestoId){
        return svc.getByPuesto(puestoId);
    }

    @PostMapping("/evaluar/worker")
    public ResponseEntity<String> worker() {
        try {
            svc.evaluarPuestosCumplenCondicionesSync();
            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }
}
