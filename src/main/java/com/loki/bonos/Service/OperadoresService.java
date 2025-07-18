package com.loki.bonos.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.loki.bonos.Models.Operadores;
import com.loki.bonos.Repository.OperadoresRepository;

import java.util.List;
import java.util.Optional;

@Service
public class OperadoresService {

    @Autowired
    private OperadoresRepository operadoresRepository;

    // Obtener todos los operadores
    public List<Operadores> getAllOperadores(String tipo) {
        if (tipo != null && !tipo.isEmpty()) {
            return operadoresRepository.findByTipo(tipo);
        }
        return operadoresRepository.findAll();
    }

    // Obtener un operador por ID
    public Optional<Operadores> getOperadoresById(Long id) {
        return operadoresRepository.findById(id);
    }

    // Guardar un nuevo operador
    public Operadores saveOperadores(Operadores operadores) {
        return operadoresRepository.save(operadores);
    }

    // Actualizar un operador existente
    public Operadores updateOperadores(Long id, Operadores operadoresDetails) {
        return operadoresRepository.findById(id)
                .map(operadores -> {
                    operadores.setName(operadoresDetails.getName());
                    operadores.setClase(operadoresDetails.getClase());
                    operadores.setTipo(operadoresDetails.getTipo()); // Actualizar el campo tipo
                    return operadoresRepository.save(operadores);
                }).orElseThrow(() -> new RuntimeException("Operador no encontrado con id " + id));
    }

    // Eliminar un operador por ID
    public void deleteOperadores(Long id) {
        operadoresRepository.deleteById(id);
    }
}

