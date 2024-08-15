package de.tadris.fitness.osm

import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
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

class OAuthAuthentication() : Activity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        val data = intent?.data
        if (data != null) {
            val code = data.getQueryParameter("code")
            if (code != null) {
                handleAuthorizationCode(code)
            } else {
                TODO("Throw Error?")
            }
        } else {
            preferences = this.getSharedPreferences("osm_oauth", android.content.Context.MODE_PRIVATE)

            val token = preferences.getString("accessToken", "")
            Log.d("oauth", token.toString())

            val url = oauthService.authorizationUrl

            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            customTabsIntent.launchUrl(this, Uri.parse(url))
        }
    }

    override fun onNewIntent(intent: Intent?) {
        Log.d("oauth", "onNewIntent: ")
        super.onNewIntent(intent)
        val code = intent?.data?.getQueryParameter("code")
        if (code != null) {
            handleAuthorizationCode(code)
        } else {
            // handle error
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("oauth", "onPause: ")
    }

    override fun onStop() {
        super.onStop()
        Log.d("oauth", "onStop: ")
    }

    fun handleAuthorizationCode(code: String) {
        val token = oauthService.getAccessToken(code)
        preferences.edit()
            .putString("accessToken", token.toString())
            .apply()
        Log.d("oauth", token.toString())
            //.putString("tokenSecret", oAuthConsumer.getTokenSecret())
            //.putBoolean("authenticated", true).apply()
    }

    fun clearAccessToken() {
        TODO("Needs to be implemented")
    }

    interface Listener {
        fun authenticationFailed()
        fun authenticationComplete()
    }
}
