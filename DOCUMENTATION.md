# Govt Exam Prep - Native Android App (Java)
## Complete Developer & Buyer Documentation (Envato / CodeCanyon Standard)

Welcome to the **Govt Job Preliminary (MCQ) Exam Preparation Native Android Application** codebase! This application has been engineered in native **Java** with full backwards compatibility from **Android 5.0 Lollipop (`minSdk 21`)** through **Android 16 (`targetSdk 36`)**. It integrates seamlessly with our Laravel 11 + Inertia.js (Sanctum REST API) cPanel backend.

---

## 1. Quick Start: Configure Server URL & APIs in 1 Minute

All server connections, REST API routes, network timeouts, app branding, and default engine limits are **centralized in one single master file**:

📂 **[`app/src/main/java/com/govtexam/prep/AppConfig.java`](file:///c:/Users/Admin/Documents/codester/questions-java/questions_android_app/app/src/main/java/com/govtexam/prep/AppConfig.java)**

### Step 1: Change Backend URL
Open `AppConfig.java` and set `SERVER_URL` to your live domain (ensure it ends with a trailing slash `/`):

```java
// app/src/main/java/com/govtexam/prep/AppConfig.java
public static final String SERVER_URL = "https://yourdomain.com/";
```

### Step 2: Customize API Endpoints (If Needed)
Every endpoint path is stored in `AppConfig.java`. When you modify a constant here, it automatically updates throughout Retrofit and the entire application:

```java
public static final String API_PREFIX = "api/v1/";

public static final String ENDPOINT_APP_CONFIG           = API_PREFIX + "app-config";
public static final String ENDPOINT_HOME_FEED            = API_PREFIX + "home-feed";
public static final String ENDPOINT_REGISTER             = API_PREFIX + "auth/register";
public static final String ENDPOINT_LOGIN                = API_PREFIX + "auth/login";
public static final String ENDPOINT_LOGOUT               = API_PREFIX + "auth/logout";
public static final String ENDPOINT_ME                   = API_PREFIX + "auth/me";
public static final String ENDPOINT_QUESTIONS            = API_PREFIX + "questions";
public static final String ENDPOINT_CUSTOM_EXAM_GENERATE = API_PREFIX + "exams/custom-generate";
public static final String ENDPOINT_EXAM_SUBMIT          = API_PREFIX + "exams/{id}/submit";
public static final String ENDPOINT_LEADERBOARD          = API_PREFIX + "leaderboard";
public static final String ENDPOINT_STATISTICS           = API_PREFIX + "statistics";
public static final String ENDPOINT_USER_PROFILE         = API_PREFIX + "profile/{username}";
// ... (All endpoints registered in AppConfig.java)
```

---

## 2. Technical Stack & Minimum Requirements

- **Development IDE:** Android Studio Ladybug / Koala / Iguana / Hedgehog or newer.
- **Java Version:** JDK 17 (Temurin or OpenJDK).
- **Gradle Version:** Gradle 8.7 with Android Gradle Plugin (AGP) 8.4.1.
- **SDK Targets:**
  - `minSdk`: **21** (Android 5.0 Lollipop)
  - `targetSdk`: **36** (Android 16)
  - `compileSdk`: **36**
- **Architecture:** Repository Pattern + Android Architecture Components (LiveData, ViewModel, Room SQLite, ViewBinding).
- **Core Libraries:**
  - **Networking:** Retrofit 2.11.0 + OkHttp 4.12.0 (hardened with TLS 1.2 SocketFactory for API 21).
  - **JSON Parsing:** GSON 2.10.1 with polymorphic and named envelope deserializers.
  - **Local Persistence:** Room 2.6.1 SQLite database with fallback destructive migration.
  - **Image Loading:** Bumptech Glide 4.16.0 with WebP decoding and memory/disk caching.
  - **Charts:** MPAndroidChart 3.1.0 for exam performance trends and difficulty distributions.
  - **Security:** `EncryptedSharedPreferences` on API 23+ with fallback on API 21-22.

---

## 3. Project Directory Structure

```
questions_android_app/
├── .github/
│   └── workflows/
│       └── build-and-release.yml   # CI/CD: Automated signed APK build & GitHub Release
├── app/
│   ├── src/main/
│   │   ├── java/com/govtexam/prep/
│   │   │   ├── AppConfig.java       # ⭐ MASTER CONFIG: All APIs, URLs & developer settings
│   │   │   ├── GovtExamApp.java     # MultiDex Application class
│   │   │   ├── data/
│   │   │   │   ├── local/           # Room Database, DAOs, Entities, SessionManager
│   │   │   │   ├── model/           # POJO Data models (Question, User, Comment, ExamResult)
│   │   │   │   ├── remote/          # ApiClient, ApiService, TLS 1.2, Requests/Responses
│   │   │   │   └── repository/      # Offline-first repositories
│   │   │   ├── service/             # Firebase Push Notification service
│   │   │   ├── ui/                  # Activities, Fragments, Adapters (Auth, Exam, Profile)
│   │   │   └── util/                # Account helpers, GlideModule, FeatureFlagManager
│   │   ├── res/                     # 37 XML layouts, Material vector drawables, colors, themes
│   │   └── AndroidManifest.xml
│   └── build.gradle                 # App dependencies, compileSdk 36, desugaring
├── build.gradle                     # Project root gradle script
├── settings.gradle
└── gradlew / gradlew.bat            # Gradle 8.7 wrappers
```

---

## 4. How to Customize the Application

### A. Change Application Name
Open `app/src/main/res/values/strings.xml` and edit:
```xml
<string name="app_name">Your App Name</string>
```

### B. Change Package Name / Application ID
1. Open `app/build.gradle` and change `applicationId`:
   ```groovy
   defaultConfig {
       applicationId "com.yourcompany.yourapp"
       // ...
   }
   ```
2. In Android Studio, right click the package `com.govtexam.prep` in the Project panel -> **Refactor** -> **Rename** -> **Rename Package**.

### C. Change App Theme & Colors (Rebranding in 2 Minutes)
Open `app/src/main/res/values/colors.xml` and update the primary palette:
```xml
<!-- Primary Brand Colors -->
<color name="primary">#1E3A8A</color>       <!-- Your primary brand color -->
<color name="secondary">#0EA5E9</color>     <!-- Your accent/secondary color -->
<color name="accent_gold">#F59E0B</color>   <!-- Badges and trophy highlights -->
```

### D. Setup Firebase Cloud Messaging (Push Notifications)
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Create a project and add an Android app with your `applicationId`.
3. Download `google-services.json` and paste it into the `questions_android_app/app/` folder.
4. FCM tokens are automatically synchronized with the backend via `POST /api/v1/profile/fcm-token`.

---

## 5. Complete REST API Endpoints Registry

All routes connect to the Laravel 11 backend at `https://music.e-bd.shop/` (or your customized `SERVER_URL` in `AppConfig.java`):

| AppConfig Constant | Route | HTTP | Description |
| :--- | :--- | :---: | :--- |
| `ENDPOINT_APP_CONFIG` | `api/v1/app-config` | `GET` | Remote branding, negative marking rates & feature toggles |
| `ENDPOINT_HOME_FEED` | `api/v1/home-feed` | `GET` | Dynamic banners, quotes, subjects & trending categories |
| `ENDPOINT_REGISTER` | `api/v1/auth/register` | `POST` | Instant registration without OTP |
| `ENDPOINT_LOGIN` | `api/v1/auth/login` | `POST` | Sanctum Bearer token login |
| `ENDPOINT_LOGOUT` | `api/v1/auth/logout` | `POST` | Revoke user token |
| `ENDPOINT_ME` | `api/v1/auth/me` | `GET` | Current user details & rank |
| `ENDPOINT_SEARCH_SUGGESTIONS` | `api/v1/search/suggestions` | `GET` | Live keyword & tag search suggestions |
| `ENDPOINT_SEARCH_RESULTS` | `api/v1/search/results` | `GET` | Paginated search with taxonomy and difficulty filters |
| `ENDPOINT_QUESTIONS` | `api/v1/questions` | `POST` | Create question (`title`, 4 options, `Easy`/`Medium`/`Hard`) |
| `ENDPOINT_QUESTION_DETAIL` | `api/v1/questions/{id}` | `GET` | Question details with author, metrics, and interactions |
| `ENDPOINT_QUESTION_ANSWER` | `api/v1/questions/{id}/answer` | `POST` | Instant answer check, accuracy calculation & rank recalculation |
| `ENDPOINT_QUESTION_VOTE` | `api/v1/questions/{id}/vote` | `POST` | Crowd-sourced community voting |
| `ENDPOINT_QUESTION_STAR` | `api/v1/questions/{id}/star` | `POST` | Star / bookmark question toggle |
| `ENDPOINT_QUESTION_COMMENTS` | `api/v1/questions/{id}/comments` | `GET` | Approved comments with nested replies |
| `ENDPOINT_STORE_COMMENT` | `api/v1/questions/{id}/comments` | `POST` | Post comment (bad-word profanity filtered) |
| `ENDPOINT_QUESTION_REPORT` | `api/v1/questions/{id}/report` | `POST` | File moderation report on question |
| `ENDPOINT_CUSTOM_EXAM_GENERATE` | `api/v1/exams/custom-generate` | `POST` | Generate practice exam matching student filters |
| `ENDPOINT_EXAM_SUBMIT` | `api/v1/exams/{id}/submit` | `POST` | Automated negative marking and answer sheet grading |
| `ENDPOINT_LEADERBOARD` | `api/v1/leaderboard` | `GET` | Global rankings filtered by tier (`all`, `easy`, `medium`, `hard`) |
| `ENDPOINT_STATISTICS` | `api/v1/statistics` | `GET` | Analytics for MPAndroidChart pie/line charts |
| `ENDPOINT_USER_PROFILE` | `api/v1/profile/{username}` | `GET` | 360° profile with exams, questions, stars, mistakes |
| `ENDPOINT_PROFILE_UPDATE` | `api/v1/profile/update` | `POST` | Update bio, academic info, target exam, and avatar |
| `ENDPOINT_NOTIFICATIONS` | `api/v1/notifications` | `GET` | In-app notifications with unread counter |

---

## 6. How to Build & Publish APK / Google Play App Bundle

### Method A: Build Locally in Android Studio / Terminal
```bash
# Build Debug APK (installs immediately with logging)
./gradlew assembleDebug

# Build Production Release APK
./gradlew assembleRelease

# Build Google Play Store App Bundle (.aab)
./gradlew bundleRelease
```
- Release APK output: `app/build/outputs/apk/release/`
- App Bundle output: `app/build/outputs/bundle/release/`

### Method B: Automated GitHub Actions CI/CD
1. Push code to your GitHub repository.
2. Push a version tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
3. GitHub Actions builds the APK, signs it, verifies it with `apksigner`, generates SHA-256 checksums, and publishes it to your GitHub repository's **Releases** page automatically.

---

## 7. Support & Inquiries
For customization assistance, bug reports, or feature requests, contact our developer support team:
- **Email:** `support@e-bd.shop`
- **Backend URL:** `https://music.e-bd.shop`
