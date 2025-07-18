package com.loki.bonos.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.loki.bonos.DTOs.VariablesDTO;
import com.loki.bonos.Models.Variables;
import com.loki.bonos.Service.VariablesService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/variables")
public class VariablesController {

    @Autowired
    private VariablesService variablesService;

    // Obtener todas las variables
    @GetMapping
    public List<VariablesDTO> getAllVariables() {
        return variablesService.getAllVariables().stream()
                .map(VariablesDTO::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VariablesDTO> getVariablesById(@PathVariable Long id) {
        Optional<Variables> variables = variablesService.findById(id);
        return variables.map(v -> ResponseEntity.ok(new VariablesDTO(v)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Crear una nueva variable
    @PostMapping
    public VariablesDTO createVariables(@RequestBody VariablesDTO variablesDTO) {
        Variables variables = new Variables();
        variables.setName(variablesDTO.getName());
        variables.setType(variablesDTO.getType());

        Variables savedVariables = variablesService.saveVariables(variables);
        return new VariablesDTO(savedVariables);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VariablesDTO> updateVariables(@PathVariable Long id, @RequestBody VariablesDTO variablesDTO) {
        Optional<Variables> existingVariables = variablesService.findById(id);
        if (existingVariables.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Variables variables = existingVariables.get();
        variables.setName(variablesDTO.getName());
        variables.setType(variablesDTO.getType());

        Variables updatedVariables = variablesService.updateVariables(id, variables);
        return ResponseEntity.ok(new VariablesDTO(updatedVariables));
    }

    // Eliminar una variable por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVariables(@PathVariable Long id) {
        variablesService.deleteVariables(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/eliminartodo")
    public ResponseEntity<String> deleteAllVariables() {
        variablesService.deleteAllVariables();
        return ResponseEntity.ok("✅ Todos los registros de Variables han sido eliminados.");
    }
}
