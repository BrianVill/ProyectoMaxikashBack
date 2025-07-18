package com.loki.variablesCreditoPorDia.Controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.loki.tasks.CloudTasksService;
import com.loki.tasks.EmptyRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


import com.loki.variablesCreditoPorDia.Models.PuestoCumpleBono;
import com.loki.variablesCreditoPorDia.Service.PuestoCumpleBonoService;

@RestController
@RequestMapping("/puestocumplebono")
public class PuestoCumpleBonoController {

    @Autowired private PuestoCumpleBonoService svc;
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
            tasksSvc.enqueueTask(resolveBaseUrl() + "/puestocumplebono/evaluar/worker", payload);

            return ResponseEntity.ok("✅ Evaluación de bonos encolada.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    @GetMapping                    public List<Map<String,Object>> getAll()              { return svc.getAll(); }
    @GetMapping("/{puestoId}")     public List<Map<String,Object>> getPorPuesto(@PathVariable UUID puestoId){
        return svc.getByPuesto(puestoId);
    }

    /* lectura enriquecida opcional */
    @GetMapping("/fechas")
    public List<Map<String,Object>> getEntreFechas(
            @RequestParam @DateTimeFormat(pattern="dd/MM/yyyy") LocalDate ini,
            @RequestParam @DateTimeFormat(pattern="dd/MM/yyyy") LocalDate fin){
        return svc.getByFechaBetween(ini, fin);
    }

    @PostMapping("/evaluar/worker")
    public ResponseEntity<String> worker() {
        try {
            svc.evaluarPuestosCumplenBonosSync();
            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }
}
