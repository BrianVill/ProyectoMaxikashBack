package com.loki.bonos.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.loki.bonos.DTOs.BonoResponseDTO;
import com.loki.bonos.Models.Bono;
import com.loki.bonos.Service.BonoService;

import java.util.List;

@RestController
@RequestMapping("/bonos")
public class BonoController {

    @Autowired
    private BonoService bonoService;

    // Obtener todos los bonos
    @GetMapping
    public List<Bono> getAllBonos() {
        return bonoService.getAllBonos();
    }

    // Obtener bono por ID
    @GetMapping("/{id}")
    public ResponseEntity<BonoResponseDTO> getBonoById(@PathVariable Long id) {
        BonoResponseDTO bono = bonoService.getBonoById(id);
        return ResponseEntity.ok(bono);
    }

    // Crear un nuevo bono
    @PostMapping
    public Bono createBono(@RequestBody Bono bono) {
        return bonoService.saveBono(bono);
    }

    // Actualizar un bono existente
    @PutMapping("/{id}")
    public ResponseEntity<Bono> updateBono(@PathVariable Long id, @RequestBody Bono bonoDetails) {
        Bono updatedBono = bonoService.updateBono(id, bonoDetails);
        return ResponseEntity.ok(updatedBono);
    }

    // Eliminar un bono por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBono(@PathVariable Long id) {
        bonoService.deleteBono(id);
        return ResponseEntity.noContent().build();
    }
}

