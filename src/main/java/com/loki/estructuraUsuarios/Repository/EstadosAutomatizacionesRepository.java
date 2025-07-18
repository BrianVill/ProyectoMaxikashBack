package com.loki.estructuraUsuarios.Repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loki.estructuraUsuarios.Models.EstadosAutomatizaciones;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstadosAutomatizacionesRepository extends JpaRepository<EstadosAutomatizaciones, Long> {
    Optional<EstadosAutomatizaciones> findByNombre(String nombre); // 🆕 Buscar estado por nombre

    List<EstadosAutomatizaciones> findAllByNombre(String nombre);
}