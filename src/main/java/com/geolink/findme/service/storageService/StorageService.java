package com.geolink.findme.service.storageService;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface abstraite définissant les opérations de stockage de fichiers médias (photos d'adresses).
 */
public interface StorageService {

    /**
     * Stocke un fichier téléversé et retourne son nom unique généré.
     *
     * @param file   le fichier envoyé via MultipartFile
     * @param folder le sous-dossier de destination (ex: "addresses")
     * @return le nom du fichier généré (ex: "a1b2c3d4-5678-90ab-cdef-1234567890ab.jpg")
     */
    String store(MultipartFile file, String folder);

    /**
     * Charge un fichier sous forme de Ressource Spring.
     *
     * @param filename le nom du fichier
     * @param folder   le sous-dossier
     * @return la ressource chargée
     */
    Resource loadAsResource(String filename, String folder);

    /**
     * Supprime un fichier stocké s'il existe.
     *
     * @param filename le nom du fichier
     * @param folder   le sous-dossier
     */
    void delete(String filename, String folder);

    /**
     * Génère l'URL publique complète pour accéder au fichier.
     *
     * @param filename le nom du fichier
     * @param folder   le sous-dossier
     * @return l'URL publique complète (ex: "http://localhost:8080/api/files/addresses/filename.jpg")
     */
    String getPublicUrl(String filename, String folder);
}
