/*
 * Copyright (c) 2026 Jannis Scheibe <jannis@tadris.de>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     FitoTrack is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.tadris.fitness.util.osm

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.github.scribejava.core.builder.ScopeBuilder
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.builder.api.DefaultApi20
import de.tadris.fitness.R
import de.tadris.fitness.ui.FitoTrackActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

object OsmApi : DefaultApi20() {
    override fun getAccessTokenEndpoint(): String {
        return "https://www.openstreetmap.org/oauth2/token"
    }

    override fun getAuthorizationBaseUrl(): String {
        return "https://www.openstreetmap.org/oauth2/authorize"
    }
}

class OAuthAuthentication : FitoTrackActivity() {

    lateinit var preferences : SharedPreferences

    val oauthService = ServiceBuilder("kZKYrve0YEyWUZBykoG4cJGGPCWE0hn0nd0hsjjrVig")
        .apiSecret("7ru8t1mBY77zzmZ7SiGGzek-JOdM7sHtXg9UmDkvxaE")
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
        preferences = this.getSharedPreferences("osm_oauth", MODE_PRIVATE)

        val token = preferences.getString("accessToken", null)
        if (token == null) {
            if (intent?.data != null) {
                handleIntent(intent)
            } else {
                val b = !preferences.getBoolean("showingCustomTab", false)
                if (b) {
                    val url = getAuthorizationUrl()
                    val customTabsIntent = CustomTabsIntent.Builder().build()
                    customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    customTabsIntent.launchUrl(this, url.toUri())
                    preferences.edit {
                        putBoolean("showingCustomTab", true)
                    }
                } else {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
        } else {
            setResult(RESULT_OK)
            finishAndReset()
        }
    }

    override fun onResume() {
        super.onResume()
        val token = preferences.getString("accessToken", null)
        if (token != null) {
            setResult(RESULT_OK)
            finishAndReset()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.data == null) {
            setResult(RESULT_CANCELED)
            finishAndReset()
        } else {
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent) {
        val data = intent.data ?: return

        val code = data.getQueryParameter("code")
        if (code != null) {
            handleAuthorizationCode(code, object : Listener {
                override fun authenticationComplete() {
                    setResult(RESULT_OK)
                    finish()
                }

                override fun authenticationFailed() {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            })
        } else {
            // User denied access
            lifecycleScope.launch {
                // This sleep is important to guarantee that denying access will navigate back to the ShowGpsWorkoutActivity.
                delay(1000)
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    fun finishAndReset() {
        resetCustomTabState(this)
        finish()
    }

    fun handleAuthorizationCode(code: String, listener: Listener) {
        thread {
            try {
                val token = oauthService.getAccessToken(code)
                preferences.edit {
                    putString("accessToken", token.accessToken)
                    putBoolean("authenticated", true)
                }

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
        private const val TAG = "OAuthAuthentication"

        fun clearAccessToken(ctx: Context) {
            val preferences = ctx.getSharedPreferences("osm_oauth", MODE_PRIVATE)
            preferences.edit {
                remove("accessToken")
                putBoolean("authenticated", false)
            }

            Toast.makeText(ctx, R.string.removedOAuthToken, Toast.LENGTH_SHORT).show()
        }

        fun getAccessToken(ctx: Context): String? {
            val preferences = ctx.getSharedPreferences("osm_oauth", MODE_PRIVATE)
            return preferences.getString("accessToken", null)
        }

        fun resetCustomTabState(ctx: Context) {
            val preferences = ctx.getSharedPreferences("osm_oauth", MODE_PRIVATE)
            preferences.edit {
                putBoolean("showingCustomTab", false)
            }
        }
    }
}
