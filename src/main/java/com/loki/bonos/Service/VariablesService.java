package com.loki.bonos.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.loki.bonos.Models.Variables;
import com.loki.bonos.Repository.VariablesRepository;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class VariablesService {

    @Autowired
    private VariablesRepository variablesRepository;

    // Obtener todas las variables
    public List<Variables> getAllVariables() {
        return variablesRepository.findAll();
    }

    // Obtener una variable por ID
    public Optional<Variables> findById(Long id) {
        return variablesRepository.findById(id);
    }

    // Guardar una nueva variable
    public Variables saveVariables(Variables variables) {
        return variablesRepository.save(variables);
    }

    // <<-- Agregar este método
    public Optional<Variables> findByName(String name) {
        return variablesRepository.findByName(name);
    }

    // Actualizar una variable existente
    public Variables updateVariables(Long id, Variables variablesDetails) {
        return variablesRepository.findById(id)
                .map(variables -> {
                    variables.setName(variablesDetails.getName());
                    variables.setType(variablesDetails.getType());  // Se actualiza la relación
                    return variablesRepository.save(variables);
                }).orElseThrow(() -> new RuntimeException("Variable no encontrada con id " + id));
    }

    // Eliminar una variable por ID
    public void deleteVariables(Long id) {
        variablesRepository.deleteById(id);
    }

    @Transactional
    public void deleteAllVariables() {
        variablesRepository.deleteAll();
    }
}

