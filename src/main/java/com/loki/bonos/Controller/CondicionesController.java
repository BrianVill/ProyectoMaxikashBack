package com.loki.bonos.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.loki.bonos.DTOs.CondicionesDTO;
import com.loki.bonos.DTOs.CondicionesResponseDTO;
import com.loki.bonos.Service.CondicionesService;

import java.util.List;

@RestController
@RequestMapping("/condiciones")
public class CondicionesController {

    @Autowired
    private CondicionesService condicionesService;

    @GetMapping
    public List<CondicionesResponseDTO> getAllCondiciones(@RequestParam(value = "b", required = false) Long bonoId) {
    if (bonoId != null) {
        return condicionesService.findByBonoId(bonoId);
    }
    return condicionesService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CondicionesResponseDTO> getCondicionById(@PathVariable Long id) {
        CondicionesResponseDTO condicion = condicionesService.findById(id);
        if (condicion != null) {
            return ResponseEntity.ok(condicion);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public CondicionesDTO createCondicion(@RequestBody CondicionesDTO condicionDTO) {
        return condicionesService.save(condicionDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CondicionesDTO> updateCondicion(@PathVariable Long id, @RequestBody CondicionesDTO condicionDTO) {
        CondicionesDTO updatedCondicion = condicionesService.update(id, condicionDTO);
        if (updatedCondicion != null) {
            return ResponseEntity.ok(updatedCondicion);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCondicion(@PathVariable Long id) {
        condicionesService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // 🔹 Nuevo endpoint para obtener solo condiciones de tipoId = 1 (filtros)
    @GetMapping("/filtros")
    public List<CondicionesResponseDTO> getFiltros() {
        return condicionesService.findFiltros();
    }
}