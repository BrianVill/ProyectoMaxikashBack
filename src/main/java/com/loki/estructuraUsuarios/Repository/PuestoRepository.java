package com.loki.estructuraUsuarios.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loki.estructuraUsuarios.Models.Puesto;


import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PuestoRepository extends JpaRepository<Puesto, UUID> {

    // Buscar puestos por nombre
    Optional<Puesto> findByNombre(String nombre);

    // Filtrar puestos por nivel
    List<Puesto> findByNivel(int nivel);

    // Filtrar puestos por idPadreDirecto
    List<Puesto> findByIdPadreDirecto(UUID idPadreDirecto);
    
    List<Puesto> findAllByOrderByNivelAsc();

    /*@Query("SELECT u FROM Puesto u LEFT JOIN FETCH u.credito WHERE u.id = :id")
    Optional<Puesto> findByIdWithCredito(@Param("id") UUID id);*/

    Optional<Puesto> findById(UUID id); // ✅ Buscar por idpuesto

    boolean existsById(UUID id); // ✅ Validar si ya existe un idpuesto

    List<Puesto> findByIdIn(Collection<String> idPuestos);

    /*@Modifying
    @Transactional
    @Query("UPDATE Puesto u SET u.sueldoFinal = u.sueldoFinal + :monto WHERE u.id = :idGestor")
    void actualizarSueldoFinal(@Param("idGestor") UUID idGestor, @Param("monto") Double monto);*/

}
