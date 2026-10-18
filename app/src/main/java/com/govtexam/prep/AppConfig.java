package com.govtexam.prep;

/**
 * =========================================================================================
 * MASTER DEVELOPER CONFIGURATION & REST API REGISTRY (ENVATO / CODECANYON STANDARD)
 * =========================================================================================
 * 
 * Edit this single file to configure your backend server URL, all REST API endpoints,
 * network timeouts, branding, support contacts, privacy policies, and default engine rules.
 * 
 * Instructions for Developers & Buyers:
 * 1. Change SERVER_URL to point to your live Laravel cPanel backend domain.
 *    (Must include the trailing slash, e.g. "https://music.e-bd.shop/").
 * 2. All endpoint constants below map directly to routes defined in routes/api.php.
 * 3. Update APP_NAME, SUPPORT_EMAIL, WEBSITE_URL, and PRIVACY_POLICY_URL.
 * 4. To rebuild the APK, run: ./gradlew assembleRelease
 * =========================================================================================
 */
public final class AppConfig {

    private AppConfig() {
        // Private constructor to prevent direct instantiation
    }

    // =====================================================================================
    // 1. BACKEND SERVER DOMAIN CONFIGURATION
    // =====================================================================================
    /**
     * Primary Base URL for the Laravel 11 + Sanctum REST API backend.
     * Ensure this URL ends with a trailing slash '/'.
     */
    public static final String SERVER_URL = "https://music.e-bd.shop/";

    /**
     * Standard API Version prefix matching routes/api.php.
     */
    public static final String API_PREFIX = "api/v1/";

    // =====================================================================================
    // 2. COMPLETE REST API ENDPOINT REGISTRY (CENTRALIZED FOR EASY EDITING)
    // =====================================================================================
    
    // --- System, App Config & Dynamic Feeds ---
    /** Remote application configuration, branding, and feature flags */
    public static final String ENDPOINT_APP_CONFIG = API_PREFIX + "app-config";

    /** Dynamic Home feed (banners, daily quotes, trending subjects, popular categories) */
    public static final String ENDPOINT_HOME_FEED = API_PREFIX + "home-feed";

    // --- Authentication & Account Management ---
    /** Student registration (name, username, phone, email, password) */
    public static final String ENDPOINT_REGISTER = API_PREFIX + "auth/register";

    /** Student login (email/phone + password) -> returns Sanctum Bearer token */
    public static final String ENDPOINT_LOGIN = API_PREFIX + "auth/login";

    /** Sanctum token revocation (logout) */
    public static final String ENDPOINT_LOGOUT = API_PREFIX + "auth/logout";

    /** Get currently authenticated student profile */
    public static final String ENDPOINT_ME = API_PREFIX + "auth/me";

    /** Sync Firebase Cloud Messaging (FCM) push token */
    public static final String ENDPOINT_FCM_TOKEN = API_PREFIX + "profile/fcm-token";

    // --- Search & Live Suggestions ---
    /** Live keyword & tag search suggestions while typing */
    public static final String ENDPOINT_SEARCH_SUGGESTIONS = API_PREFIX + "search/suggestions";

    /** Search results with taxonomy, difficulty, and sort filters */
    public static final String ENDPOINT_SEARCH_RESULTS = API_PREFIX + "search/results";

    // --- Questions & Community MCQ Interaction ---
    /** Create community question with optional image attachment */
    public static final String ENDPOINT_QUESTIONS = API_PREFIX + "questions";

    /** Single question details with author, telemetry, and interactions */
    public static final String ENDPOINT_QUESTION_DETAIL = API_PREFIX + "questions/{id}";

    /** Instant answer checking with telemetry recording & rank recalculation */
    public static final String ENDPOINT_QUESTION_ANSWER = API_PREFIX + "questions/{id}/answer";

    /** Community crowd-sourced answer voting (A, B, C, D) */
    public static final String ENDPOINT_QUESTION_VOTE = API_PREFIX + "questions/{id}/vote";

    /** Star / Favorite question bookmark toggle (anti-duplication guarded) */
    public static final String ENDPOINT_QUESTION_STAR = API_PREFIX + "questions/{id}/star";

    /** Submit moderation report on question */
    public static final String ENDPOINT_QUESTION_REPORT = API_PREFIX + "questions/{id}/report";

    /** Batch telemetry views recording */
    public static final String ENDPOINT_QUESTION_VIEWS = API_PREFIX + "questions/telemetry/views";

    // --- Moderated Discussion Comments & Nested Replies ---
    /** Fetch approved comments with user details and replies */
    public static final String ENDPOINT_QUESTION_COMMENTS = API_PREFIX + "questions/{id}/comments";

