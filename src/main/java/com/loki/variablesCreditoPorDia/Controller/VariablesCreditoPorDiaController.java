package com.loki.variablesCreditoPorDia.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.loki.variablesCreditoPorDia.DTOs.VariablesCreditoPorDiaDTO;
import com.loki.variablesCreditoPorDia.Exceptions.ResourceNotFoundException;
import com.loki.variablesCreditoPorDia.Service.VariablesCreditoPorDiaService;

import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/variablesdiarias")
public class VariablesCreditoPorDiaController {

    @Autowired
    private VariablesCreditoPorDiaService service;

    // ✅ Obtener una variable de crédito por ID
    @GetMapping("/{id}")
    public ResponseEntity<VariablesCreditoPorDiaDTO> getById(@PathVariable Long id) {
        Optional<VariablesCreditoPorDiaDTO> entity = service.getById(id);
        return entity.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // ✅ Obtener todas las variables de crédito
    @GetMapping
    public ResponseEntity<List<VariablesCreditoPorDiaDTO>> getAll() {
        List<VariablesCreditoPorDiaDTO> allEntities = service.getAll();
        return ResponseEntity.ok(allEntities);
    }

    // ✅ Eliminar una variable de crédito por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean deleted = service.delete(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ✅ Manejo de excepciones personalizadas
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @DeleteMapping("/eliminartodo")
    public ResponseEntity<String> deleteAll() {
        service.deleteAll();
        return ResponseEntity.ok("✅ Todos los registros de VariablesCreditoPorDia han sido eliminados.");
    }
}
