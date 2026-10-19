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
 * Displays explicit reason and date, and auto-navigates to LoginActivity.
 */
public class AccountStatusDialogHelper {

    private static AlertDialog activeDialog;

    public static synchronized void showAccountSuspended(Activity activity, String reason, String expiresAt) {
        if (activity == null || activity.isFinishing()) return;
        if (activeDialog != null && activeDialog.isShowing()) {
            try {
                activeDialog.dismiss();
            } catch (Exception ignored) {}
        }

        // Auto logout: Clear active session immediately
        SessionManager.getInstance().clearSession();

        String displayReason = !TextUtils.isEmpty(reason) ? reason : "Administrative action / community guidelines.";
        String displayDate = !TextUtils.isEmpty(expiresAt) ? expiresAt : "Indefinitely";

        StringBuilder message = new StringBuilder();
        message.append("You are suspended for: ").append(displayReason).append("\n\n");
        message.append("Date: ").append(displayDate);

        activeDialog = new MaterialAlertDialogBuilder(activity)
                .setTitle("Account Suspended")
                .setMessage(message.toString())
                .setIcon(R.drawable.ic_warning)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    if (!(activity instanceof LoginActivity)) {
                        Intent intent = new Intent(activity, LoginActivity.class);
                        intent.putExtra("extra_status", "suspended");
                        intent.putExtra("extra_reason", displayReason);
                        intent.putExtra("extra_expires", displayDate);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        activity.startActivity(intent);
                        activity.finish();
                    }
                })
                .show();
    }

    public static synchronized void showAccountDisabled(Activity activity, String reason) {
        if (activity == null || activity.isFinishing()) return;
        if (activeDialog != null && activeDialog.isShowing()) {
            try {
                activeDialog.dismiss();
            } catch (Exception ignored) {}
        }

        SessionManager.getInstance().clearSession();

        String displayReason = !TextUtils.isEmpty(reason) ? reason : "Account deactivated by administrator.";

        activeDialog = new MaterialAlertDialogBuilder(activity)
                .setTitle("Account Deactivated")
                .setMessage("Your account has been deactivated.\n\nReason: " + displayReason)
                .setIcon(R.drawable.ic_warning)
                .setCancelable(false)
                .setPositiveButton("Understood", (dialog, which) -> {
                    dialog.dismiss();
                    if (!(activity instanceof LoginActivity)) {
                        Intent intent = new Intent(activity, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        activity.startActivity(intent);
                        activity.finish();
                    }
                })
                .show();
    }
}
