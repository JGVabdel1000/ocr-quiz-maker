package be.esi.prj.model.services;

import be.esi.prj.App;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

/**
 * Service class for performing Optical Character Recognition (OCR) using Tesseract.
 * The class configures the Tesseract OCR library based on the operating system
 * and provides a method to scan an image and extract text.
 */
public class OcrService {

    private static final String LANGUAGE = "fra";
    //private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    /**
     * Scans an image file and extracts text using Tesseract OCR.
     *
     * @param file The image file to process.
     * @return The extracted text from the image.
     * @throws TesseractException If Tesseract encounters an error during OCR.
     */
    public String scan(File file) throws TesseractException {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(getTrainedDataDirectory());
        tesseract.setLanguage(LANGUAGE);

        return tesseract.doOCR(file);
    }

    private String getTrainedDataDirectory() {
        try {
            String dataDirectory;

            if (isLinuxOrMac()) {
                dataDirectory = getLinuxOrMacDataDirectory();
            } else if (isWindows()) {
                dataDirectory = getWindowsDataDirectory();
            } else {
                throw new UnsupportedOperationException("Unsupported operating system: " +  System.getProperty("os.name"));
            }

            return dataDirectory;
        } catch (Exception e) {
            throw new RuntimeException("Unable to find Tesseract trained data directory!", e);
        }
    }

    private boolean isLinuxOrMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nix") || os.contains("nux") || os.contains("mac");
    }

    private boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }
    private String getLinuxOrMacDataDirectory() {
        return App.class.getResource("data").getFile();
    }

    private String getWindowsDataDirectory() throws URISyntaxException {
        // On Windows, construct the path to the 'data' directory
        String dataDirectory = Paths.get(
                App.class.getClassLoader().getResource("be/esi/prj/data").toURI()
        ).toFile().getAbsolutePath();

        // Ensure that TESSDATA_PREFIX points to the correct directory
        System.setProperty("TESSDATA_PREFIX", dataDirectory + "/");

        return dataDirectory;
    }
}