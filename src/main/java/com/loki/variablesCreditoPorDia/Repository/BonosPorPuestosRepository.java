package com.loki.variablesCreditoPorDia.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.loki.variablesCreditoPorDia.Models.BonosPorPuestos;

@Repository
public interface BonosPorPuestosRepository extends JpaRepository<BonosPorPuestos, Long> {

    List<BonosPorPuestos> findByPuestoId(UUID puestoId);

    Optional<BonosPorPuestos> findByPuestoIdAndIdBono(
            UUID puestoId, Long idBono);

    @Query("SELECT b FROM BonosPorPuestos b WHERE b.fecha BETWEEN :ini AND :fin")
    List<BonosPorPuestos> findByFechaBetween(
            @Param("ini") LocalDate ini,
            @Param("fin") LocalDate fin);
}

