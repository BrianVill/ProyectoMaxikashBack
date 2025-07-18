package com.loki.bonos.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loki.bonos.DTOs.BonoResponseDTO;
import com.loki.bonos.Models.Bono;
import com.loki.bonos.Repository.BonoRepository;

import java.util.List;

@Service
public class BonoService {

    @Autowired
    private BonoRepository bonoRepository;

    public List<Bono> getAllBonos() {
        return bonoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public BonoResponseDTO getBonoById(Long id) {
        Bono bono = bonoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bono no encontrado con id " + id));

        return new BonoResponseDTO(
                bono.getId(),
                bono.getNombre(),
                bono.getGrupo(),
                bono.getLunes(),
                bono.getMartes(),
                bono.getMiercoles(),
                bono.getJueves(),
                bono.getViernes(),
                bono.getSabado(),
                bono.getDomingo(),
                null, // condiciones
                null // acciones
        );
    }

    public Bono saveBono(Bono bono) {
        return bonoRepository.save(bono);
    }

    public Bono updateBono(Long id, Bono bonoDetails) {
        return bonoRepository.findById(id)
                .map(bono -> {
                    bono.setNombre(bonoDetails.getNombre());
                    bono.setGrupo(bonoDetails.getGrupo());
                    bono.setLunes(bonoDetails.getLunes());
                    bono.setMartes(bonoDetails.getMartes());
                    bono.setMiercoles(bonoDetails.getMiercoles());
                    bono.setJueves(bonoDetails.getJueves());
                    bono.setViernes(bonoDetails.getViernes());
                    bono.setSabado(bonoDetails.getSabado());
                    bono.setDomingo(bonoDetails.getDomingo());
                    return bonoRepository.save(bono);
                }).orElseThrow(() -> new RuntimeException("Bono no encontrado con id " + id));
    }

    public void deleteBono(Long id) {
        bonoRepository.deleteById(id);
    }
}