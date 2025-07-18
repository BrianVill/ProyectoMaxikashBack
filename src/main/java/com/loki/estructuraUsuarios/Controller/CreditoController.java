package com.loki.estructuraUsuarios.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.loki.estructuraUsuarios.DTOs.CreditoDTO;
import com.loki.estructuraUsuarios.Models.Credito;
import com.loki.estructuraUsuarios.Repository.CreditoRepository;

import java.util.List;

import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/creditos")
public class CreditoController {

    @Autowired
    private CreditoRepository creditoRepository;

    @GetMapping
    public List<CreditoDTO> getAllCreditos() {
        return creditoRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditoDTO> getCreditoById(@PathVariable String id) {
        return creditoRepository.findById(id)
                .map(credito -> ResponseEntity.ok(toDto(credito)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CreditoDTO> createCredito(@RequestBody CreditoDTO dto) {
        Credito c = toEntity(dto);
        // If your PK is a user-supplied String ID, you might want checks here.
        c = creditoRepository.save(c);
        return ResponseEntity.ok(toDto(c));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CreditoDTO> updateCredito(@PathVariable String id, @RequestBody CreditoDTO dto) {
        Optional<Credito> optional = creditoRepository.findById(id);
        if (!optional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Credito existing = optional.get();
        existing.setNombre(dto.getNombre());
        existing.setColor(dto.getColor());

        existing = creditoRepository.save(existing);
        return ResponseEntity.ok(toDto(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCredito(@PathVariable String id) {
        if (!creditoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        creditoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private CreditoDTO toDto(Credito c) {
        CreditoDTO dto = new CreditoDTO();
        dto.setId(c.getId());
        dto.setNombre(c.getNombre());
        dto.setColor(c.getColor());
        return dto;
    }

    private Credito toEntity(CreditoDTO dto) {
        Credito c = new Credito();
        c.setId(dto.getId());
        c.setNombre(dto.getNombre());
        c.setColor(dto.getColor());
        return c;
    }
}