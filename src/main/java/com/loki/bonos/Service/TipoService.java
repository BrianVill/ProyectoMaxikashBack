package com.loki.bonos.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loki.bonos.Models.Tipo;
import com.loki.bonos.Repository.TipoRepository;

import java.util.List;
import java.util.Optional;

@Service
public class TipoService {

    @Autowired
    private TipoRepository tipoRepository;

    @Transactional
    public Tipo saveTipo(Tipo tipo) {
        return tipoRepository.save(tipo);
    }

    @Transactional(readOnly = true)
    public List<Tipo> findAll() {
        return tipoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Tipo> findById(Long id) {
        return tipoRepository.findById(id);
    }

    @Transactional
    public Tipo updateTipo(Long id, Tipo newTipo) {
        return tipoRepository.findById(id)
                .map(tipo -> {
                    tipo.setTipo(newTipo.getTipo());
                    return tipoRepository.save(tipo);
                })
                .orElseThrow(() -> new RuntimeException("Tipo con ID " + id + " no encontrado."));
    }

    @Transactional
    public void deleteTipo(Long id) {
        tipoRepository.deleteById(id);
    }
}