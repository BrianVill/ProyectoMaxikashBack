package com.loki.estructuraUsuarios.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.loki.estructuraUsuarios.Models.Nivel;

@Repository
public interface NivelRepository extends JpaRepository<Nivel, Long> {
    Optional<Nivel> findByNivel(int nivel);
    Optional<Nivel> findByNombre(String nombre);

    @Query("SELECT MAX(n.nivel) FROM Nivel n")
    Integer findMaxNivel();

    
}

