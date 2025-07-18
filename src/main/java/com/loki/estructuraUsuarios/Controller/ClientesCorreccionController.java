package com.loki.estructuraUsuarios.Controller;

import com.loki.estructuraUsuarios.Models.EstadosAutomatizaciones;
import com.loki.estructuraUsuarios.Service.ClientesCorreccionService;
import com.loki.estructuraUsuarios.Service.EstadosAutomatizacionesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loki.tasks.CloudTasksService;
import com.loki.tasks.StorageService;

import com.loki.estructuraUsuarios.Controller.ClientesCorreccionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/clientes/correccion")
@RequiredArgsConstructor
public class ClientesCorreccionController {

    @Autowired private ClientesCorreccionService   correccionService;
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


    private static final String ESTADO_NOMBRE = "Correccion";
    /* ---------------------------------------------------------------------
       POST /clientes/correccion/actualizar
       --------------------------------------------------------------------- */
       @PostMapping(value="/actualizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
       public ResponseEntity<String> actualizar(
               @RequestPart("file") MultipartFile file,
               @RequestParam(value="mensaje", required=false) String mensaje,
               @RequestParam(value="to",      required=false) List<String> to) {
   
           try {
               actualizarEstado("En Proceso", "");
   
               byte[] bytes = file.getBytes();
               String objectName = storageSvc.save(bytes, "correccion/", ".xlsx");
   
               ClientesCorreccionRequest req = new ClientesCorreccionRequest();
               req.objectName = objectName;
               req.mensaje = mensaje;
               req.to = to;
   
               String payload = objectMapper.writeValueAsString(req);
               tasksSvc.enqueueTask(resolveBaseUrl() + "/clientes/correccion/actualizar/worker", payload);
   
               return ResponseEntity.accepted()
                       .body("Corrección encolada. Revisa el estado con /estados/Correccion.");
   
           } catch (Exception e) {
               actualizarEstado("Error", e.getMessage());
               return ResponseEntity.badRequest()
                       .body("Error al lanzar la corrección: " + e.getMessage());
           }
       }

    /**
     * Worker endpoint invoked by Cloud Tasks.
     */
    @PostMapping("/actualizar/worker")
    public ResponseEntity<String> correccionWorker(@RequestBody ClientesCorreccionRequest req) {

        try {
            byte[] bytes = storageSvc.read(req.objectName);
            correccionService.corregirAsignacionesSync(bytes, req.mensaje, req.to);
            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(e.getMessage());

        }
    }

    /* --------------------------------------------------------------------- */
    /* helper estado */
    private void actualizarEstado(String estado,String msg){
        estadosSvc.actualizar(ESTADO_NOMBRE, new EstadosAutomatizaciones(
                null, ESTADO_NOMBRE, estado, msg, LocalDate.now()));
    }
}
