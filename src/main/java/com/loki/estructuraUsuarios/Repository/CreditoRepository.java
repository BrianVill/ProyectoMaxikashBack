package com.loki.estructuraUsuarios.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.loki.estructuraUsuarios.Models.Credito;

import java.util.List;
import java.util.Optional;


@Repository
public interface CreditoRepository extends JpaRepository<Credito, String> {

    /**
     * Busca un Crédito por su PK (id).
     * @param idCredito el valor de la PK
     */
    @Query("SELECT c FROM Credito c WHERE c.id = :idCredito")
    Optional<Credito> findByIdCredito(@Param("idCredito") String idCredito);

    /**
     * Lista (normalmente 0 o 1 elemento) de Créditos con esa PK.
     */
    @Query("SELECT c FROM Credito c WHERE c.id = :idCredito")
    List<Credito> findAllByIdCredito(@Param("idCredito") String idCredito);

    /**
     * Comprueba si, para un Puesto dado, existe un Crédito con esa PK.
     */
    /*@Query("""
        SELECT CASE WHEN COUNT(c)>0 THEN true ELSE false END
          FROM Credito c
         WHERE c.puesto = :puesto
           AND c.id     = :idCredito
        """)
    boolean existsByPuestoAndIdCredito(@Param("puesto") Puesto puesto,
                                       @Param("idCredito") String idCredito);
    */
    /**
     * Busca un Crédito por Puesto e id (PK).
     */
    /*@Query("SELECT c FROM Credito c WHERE c.puesto = :puesto AND c.id = :idCredito")
    Optional<Credito> findByPuestoAndIdCredito(@Param("puesto") Puesto puesto,
                                               @Param("idCredito") String idCredito);
    */
    /**
     * Obtiene el Puesto asociado a un Crédito dado (por su PK).
     */
    /*@Query("SELECT c.puesto FROM Credito c WHERE c.id = :idCredito")
    Optional<Puesto> findPuestoByIdCredito(@Param("idCredito") String idCredito);
    */


    /**
     * Equivalente a findAll(); reservado para futuros filtros de "activo".
     */
    /*@Query("SELECT c FROM Credito c")
    List<Credito> findAllActivos();*/
}
