package com.loki.estructuraUsuarios.Controller;

import com.loki.estructuraUsuarios.Models.EstadosAutomatizaciones;
import com.loki.estructuraUsuarios.Service.ClientesImportService;
import com.loki.estructuraUsuarios.Service.EstadosAutomatizacionesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loki.tasks.CloudTasksService;
import com.loki.tasks.StorageService;

import com.loki.estructuraUsuarios.Controller.ClientesImportRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
/**
 * Controlador para importar clientes de nivel 1 desde un Excel.
 */
@RestController
@RequestMapping("/clientes")
public class ClientesImportController {

    private static final String PROCESO = "Clientes";
    @Autowired
    private ClientesImportService svc;
    @Autowired
    private EstadosAutomatizacionesService estadosSvc;
    @Autowired
    private StorageService storageSvc;
    @Autowired
    private CloudTasksService tasksSvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Value("${tasks.worker.base-url}")
    private String baseUrl;

    private String resolveBaseUrl() {
        return (baseUrl == null || baseUrl.isBlank())
                ? ServletUriComponentsBuilder.fromCurrentContextPath()
                        .build().toUriString()
                : baseUrl;
    }

    /**
     * POST /clientes/import
     *
     * Campos:
     * file – Excel
     * mensaje – texto opcional para el e-mail
     * to – correos destino (repetido o separados por , ;)
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> importClientes(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "mensaje", required = false) String mensaje,
            @RequestParam(value = "to", required = false) List<String> to) {

        estadosSvc.actualizar(PROCESO, new EstadosAutomatizaciones(
                null, PROCESO, "En Proceso", "", LocalDate.now()));

        try {
            byte[] bytes = file.getBytes();
            String objectName = storageSvc.save(bytes, "clientes/", ".xlsx");

            ClientesImportRequest req = new ClientesImportRequest();
            req.objectName = objectName;
            req.mensaje = mensaje;
            req.to = to;

            String payload = objectMapper.writeValueAsString(req);
            tasksSvc.enqueueTask(resolveBaseUrl() + "/clientes/import/worker", payload);

            return ResponseEntity.accepted().body(
                    Map.of("ok", "Proceso encolado. Consulta /estados/" + PROCESO));

        } catch (Exception ex) {

            estadosSvc.actualizar(PROCESO, new EstadosAutomatizaciones(
                    null, PROCESO, "Error", ex.getMessage(), LocalDate.now()));

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * Worker endpoint invoked by Cloud Tasks.
     * 
     */
    @PostMapping("/import/worker")
    public ResponseEntity<String> importWorker(@RequestBody ClientesImportRequest req) {
        Exception err = svc.processFromStorageWithRetry(req.objectName, req.mensaje, req.to);
        return (err == null)
                ? ResponseEntity.ok("ok")
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err.getMessage());
    }

}
