package com.loki.estructuraUsuarios.Controller;

import com.loki.estructuraUsuarios.Models.AsignacionRequest;
import com.loki.estructuraUsuarios.Models.EstadosAutomatizaciones;
import com.loki.estructuraUsuarios.Service.ClientesAsignacionService;
import com.loki.estructuraUsuarios.Service.EstadosAutomatizacionesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loki.tasks.CloudTasksService;
import com.loki.estructuraUsuarios.Controller.ClientesAsignacionRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Lanza la asignación de clientes y permite descargar el XLS resultante.
 */
@RestController
@RequestMapping("/clientes/asignacion")
@RequiredArgsConstructor
public class ClientesAsignacionController {

    /* ────────── inyección ────────── */
    @Autowired private final ClientesAsignacionService      clientesAsignacionService;
    @Autowired private final EstadosAutomatizacionesService estadosAutomatizacionesService;
    @Autowired private final CloudTasksService            tasksSvc;
    @Autowired private final ObjectMapper                 objectMapper;

    @Value("${tasks.worker.base-url}")
    private String baseUrl;

    private String resolveBaseUrl() {
        return (baseUrl == null || baseUrl.isBlank())
                ? ServletUriComponentsBuilder.fromCurrentContextPath()
                        .build().toUriString()
                : baseUrl;
    }


    /* ────────── constantes ────────── */
    private static final String  PROCESO   = "Asignacion";
    private static final String  FILE_PATH = "/tmp/uploads/Clientes.xlsx";
    private static final MediaType XLSX_MIME =
            MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    /* ════════════════════════════════════════════════════════════════
       1.  POST /clientes/asignacion
           Dispara la tarea (responde 202 Accepted)
       ════════════════════════════════════════════════════════════════ */
       @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
       public ResponseEntity<Void> asignarClientes(@RequestBody AsignacionRequest req) {
   
           try {
               /* 1) estado “En Proceso” */
               actualizarEstado("En Proceso", "");
   
               /* 2) encolar la tarea */
               ClientesAsignacionRequest taskReq = new ClientesAsignacionRequest();
               taskReq.threshold = req.getThreshold();
               taskReq.capacity = req.getCapacity();
               taskReq.maxThreshold = req.getMaxThreshold();
               taskReq.mensaje = req.getMensaje();
               taskReq.to = req.getTo();
   
               String payload = objectMapper.writeValueAsString(taskReq);
               tasksSvc.enqueueTask(resolveBaseUrl() + "/clientes/asignacion/worker", payload);
   
               /* 3) respuesta inmediata */
               return ResponseEntity.accepted().build();   // 202
   
           } catch (Exception e) {
               e.printStackTrace();
               actualizarEstado("Error", e.getMessage());
               return ResponseEntity.badRequest().build(); // 400
           }
       }

    /* ════════════════════════════════════════════════════════════════
       2.  GET /clientes/asignacion/archivo
           Devuelve el XLSX generado por el proceso
       ════════════════════════════════════════════════════════════════ */
    @GetMapping("/archivo")
    public ResponseEntity<Resource> descargarArchivo() {

        FileSystemResource file = new FileSystemResource(FILE_PATH);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();      // 404
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=asignacion_resultado.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.getFile().length())
                .contentType(XLSX_MIME)
                .body(file);
    }

    /**
     * Worker endpoint invoked by Cloud Tasks.
     */
    @PostMapping("/worker")
    public ResponseEntity<String> asignacionWorker(@RequestBody ClientesAsignacionRequest req) {
        try {
            clientesAsignacionService.asignarClientesSync(
                    req.threshold,
                    req.capacity,
                    req.maxThreshold,
                    req.mensaje,
                    req.to);
            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(e.getMessage());
        }
    }

    /* ════════════════════════════════════════════════════════════════
       Helper: UPSERT en la tabla estados_automatizaciones
       ════════════════════════════════════════════════════════════════ */
    private void actualizarEstado(String nuevoEstado, String mensaje) {
        EstadosAutomatizaciones ea = new EstadosAutomatizaciones();
        ea.setNombre (PROCESO);
        ea.setEstado (nuevoEstado);
        ea.setMensaje(mensaje);
        ea.setFecha  (LocalDate.now());
        estadosAutomatizacionesService.actualizar(PROCESO, ea);
    }
}
