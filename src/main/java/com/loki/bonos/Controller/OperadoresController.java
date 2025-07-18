package com.loki.bonos.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.loki.bonos.Models.Operadores;
import com.loki.bonos.Service.OperadoresService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/operadores")
public class OperadoresController {

    @Autowired
    private OperadoresService operadoresService;

    @GetMapping
    public List<Operadores> getAllOperadores(@RequestParam(value = "t", required = false) String tipo) {
        return operadoresService.getAllOperadores(tipo);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Operadores> getOperadoresById(@PathVariable Long id) {
        Optional<Operadores> operadores = operadoresService.getOperadoresById(id);
        return operadores.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Operadores createOperadores(@RequestBody Operadores operadores) {
        return operadoresService.saveOperadores(operadores);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Operadores> updateOperadores(@PathVariable Long id, @RequestBody Operadores operadoresDetails) {
        Operadores updatedOperadores = operadoresService.updateOperadores(id, operadoresDetails);
        return ResponseEntity.ok(updatedOperadores);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOperadores(@PathVariable Long id) {
        operadoresService.deleteOperadores(id);
        return ResponseEntity.noContent().build();
    }
}


