package com.loki.estructuraUsuarios.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loki.estructuraUsuarios.Models.Bucket;

@Repository
public interface BucketRepository extends JpaRepository<Bucket, Integer> {
}

