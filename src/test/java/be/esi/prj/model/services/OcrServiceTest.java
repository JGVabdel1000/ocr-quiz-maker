package be.esi.prj.model.services;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OcrServiceTest {

    private OcrService ocrService;

    @BeforeEach
    void setUp() {
        ocrService = new OcrService();
    }

    @Test
    void scan_shouldReturnExtractedText() throws Exception {
        // Arrange : on simule un Tesseract qui renvoie du texte
        try (MockedConstruction<Tesseract> mocked = mockConstruction(Tesseract.class,
                (mock, context) -> when(mock.doOCR(any(File.class))).thenReturn("Texte extrait"))
        ) {
            File fakeFile = new File("fake-path.png");

            // Act
            String result = ocrService.scan(fakeFile);

            // Assert
            assertEquals("Texte extrait", result);
            Tesseract tesseract = mocked.constructed().get(0);
            verify(tesseract).doOCR(fakeFile);
        }
    }

    @Test
    void scan_shouldThrowTesseractException_whenOcrFails() throws Exception {
        // Arrange : on simule un Tesseract qui lance une exception
        try (MockedConstruction<Tesseract> mocked = mockConstruction(Tesseract.class,
                (mock, context) -> when(mock.doOCR(any(File.class))).thenThrow(new TesseractException("Erreur OCR")))
        ) {
            File fakeFile = new File("fake-path.png");

            // Act + Assert
            assertThrows(TesseractException.class, () -> ocrService.scan(fakeFile));
        }
    }

    @Test
    void getTrainedDataDirectory_shouldThrowException_whenUnsupportedOS() {
        // On sauvegarde l'OS réel
        String originalOsName = System.getProperty("os.name");

        try {
            // Simulation temporaire d'un OS inconnu
            System.setProperty("os.name", "unknownOS");

            OcrService service = new OcrService();
            Exception exception = assertThrows(RuntimeException.class, () -> {
                service.scan(new File("fake.png"));
            });

            assertTrue(exception.getMessage().contains("Unable to find Tesseract trained data directory"));
        } finally {
            // On restaure l'OS réel pour ne pas casser les autres tests
            System.setProperty("os.name", originalOsName);
        }
    }

}
