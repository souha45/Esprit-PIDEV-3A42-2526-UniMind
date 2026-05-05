# UniMind — Desktop Application

> This project was developed as part of the **PIDEV – 3rd Year Engineering Program** at **Esprit School of Engineering** (Academic Year 2025–2026).

**UniMind** is a desktop application built with **Java** and **JavaFX**, designed to support student mental health by providing tools for psychological follow-up, meditation sessions, event management, AI-powered emotional recommendations, and questionnaire-based assessments.

---

## ✨ Features

- 🧠 **Psychological questionnaires** and mental health assessments
- 🧘 **Meditation session management** with audio/video playback, YouTube-like progress bar, +10s/−10s seek controls
- 🎧 **Ambient sounds panel** powered by the Freesound API (ocean, rain, forest, fire…)
- 🤖 **AI emotional recommendation** — select your mood (😊😔😫😤😩😐😰), get 3 personalized meditation sessions suggested by Gemini AI
- 🎤 **Voice assistant** — navigate the app hands-free using spoken commands (powered by Vosk, fully offline)
- 📅 **Event and participation management**
- 👤 **User profile and role management** (Student, Psychologist, Admin, Responsable)
- 📊 **Admin dashboard** with statistics
- 🔐 **Authentication**, account verification and password reset
- 💬 **Forum** — community posts and comments
- 💊 **Psychological treatment** and follow-up tracking
- ❤️ **Favorites** — bookmark meditation sessions and events
- ✨ **Daily inspirational quote** fetched from ZenQuotes API

---

## 🛠 Tech Stack

### Frontend
- JavaFX 21 (FXML + CSS)
- Custom UI components with inline styling

### Backend
- Java 17
- JDBC (direct SQL — no ORM)
- MySQL

### External APIs & Libraries
| Library / API | Usage |
|---|---|
| **Gemini 2.5 Flash** (Google AI) | Emotional meditation recommendations |
| **Vosk** (offline speech recognition) | Voice navigation assistant |
| **Freesound API** | Ambient relaxation sounds |
| **ZenQuotes API** | Daily inspirational quotes |
| **Jackson** | JSON parsing |
| **JNA** | Native bindings for Vosk |

---

## 🏗 Architecture

UniMind follows the **MVC pattern** adapted for JavaFX:

```
src/
├── main/
│   ├── java/org/example/
│   │   ├── controllers/        # JavaFX FXML controllers
│   │   ├── entities/           # Java entity classes (models)
│   │   ├── services/           # Business logic & API services
│   │   │   ├── GeminiRecommandationService.java
│   │   │   ├── FreesoundService.java
│   │   │   └── VoiceAssistantService.java
│   │   ├── enums/              # Java enumerations
│   │   └── utils/              # Database connection, session, navigation
│   └── resources/
│       ├── org/example/views/  # FXML layout files
│       ├── css/                # Stylesheets (admin.css, etudiant.css)
│       ├── images/             # Static assets
│       └── vosk-model-fr/      # Vosk French language model (offline ASR)
```

---

## 👥 Contributors

| Name |
|---|
| Louati Islem |
| Abdelkefi Nermine |
| Hraghui Ghofrane |
| Khenissi Souha |
| Gouider Omaima |
| Eddouch Nadine |

---

## 🎓 Academic Context

Developed at **Esprit School of Engineering** — Tunisia  
**PIDEV – 3A42** | Academic Year **2025–2026**

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+**
- **Maven 3.8+**
- **MySQL 8+**
- A microphone (optional — for the voice assistant)

### Installation

```bash
# Clone the repository
git clone https://github.com/your-org/unimind-javafx.git
cd unimind-javafx
```

### Database Setup

```sql
-- Create the database
CREATE DATABASE unimind;
```

Then import the provided SQL dump:

```bash
mysql -u root -p unimind < unimind_dump.sql
```

### Configure the Database Connection

Edit `src/main/java/org/example/utils/MyDataBase_Unimind.java`:

```java
private static final String URL      = "jdbc:mysql://localhost:3306/unimind";
private static final String USER     = "your_mysql_user";
private static final String PASSWORD = "your_mysql_password";
```

### Configure API Keys

**Gemini AI** — get a free key at [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey):

```java
// GeminiRecommandationService.java
private static final String API_KEY = "YOUR_GEMINI_API_KEY";
```

**Freesound** — get a free key at [freesound.org/apiv2/apply](https://freesound.org/apiv2/apply/):

```java
// FreesoundService.java
private static final String API_KEY = "YOUR_FREESOUND_API_KEY";
```

### Voice Assistant Setup (optional)

1. Download the French model (~40 MB) from [alphacephei.com/vosk/models](https://alphacephei.com/vosk/models) → `vosk-model-small-fr-0.22`
2. Rename the folder to `vosk-model-fr`
3. Place it in `src/main/resources/vosk-model-fr/`

### Run the Application

```bash
mvn clean javafx:run
```

Or open the project in **IntelliJ IDEA** and run `Main.java`.

---

## 🎤 Voice Commands

Once the voice assistant is active (click the 🎤 button in the sidebar), you can say:

| Say… | Navigates to |
|---|---|
| *"dashboard"* / *"accueil"* | Dashboard |
| *"méditation"* / *"séances"* | Meditation sessions |
| *"traitements"* | Treatments |
| *"rendez-vous"* | Appointments |
| *"événements"* | Events |
| *"mes favoris"* | Favorite sessions |
| *"profil"* | My profile |
| *"déconnecter"* / *"quitter"* | Logout |

---

## 📦 Key Maven Dependencies

```xml
<!-- JavaFX -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>17.0.6</version>
</dependency>
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-media</artifactId>
    <version>17.0.6</version>
</dependency>

<!-- Vosk — offline speech recognition -->
<dependency>
    <groupId>com.alphacephei</groupId>
    <artifactId>vosk</artifactId>
    <version>0.3.45</version>
</dependency>
<dependency>
    <groupId>net.java.dev.jna</groupId>
    <artifactId>jna</artifactId>
    <version>5.7.0</version>
</dependency>

<!-- Jackson — JSON parsing -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>

<!-- MySQL connector -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```

---

## 🚧 Deployment

Deployment in progress — Coming soon

---

## 🙏 Acknowledgments

Special thanks to our professors and supervisors at **Esprit School of Engineering** for their guidance and support throughout this project.
