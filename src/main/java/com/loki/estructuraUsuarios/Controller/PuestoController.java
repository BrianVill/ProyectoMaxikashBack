package com.loki.estructuraUsuarios.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.loki.estructuraUsuarios.DTOs.PuestoDTO;
import com.loki.estructuraUsuarios.Exceptions.ResourceNotFoundException;
import com.loki.estructuraUsuarios.Models.Puesto;
import com.loki.estructuraUsuarios.Repository.PuestoRepository;
import com.loki.estructuraUsuarios.Service.PuestoService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/puesto")
public class PuestoController {

    @Autowired
    private PuestoRepository puestoRepository;
    
    @PostMapping
    public ResponseEntity<PuestoDTO> createPuesto(@RequestBody PuestoDTO dto) {
        Puesto puesto = toEntity(dto);
        // If you're not generating UUID in DB, generate it here if null
        if (puesto.getId() == null) {
            puesto.setId(UUID.randomUUID());
        }
        puesto = puestoRepository.save(puesto);
        return ResponseEntity.ok(toDto(puesto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PuestoDTO> updatePuesto(@PathVariable UUID id, @RequestBody PuestoDTO dto) {
        Optional<Puesto> optional = puestoRepository.findById(id);
        if (!optional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Puesto existing = optional.get();
        existing.setNombre(dto.getNombre());
        existing.setLat(dto.getLat());
        existing.setLon(dto.getLon());
        existing.setNivel(dto.getNivel());
        // For padreDirecto, you might want to fetch it from repository (if not null) 
        // or handle removal if dto.getPadreDirectoId() is null, etc.
        // existing.setPadreDirecto(...)

        existing = puestoRepository.save(existing);
        return ResponseEntity.ok(toDto(existing));
    }

    private PuestoDTO toDto(Puesto p) {
        PuestoDTO dto = new PuestoDTO();
        dto.setId(p.getId());
        dto.setNombre(p.getNombre());
        dto.setLat(p.getLat());
        dto.setLon(p.getLon());
        dto.setNivel(p.getNivel());
        if (p.getIdPadreDirecto() != null) {
            dto.setIdPadreDirecto(p.getIdPadreDirecto());
        }
        return dto;
    }

    private Puesto toEntity(PuestoDTO dto) {
        Puesto p = new Puesto();
        p.setId(dto.getId());
        p.setNombre(dto.getNombre());
        p.setLat(dto.getLat());
        p.setLon(dto.getLon());
        p.setNivel(dto.getNivel() != null ? dto.getNivel() : null);
        // For padreDirecto, you'd fetch from repo by padreDirectoId if present.
        return p;
    }

    @Autowired
    private PuestoService puestoService;

    public PuestoController(ObjectMapper objectMapper) {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @GetMapping
    public List<PuestoDTO> getPuestos() {
        return puestoService.getAllPuestosOrdenadosPorNivel();
    }

    // Obtener puesto por ID con los datos filtrados
    @GetMapping("/{id}")
    public ResponseEntity<PuestoDTO> getPuestoById(@PathVariable UUID id) {
        Optional<PuestoDTO> puestoDTO = puestoService.getPuestoDTOById(id);
        return puestoDTO.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Filtrar puestos por nivel
    @GetMapping("/nivel/{nivel}")
    public List<PuestoDTO> getPuestosByNivel(@PathVariable int nivel) {
        return puestoService.getPuestosByNivel(nivel);
    }

    // Filtrar puestos por idPadreDirecto
    @GetMapping("/padre/{idPadreDirecto}")
    public List<PuestoDTO> getPuestosByIdPadreDirecto(@PathVariable UUID idPadreDirecto) {
        return puestoService.getPuestosByIdPadreDirecto(idPadreDirecto);
    }

    // Eliminar un puesto
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePuesto(@PathVariable UUID id) {
        puestoService.deletePuesto(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/nivel/{nivel}")
    public ResponseEntity<String> deletePuestosByNivel(@PathVariable int nivel) {
        try {
            puestoService.deletePuestosByNivel(nivel);
            return ResponseEntity.ok("✅ Puestos de nivel " + nivel + " eliminados correctamente.");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

}
