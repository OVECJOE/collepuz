package org.mrserious;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDFQuestionExtractor {
    // Regex patterns for different question formats
    private static final Pattern QUESTION_PATTERNS = Pattern.compile(
    "(?i)(?:^|\\n)\\s*(?:" +
            "(?:\\d+\\.?\\s*)" +           // 1. Question
            "|(?:Q\\.?\\s*\\d+\\.?\\s*)" + // Q1. Question
            "|(?:Question\\s+\\d+\\.?\\s*)" + // Question 1.
            ")(.+?\\?)",
    Pattern.MULTILINE | Pattern.DOTALL
    );

    private static final Pattern OPTION_PATTERN = Pattern.compile(
        "(?i)(?:^|\\n)\\s*([A-D])\\.?\\s*[\\)\\.]?\\s*([^\\n]+?)(?=\\n|$)",
        Pattern.MULTILINE
    );

    private static final Pattern ANSWER_PATTERN = Pattern.compile(
        "(?i)(?:answer|correct|solution)\\s*:?\\s*([A-D])",
        Pattern.MULTILINE
    );

    public static List<QuizGame.Question> extractQuestionsFromPDFs(String folderPath) throws Exception {
        System.out.println("📖 Extracting questions from PDF files using PDFBox...");

        Path folder = Paths.get(folderPath);
        if (!Files.exists(folder) || !Files.isDirectory(folder)) {
            System.out.println("❌ Folder not found: " + folderPath);
            return Collections.emptyList();
        }

        List<QuizGame.Question> allQuestions = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.pdf")) {
            for (Path pdfFile : stream) {
                System.out.println("⏳ Processing: " + pdfFile.getFileName());
                List<QuizGame.Question> questions = extractFromSinglePDF(pdfFile.toFile());
                allQuestions.addAll(questions);
                System.out.printf("   ✅ Found %d questions\n", questions.size());
            }
        }

        if (allQuestions.isEmpty()) {
            System.out.println("⚠️ No questions extracted from PDFs. Check PDF format.");
            return Collections.emptyList();
        }

        System.out.printf("✅ Total extracted: %d questions from PDFs\n", allQuestions.size());
        return allQuestions;
    }

    private static List<QuizGame.Question> extractFromSinglePDF(File pdfFile) {
        List<QuizGame.Question> questions = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Clean up the text
            text = text.replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                .replaceAll("\\n{3,}", "\n\n");

            questions.addAll(extractQuestionsUsingPatterns(text));
            questions.addAll(extractQuestionsUsingHeuristics(text));
        } catch (IOException e) {
            System.out.printf("⚠️ Error reading PDF %s: %s\n", pdfFile.getName(), e.getMessage());
        }

        return removeDuplicates(questions);
    }

    private static List<QuizGame.Question> extractQuestionsUsingPatterns(String text) {
        List<QuizGame.Question> questions = new ArrayList<>();

        Matcher questionMatcher = QUESTION_PATTERNS.matcher(text);

        while (questionMatcher.find()) {
            String questionText = questionMatcher.group(1).trim();
            int questionEnd = questionMatcher.end();

            // Look for options after the question
            String remainingText = text.substring(questionEnd);
            String[] lines = remainingText.split("\n", 20); // Look at next 20 lines max

            Map<String, String> options = new HashMap<>();
            String correctAnswer = "";

            for (String line : lines) {
                Matcher optionMatcher = OPTION_PATTERN.matcher(line);
                if (optionMatcher.find()) {
                    String optionLabel = optionMatcher.group(1).toUpperCase();
                    String optionText = optionMatcher.group(2).trim();
                    options.put(optionLabel, optionText);
                }

                // Check for answer in this section
                Matcher answerMatcher = ANSWER_PATTERN.matcher(line);
                if (answerMatcher.find()) {
                    correctAnswer = answerMatcher.group(1).toUpperCase();
                }

                // Stop if we hit another question or empty lines
                if (line.trim().isEmpty() || QUESTION_PATTERNS.matcher(line).find()) {
                    break;
                }
            }

            if (options.size() >= 2) {
                List<String> optionsList = new ArrayList<>(options.values());

                // If no explicit correct answer found, try to detect it
                if (correctAnswer.isEmpty()) {
                    correctAnswer = detectCorrectAnswer(options, remainingText);
                }

                String correctAnswerText = options.getOrDefault(correctAnswer, optionsList.getFirst());
                questions.add(new QuizGame.Question(
                    cleanQuestionText(questionText),
                    optionsList,
                    correctAnswerText,
                    "medium",
                    "PDF Extract"
                ));
            }
        }

        return questions;
    }

    private static List<QuizGame.Question> extractQuestionsUsingHeuristics(String text) {
        List<QuizGame.Question> questions = new ArrayList<>();
        String[] paragraphs = text.split("\n\n+");

        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i];

            // Check if this looks like a question
            if (isLikelyQuestion(paragraph)) {
                List<String> options = new ArrayList<>();
                String correctAnswer = "";

                // Look in current and next few paragraph list as options
                for (int j = i; j < Math.min(i + 5, paragraphs.length); j++) {
                    String currentPara = paragraphs[j];

                    // Extract bullet points or numbered lists as options
                    String[] lines = currentPara.split("\n");
                    for (String line : lines) {
                        if (isLikelyOption(line)) {
                            String option = cleanOptionText(line);
                            if (!option.isEmpty() && !options.contains(option)) {
                                options.add(option);

                                // Check if this option is marked as correct
                                if (isMarkedAsCorrect(line)) {
                                    correctAnswer = option;
                                }
                            }
                        }
                    }
                }

                if (options.size() >= 2) {
                    if (correctAnswer.isEmpty()) {
                        correctAnswer = options.getFirst();
                    }

                    questions.add(new QuizGame.Question(
                        cleanQuestionText(paragraph),
                        options,
                        correctAnswer,
                        "medium",
                        "PDF Heuristic"
                    ));
                }
            }
        }

        return questions;
    }

    private static boolean isLikelyQuestion(String text) {
        text = text.toLowerCase();
        return text.contains("?") ||
            text.matches(".*\\b(?:what|which|who|when|where|why|how)\\b.*") ||
            text.matches(".*\\b(?:identify|choose|select|determine)\\b.*") ||
            text.matches("^\\s*\\d+\\..*") ||
            text.matches("^\\s*q\\s*\\d+.*");
    }

    private static boolean isLikelyOption(String line) {
        line = line.trim().toLowerCase();
        return line.matches("^[a-d][\\.\\)].*") ||
            line.matches("^\\([a-d]\\).*") ||
            line.matches("^[•·▪▫]\\s*.*") ||
            line.matches("^-\\s+.*") ||
            line.matches("^\\d+[\\.\\)]\\s+.*");
    }

    private static boolean isMarkedAsCorrect(String line) {
        line = line.toLowerCase();
        return line.contains("*") ||
            line.contains("✓") ||
            line.contains("correct") ||
            line.contains("answer") ||
            line.contains("✔") ||
            line.contains("√");
    }

    private static String detectCorrectAnswer(Map<String, String> options, String context) {
        context = context.toLowerCase();

        // Look for patterns like "the answer is A" or "correct answer: B"
        Pattern answerPattern = Pattern.compile(
            "(?:answer|correct|solution)\\s*(?:is|:)?\\s*([a-d])",
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = answerPattern.matcher(context);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }

        return options.keySet().stream().findFirst().orElse("A");
    }

    private static String cleanQuestionText(String text) {
        return text.replaceAll("^\\s*\\d+\\.?\\s*", "")
            .replaceAll("^\\s*Q\\.?\\s*\\d+\\.?\\s*", "")
            .replaceAll("^\\s*Question\\s+\\d+\\.?\\s*", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static String cleanOptionText(String text) {
        return text.replaceAll("^\\s*[A-Da-d][\\.\\)]\\s*", "")
            .replaceAll("^\\s*\\([A-Da-d]\\)\\s*", "")
            .replaceAll("^\\s*[•·▪▫-]\\s*", "")
            .replaceAll("^\\s*\\d+[\\.\\)]\\s*", "")
            .replaceAll("[*✓✔√]", "")
            .replaceAll("\\s*\\(correct\\)\\s*", "")
            .replaceAll("\\s*\\[correct\\]\\s*", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static List<QuizGame.Question> removeDuplicates(List<QuizGame.Question> questions) {
        Set<String> seen = new HashSet<>();
        List<QuizGame.Question> unique = new ArrayList<>();

        for (QuizGame.Question question : questions) {
            String key = question.text().toLowerCase().replaceAll("\\s+", " ").trim();
            if (!seen.contains(key) && key.length() > 10) {
                seen.add(key);
                unique.add(question);
            }
        }

        return unique;
    }
}
