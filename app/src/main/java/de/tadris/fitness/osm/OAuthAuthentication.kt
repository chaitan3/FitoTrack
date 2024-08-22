package de.tadris.fitness.osm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.github.scribejava.core.builder.ScopeBuilder
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.builder.api.DefaultApi20

object OsmApi : DefaultApi20() {
    override fun getAccessTokenEndpoint(): String {
        return "https://www.openstreetmap.org/oauth2/token"
    }

    override fun getAuthorizationBaseUrl(): String {
        return "https://www.openstreetmap.org/oauth2/authorize"
    }
}

class OAuthAuthentication() : ComponentActivity() {
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
                throw Exception("Unexpected response from OSM authorizer!")
            }
        } else {
            val token = preferences.getString("accessToken", null)

            if (token == null) {
                Log.d("oauth", token.toString())

                val url = getAuthorizationUrl()

                val builder = CustomTabsIntent.Builder()
                val customTabsIntent = builder.build()
                customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                customTabsIntent.launchUrl(this, Uri.parse(url))
            } else {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    fun handleAuthorizationCode(code: String, listener: Listener) {
        Thread() {
            val token = oauthService.getAccessToken(code)
            preferences.edit()
                .putString("accessToken", token.accessToken)
                .putBoolean("authenticated", true)
                .apply()

            Log.d("oauth", token.rawResponse)

            //if (token.)
            listener.authenticationComplete()
        }.start()
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
