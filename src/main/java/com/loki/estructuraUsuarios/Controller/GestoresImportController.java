package com.loki.estructuraUsuarios.Controller;

import com.loki.estructuraUsuarios.Models.EstadosAutomatizaciones;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loki.estructuraUsuarios.Service.EstadosAutomatizacionesService;
import com.loki.estructuraUsuarios.Service.GestoresImportService;
import com.loki.tasks.CloudTasksService;
import com.loki.tasks.StorageService;
import com.loki.estructuraUsuarios.Controller.GestoresImportRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/gestores")
public class GestoresImportController {

    private static final String PROCESO = "Gestor";

    @Autowired private GestoresImportService        svc;
    @Autowired private EstadosAutomatizacionesService estadosSvc;
    @Autowired private StorageService               storageSvc;
    @Autowired private CloudTasksService            tasksSvc;
    @Autowired private ObjectMapper                 objectMapper;

    @Value("${tasks.worker.base-url}")
    private String baseUrl;

    private String resolveBaseUrl() {
        return (baseUrl == null || baseUrl.isBlank())
                ? ServletUriComponentsBuilder.fromCurrentContextPath()
                        .build().toUriString()
                : baseUrl;
    }

    /**
     * POST /gestores/import
     *
     * Campos (multipart):
     *  • file     – Excel jerarquía (obligatorio)
     *  • mensaje  – texto opcional que se incluirá en el e-mail
     *  • to       – uno o varios correos destino (repetido o separados por "," / ";")
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String,String>> uploadExcelHierarchy(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mensaje", required = false) String mensaje,
            @RequestParam(value = "to",      required = false) List<String> to) {

        /* 1) registrar estado “En Proceso” */
        estadosSvc.actualizar(PROCESO, new EstadosAutomatizaciones(
                null, PROCESO, "En Proceso", "", LocalDate.now()));

        try {
            byte[] hierarchyBytes = file.getBytes();
            String objectName = storageSvc.save(hierarchyBytes, "gestores/", ".xlsx");

            GestoresImportRequest req = new GestoresImportRequest();
            req.objectName = objectName;
            req.mensaje = mensaje;
            req.to = to;

            String payload = objectMapper.writeValueAsString(req);
            tasksSvc.enqueueTask(resolveBaseUrl() + "/gestores/import/worker", payload);


            return ResponseEntity.accepted().body(
                    Map.of("ok", "Proceso encolado. Consulta /estados/" + PROCESO));

        } catch (IOException ex) {

            /* fallo antes del hilo async */
            estadosSvc.actualizar(PROCESO, new EstadosAutomatizaciones(
                    null, PROCESO, "Error", ex.getMessage(), LocalDate.now()));

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * Worker endpoint invoked by Cloud Tasks.
     */
    @PostMapping("/import/worker")
    public ResponseEntity<String> importWorker(@RequestBody GestoresImportRequest req) {
        Exception err = svc.processFromStorageWithRetry(req.objectName, req.mensaje, req.to);
        if (err == null) {
            return ResponseEntity.ok("ok");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(err.getMessage());
    }
}
