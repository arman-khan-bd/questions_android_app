package com.questions.govt.data.remote;

import com.questions.govt.AppConfig;
import com.questions.govt.data.model.AppConfigData;
import com.questions.govt.data.model.Comment;
import com.questions.govt.data.model.CustomExamRequest;
import com.questions.govt.data.model.ExamQuestionPreview;
import com.questions.govt.data.model.ExamResult;
import com.questions.govt.data.model.NotificationItem;
import com.questions.govt.data.model.Question;
import com.questions.govt.data.model.User;
import com.questions.govt.data.remote.request.LoginRequest;
import com.questions.govt.data.remote.request.RegisterRequest;
import com.questions.govt.data.remote.response.AnswerCheckResponse;
import com.questions.govt.data.remote.response.ApiResponse;
import com.questions.govt.data.remote.response.AuthData;
import com.questions.govt.data.remote.response.CustomExamGenerateResponse;
import com.questions.govt.data.remote.response.ExamDetailResponse;
import com.questions.govt.data.remote.response.HomeFeedResponse;
import com.questions.govt.data.remote.response.LeaderboardResponse;
import com.questions.govt.data.remote.response.PaginatedList;
import com.questions.govt.data.remote.response.SearchSuggestionsResponse;
import com.questions.govt.data.remote.response.StarResponse;
import com.questions.govt.data.remote.response.StatisticsResponse;
import com.questions.govt.data.remote.response.SubjectDetailsResponse;
import com.questions.govt.data.remote.response.UserProfileResponse;
import com.questions.govt.data.remote.response.VoteResponse;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Retrofit 2.11.0 REST API interface matching the Laravel Sanctum cPanel backend.
 * All route constants are centrally registered in AppConfig.java for easy developer customization.
 */
public interface ApiService {

    // 1. App Configuration, Branding & Remote Feature Flags
    @GET(AppConfig.ENDPOINT_APP_CONFIG)
    Call<ApiResponse<AppConfigData>> getAppConfig();

    // 2. Sanctum Authentication & FCM Token
    @POST(AppConfig.ENDPOINT_REGISTER)
    Call<ApiResponse<AuthData>> register(@Body RegisterRequest request);

    @POST(AppConfig.ENDPOINT_LOGIN)
    Call<ApiResponse<AuthData>> login(@Body LoginRequest request);

    @POST(AppConfig.ENDPOINT_LOGOUT)
    Call<ApiResponse<Void>> logout();

    @GET(AppConfig.ENDPOINT_ME)
    Call<ApiResponse<User>> getCurrentUser();

    @POST(AppConfig.ENDPOINT_FCM_TOKEN)
    Call<ApiResponse<Void>> syncFcmToken(@Body Map<String, String> body);

    // 3. Home Feed & Taxonomy
    @GET(AppConfig.ENDPOINT_HOME_FEED)
    Call<ApiResponse<HomeFeedResponse>> getHomeFeed();

    @GET(AppConfig.ENDPOINT_SUBJECT_DETAILS)
    Call<ApiResponse<SubjectDetailsResponse>> getSubjectDetails(@Path("id") long id);

    // 4. Search & Suggestions
    @GET(AppConfig.ENDPOINT_SEARCH_SUGGESTIONS)
    Call<ApiResponse<SearchSuggestionsResponse>> getSearchSuggestions(@Query("q") String query);

    @GET(AppConfig.ENDPOINT_SEARCH_RESULTS)
    Call<ApiResponse<PaginatedList<Question>>> searchQuestions(@QueryMap Map<String, String> filters);

    // 5. Questions & Moderated Telemetry
    @Multipart
    @POST(AppConfig.ENDPOINT_QUESTIONS)
    Call<ApiResponse<Question>> createQuestion(
            @PartMap Map<String, RequestBody> fields,
            @Part MultipartBody.Part image
    );

    @GET(AppConfig.ENDPOINT_QUESTION_DETAIL)
    Call<ApiResponse<Question>> getQuestionDetail(@Path("id") long questionId);

