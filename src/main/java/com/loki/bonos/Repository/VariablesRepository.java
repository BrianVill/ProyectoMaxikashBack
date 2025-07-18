package com.loki.bonos.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loki.bonos.Models.Variables;

@Repository
public interface VariablesRepository extends JpaRepository<Variables, Long> {
    Optional<Variables> findByName(String name);

}

