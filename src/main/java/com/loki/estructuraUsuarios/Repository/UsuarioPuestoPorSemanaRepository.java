package com.loki.estructuraUsuarios.Repository;

import com.loki.estructuraUsuarios.Models.UsuarioPuestoPorSemana;
import com.loki.estructuraUsuarios.DTOs.UsuarioPorPuestoDTO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioPuestoPorSemanaRepository
    extends JpaRepository<UsuarioPuestoPorSemana, Long> {

  /* ------------------------------------------------------------------ */
  /* 1. Devuelve el usuario asignado a un puesto en una semana concreta */
  /* ------------------------------------------------------------------ */
  @Query("""
      SELECT up.usuarioId
        FROM UsuarioPuestoPorSemana up
       WHERE up.puestoId    = :puestoId
         AND up.fechaInicio = :start
         AND up.fechaFin    = :end
      """)
  UUID findUsuarioIdByPuestoAndSemana(@Param("puestoId") UUID puestoId,
      @Param("start") LocalDate start,
      @Param("end") LocalDate end);

  /* ------------------------------------------------------------------ */
  /* 2. Todos los registros que empiecen/terminen exactamente en esas */
  /* fechas (útil para consultas masivas) */
  /* ------------------------------------------------------------------ */
  List<UsuarioPuestoPorSemana> findAllByFechaInicioAndFechaFin(
      LocalDate start, LocalDate end);

  /* ------------------------------------------------------------------ */
  /* 3. Registro puntual por usuario-puesto-semana */
  /* ------------------------------------------------------------------ */
  @Query("""
      SELECT up
        FROM UsuarioPuestoPorSemana up
       WHERE up.usuarioId   = :usuarioId
         AND up.puestoId    = :puestoId
         AND up.fechaInicio = :start
         AND up.fechaFin    = :end
      """)
  Optional<UsuarioPuestoPorSemana> findByUserAndPuestoAndDates(
      @Param("usuarioId") UUID usuarioId,
      @Param("puestoId") UUID puestoId,
      @Param("start") LocalDate start,
      @Param("end") LocalDate end);

  /* ------------------------------------------------------------------ */
  /* 4. Registro que cubra la semana (start ≤ fechaInicio, end ≥ fechaFin) */
  /* ------------------------------------------------------------------ */
  @Query("""
      SELECT up
        FROM UsuarioPuestoPorSemana up
       WHERE up.puestoId    = :puestoId
         AND up.fechaInicio <= :start
         AND up.fechaFin    >= :end
      """)
  List<UsuarioPuestoPorSemana> findByPuestoAndSemana(
      @Param("puestoId") UUID puestoId,
      @Param("start") LocalDate start,
      @Param("end") LocalDate end);

  @Query("""
      SELECT up
        FROM UsuarioPuestoPorSemana up
       WHERE up.usuarioId    = :usuarioId
         AND up.fechaInicio <= :start
         AND up.fechaFin    >= :end
      """)
  Optional<UsuarioPuestoPorSemana> findByUsuarioAndSemana(
      @Param("usuarioId") UUID usuarioId,
      @Param("start") LocalDate start,
      @Param("end") LocalDate end);

  @Query("""
         SELECT up
           FROM UsuarioPuestoPorSemana up
          WHERE up.puestoId     = :puestoId
            AND up.fechaInicio <= :fecha
            AND up.fechaFin    >= :fecha
      """)
  List<UsuarioPuestoPorSemana> findAllByPuestoIdAndFecha(@Param("puestoId") UUID puestoId,
      @Param("fecha") LocalDate fecha);

  boolean existsByUsuarioIdAndFechaInicio(UUID usuarioId, LocalDate fechaInicio);

  
  @Query("""
       SELECT new com.loki.estructuraUsuarios.DTOs.UsuarioPorPuestoDTO(
           upps.puestoId,
           u.id,
           u.nombre,
           u.color
       )
       FROM Usuario u
       JOIN UsuarioPuestoPorSemana upps
         ON u.id = upps.usuarioId
      WHERE upps.fechaInicio <= :fecha
        AND upps.fechaFin >= :fecha
       """)
  List<UsuarioPorPuestoDTO> findDisponibles(@Param("fecha") LocalDate fecha);

  @Query("""
       SELECT c.puestoId, COUNT(c)
       FROM   CreditoPuestoPorSemana c
       WHERE  c.fechaInicio = :inicio
         AND  c.fechaFin    = :fin
         AND  c.puestoId    IS NOT NULL
       GROUP BY c.puestoId
       """)
  List<Object[]> contarAsignados(LocalDate inicio, LocalDate fin);

}