    @POST(AppConfig.ENDPOINT_QUESTION_VIEWS)
    Call<ApiResponse<Void>> recordQuestionViews(@Body Map<String, List<Long>> body);

    @POST(AppConfig.ENDPOINT_QUESTION_ANSWER)
    Call<ApiResponse<AnswerCheckResponse>> checkAnswer(
            @Path("id") long questionId,
            @Body Map<String, String> body
    );

    @POST(AppConfig.ENDPOINT_QUESTION_VOTE)
    Call<ApiResponse<VoteResponse>> voteQuestionAnswer(
            @Path("id") long questionId,
            @Body Map<String, Object> body
    );

    @POST(AppConfig.ENDPOINT_QUESTION_STAR)
    Call<ApiResponse<StarResponse>> toggleStarQuestion(@Path("id") long questionId);

    @POST(AppConfig.ENDPOINT_QUESTION_REPORT)
    Call<ApiResponse<Void>> reportQuestion(
            @Path("id") long questionId,
            @Body Map<String, String> body
    );

    // 6. Bad-Word Moderated Comments & Nested Replies
    @GET(AppConfig.ENDPOINT_QUESTION_COMMENTS)
    Call<ApiResponse<List<Comment>>> getComments(@Path("id") long questionId);

    @POST(AppConfig.ENDPOINT_STORE_COMMENT)
    Call<ApiResponse<Comment>> postComment(
            @Path("id") long questionId,
            @Body Map<String, String> body
    );

    @POST(AppConfig.ENDPOINT_COMMENT_REPLY)
    Call<ApiResponse<Comment>> postReply(
            @Path("id") long parentCommentId,
            @Body Map<String, String> body
    );

    // 7. Exam Engine & Submissions
    @POST(AppConfig.ENDPOINT_CUSTOM_EXAM_GENERATE)
    Call<ApiResponse<CustomExamGenerateResponse>> generateCustomExam(@Body CustomExamRequest request);

    @GET(AppConfig.ENDPOINT_EXAM_DETAIL)
    Call<ApiResponse<ExamDetailResponse>> getExamDetail(@Path("id") long examId);

    @POST(AppConfig.ENDPOINT_EXAM_SUBMIT)
    Call<ApiResponse<ExamResult>> submitExam(
            @Path("id") long examId,
            @Body Map<String, Object> submissionBody
    );

    @GET(AppConfig.ENDPOINT_EXAM_PREVIEW)
    Call<ApiResponse<List<ExamQuestionPreview>>> getExamAnswerPreview(@Path("id") long examId);

    // 8. Leaderboard
    @GET(AppConfig.ENDPOINT_LEADERBOARD)
    Call<ApiResponse<LeaderboardResponse>> getLeaderboard(
            @Query("tier") String tier,
            @Query("limit") Integer limit
    );

    // 9. Statistics (MPAndroidChart)
    @GET(AppConfig.ENDPOINT_STATISTICS)
    Call<ApiResponse<StatisticsResponse>> getStatistics();

    // 10. Facebook-Style Profile
    @GET(AppConfig.ENDPOINT_USER_PROFILE)
    Call<ApiResponse<UserProfileResponse>> getUserProfile(@Path("username") String username);

    @Multipart
    @POST(AppConfig.ENDPOINT_PROFILE_UPDATE)
    Call<ApiResponse<User>> updateProfile(
            @PartMap Map<String, RequestBody> fields,
            @Part MultipartBody.Part avatar,
            @Part MultipartBody.Part coverPhoto
    );

    // 11. Push & In-App Notifications
    @GET(AppConfig.ENDPOINT_NOTIFICATIONS)
    Call<ApiResponse<List<NotificationItem>>> getNotifications();

    @POST(AppConfig.ENDPOINT_NOTIFICATION_READ)
    Call<ApiResponse<Void>> markNotificationRead(@Path("id") long notificationId);
}
