package org.mrserious;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QuizGame {
    private static final Logger logger = Logger.getLogger(QuizGame.class.getName());
    private static final Scanner scanner = new Scanner(System.in);
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Free Quiz APIs
    private static final String OPENTB_API = "https://opentdb.com/api.php";
    private static final String QUIZ_API = "http://quizapi.io/api/v1/questions";

    public static void main() {
        System.out.println("🎓 Welcome to the Ultimate CollePuz 🎓");
        System.out.println("======================================");

        try {
            startQuizSession();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ An error occurred", e);
        }
    }

    private static void startQuizSession() throws Exception {
        System.out.print("\n\uD83D\uDCC1 Enter path to your PDF questions folder (or press Enter to use online quiz): ");
        String folderPath = scanner.nextLine().trim();

        List<Question> questions;
        if (folderPath.isEmpty()) {
            questions = getOnlineQuestions();
        } else {
            questions = PDFQuestionExtractor.extractQuestionsFromPDFs(folderPath);
        }

        if (questions.isEmpty()) {
            System.out.println("❌ No questions found. Exiting...");
            return;
        }

        Collections.shuffle(questions);

        System.out.println("\n\uD83C\uDFAF Select difficulty level:");
        System.out.println("1. Easy");
        System.out.println("2. Medium");
        System.out.println("3. Hard");
        System.out.print("Choice (1-3): ");

        int difficultyChoice = scanner.nextInt();
        scanner.nextLine(); // consume newline

        String difficulty = switch (difficultyChoice) {
            case 1 -> "easy";
            case 3 -> "hard";
            default -> "medium";
        };

        // Filter questions by difficulty if available
        List<Question> filteredQuestions = questions.stream()
                .filter(q -> q.difficulty().equalsIgnoreCase(difficulty) || q.difficulty().isEmpty())
                .toList();

        if (filteredQuestions.isEmpty()) {
            filteredQuestions = questions; // Use all questions if no match
        }

        startQuiz(filteredQuestions);
    }

    private static List<Question> getOnlineQuestions() {
        System.out.println("\n📚 Getting questions from online sources...");

        // Try Open Trivia Database first
        try {
            return getQuestionsFromOpenTDB();
        } catch (Exception e) {
            System.out.println("⚠️ Primary API failed, trying backup...");
            return getBackupQuestions();
        }
    }

    private static List<Question> getQuestionsFromOpenTDB() throws Exception {
        String url = OPENTB_API + "?amount=20&type=multiple";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("API request failed with status: " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode results = root.get("results");

        List<Question> questions = new ArrayList<>();

        for (JsonNode questionNode : results) {
            String questionText = decodeHtml(questionNode.get("question").asText());
            String correctAnswer = decodeHtml(questionNode.get("correct_answer").asText());
            String difficulty = questionNode.get("difficulty").asText();

            List<String> options = new ArrayList<>();
            options.add(correctAnswer);

            JsonNode incorrectAnswers = questionNode.get("incorrect_answers");
            for (JsonNode incorrectAns : incorrectAnswers) {
                options.add(decodeHtml(incorrectAns.asText()));
            }

            Collections.shuffle(options);
            questions.add(new Question(
                    questionText,
                    options,
                    correctAnswer,
                    difficulty,
                    questionNode.get("category").asText()
            ));
        }

        return questions;
    }

    private static List<Question> getBackupQuestions() {
        String url = QUIZ_API + "?apiKey=C1GLDQ8hj5UDimXvvuSOU6VV0aTkllhdEhKtyRUu&limit=20";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("API request failed with status: " + response.statusCode());
            }

            JsonNode results = objectMapper.readTree(response.body());

            List<Question> questions = new ArrayList<>();
            char[] optionSuffixes = {'a', 'b', 'c', 'd', 'e', 'f'};

            for (JsonNode questionNode : results) {
                String questionText = decodeHtml(questionNode.get("question").asText());
//                String explanation = decodeHtml(questionNode.get("explanation").asText());
                String correctAnswer = questionNode.get("correct_answer").asText();
                String difficulty = questionNode.get("difficulty").asText().toLowerCase();

                List<String> options = new ArrayList<>();
                for (char suffix : optionSuffixes) {
                    String option = questionNode.get("answers").get("answer_" + suffix).asText();
                    if (option != null) {
                        options.add(decodeHtml(option));
                    }
                }

                // Get a correct answer
                if (correctAnswer == null) {
                    for (char suffix : optionSuffixes) {
                        String ans = "answer_" + suffix;
                        boolean isCorrect = questionNode.get("correct_answers").get(ans + "_correct").asBoolean();
                        if (isCorrect) {
                            correctAnswer = ans;
                            break;
                        }
                    }
                }

                Collections.shuffle(options);
                questions.add(new Question(
                        questionText,
                        options,
                        questionNode.get(correctAnswer).asText(),
                        difficulty,
                        questionNode.get("category").asText()
                ));
            }

            return questions;
        } catch (Exception e) {
            return List.of(
                    new Question(
                            "What is the capital of France?",
                            List.of("Paris", "London", "Berlin", "Madrid"),
                            "Paris",
                            "easy",
                            "Geography"
                    ),
                    new Question(
                            "Which planet is known as the Red Planet?",
                            List.of("Mars", "Venus", "Jupiter", "Saturn"),
                            "Mars",
                            "easy",
                            "Science"
                    ),
                    new Question(
                            "What is 2 + 2?",
                            List.of("4", "3", "5", "6"),
                            "4",
                            "easy",
                            "Mathematics"
                    )
            );
        }
    }

    private static void startQuiz(List<Question> questions) {
        System.out.println("\n🚀 Starting Quiz! Type 'quit' anytime to exit.");
        System.out.println("===============================================");

        int score = 0;
        int totalQuestions = Math.min(20, questions.size());

        for (int i = 0; i < totalQuestions; i++) {
            Question question = questions.get(i);
            System.out.printf("\n📝 Question %d/%d [%s - %s]\n", i + 1, totalQuestions, question.difficulty().toUpperCase(), question.category());
            System.out.println("─".repeat(50));
            System.out.println(question.text());

            List<String> shuffledOptions = new ArrayList<>(question.options());
            Collections.shuffle(shuffledOptions);

            for (int j = 0; j < shuffledOptions.size(); j++) {
                System.out.printf("%d. %s\n", j + 1, shuffledOptions.get(j));
            }

            System.out.print("\n💭 Your answer (1-" + shuffledOptions.size() + "): ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("quit")) {
                System.out.println("\n👋 Thanks for playing! Final score: " + score + "/" + i);
                return;
            }

            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= shuffledOptions.size()) {
                    String selectedAnswer = shuffledOptions.get(choice - 1);
                    if (selectedAnswer.equals(question.correctAnswer())) {
                        System.out.println("✅ Correct! Well done!");
                        score++;
                    } else {
                        System.out.println("❌ Wrong! The correct answer was: " + question.correctAnswer());
                    }
                } else {
                    System.out.println("❌ Invalid choice! The correct answer was: " + question.correctAnswer());
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Please enter a valid number! The correct answer was: " + question.correctAnswer());
            }

            // Show current score
            System.out.printf("📊 Current Score: %d/%d\n", score, i + 1);
            if (i < totalQuestions - 1) {
                System.out.print("Press Enter to continue...");
                scanner.nextLine();
            }
        }

        showFinalResults(score, totalQuestions);
    }

    private static void showFinalResults(int score, int total) {
        System.out.println("\n🎉 Quiz Complete! 🎉");
        System.out.println("═══════════════════════");
        System.out.printf("📊 Final Score: %d/%d (%.1f%%)\n",
                score, total, (score * 100.0 / total));

        String performance = switch (score * 100 / total) {
            case int p when p >= 90 -> "🏆 Outstanding! You're a quiz master!";
            case int p when p >= 80 -> "🥇 Excellent work! Keep it up!";
            case int p when p >= 70 -> "🥈 Good job! You're doing well!";
            case int p when p >= 60 -> "🥉 Not bad! Room for improvement!";
            default -> "📚 Keep studying! You'll get better!";
        };

        System.out.println(performance);
        System.out.println("\n💡 Tip: Try different difficulty levels to challenge yourself!");
    }

    private static String decodeHtml(String text) {
        return text.replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&rdquo;", "\"")
            .replace("&ldquo;", "\"")
            .replace("&rsquo;", "'");
    }

    public record Question(
        String text,
        List<String> options,
        String correctAnswer,
        String difficulty,
        String category
    ) {}
}