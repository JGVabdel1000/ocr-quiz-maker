package be.esi.prj.utils;

import be.esi.prj.model.orm.Folder;
import be.esi.prj.model.services.GeminiApiService;
import be.esi.prj.model.services.OcrService;
import be.esi.prj.model.services.QuizService;
import be.esi.prj.model.services.UserService;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;

public class QuizTask extends Thread {
    private final File file;
    private final List<Folder> availableFolders;
    private final OcrService ocrService;
    private final QuizService quizService;
    private final UserService userService;
    private final Logger logger;
    private final List<QuizResult> quizResults;

    public QuizTask(File file, List<Folder> availableFolders,
                    OcrService ocrService, QuizService quizService,
                    UserService userService, Logger logger, List<QuizResult> quizResults) {
        this.file = file;
        this.availableFolders = availableFolders;
        this.ocrService = ocrService;
        this.quizService = quizService;
        this.userService = userService;
        this.logger = logger;
        this.quizResults = quizResults;
    }

    @Override
    public void run() {
        try {
            String text = ocrService.scan(file);
            if (text == null) {
                logger.info("Aucun texte extrait du fichier {}", file.getName());
                return;
            }

            // Génère le quiz sans afficher d'alerte ici
            List<String> quiz = GeminiApiService.generateQuizFromText(text);
            String question = quiz.get(0);
            String answer = quiz.get(1);

            // Ajoute le résultat dans la collection partagée (thread-safe)
            synchronized (quizResults) {
                quizResults.add(new QuizResult(file, question, answer));
            }
        } catch (Exception e) {
            logger.error("Error processing file: " + file.getName(), e);
        }
    }
}