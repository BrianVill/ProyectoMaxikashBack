package com.loki.estructuraUsuarios.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loki.estructuraUsuarios.Models.Nivel;
import com.loki.estructuraUsuarios.Service.NivelService;

@RestController
@RequestMapping("/nivel")
public class NivelController {

    @Autowired
    private NivelService nivelService;

    // Obtener todos los nivel
    @GetMapping
    public ResponseEntity<List<Nivel>> getAllNivel() {
        return ResponseEntity.ok(nivelService.getAllNivel());
    }

    // Obtener un nivel por ID
    @GetMapping("/{id}")
    public ResponseEntity<Nivel> getNivelById(@PathVariable Long id) {
        return ResponseEntity.ok(nivelService.getNivelById(id));
    }

    // Crear un nuevo nivel
    @PostMapping
    public ResponseEntity<Nivel> createNivel(@RequestBody Nivel nivel) {
        Nivel savedNivel = nivelService.saveNivel(nivel);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedNivel);
    }

    // Actualizar un nivel existente
    @PutMapping("/{id}")
    public ResponseEntity<Nivel> updateNivel(@PathVariable Long id, @RequestBody Nivel nivelDetails) {
        Nivel updatedNivel = nivelService.updateNivel(id, nivelDetails);
        return ResponseEntity.ok(updatedNivel);
    }

    // Eliminar un nivel
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNivel(@PathVariable Long id) {
        nivelService.deleteNivel(id);
        return ResponseEntity.noContent().build();
    }
}

