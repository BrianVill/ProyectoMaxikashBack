package com.loki.variablesCreditoPorDia.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.loki.variablesCreditoPorDia.Models.PuestoCumpleBono;

@Repository
public interface PuestoCumpleBonoRepository extends JpaRepository<PuestoCumpleBono, Long> {

    List<PuestoCumpleBono> findByPuestoId(UUID puestoId);

    Optional<PuestoCumpleBono> findByPuestoIdAndIdBono(
            UUID puestoId, Long idBono);

    @Query("SELECT p FROM PuestoCumpleBono p WHERE p.fecha BETWEEN :ini AND :fin")
    List<PuestoCumpleBono> findByFechaBetween(
            @Param("ini") LocalDate ini,
            @Param("fin") LocalDate fin);
}

