package be.esi.prj.utils;

import java.io.File;

// Classe pour stocker le résultat du quiz
public class QuizResult {
    private final File file;
    private final String question;
    private final String answer;

    public QuizResult(File file, String question, String answer) {
        this.file = file;
        this.question = question;
        this.answer = answer;
    }

    public File getFile() { return file; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
}
