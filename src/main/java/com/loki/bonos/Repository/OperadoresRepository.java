package com.loki.bonos.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loki.bonos.Models.Operadores;

@Repository
public interface OperadoresRepository extends JpaRepository<Operadores, Long> {
    List<Operadores> findByTipo(String tipo);
}
