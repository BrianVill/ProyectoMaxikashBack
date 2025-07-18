package com.loki.bonos.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.loki.bonos.DTOs.CondicionesDTO;
import com.loki.bonos.DTOs.CondicionesResponseDTO;
import com.loki.bonos.DTOs.OperadoresDTO;
import com.loki.bonos.DTOs.VariablesDTO;
import com.loki.bonos.Models.Bono;
import com.loki.bonos.Models.Condiciones;
import com.loki.bonos.Models.Operadores;
import com.loki.bonos.Models.Tipo;
import com.loki.bonos.Models.Variables;
import com.loki.bonos.Repository.CondicionesRepository;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CondicionesService {

    @Autowired
    private CondicionesRepository condicionesRepository;

    @Transactional
    public List<CondicionesResponseDTO> findAll() {
        return condicionesRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public CondicionesResponseDTO findById(Long id) {
        Condiciones condicion = condicionesRepository.findById(id).orElse(null);
        return condicion != null ? convertToResponseDTO(condicion) : null;
    }

    public CondicionesDTO save(CondicionesDTO condicionDTO) {
        Condiciones condicion = convertToEntity(condicionDTO);
        Condiciones savedCondicion = condicionesRepository.save(condicion);
        return convertToDTO(savedCondicion);
    }

    public CondicionesDTO update(Long id, CondicionesDTO condicionDTO) {
        Condiciones existingCondicion = condicionesRepository.findById(id).orElse(null);
        
        if (existingCondicion != null) {
            // 1. Crear las instancias y establecer los IDs manualmente
            Bono bono = new Bono();
            bono.setId(condicionDTO.getBonoId());
    
            Variables variable = new Variables();
            variable.setId(condicionDTO.getVariableId());
    
            Operadores operador = new Operadores();
            operador.setId(condicionDTO.getOperadorId());
    
            // 2. Establecer los valores en la entidad existente
            existingCondicion.setBono(bono);
            existingCondicion.setVariable(variable);
            existingCondicion.setOperador(operador);
            existingCondicion.setValor(condicionDTO.getValor());
    
            // 🔹 **Corrección aquí**: Verificar si `tipoId` está presente y actualizarlo.
            if (condicionDTO.getTipoId() != null) {
                Tipo tipo = new Tipo();
                tipo.setId(condicionDTO.getTipoId());
                existingCondicion.setTipo(tipo);
            } else {
                existingCondicion.setTipo(null); // Permite eliminar la relación si se envía `null`
            }
    
            // 3. Guardar la condición actualizada
            Condiciones updatedCondicion = condicionesRepository.save(existingCondicion);
            return convertToDTO(updatedCondicion);
        }
        
        return null;
    }    

    public void delete(Long id) {
        condicionesRepository.deleteById(id);
    }

    // CondicionesService.java
    private CondicionesResponseDTO convertToResponseDTO(Condiciones condicion) {
        return new CondicionesResponseDTO(
            condicion.getId(),
            condicion.getBono().getId(),
            new VariablesDTO(condicion.getVariable()),
            new OperadoresDTO(condicion.getOperador()),
            condicion.getValor(),
            condicion.getTipo() != null ? condicion.getTipo().getId() : null,
            condicion.getTipo() != null ? condicion.getTipo().getTipo() : null
        );
    }
    
    private CondicionesDTO convertToDTO(Condiciones condicion) {
        return new CondicionesDTO(
                condicion.getBono().getId(),
                condicion.getVariable().getId(),
                condicion.getOperador().getId(),
                condicion.getValor(),
                condicion.getTipo() != null ? condicion.getTipo().getId() : null
        );
    }
    

    private Condiciones convertToEntity(CondicionesDTO dto) {
        Condiciones condicion = new Condiciones();

        Bono bono = new Bono();
        bono.setId(dto.getBonoId());

        Variables variable = new Variables();
        variable.setId(dto.getVariableId());

        Operadores operador = new Operadores();
        operador.setId(dto.getOperadorId());

        condicion.setBono(bono);
        condicion.setVariable(variable);
        condicion.setOperador(operador);
        condicion.setValor(dto.getValor());

        // Si se envía el tipo, asignarlo a la condición.
        if (dto.getTipoId() != null) {
            Tipo tipo = new Tipo();
            tipo.setId(dto.getTipoId());
            condicion.setTipo(tipo);
        }

        return condicion;
    }

    @Transactional
    public List<CondicionesResponseDTO> findFiltros() {
        return condicionesRepository.findByTipo_Id(1L).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }
    
    // CondicionesService
    public List<CondicionesResponseDTO> findByBonoId(Long bonoId) {
        return condicionesRepository.findByBono_Id(bonoId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

}
