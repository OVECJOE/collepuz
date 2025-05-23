# 🎓 CLI Quiz Game for College Students

A minimalist yet powerful command-line quiz game that helps college students practice with questions from their own PDF materials or from online quiz APIs.

## ✨ Features

- **📁 PDF Question Extraction**: Automatically extract questions from your past exam PDFs
- **🌐 Online Quiz Integration**: Fallback to free online quiz APIs when no PDFs provided
- **🎯 Difficulty Levels**: Choose from Easy, Medium, or Hard questions
- **📊 Smart Scoring**: Real-time scoring with performance feedback
- **🎲 Randomized Questions**: Questions and answer options are shuffled for better practice
- **💡 Multi-format Support**: Handles various question formats commonly found in academic materials

## 🚀 Quick Start

### Prerequisites

- **Java 24** (with preview features enabled)
- **Maven 3.8+** for dependency management

### Installation

1. **Clone or download the project files**
2. **Create the project structure**:
   ```
   quiz-game/
   ├── src/main/java/
   │   ├── QuizGame.java
   │   └── PDFQuestionExtractor.java
   ├── pom.xml
   └── README.md
   ```

3. **Build the project**:
   ```bash
   mvn clean compile
   ```

4. **Create executable JAR**:
   ```bash
   mvn package
   ```

### Running the Game

**Option 1: Run directly with Maven**
```bash
mvn exec:java -Dexec.mainClass="QuizGame"
```

**Option 2: Run the JAR file**
```bash
java --enable-preview -jar target/quiz-game.jar
```

**Option 3: Run with Java directly**
```bash
java --enable-preview -cp target/classes QuizGame
```

## 📖 How to Use

### 1. Starting the Game
When you run the game, you'll see:
```
🎓 Welcome to the Ultimate CLI Quiz Game! 🎓
═══════════════════════════════════════════

📁 Enter path to your PDF question folder (or press Enter to use online quiz):
```

### 2. Choose Your Question Source

**Option A: Use Your PDF Files**
- Enter the path to a folder containing your PDF files
- The game will extract questions automatically
- Supports various question formats (multiple choice, numbered questions, etc.)

**Option B: Use Online Questions**
- Just press Enter without typing a path
- The game will fetch questions from free online quiz APIs
- Great for general knowledge or when you don't have PDF materials

### 3. Select Difficulty
Choose from:
- **Easy**: Basic level questions
- **Medium**: Intermediate difficulty
- **Hard**: Challenging questions

### 4. Answer Questions
- Questions are presented one at a time
- Type the number corresponding to your answer
- Get immediate feedback on whether you're correct
- See your running score after each question

### 5. View Final Results
After completing the quiz, you'll see:
- Your final score and percentage
- Performance feedback
- Encouragement to try different difficulty levels

## 📄 PDF Format Requirements

For best results, your PDF files should contain questions in these formats:

### Supported Question Formats:
```
1. What is the capital of France?
A. London
B. Paris
C. Berlin
D. Madrid
Answer: B

Q1. Which programming language is used for this project?
a) Python
b) Java
c) C++
d) JavaScript
Correct Answer: b

Question 3: What does API stand for?
(A) Application Programming Interface
(B) Advanced Programming Integration
(C) Automated Process Integration
(D) Application Process Interface
```

### Tips for Better PDF Extraction:
- Use clear numbering (1., 2., 3. or Q1, Q2, etc.)
- Include answer choices labeled with A, B, C, D or a, b, c, d
- Mark correct answers explicitly when possible
- Avoid complex formatting or tables

## 🔧 API Integration

The game uses these free APIs as fallbacks:

1. **Open Trivia Database** (`https://opentdb.com/api.php`)
    - Free trivia questions
    - Multiple categories and difficulties
    - No API key required

2. **Backup Questions**: Built-in questions when APIs are unavailable

### Making HTTP Requests in Java 24

The game demonstrates modern Java HTTP client usage:

```java
HttpClient httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build();

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(apiUrl))
    .timeout(Duration.ofSeconds(30))
    .header("Accept", "application/json")
    .build();

HttpResponse<String> response = httpClient.send(request, 
    HttpResponse.BodyHandlers.ofString());
```

## 🛠️ Technical Details

### Dependencies Used:
- **Jackson**: JSON parsing for API responses
- **Apache PDFBox**: PDF text extraction and processing
- **Java 24 Features**: Records, pattern matching, switch expressions

### Key Components:
- `QuizGame.java`: Main game logic and API integration
- `PDFQuestionExtractor.java`: Advanced PDF processing with multiple extraction strategies
- Pattern matching for question detection
- Heuristic-based extraction for various PDF formats

## 🎯 Customization Options

### Add New APIs
To integrate additional quiz APIs, modify the `getOnlineQuestions()` method:

```java
private static List<Question> getQuestionsFromNewAPI() {
    String url = "https://your-quiz-api.com/questions";
    // Add your API logic here
}
```

### Modify Question Extraction
Enhance PDF extraction by updating patterns in `PDFQuestionExtractor.java`:

```java
private static final Pattern CUSTOM_PATTERN = Pattern.compile(
    "your-custom-regex-pattern",
    Pattern.MULTILINE
);
```

### Change Scoring System
Modify the `showFinalResults()` method to implement different scoring logic.

## 🚨 Troubleshooting

### Common Issues:

1. **"No questions found in PDFs"**
    - Check PDF format matches supported patterns
    - Ensure PDFs contain text (not scanned images)
    - Try with different PDF files

2. **"API request failed"**
    - Check internet connection
    - APIs might be temporarily down (game will use backup questions)

3. **Java version errors**
    - Ensure Java 24 is installed
    - Use `--enable-preview` flag when running

4. **Build failures**
    - Check Maven version (3.8+ required)
    - Ensure all dependencies are downloaded

## 🎓 Perfect for Students

This tool is ideal for:
- **Exam Preparation**: Practice with your own past papers
- **Study Groups**: Share PDF question banks
- **Quick Reviews**: Fast quiz sessions between classes
- **Self Assessment**: Track your progress over time

## 📚 Learning Opportunities

While using this quiz game, you'll learn about:
- Modern Java HTTP client usage
- JSON processing with Jackson
- PDF text extraction techniques
- Pattern matching and regex
- Command-line interface design
- API integration best practices

## 🤝 Contributing

Feel free to enhance the game by:
- Adding support for more PDF formats
- Integrating additional quiz APIs
- Improving question extraction algorithms
- Adding new difficulty assessment features

## 📝 License

This project is open source and available for educational use.

---

**Happy Studying! 🎓✨**