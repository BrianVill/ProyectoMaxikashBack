package com.loki.estructuraUsuarios.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.loki.estructuraUsuarios.Models.EstadosAutomatizaciones;
import com.loki.estructuraUsuarios.Service.EstadosAutomatizacionesService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/estados")
public class EstadosAutomatizacionesController {

    @Autowired
    private EstadosAutomatizacionesService service;

    @GetMapping
    public List<EstadosAutomatizaciones> obtenerTodos() {
        return service.obtenerTodos();
    }

    @GetMapping("/{nombre}")
    public ResponseEntity<EstadosAutomatizaciones> obtenerPorId(@PathVariable String nombre) {
        Optional<EstadosAutomatizaciones> estado = service.obtenerPorNombre(nombre);
        return estado.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EstadosAutomatizaciones> crear(@RequestBody EstadosAutomatizaciones estado) {
        EstadosAutomatizaciones nuevoEstado = service.guardar(estado);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoEstado);
    }

    @PutMapping("/{nombre}")
    public ResponseEntity<?> actualizar(@PathVariable String nombre, @RequestBody EstadosAutomatizaciones nuevoEstado) {
        try {
            EstadosAutomatizaciones estadoActualizado = service.actualizar(nombre, nuevoEstado);
            return ResponseEntity.ok(estadoActualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<EstadosAutomatizaciones> obtenerPorNombre(@PathVariable String nombre) {
        Optional<EstadosAutomatizaciones> estado = service.obtenerPorNombre(nombre);
        return estado.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
