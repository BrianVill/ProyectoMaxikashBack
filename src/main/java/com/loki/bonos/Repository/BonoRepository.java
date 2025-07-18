package com.loki.bonos.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loki.bonos.Models.Bono;

@Repository
public interface BonoRepository extends JpaRepository<Bono, Long> {
    
}

