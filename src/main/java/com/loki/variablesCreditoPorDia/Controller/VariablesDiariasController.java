package com.loki.variablesCreditoPorDia.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loki.estructuraUsuarios.Models.EstadosAutomatizaciones;
import com.loki.estructuraUsuarios.Service.EstadosAutomatizacionesService;
import com.loki.tasks.CloudTasksService;
import com.loki.tasks.StorageService;
import com.loki.variablesCreditoPorDia.Controller.VariablesDiariasImportRequest;
import com.loki.variablesCreditoPorDia.Service.VariablesDiariasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/variablesdiarias")
public class VariablesDiariasController {

    /* ────────── DI —───────── */
    @Autowired private VariablesDiariasService        svc;
    @Autowired private EstadosAutomatizacionesService estadosSvc;
    @Autowired private StorageService                 storageSvc;
    @Autowired private CloudTasksService              tasksSvc;
    @Autowired private ObjectMapper                   objectMapper;

    /* ────────── config ────────── */
    @Value("${tasks.worker.base-url}") private String baseUrl;

    private String resolveBaseUrl() {
        return (baseUrl == null || baseUrl.isBlank())
               ? ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
               : baseUrl;
    }

    private static final String PROCESO = "Variables";

    /* ══════════════════════════════════════════════════════
       POST /variablesdiarias/import   (encola la tarea)
       ══════════════════════════════════════════════════════ */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importar(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "fecha",   required = false) String fecha,
                                           @RequestParam(value = "mensaje", required = false) String mensaje,
                                           @RequestParam(value = "to",      required = false) List<String> to) {

        estadosSvc.actualizar(PROCESO,
                new EstadosAutomatizaciones(null, PROCESO,
                        "En Proceso", "", LocalDate.now()));

        try {
            byte[] bytes = file.getBytes();
            String objectName = storageSvc.save(bytes, "variablesdiarias/", ".xlsx");

            VariablesDiariasImportRequest req = new VariablesDiariasImportRequest();
            req.objectName = objectName;
            req.fecha      = fecha;
            req.mensaje    = mensaje;
            req.to         = to;

            tasksSvc.enqueueTask(
                    resolveBaseUrl() + "/variablesdiarias/import/worker",
                    objectMapper.writeValueAsString(req));

            return ResponseEntity.accepted()
                    .body("Importación encolada. Consulta /estados/" + PROCESO);

        } catch (Exception ex) {
            estadosSvc.actualizar(PROCESO,
                    new EstadosAutomatizaciones(null, PROCESO,
                            "Error", ex.getMessage(), LocalDate.now()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al iniciar: " + ex.getMessage());
        }
    }

    /* ══════════════════════════════════════════════════════
       Endpoint que invoca Cloud Tasks (worker)
       ══════════════════════════════════════════════════════ */
    @PostMapping("/import/worker")
    public ResponseEntity<String> importWorker(@RequestBody VariablesDiariasImportRequest req) {

        try {
            byte[] bytes = storageSvc.read(req.objectName);
            svc.importarVariablesDiariasSync(bytes,
                                             req.fecha,
                                             req.mensaje,
                                             req.to);
            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(e.getMessage());
        }
    }
}
