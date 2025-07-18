package com.loki.estructuraUsuarios.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.loki.estructuraUsuarios.Models.Bucket;
import com.loki.estructuraUsuarios.Repository.BucketRepository;

import java.util.List;
import java.util.Optional;

@Service
public class BucketService {

    @Autowired
    private BucketRepository bucketRepository;

    public List<Bucket> getAllBuckets() {
        return bucketRepository.findAll();
    }

    public Bucket saveBucket(Bucket bucket) {
        return bucketRepository.save(bucket);
    }

    public Optional<Bucket> getBucketById(int id) {
        return bucketRepository.findById(id);
    }

    public Bucket updateBucket(Bucket bucket) {
        return bucketRepository.save(bucket);
    }

    public void deleteBucket(int id) {
        bucketRepository.deleteById(id);
    }
}

