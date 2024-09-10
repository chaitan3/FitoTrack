package de.tadris.fitness.osm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import com.github.scribejava.core.builder.ScopeBuilder
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.builder.api.DefaultApi20
import de.tadris.fitness.ui.FitoTrackActivity
import kotlin.concurrent.thread

object OsmApi : DefaultApi20() {
    override fun getAccessTokenEndpoint(): String {
        return "https://www.openstreetmap.org/oauth2/token"
    }

    override fun getAuthorizationBaseUrl(): String {
        return "https://www.openstreetmap.org/oauth2/authorize"
    }
}

class OAuthAuthentication() : FitoTrackActivity() {
    lateinit var preferences : SharedPreferences

    val oauthService = ServiceBuilder("rUrENrM0e12uvqK5gXYYWbNtGw-H6Ok518gjZ6JTjLA")
        .apiSecret("x-xpLlYCqm2k8VQi1NdRIj-m7mR1PA2_uHsddx7k0K8")
        .defaultScope(
            ScopeBuilder()
                .withScope("read_gpx")
                .withScope("write_gpx")
        )
        .callback("auth://callback")
        .build(OsmApi)

    interface Listener {
        fun authenticationFailed()
        fun authenticationComplete()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = this.getSharedPreferences("osm_oauth", Context.MODE_PRIVATE)
    }

    override fun onResume() {
        super.onResume()

        val data = intent?.data
        if (data != null) {
            val code = data.getQueryParameter("code")
            if (code != null) {
                handleAuthorizationCode(code, object: Listener {
                    override fun authenticationComplete() {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }

                    override fun authenticationFailed() {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                })
            } else {
                // User denied access
                thread {
                    // This sleep is important to guarantee that denying access will navigate back to the ShowGpsWorkoutActivity.
                    Thread.sleep(1000)
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }
        } else {
            val token = preferences.getString("accessToken", null)

            if (token == null) {
                val b = !preferences.getBoolean("showingCustomTab", false)
                if (b) {
                    val url = getAuthorizationUrl()
                    val customTabsIntent = CustomTabsIntent.Builder().build()
                    customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    customTabsIntent.launchUrl(this, Uri.parse(url))
                    preferences.edit().putBoolean("showingCustomTab", true).apply()
                } else {
                    setResult(Activity.RESULT_CANCELED)
                    finishAndReset()
                }
            } else {
                setResult(Activity.RESULT_OK)
                finishAndReset()
            }
        }
    }

    fun finishAndReset() {
        preferences.edit().putBoolean("showingCustomTab", false).apply()
        finish()
    }

    fun handleAuthorizationCode(code: String, listener: Listener) {
        thread {
            try {
                val token = oauthService.getAccessToken(code)
                preferences.edit()
                    .putString("accessToken", token.accessToken)
                    .putBoolean("authenticated", true)
                    .apply()

                listener.authenticationComplete()
            } catch (e: Exception) {
                listener.authenticationFailed()
            }
        }
    }

    private fun getAuthorizationUrl() : String {
        return oauthService.authorizationUrl
    }

    companion object {
        fun clearAccessToken(ctx: Context) {
            val preferences = ctx.getSharedPreferences("osm_oauth", Context.MODE_PRIVATE)
            preferences.edit()
                .remove("accessToken")
                .putBoolean("authenticated", false)
                .apply()
        }

        fun getAccessToken(ctx: Context): String? {
            val preferences = ctx.getSharedPreferences("osm_oauth", Context.MODE_PRIVATE)
            return preferences.getString("accessToken", null)
        }
    }
}
