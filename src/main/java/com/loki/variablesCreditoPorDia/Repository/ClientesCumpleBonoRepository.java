package com.loki.variablesCreditoPorDia.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loki.variablesCreditoPorDia.Models.ClientesCumpleBono;

@Repository
public interface ClientesCumpleBonoRepository extends JpaRepository<ClientesCumpleBono, Long> {

     List<ClientesCumpleBono> findByIdCredito(String idCredito);

     Optional<ClientesCumpleBono> findByIdCreditoAndIdBono(
               String idCredito, Long idBono);
}
