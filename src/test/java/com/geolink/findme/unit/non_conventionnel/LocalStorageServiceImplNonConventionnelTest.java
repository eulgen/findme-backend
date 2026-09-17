package com.geolink.findme.unit.non_conventionnel;

import com.geolink.findme.exception.InvalidFileException;
import com.geolink.findme.service.storageService.LocalStorageServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tests unitaires LocalStorageServiceImpl (Cas Non Conventionnels)")
class LocalStorageServiceImplNonConventionnelTest {

    private LocalStorageServiceImpl storageService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        storageService = new LocalStorageServiceImpl();
        ReflectionTestUtils.setField(storageService, "storageLocation", tempDir.toString());
        ReflectionTestUtils.setField(storageService, "backendBaseUrl", "http://localhost:8080");
        storageService.init();
    }

    @Test
    @DisplayName("Devrait lever InvalidFileException si le fichier fourni est null")
    void devrait_lever_exception_si_fichier_null() {
        assertThatThrownBy(() -> storageService.store(null, "profiles"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Le fichier fourni est vide");
    }

    @Test
    @DisplayName("Devrait lever InvalidFileException si le fichier fourni est vide (0 octet)")
    void devrait_lever_exception_si_fichier_vide() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                new byte[0]
        );

        assertThatThrownBy(() -> storageService.store(emptyFile, "profiles"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Le fichier fourni est vide");
    }

    @Test
    @DisplayName("Devrait lever InvalidFileException si la taille du fichier dépasse 5 Mo")
    void devrait_lever_exception_si_taille_fichier_depasse_5_mo() {
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6 Mo
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeContent
        );

        assertThatThrownBy(() -> storageService.store(largeFile, "profiles"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("La taille du fichier dépasse la limite autorisée de 5 Mo");
    }

    @Test
    @DisplayName("Devrait lever InvalidFileException si le format de fichier n'est pas autorisé (ex: PDF)")
    void devrait_lever_exception_si_format_fichier_non_supporte() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "content".getBytes()
        );

        assertThatThrownBy(() -> storageService.store(pdfFile, "profiles"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Format de fichier non supporté");
    }

    @Test
    @DisplayName("Devrait lever InvalidFileException si le fichier contient une tentative de traversée de répertoire (Path Traversal)")
    void devrait_lever_exception_si_tentative_path_traversal() {
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "file",
                "../../malicious.sh",
                "image/jpeg",
                "content".getBytes()
        );

        assertThatThrownBy(() -> storageService.store(maliciousFile, "profiles"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("interdit");
    }
}
