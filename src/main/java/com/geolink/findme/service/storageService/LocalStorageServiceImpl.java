package com.geolink.findme.service.storageService;

import com.geolink.findme.exception.InvalidFileException;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Implémentation du service de stockage basé sur le système de fichiers local.
 */
@Service
@Slf4j
public class LocalStorageServiceImpl implements StorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 Mo

    @Value("${app.storage.location:uploads}")
    private String storageLocation;

    @Value("${app.backend.base-url:http://localhost:8080}")
    private String backendBaseUrl;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(storageLocation).toAbsolutePath().normalize();
            Files.createDirectories(this.rootLocation);
            log.info("Dossier de stockage initialisé sous : {}", this.rootLocation);
        } catch (IOException e) {
            log.error("Impossible de créer le dossier de stockage : {}", storageLocation, e);
            throw new RuntimeException("Impossible d'initialiser le stockage de fichiers", e);
        }
    }

    @Override
    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Le fichier fourni est vide");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("La taille du fichier dépasse la limite autorisée de 5 Mo");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidFileException("Format de fichier non supporté. Formats acceptés : JPEG, PNG, WEBP");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg"
        );
        if (originalFilename.contains("..")) {
            throw new InvalidFileException("Tentative de stockage hors du dossier cible interdit");
        }
        String extension = "";
        int extIndex = originalFilename.lastIndexOf(".");
        if (extIndex >= 0) {
            extension = originalFilename.substring(extIndex).toLowerCase();
        } else {
            extension = ".jpg";
        }

        String generatedFilename = UUID.randomUUID().toString() + extension;

        try {
            Path targetFolder = this.rootLocation.resolve(folder).normalize();
            if (!Files.exists(targetFolder)) {
                Files.createDirectories(targetFolder);
            }

            Path destinationFile = targetFolder.resolve(generatedFilename).normalize();
            if (!destinationFile.getParent().equals(targetFolder)) {
                throw new InvalidFileException("Tentative de stockage hors du dossier cible interdit");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Fichier enregistré avec succès : {}", destinationFile);
            return generatedFilename;
        } catch (IOException e) {
            log.error("Erreur lors de l'enregistrement du fichier {}", originalFilename, e);
            throw new InvalidFileException("Échec de la sauvegarde du fichier sur le disque");
        }
    }

    @Override
    public Resource loadAsResource(String filename, String folder) {
        try {
            Path file = this.rootLocation.resolve(folder).resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new InvalidFileException("Fichier non trouvé : " + filename);
            }
        } catch (MalformedURLException e) {
            throw new InvalidFileException("Fichier introuvable : " + filename);
        }
    }

    @Override
    public void delete(String filename, String folder) {
        if (filename == null || filename.isBlank()) {
            return;
        }
        try {
            Path file = this.rootLocation.resolve(folder).resolve(filename).normalize();
            Files.deleteIfExists(file);
            log.info("Fichier supprimé : {}", file);
        } catch (IOException e) {
            log.warn("Impossible de supprimer le fichier : {}", filename, e);
        }
    }

    @Override
    public String getPublicUrl(String filename, String folder) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        if (filename.startsWith("http://") || filename.startsWith("https://")) {
            return filename;
        }
        String cleanBaseUrl = backendBaseUrl.replaceAll("/+$", "");
        return cleanBaseUrl + "/api/files/" + folder + "/" + filename;
    }
}
