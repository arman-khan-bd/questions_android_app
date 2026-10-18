package com.questions.govt.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.questions.govt.data.local.converter.Converters;
import com.questions.govt.data.local.dao.AppConfigDao;
import com.questions.govt.data.local.dao.ExamDao;
import com.questions.govt.data.local.dao.NotificationDao;
import com.questions.govt.data.local.dao.QuestionDao;
import com.questions.govt.data.local.dao.TaxonomyDao;
import com.questions.govt.data.local.dao.UserDao;
import com.questions.govt.data.local.entity.AppConfigEntity;
import com.questions.govt.data.local.entity.NotificationEntity;
import com.questions.govt.data.local.entity.OfflineExamSubmission;
import com.questions.govt.data.model.ExamResult;
import com.questions.govt.data.model.Question;
import com.questions.govt.data.model.TaxonomyItem;
import com.questions.govt.data.model.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main Room Database for Govt Exam Prep application.
 * Provides offline caching for taxonomy trees, questions, starred items,
 * mistake notebooks, exam results, offline queues, notifications, and user profiles.
 */
@Database(
        entities = {
                Question.class,
                TaxonomyItem.class,
                ExamResult.class,
                NotificationEntity.class,
                OfflineExamSubmission.class,
                AppConfigEntity.class,
                User.class
        },
        version = 3,
        exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "govt_exam_prep_db";
    private static volatile AppDatabase instance;

    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public abstract QuestionDao questionDao();
    public abstract TaxonomyDao taxonomyDao();
    public abstract ExamDao examDao();
    public abstract NotificationDao notificationDao();
    public abstract AppConfigDao appConfigDao();
    public abstract UserDao userDao();

    public static AppDatabase getInstance(final Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
