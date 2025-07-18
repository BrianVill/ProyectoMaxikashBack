package com.loki.bonos.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.loki.bonos.Models.Tipo;
import com.loki.bonos.Service.TipoService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/tipos")
public class TipoController {

    @Autowired
    private TipoService tipoService;

    @GetMapping
    public List<Tipo> getAllTipos() {
        return tipoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tipo> getTipoById(@PathVariable Long id) {
        Optional<Tipo> tipo = tipoService.findById(id);
        return tipo.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Tipo> createTipo(@RequestBody Tipo tipo) {
        Tipo savedTipo = tipoService.saveTipo(tipo);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTipo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tipo> updateTipo(@PathVariable Long id, @RequestBody Tipo tipo) {
        try {
            Tipo updatedTipo = tipoService.updateTipo(id, tipo);
            return ResponseEntity.ok(updatedTipo);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTipo(@PathVariable Long id) {
        tipoService.deleteTipo(id);
        return ResponseEntity.noContent().build();
    }
}
