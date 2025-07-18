package com.loki.estructuraUsuarios.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.loki.estructuraUsuarios.Models.Asignacion;
import com.loki.estructuraUsuarios.Repository.AsignacionRepository;

import java.util.List;
import java.util.Optional;

@Service
public class AsignacionService {

    @Autowired
    private AsignacionRepository asignacionRepository;

    public List<Asignacion> getAllAsignaciones() {
        return asignacionRepository.findAll();
    }

    public Asignacion saveAsignacion(Asignacion asignacion) {
        return asignacionRepository.save(asignacion);
    }

    public Optional<Asignacion> getAsignacionById(int id) {
        return asignacionRepository.findById(id);
    }

    public Asignacion updateAsignacion(Asignacion asignacion) {
        return asignacionRepository.save(asignacion);
    }

    public void deleteAsignacion(int id) {
        asignacionRepository.deleteById(id);
    }
}

