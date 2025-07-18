package com.loki.estructuraUsuarios.Controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class MapFileController {

    // Define the directory where files are stored
    private final Path uploadFolder = Paths.get("/tmp/uploads").toAbsolutePath().normalize();

    @GetMapping("/maps/{filename:.+}")
    public ResponseEntity<Resource> getMapFile(@PathVariable String filename) {
        try {
            // Resolve the file path
            Path filePath = uploadFolder.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Optionally, determine the file's content type
                String contentType = "text/html";
                try {
                    String detectedType = Files.probeContentType(filePath);
                    if (detectedType != null) {
                        contentType = detectedType;
                    }
                } catch (IOException ex) {
                    // Fallback to default if type detection fails
                }

                return ResponseEntity.ok()
                        // Set content disposition to inline so that the browser renders it
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
