package com.loki.estructuraUsuarios.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.loki.estructuraUsuarios.Exceptions.ResourceNotFoundException;
import com.loki.estructuraUsuarios.Models.Nivel;
import com.loki.estructuraUsuarios.Repository.NivelRepository;

import jakarta.transaction.Transactional;

@Service
public class NivelService {

    @Autowired
    private NivelRepository nivelRepository;

    // Obtener todos los nivel
    @Transactional
    public List<Nivel> getAllNivel() {
        return nivelRepository.findAll();
    }

    // Obtener nivel por ID
    @Transactional
    public Nivel getNivelById(Long id) {
        return nivelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nivel con ID " + id + " no encontrado."));
    }

    // Guardar un nuevo nivel
    @Transactional
    public Nivel saveNivel(Nivel nivel) {
        if (nivelRepository.findByNivel(nivel.getNivel()).isPresent()) {
            throw new IllegalArgumentException("El nivel " + nivel.getNivel() + " ya existe.");
        }
        if (nivelRepository.findAll().stream().anyMatch(n -> n.getNombre().equalsIgnoreCase(nivel.getNombre()))) {
            throw new IllegalArgumentException("El nombre '" + nivel.getNombre() + "' ya está asociado a otro nivel.");
        }
        // Valida que el color sea un HEX válido (opcional)
        validateColorHex(nivel.getColor());
        return nivelRepository.save(nivel);
    }

    // Actualizar un nivel existente
    @Transactional
    public Nivel updateNivel(Long id, Nivel nivelDetails) {
        return nivelRepository.findById(id).map(nivel -> {
            if (nivel.getNivel() != nivelDetails.getNivel() &&
                    nivelRepository.findByNivel(nivelDetails.getNivel()).isPresent()) {
                throw new IllegalArgumentException("El nivel " + nivelDetails.getNivel() + " ya existe.");
            }
            if (!nivel.getNombre().equalsIgnoreCase(nivelDetails.getNombre()) &&
                    nivelRepository.findAll().stream()
                            .anyMatch(n -> n.getNombre().equalsIgnoreCase(nivelDetails.getNombre()))) {
                throw new IllegalArgumentException(
                        "El nombre '" + nivelDetails.getNombre() + "' ya está asociado a otro nivel.");
            }
            nivel.setNivel(nivelDetails.getNivel());
            nivel.setNombre(nivelDetails.getNombre());
            nivel.setColor(nivelDetails.getColor()); // Actualizar color
            validateColorHex(nivelDetails.getColor()); // Validar color HEX (opcional)
            return nivelRepository.save(nivel);
        }).orElseThrow(() -> new ResourceNotFoundException("Nivel con ID " + id + " no encontrado."));
    }

    // Validar color HEX (opcional)
    private void validateColorHex(String color) {
        if (color != null && !color.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("El color debe ser un código HEX válido, por ejemplo: #FFFFFF");
        }
    }

    // Eliminar un nivel
    @Transactional
    public void deleteNivel(Long id) {
        if (!nivelRepository.existsById(id)) {
            throw new ResourceNotFoundException("Nivel con ID " + id + " no encontrado.");
        }
        nivelRepository.deleteById(id);
    }
}
