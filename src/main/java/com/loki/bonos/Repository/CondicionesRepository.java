package com.loki.bonos.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loki.bonos.Models.Condiciones;

public interface CondicionesRepository extends JpaRepository<Condiciones, Long> {
    List<Condiciones> findByBono_Id(Long bonoId);
    // 🔹 Nuevo método para filtrar por tipoId (Ejemplo: tipoId = 1 para "filtros")
    List<Condiciones> findByTipo_Id(Long tipoId);
}
