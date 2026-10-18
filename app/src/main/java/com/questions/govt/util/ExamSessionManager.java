package com.questions.govt.util;

import com.questions.govt.data.model.Question;
import com.questions.govt.data.remote.response.CustomExamGenerateResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe In-Memory Session Manager for live exams and self-tests.
 * Completely eliminates Android Intent Binder TransactionTooLargeException
 * and NotSerializableException when transferring large Question banks to ExamArenaActivity.
 */
public class ExamSessionManager {

    private static volatile ExamSessionManager instance;

    private CustomExamGenerateResponse currentSession;
    private boolean isSelfTestExam = false;
    private final List<Question> selectedQuestionBank = new ArrayList<>();

    private ExamSessionManager() {
    }

    public static ExamSessionManager getInstance() {
        if (instance == null) {
            synchronized (ExamSessionManager.class) {
                if (instance == null) {
                    instance = new ExamSessionManager();
                }
            }
        }
        return instance;
    }

    public synchronized void setSession(CustomExamGenerateResponse session, boolean isSelfTest) {
        this.currentSession = session;
        this.isSelfTestExam = isSelfTest;
    }

    public synchronized CustomExamGenerateResponse getSession() {
        return currentSession;
    }

    public synchronized boolean isSelfTest() {
        return isSelfTestExam;
    }

    public synchronized void setSelectedQuestions(List<Question> questions) {
        selectedQuestionBank.clear();
        if (questions != null) {
            selectedQuestionBank.addAll(questions);
        }
    }

    public synchronized List<Question> getSelectedQuestions() {
        return new ArrayList<>(selectedQuestionBank);
    }

    public synchronized void toggleQuestionSelection(Question question) {
        if (question == null) return;
        for (int i = 0; i < selectedQuestionBank.size(); i++) {
            if (selectedQuestionBank.get(i).getId() == question.getId()) {
                selectedQuestionBank.remove(i);
                return;
            }
        }
        selectedQuestionBank.add(question);
    }

    public synchronized boolean isQuestionSelected(long questionId) {
        for (Question q : selectedQuestionBank) {
            if (q.getId() == questionId) return true;
        }
        return false;
    }

    public synchronized int getSelectedCount() {
        return selectedQuestionBank.size();
    }

    public synchronized void clearSelectedQuestions() {
        selectedQuestionBank.clear();
    }

    public synchronized void clear() {
        this.currentSession = null;
        this.isSelfTestExam = false;
        this.selectedQuestionBank.clear();
    }
}
