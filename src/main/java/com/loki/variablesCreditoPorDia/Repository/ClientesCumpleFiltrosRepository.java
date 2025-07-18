package com.loki.variablesCreditoPorDia.Repository;

import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.loki.variablesCreditoPorDia.Models.ClientesCumpleFiltros;

@Repository
public interface ClientesCumpleFiltrosRepository
        extends JpaRepository<ClientesCumpleFiltros, Long> {

    List<ClientesCumpleFiltros> findByIdCredito(String idCredito);

    @Query("SELECT c FROM ClientesCumpleFiltros c WHERE c.fecha = :f")
    List<ClientesCumpleFiltros> findByFecha(@Param("f") LocalDate f);

    Optional<ClientesCumpleFiltros> findByIdCreditoAndIdCondicion(
            String idCredito, Long idCondicion);

    @Query("SELECT MAX(c.fecha) FROM ClientesCumpleFiltros c")
    Optional<LocalDate> findMaxFecha();

    
}
