package com.questions.govt.util;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.questions.govt.R;
import com.questions.govt.data.local.SessionManager;
import com.questions.govt.ui.auth.LoginActivity;

/**
 * Handles presentation of non-dismissible Account Suspended / Disabled alerts.
 */
public class AccountStatusDialogHelper {

    private static AlertDialog activeDialog;

    public static synchronized void showAccountSuspended(Activity activity, String reason, String expiresAt) {
        if (activity == null || activity.isFinishing()) return;
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
        }

        StringBuilder message = new StringBuilder();
        message.append(activity.getString(R.string.account_suspended_msg)).append("\n\n");
        if (!TextUtils.isEmpty(reason)) {
            message.append(activity.getString(R.string.suspension_reason, reason)).append("\n");
        }
        if (!TextUtils.isEmpty(expiresAt)) {
            message.append(activity.getString(R.string.suspension_expires, expiresAt));
        }

        activeDialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.account_suspended_title)
                .setMessage(message.toString())
                .setIcon(R.drawable.ic_warning)
                .setCancelable(false)
                .setPositiveButton("Logout", (dialog, which) -> {
                    SessionManager.getInstance().clearSession();
                    Intent intent = new Intent(activity, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(intent);
                    activity.finish();
                })
                .show();
    }

    public static synchronized void showAccountDisabled(Activity activity, String reason) {
        if (activity == null || activity.isFinishing()) return;
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
        }

        SessionManager.getInstance().clearSession();

        activeDialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.account_disabled_title)
                .setMessage(activity.getString(R.string.account_disabled_msg) + (TextUtils.isEmpty(reason) ? "" : "\n\nReason: " + reason))
                .setIcon(R.drawable.ic_warning)
                .setCancelable(false)
                .setPositiveButton("Understood", (dialog, which) -> {
                    Intent intent = new Intent(activity, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(intent);
                    activity.finish();
                })
                .show();
    }
}
