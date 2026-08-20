package com.geolink.findme.controller;

import com.geolink.findme.service.storageService.StorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Contrôleur REST assurant la diffusion des fichiers médias (images d'adresses).
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "Endpoints pour l'accès public aux fichiers médias et images")
public class FileController {

    private final StorageService storageService;

    @GetMapping("/{folder}/{filename:.+}")
    @Operation(summary = "Récupérer un fichier média", description = "Retourne l'image demandée avec le type MIME approprié")
    public ResponseEntity<Resource> getFile(
            @PathVariable String folder,
            @PathVariable String filename,
            HttpServletRequest request
    ) {
        Resource resource = storageService.loadAsResource(filename, folder);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // Ignorer la détection si impossible
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
