package com.loki.variablesCreditoPorDia.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.loki.variablesCreditoPorDia.Models.PuestoCumpleCondicion;

@Repository
public interface PuestoCumpleCondicionRepository extends JpaRepository<PuestoCumpleCondicion, Long> {

    List<PuestoCumpleCondicion> findByPuestoId(UUID puestoId);

    Optional<PuestoCumpleCondicion> findByPuestoIdAndIdCondicion(
            UUID puestoId, Long idCondicion);

    @Query("SELECT p FROM PuestoCumpleCondicion p WHERE p.fecha = :ultima")
    List<PuestoCumpleCondicion> findByFecha(@Param("ultima") LocalDate ultima);

    @Query("SELECT MAX(p.fecha) FROM PuestoCumpleCondicion p")
    Optional<LocalDate> findMaxFecha();
}