    /** Submit a new comment or reply (bad-word profanity filtered) */
    public static final String ENDPOINT_STORE_COMMENT = API_PREFIX + "questions/{id}/comments";

    /** Post direct reply to a parent comment */
    public static final String ENDPOINT_COMMENT_REPLY = API_PREFIX + "comments/{id}/reply";

    // --- Exam Engine, Custom Tests & Graded Submissions ---
    /** Generate custom MCQ practice quiz based on student taxonomy & difficulty */
    public static final String ENDPOINT_CUSTOM_EXAM_GENERATE = API_PREFIX + "exams/custom-generate";

    /** Get exam metadata and question list */
    public static final String ENDPOINT_EXAM_DETAIL = API_PREFIX + "exams/{id}";

    /** Submit completed exam attempt for automated negative marking and grading */
    public static final String ENDPOINT_EXAM_SUBMIT = API_PREFIX + "exams/{id}/submit";

    /** Fetch full exam answer sheet preview with explanations */
    public static final String ENDPOINT_EXAM_PREVIEW = API_PREFIX + "exams/{id}/preview";

    // --- Leaderboard & Aggregated Analytics ---
    /** Global ranked leaderboard filtered by tier (all, easy, medium, hard) */
    public static final String ENDPOINT_LEADERBOARD = API_PREFIX + "leaderboard";

    /** Aggregated exam analytics and MPAndroidChart difficulty distribution */
    public static final String ENDPOINT_STATISTICS = API_PREFIX + "statistics";

    // --- Facebook-Style 360° Profile & In-App Notifications ---
    /** Comprehensive student profile with tabbed history (exams, questions, mistakes, stars) */
    public static final String ENDPOINT_USER_PROFILE = API_PREFIX + "profile/{username}";

    /** Update student bio, academic history, target exam, and avatar */
    public static final String ENDPOINT_PROFILE_UPDATE = API_PREFIX + "profile/update";

    /** In-app notifications with unread counter */
    public static final String ENDPOINT_NOTIFICATIONS = API_PREFIX + "notifications";

    /** Mark notification as read */
    public static final String ENDPOINT_NOTIFICATION_READ = API_PREFIX + "notifications/{id}/read";

    // =====================================================================================
    // 3. NETWORK & HTTP CLIENT SETTINGS
    // =====================================================================================
    /** Connection timeout in seconds */
    public static final int CONNECT_TIMEOUT_SECONDS = 30;

    /** Read timeout in seconds */
    public static final int READ_TIMEOUT_SECONDS = 30;

    /** Write timeout in seconds */
    public static final int WRITE_TIMEOUT_SECONDS = 30;

    /** Maximum disk HTTP cache size (10 MB) */
    public static final long HTTP_CACHE_SIZE_BYTES = 10L * 1024L * 1024L;

    // =====================================================================================
    // 4. APP BRANDING, SUPPORT & LEGAL COMPLIANCE (FOR GOOGLE PLAY STORE)
    // =====================================================================================
    public static final String APP_NAME = "Govt Exam Prep";
    public static final String APP_VERSION_NAME = "1.0.0";
    public static final int APP_VERSION_CODE = 1;

    public static final String SUPPORT_EMAIL = "support@e-bd.shop";
    public static final String WEBSITE_URL = "https://music.e-bd.shop";
    public static final String PRIVACY_POLICY_URL = "https://music.e-bd.shop/privacy-policy";
    public static final String TERMS_OF_SERVICE_URL = "https://music.e-bd.shop/terms";

    // =====================================================================================
    // 5. DEFAULT ENGINE RULES & FALLBACKS (CLIENT-SIDE DEFAULT VALUES)
    // =====================================================================================
    public static final int DEFAULT_EXAM_QUESTIONS_COUNT = 20;
    public static final int DEFAULT_EXAM_DURATION_MINUTES = 20;
    public static final float DEFAULT_NEGATIVE_MARKING_RATE = 0.25f;
    public static final float PASS_PERCENTAGE_THRESHOLD = 50.0f;
    public static final int DEFAULT_PAGE_SIZE = 15;
    public static final int LEADERBOARD_PAGE_LIMIT = 50;

    // Default Score Multipliers matching backend RankCalculatorService
    public static final float MULTIPLIER_EASY = 1.0f;
    public static final float MULTIPLIER_MEDIUM = 2.0f;
    public static final float MULTIPLIER_HARD = 3.0f;

    // Default Feature Toggles
    public static final boolean ENABLE_COMMUNITY_QUESTION_CREATION = true;
    public static final boolean ENABLE_COMMUNITY_VOTING = true;
    public static final boolean ENABLE_OFFLINE_SYNC = true;
    public static final boolean ENABLE_PUSH_NOTIFICATIONS = true;
}
