package com.loki.estructuraUsuarios.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loki.estructuraUsuarios.Models.Asignacion;

@Repository
public interface AsignacionRepository extends JpaRepository<Asignacion, Integer> {
}

