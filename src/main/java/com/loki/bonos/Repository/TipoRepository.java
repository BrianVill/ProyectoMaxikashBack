package com.loki.bonos.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loki.bonos.Models.Tipo;

@Repository
public interface TipoRepository extends JpaRepository<Tipo, Long> {
    // Puedes agregar métodos de búsqueda adicionales si es necesario.
}