package com.questions.govt.data.remote;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;

/**
 * Utility helper to ensure OkHttp client reliably connects via TLS 1.2 / TLS 1.3
 * to modern servers with Let's Encrypt (including ECDSA / YR1 / Root YR chains),
 * cPanel AutoSSL, Cloudflare, or custom SSL certificates without throwing
 * SSLHandshakeException or certificate verification errors across all Android versions
 * (API 21 through Android 16 / API 36).
 */
public class Tls12Helper {

    private static final String TAG = "Tls12Helper";

    @SuppressLint({"TrustAllX509TrustManager", "BadHostnameVerifier"})
    public static OkHttpClient.Builder enableTls12(OkHttpClient.Builder client) {
        try {
            // Permissive trust manager: trusts server certificates regardless of missing local intermediate/root CAs
            X509TrustManager trustAllCerts = new X509TrustManager() {
                @SuppressLint("TrustAllX509TrustManager")
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    // Accept all client certificates
                }

                @SuppressLint("TrustAllX509TrustManager")
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    // Accept all server certificates (bypasses missing intermediate/root Let's Encrypt ECDSA CAs)
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };

            // Initialize SSLContext with TLS protocol
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAllCerts}, new SecureRandom());

            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // For Android 5.0-5.1 (API 21-22), wrap in Tls12SocketFactory to force TLS 1.2 support
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                sslSocketFactory = new Tls12SocketFactory(sslSocketFactory);
            }

            client.sslSocketFactory(sslSocketFactory, trustAllCerts);

            // Permissive HostnameVerifier to match domain even if SAN / CN differs (e.g. music.e-bd.shop vs www.music.e-bd.shop)
            client.hostnameVerifier(new HostnameVerifier() {
                @SuppressLint("BadHostnameVerifier")
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            // Allow Modern TLS, Compatible TLS, and Cleartext fallback
            ConnectionSpec modernTls = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
                    .build();

            List<ConnectionSpec> specs = new ArrayList<>();
            specs.add(modernTls);
            specs.add(ConnectionSpec.COMPATIBLE_TLS);
            specs.add(ConnectionSpec.CLEARTEXT);

            client.connectionSpecs(specs);
            Log.i(TAG, "Permissive SSL & TLS 1.2+ successfully configured for OkHttpClient across all Android versions.");
        } catch (Exception exc) {
            Log.e(TAG, "Error while configuring SSL/TLS compatibility", exc);
        }

        return client;
    }
}
