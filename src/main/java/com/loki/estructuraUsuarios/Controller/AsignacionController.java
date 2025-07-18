package com.loki.estructuraUsuarios.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.loki.estructuraUsuarios.Models.Asignacion;
import com.loki.estructuraUsuarios.Service.AsignacionService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/asignacion")
public class AsignacionController {

    @Autowired
    private AsignacionService asignacionService;

    @GetMapping
    public List<Asignacion> getAllAsignaciones() {
        return asignacionService.getAllAsignaciones();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Asignacion> getAsignacionById(@PathVariable int id) {
        Optional<Asignacion> asignacion = asignacionService.getAsignacionById(id);
        return asignacion.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Asignacion createAsignacion(@RequestBody Asignacion asignacion) {
        return asignacionService.saveAsignacion(asignacion);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Asignacion> updateAsignacion(@PathVariable int id, @RequestBody Asignacion asignacionDetails) {
        Optional<Asignacion> asignacion = asignacionService.getAsignacionById(id);
        if (asignacion.isPresent()) {
            asignacionDetails.setId(id); // Aseguramos que el ID se actualice correctamente
            return ResponseEntity.ok(asignacionService.updateAsignacion(asignacionDetails));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsignacion(@PathVariable int id) {
        asignacionService.deleteAsignacion(id);
        return ResponseEntity.noContent().build();
    }
}

