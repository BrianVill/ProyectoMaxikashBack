package com.loki.variablesCreditoPorDia.Controller;

import com.loki.estructuraUsuarios.Models.EstadosAutomatizaciones;
import com.loki.estructuraUsuarios.Service.EstadosAutomatizacionesService;
import com.loki.variablesCreditoPorDia.DTOs.ClientesCumpleFiltrosDTO;
import com.loki.variablesCreditoPorDia.Service.ClientesCumpleFiltrosService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loki.tasks.CloudTasksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/clientesfiltros")
public class ClientesCumpleFiltrosController {

    private static final String PROCESO     = "EvaluarBonos";
    private static final String URL_ESTADOS = "/estados/" + PROCESO;

    @Autowired private ClientesCumpleFiltrosService  svc;
    @Autowired private EstadosAutomatizacionesService estadosSvc;
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


    /* ---------------------------------------------------------------------
       POST /clientesfiltros/evaluar
       ▸ acepta opcionalmente:
         – mensaje : texto libre para el e-mail
         – to      : uno o varios correos destino (repetido o “a,b;c”)
       --------------------------------------------------------------------- */
    @PostMapping("/evaluar")
    public ResponseEntity<Map<String,String>> evaluar(
            @RequestParam(value="mensaje", required=false) String mensaje,
            @RequestParam(value="to",      required=false) List<String> to) {

        estadosSvc.actualizar(PROCESO, new EstadosAutomatizaciones(
                null, PROCESO, "En Proceso", "", LocalDate.now()));

        try {
            ClientesCumpleFiltrosRequest req = new ClientesCumpleFiltrosRequest();

            req.mensaje = mensaje;
            req.to = to;

            String payload = objectMapper.writeValueAsString(req);
            tasksSvc.enqueueTask(resolveBaseUrl() + "/clientesfiltros/evaluar/worker", payload);

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                                 .header(HttpHeaders.LOCATION, URL_ESTADOS)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(Map.of(
                                         "mensaje",
                                         "Proceso encolado. Consulta el estado en " + URL_ESTADOS
                                 ));

        } catch (Exception e) {

            estadosSvc.actualizar(PROCESO, new EstadosAutomatizaciones(
                    null, PROCESO, "Error", e.getMessage(), LocalDate.now()));

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(Map.of(
                                         "error",   "Error al lanzar el proceso",
                                         "detalle", e.getMessage()
                ));
        }
    }

    /**
     * Worker endpoint invoked by Cloud Tasks.
     */
    @PostMapping("/evaluar/worker")
    public ResponseEntity<String> evaluarWorker(@RequestBody ClientesCumpleFiltrosRequest req) {
        try {
            svc.evaluarClientesCumpleFiltrosSync(req.mensaje, req.to);
            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(e.getMessage());
        }
    }

    /* ------------------------ LECTURA ------------------------ */
    @GetMapping
    public List<ClientesCumpleFiltrosDTO> getAll(){ return svc.getAll(); }

    @GetMapping("/{credito}")
    public List<ClientesCumpleFiltrosDTO> getPorCredito(@PathVariable String credito){
        return svc.getByCredito(credito);
    }

    /* ------------------------ DELETE ------------------------- */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        return svc.delete(id) ? ResponseEntity.noContent().build()
                              : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
