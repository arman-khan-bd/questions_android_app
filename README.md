# Govt Exam Prep - Native Android Client (Java)

A production-ready native Android application built in **Java** for **Government Job Preliminary (MCQ) Exam Preparation** (BCS, Bank, Primary, BPSC, NTRCA), engineered to integrate with a **Laravel + Inertia.js (Sanctum REST API) cPanel backend**.

---

## ⚡ Envato / CodeCanyon Developer Quick-Start: 1-File API Setup

All server URLs, REST API endpoints, timeouts, app branding, and default engine rules are **centrally stored in one single file for effortless developer editing**:

👉 **[`app/src/main/java/com/govtexam/prep/AppConfig.java`](file:///app/src/main/java/com/govtexam/prep/AppConfig.java)**

```java
// 1. Change your backend server URL in 10 seconds:
public static final String SERVER_URL = "https://music.e-bd.shop/";

// 2. All 25+ REST API routes are registered as constants here:
public static final String ENDPOINT_APP_CONFIG           = API_PREFIX + "app-config";
public static final String ENDPOINT_HOME_FEED            = API_PREFIX + "home-feed";
public static final String ENDPOINT_LOGIN                = API_PREFIX + "auth/login";
public static final String ENDPOINT_QUESTIONS            = API_PREFIX + "questions";
public static final String ENDPOINT_CUSTOM_EXAM_GENERATE = API_PREFIX + "exams/custom-generate";
public static final String ENDPOINT_EXAM_SUBMIT          = API_PREFIX + "exams/{id}/submit";
// ... (Edit AppConfig.java to customize any route!)
```

📖 **Detailed Step-by-Step Guide:** Read the full [DOCUMENTATION.md](file:///DOCUMENTATION.md) for instructions on changing package name, branding, Firebase notifications, and building signed APK / AAB bundles.

---

## Technical Specifications & Compatibility

- **Language:** Java (Java 8+ language features enabled with `coreLibraryDesugaringEnabled true`).
- **Compatibility:** **Android 5.0 Lollipop (`minSdk = 21`)** through **Android 16 (`targetSdk = 36`, `compileSdk = 36`)**.
- **Architecture:** MVVM (Model - View - ViewModel / Repository) with Room SQLite local persistence, Retrofit 2 REST networking, and Glide v4 WebP image caching.
- **Multidex:** Enabled via `androidx.multidex:multidex:2.0.1` and `GovtExamApp extends MultiDexApplication`.
- **TLS 1.2 Backporting:** Google Play Services `ProviderInstaller.installIfNeededAsync` + fallback `Tls12SocketFactory` to eliminate SSL/TLS handshake failures on Android 5.0/5.1 when connecting to modern Let's Encrypt / cPanel SSL certificates.
- **HTTP Client:** OkHttp `4.12.0` (the final release supporting Android 5.0 / API 21).

---

## Key Modules & Features

### 1. Bootstrap, Security & Remote Maintenance
- **Splash & Version Handshake:** [SplashActivity.java](file:///app/src/main/java/com/govtexam/prep/ui/splash/SplashActivity.java) calls `GET /api/v1/config` to check maintenance status, mandatory version thresholds, and remote brand theming.
- **Maintenance Interceptor Screen:** [MaintenanceActivity.java](file:///app/src/main/java/com/govtexam/prep/ui/splash/MaintenanceActivity.java) provides a non-dismissible lockout with dynamic admin messages and live retry.
- **APK OTA Installer:** Background download of update APKs using `DownloadManager` and installation via `FileProvider` (`content://` URI) with Android 5.0–16 permissions.
- **Account Suspension & Disabled Interceptor:** [AccountStatusInterceptor.java](file:///app/src/main/java/com/govtexam/prep/data/remote/interceptor/AccountStatusInterceptor.java) catches HTTP 403 suspension responses, displays reason and expiration time dialog, and clears local credentials.

### 2. Room SQLite Offline Engine & Anti-Duplication
- **Composite Anti-Duplication:** [NotificationEntity.java](file:///app/src/main/java/com/govtexam/prep/data/local/entity/NotificationEntity.java) has a composite unique index on `(recipientId, actorId, questionId, type)` with `OnConflictStrategy.IGNORE` to guarantee zero duplicate push alerts or in-app notifications.
- **Offline Exam Resilience:** [ExamRepository.java](file:///app/src/main/java/com/govtexam/prep/data/repository/ExamRepository.java) persists pending submissions in [OfflineExamSubmission](file:///app/src/main/java/com/govtexam/prep/data/local/entity/OfflineExamSubmission.java) if connectivity drops at exam conclusion, presenting an instant calculated result to avoid user panic and auto-syncing when internet returns.
- **Local Telemetry & Caching:** Full SQLite caching for questions, subjects, categories, topics, and user profile data.

### 3. Navigation & Home Arena
- **Top Sliding Search Bar:** Animated expandable search bar with real-time debounced suggestions (`GET /api/v1/search/suggestions`), recent searches, and popular tag chips.
- **5-Item Bottom Navigation:** Home, Performance Statistics, Exam (+) Action Sheet, Facebook-Style Profile, and Navigation Drawer trigger.
- **Home Feed:** Dynamic banner carousel with auto-slide, horizontal topics chips, 2-column subject grid, and inline Custom Exam Builder.

### 4. MCQ Practice, Moderation & Comments
- **Question Card (`QuestionCardAdapter`):** Instant option evaluation, creator direct key reveal, rank-neutral answer voting, star bookmarking, and report filing.
- **Moderated Comments (`CommentBottomSheetFragment`):** Multi-level nested replies, bad-word filtering with statuses (`masked`, `flagged_pending`, or 422 blocked).
- **Report Dialog (`ReportDialogFragment`):** Submit reports with reason tags and custom student notes.
- **Question Creator (`CreateQuestionActivity`):** Community question submissions with image upload, subject/category selectors, and `auto_publish_user_questions` remote flag handling.

### 5. Exam Arena & Scoring Engine
- **Exam Arena (`ExamArenaActivity`):** Countdown timer with color transitions (amber at 5 min, red at 1 min), persistent question palette bottom sheet, and automatic submission upon expiration.
- **Results (`ExamResultActivity`):** Exact mark calculation, negative deduction subtraction (e.g., -0.25), percentile ranking, pass/fail badges, and duration analytics.
- **Answer Key Preview (`ExamAnswerPreviewActivity`):** Full post-exam review highlighting correct keys in green, student mistakes in red, community accuracy percentages, and comprehensive explanations.

### 6. Leaderboard & Performance Analytics
- **Leaderboard (`LeaderboardFragment`):** Top-3 Gold/Silver/Bronze podium card, timeframe choice chips (Daily, Weekly, All Time), difficulty-weighted modes (Easy, Medium, Hard, Custom, Top Creators), and sticky user standing banner.
- **Analytics (`StatisticsFragment`):** MPAndroidChart suite:
  - Donut PieChart for pass/fail distribution.
  - Stacked BarChart for Easy vs Medium vs Hard difficulty accuracy.
  - Cubic Spline LineChart for 15-exam score trends.
  - Horizontal BarChart for Subject Mastery with 60% passing benchmark limit line.
  - "Practice Weakest Topics" CTA to drill down into low-mastery subjects.

### 7. Facebook-Style Profile & Edit Profile
- **Profile (`ProfileFragment` / `UserProfileActivity`):** 16:9 Cover photo, overlapping circular avatar, academic credentials card (University, Degree, Graduation Year), and ViewPager2 tabs:
  1. *Exams:* Attended exam history with review key navigation.
  2. *Created Questions:* Author submissions with telemetry view counters and voting stats.
  3. *Custom Exams:* Saved configurations with retake shortcuts.
  4. *Starred:* Bookmarked questions for fast revision.
  5. *Reported:* Status tracking with creator resolve & edit buttons.
  6. *Mistake Notebook:* Automatically accumulated exam errors for targeted re-practice.
- **Edit Profile (`EditProfileActivity`):** Multipart updates for avatar and cover photos in `.webp` format and academic details.

---

## Project Structure

```
questions_android_app/
├── app/
│   ├── build.gradle                          # App build configuration (minSdk 21, desugaring, MultiDex)
│   ├── proguard-rules.pro                    # R8/Proguard optimization and serialization rules
│   └── src/main/
│       ├── AndroidManifest.xml               # Complete activity, service, and FileProvider manifest
│       ├── java/com/govtexam/prep/
│       │   ├── GovtExamApp.java              # Application class (MultiDex, TLS 1.2, Notification Channels)
│       │   ├── data/
│       │   │   ├── local/                    # Room Database, DAOs, Entities, Converters, SessionManager
│       │   │   ├── model/                    # Data models (User, Question, ExamResult, LeaderboardEntry, etc.)
│       │   │   ├── remote/                   # Retrofit ApiClient, ApiService, TLS 1.2 Socket Factory
│       │   │   │   ├── interceptor/          # Auth Bearer & Account Status 403 Interceptors
│       │   │   │   └── response/             # Response envelopes & API payloads
│       │   │   └── repository/               # Question, Exam, Taxonomy, User, Notification Repositories
│       │   ├── service/
│       │   │   └── MyFirebaseMessagingService.java  # FCM HTTP v1 push receiver with Room deduplication
│       │   ├── ui/
│       │   │   ├── auth/                     # LoginActivity, RegisterActivity
│       │   │   ├── exam/                     # ExamArenaActivity, ExamResultActivity, ExamAnswerPreviewActivity
│       │   │   ├── leaderboard/              # LeaderboardFragment, LeaderboardAdapter
│       │   │   ├── main/                     # MainActivity, HomeFragment, Adapters
│       │   │   ├── notification/             # NotificationActivity, NotificationAdapter
│       │   │   ├── profile/                  # ProfileFragment, UserProfileActivity, EditProfileActivity, Tabs
│       │   │   ├── question/                 # QuestionCardAdapter, CreateQuestionActivity, Comments, Reports
│       │   │   ├── search/                   # SearchResultActivity, SearchResultAdapter
│       │   │   ├── splash/                   # SplashActivity, MaintenanceActivity
│       │   │   └── statistics/               # StatisticsFragment (MPAndroidChart)
│       │   └── util/                         # FeatureFlagManager, GlideModule, DialogHelpers
│       └── res/                              # 37 XML Layouts, Vector Drawables, Values, XML File Provider
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties             # Gradle 8.7 distribution
├── build.gradle                              # Root buildscript (AGP 8.3.2)
├── settings.gradle                           # Repositories & project include
├── gradlew                                   # Unix wrapper script
└── gradlew.bat                               # Windows wrapper script
```

---

## Building & Running

### Requirements
- **Android Studio:** Hedgehog (2023.1.1) or newer recommended.
- **JDK:** Java 17 (standard for AGP 8.x).
- **Android SDK:** Platform 36 installed via SDK Manager.

### Steps
1. Open Android Studio.
2. Select **Open** and choose the `questions_android_app` directory.
3. Allow Gradle to sync dependencies from Google Maven and Maven Central.
4. Set backend API endpoint in [ApiClient.java](file:///app/src/main/java/com/govtexam/prep/data/remote/ApiClient.java):
   ```java
   public static final String BASE_URL = "https://your-cpanel-domain.com/";
   ```
5. Connect an Android device (API 21+) or launch an emulator.
6. Click **Run** (`Shift + F10`).
