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

package de.tadris.fitness.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.tadris.fitness.BuildConfig
import de.tadris.fitness.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    val urlRepository = stringResource(R.string.urlRepository)
    val urlPrivacy = stringResource(R.string.urlPrivacy)
    val urlChangelog = stringResource(R.string.urlChangelog)
    val urlCopyrightNotices = stringResource(R.string.urlCopyrightNotices)
    val urlBugReport = stringResource(R.string.urlBugReport)
    val urlWeblate = stringResource(R.string.urlWeblate)
    val emailAuthor = stringResource(R.string.emailAuthor)
    val urlAuthor = stringResource(R.string.urlAuthor)
    val contributors = stringArrayResource(R.array.contributors)

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.aboutTitle)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_combined),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp).clip(CircleShape),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "${stringResource(R.string.aboutVersion)} ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item { SettingsCategory(stringResource(R.string.aboutLinks)) }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.aboutSourceCode),
                    icon = { Icon(painterResource(R.drawable.ic_code), contentDescription = null) },
                    onClick = { openUrl(urlRepository) },
                )
            }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.aboutPrivacyPolicy),
                    icon = { Icon(painterResource(R.drawable.ic_privacy), contentDescription = null) },
                    onClick = { openUrl(urlPrivacy) },
                )
            }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.aboutChangelog),
                    icon = { Icon(painterResource(R.drawable.ic_changes), contentDescription = null) },
                    onClick = { openUrl(urlChangelog) },
                )
            }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.aboutOpenSourceLibraries),
                    icon = { Icon(painterResource(R.drawable.ic_library_books), contentDescription = null) },
                    onClick = { openUrl(urlCopyrightNotices) },
                )
            }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.aboutReportBug),
                    icon = { Icon(painterResource(R.drawable.ic_bug_report), contentDescription = null) },
                    onClick = { openUrl(urlBugReport) },
                )
            }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.aboutHelpTranslating),
                    icon = { Icon(painterResource(R.drawable.ic_language), contentDescription = null) },
                    onClick = { openUrl(urlWeblate) },
                )
            }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.aboutSendFeedback),
                    icon = { Icon(painterResource(R.drawable.ic_email), contentDescription = null) },
                    onClick = { openUrl("mailto:$emailAuthor") },
                )
            }
            item { SettingsCategory(stringResource(R.string.aboutAuthor)) }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.authorName),
                    icon = { Icon(painterResource(R.drawable.ic_person), contentDescription = null) },
                    onClick = { openUrl(urlAuthor) },
                )
            }
            item { SettingsCategory(stringResource(R.string.aboutContributors)) }
            contributors.forEach { contributor ->
                item {
                    ClickableSettingsItem(
                        title = contributor,
                        icon = { Icon(painterResource(R.drawable.ic_person), contentDescription = null) },
                        onClick = {},
                    )
                }
            }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.aboutUnlistedContributors),
                    icon = { Icon(painterResource(R.drawable.ic_group), contentDescription = null) },
                    onClick = {},
                )
            }
        }
    }
}
