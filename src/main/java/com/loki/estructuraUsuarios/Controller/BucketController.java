package com.loki.estructuraUsuarios.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.loki.estructuraUsuarios.Models.Bucket;
import com.loki.estructuraUsuarios.Service.BucketService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/bucket")
public class BucketController {

    @Autowired
    private BucketService bucketService;

    @GetMapping
    public List<Bucket> getAllBuckets() {
        return bucketService.getAllBuckets();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Bucket> getBucketById(@PathVariable int id) {
        Optional<Bucket> bucket = bucketService.getBucketById(id);
        return bucket.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Bucket createBucket(@RequestBody Bucket bucket) {
        return bucketService.saveBucket(bucket);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Bucket> updateBucket(@PathVariable int id, @RequestBody Bucket bucketDetails) {
        Optional<Bucket> bucket = bucketService.getBucketById(id);
        if (bucket.isPresent()) {
            bucketDetails.setId(id);
            return ResponseEntity.ok(bucketService.updateBucket(bucketDetails));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBucket(@PathVariable int id) {
        bucketService.deleteBucket(id);
        return ResponseEntity.noContent().build();
    }
}

