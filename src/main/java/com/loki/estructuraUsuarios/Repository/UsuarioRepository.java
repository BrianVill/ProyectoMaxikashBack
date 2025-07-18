package com.loki.estructuraUsuarios.Repository;

import com.loki.estructuraUsuarios.Models.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

  /* --------------------------- búsquedas básicas --------------------------- */
  Optional<Usuario> findByNombre(String nombre);

  Optional<Usuario> findById(UUID id);

  /*
   * ---------------------------------------------------------------------------
   */
  /* Incrementa sueldoFinal para TODOS los usuarios asignados a un puesto en */
  /* la semana donde cae la fecha dada. */
  /*                                                                             */
  /* Regla: */
  /* - Si sueldo_final ya tiene valor → se suma ahí. */
  /* - Si está NULL, se toma sueldo (base). */
  /* - Si ambos son NULL, se toma 0. */
  /*
   * ---------------------------------------------------------------------------
   */
  @Modifying
  @Transactional
  @Query("""
          UPDATE Usuario u
             SET u.sueldoFinal = COALESCE(u.sueldoFinal, COALESCE(u.sueldo, 0)) + :monto
           WHERE u.id IN (
                 SELECT up.usuarioId
                   FROM UsuarioPuestoPorSemana up
                  WHERE up.puestoId     = :puestoId
                    AND up.fechaInicio <= :fecha
                    AND up.fechaFin   >= :fecha
           )
      """)
  void incrementarSueldoFinalPorPuestoSemana(
      @Param("puestoId") UUID puestoId,
      @Param("fecha") LocalDate fecha,
      @Param("monto") Double monto);

  @Query("""
         SELECT u
           FROM Usuario u
          WHERE u.id IN (
              SELECT up.usuarioId
                FROM UsuarioPuestoPorSemana up
               WHERE up.puestoId     = :puestoId
                 AND up.fechaInicio <= :fecha
                 AND up.fechaFin    >= :fecha
          )
      """)
  List<Usuario> findUsuariosPorPuestoSemana(@Param("puestoId") UUID puestoId,
      @Param("fecha") LocalDate fecha);

  

}
