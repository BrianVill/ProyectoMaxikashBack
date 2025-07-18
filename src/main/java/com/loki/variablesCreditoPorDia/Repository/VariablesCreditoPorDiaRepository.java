package com.loki.variablesCreditoPorDia.Repository;

import java.time.LocalDate;
import java.util.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.loki.variablesCreditoPorDia.Models.VariablesCreditoPorDia;

@Repository
public interface VariablesCreditoPorDiaRepository extends JpaRepository<VariablesCreditoPorDia, Long> {

       /* búsquedas por crédito */
       List<VariablesCreditoPorDia> findByIdCredito(String idCredito);

       /* búsquedas por puesto + variable */
       @Query("SELECT v FROM VariablesCreditoPorDia v " +
                     "WHERE v.puestoId = :puestoId AND v.idVariable = :idVariable")
       List<VariablesCreditoPorDia> findByPuestoIdAndIdVariable(@Param("puestoId") UUID puestoId,
                     @Param("idVariable") Long idVariable);

       /* upsert helper */
       Optional<VariablesCreditoPorDia> findByIdCreditoAndIdVariableAndFecha(
                     String idCredito, Long idVariable, LocalDate fecha);

       List<VariablesCreditoPorDia> findByPuestoId(UUID puestoId);

       @Query("select v from VariablesCreditoPorDia v where v.fecha = :fecha")
       List<VariablesCreditoPorDia> findByFecha(@Param("fecha") LocalDate fecha);

       Page<VariablesCreditoPorDia> findAll(Pageable pageable);
}
