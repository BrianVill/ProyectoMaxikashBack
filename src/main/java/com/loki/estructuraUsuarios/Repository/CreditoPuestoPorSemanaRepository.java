package com.loki.estructuraUsuarios.Repository;

import com.loki.estructuraUsuarios.Models.CreditoPuestoPorSemana;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface CreditoPuestoPorSemanaRepository extends JpaRepository<CreditoPuestoPorSemana, Long> {

  @Query("SELECT cpps FROM CreditoPuestoPorSemana cpps " +
      "WHERE cpps.creditoId = :creditoId " +
      "AND cpps.fechaInicio = :startDate " +
      "AND cpps.fechaFin = :endDate")
  Optional<CreditoPuestoPorSemana> findByCreditoIdAndDateRange(
      @Param("creditoId") String creditoId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  List<CreditoPuestoPorSemana> findByFechaInicioAndFechaFin(LocalDate start, LocalDate end);

  @Modifying
  @Query("""
      update CreditoPuestoPorSemana c
        set c.puestoId = :puestoId
      where c.creditoId   = :creditoId
        and c.fechaInicio = :ini
        and c.fechaFin    = :fin
  """)
  int updatePuesto(@Param("creditoId") String   creditoId,
                  @Param("puestoId")  UUID     puestoId,
                  @Param("ini")       LocalDate ini,
                  @Param("fin")       LocalDate fin);


  @Modifying
  @Query(value = """
      INSERT INTO credito_puesto_por_semana
          (credito_id, puesto_id, fecha_inicio, fecha_fin)
      VALUES
          (:creditoId, :puestoId, :start, :end)
      ON DUPLICATE KEY UPDATE
          puesto_id = VALUES(puesto_id)
      """, nativeQuery = true)
  void upsert(String creditoId,
              UUID puestoId,
              LocalDate start,
              LocalDate end);


  @Query("""
      SELECT cpps FROM CreditoPuestoPorSemana cpps
      WHERE cpps.creditoId = :credito
        AND cpps.fechaInicio <= :fecha
        AND cpps.fechaFin   >= :fecha
      """)
  Optional<CreditoPuestoPorSemana> findSemana(String credito, LocalDate fecha);

  List<CreditoPuestoPorSemana> findAllByFechaInicioAndFechaFinAndPuestoIdIsNull(
      LocalDate fechaInicio,
      LocalDate fechaFin);

  List<CreditoPuestoPorSemana> findAllByFechaInicioAndFechaFinAndPuestoIdIsNotNull(
      LocalDate fechaInicio,
      LocalDate fechaFin);

  @Query("""
        select cpps.creditoId , cpps.puestoId
          from CreditoPuestoPorSemana cpps
         where cpps.fechaInicio <= :fecha
           and cpps.fechaFin    >= :fecha
      """)
  List<Object[]> findPuestoByFecha(@Param("fecha") LocalDate fecha);

  /*  ⬇️ NUEVO — sin filtro de puestoId */
  List<CreditoPuestoPorSemana> findAllByFechaInicioAndFechaFin(LocalDate start, LocalDate end);

  @Query(value = """
        SELECT id_credito, id_puesto
          FROM credito_puesto_por_semana
         WHERE id_credito IN (:ids)
           AND fecha_inicio <= :f
           AND fecha_fin    >= :f
      """, nativeQuery = true)
  List<Object[]> findPuestoByIdsAndFecha(@Param("ids") Set<String> ids,
      @Param("f") LocalDate f);

  @Query("""
      SELECT cpps.creditoId, cpps.puestoId
        FROM CreditoPuestoPorSemana cpps
       WHERE cpps.creditoId IN :creditos
         AND cpps.fechaInicio <= :fecha
         AND cpps.fechaFin    >= :fecha
      """)
  List<Object[]> findPuestoByFechaAndCreditos(@Param("fecha") LocalDate fecha,
      @Param("creditos") Collection<String> creditos);

}
