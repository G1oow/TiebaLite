package com.huanchengfly.tieba.post.ui.page.settings.more

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OfflineBolt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.dependNot
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.ListPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.page.settings.LeadingIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.ImageCacheUtil
import com.huanchengfly.tieba.post.utils.NetworkConfig
import com.huanchengfly.tieba.post.utils.appPreferences
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun MoreSettingsPage(
    navigator: NavHostController,
) {
    val coroutineScope = rememberCoroutineScope()
    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_settings_more),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.popBackStack() })
                }
            )
        },
    ) { paddingValues ->
        val snackbarHostState = LocalSnackbarHostState.current
        val context = LocalContext.current
        var cacheSize by remember { mutableStateOf("0.0B") }
        LaunchedEffect(Unit) {
            thread {
                cacheSize = ImageCacheUtil.getCacheSize(context)
            }
        }
        PrefsScreen(
            dataStore = LocalContext.current.dataStore,
            dividerThickness = 0.dp,
            modifier = Modifier
                .fillMaxSize(),
        ) {
            if (context.appPreferences.showExperimentalFeatures) {
                prefsItem {
                    SwitchPref(
                        leadingIcon = {
                            LeadingIcon {
                                AvatarIcon(
                                    icon = Icons.Outlined.BugReport,
                                    size = Sizes.Small,
                                    contentDescription = null,
                                )
                            }
                        },
                        key = "checkCIUpdate",
                        title = stringResource(id = R.string.title_check_ci_update),
                        defaultChecked = false,
                        summary = stringResource(id = R.string.tip_check_ci_update)
                    )
                }
            }
            prefsItem {
                SwitchPref(
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = ImageVector.vectorResource(id = R.drawable.ic_chrome),
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    key = "use_webview",
                    title = stringResource(id = R.string.title_use_webview),
                    defaultChecked = true,
                    summary = {
                        if (it) stringResource(id = R.string.tip_use_webview_on)
                        else stringResource(id = R.string.tip_use_webview)
                    },
                )
            }
            prefsItem {
                SwitchPref(
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = ImageVector.vectorResource(id = R.drawable.ic_today),
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    enabled = dependNot(key = "use_webview"),
                    key = "use_custom_tabs",
                    title = stringResource(id = R.string.title_use_custom_tabs),
                    defaultChecked = true,
                    summary = {
                        if (it) stringResource(id = R.string.tip_use_custom_tab_on)
                        else stringResource(id = R.string.tip_use_custom_tab)
                    },
                )
            }
            prefsItem {
                // 最大总并发请求数（API 与图片共享调度器上限，切换后立即生效无需重启）。
                ListPref(
                    key = NetworkConfig.PREF_KEY_MAX_REQUESTS,
                    title = stringResource(id = R.string.title_max_concurrent_requests),
                    summary = stringResource(id = R.string.summary_max_concurrent_requests),
                    defaultValue = NetworkConfig.DEFAULT_MAX_REQUESTS.toString(),
                    useSelectedAsSummary = true,
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.Speed,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    entries = mapOf(
                        "32" to stringResource(id = R.string.entry_concurrent_requests_count, 32),
                        "64" to stringResource(id = R.string.entry_concurrent_requests_recommended, 64),
                        "128" to stringResource(id = R.string.entry_concurrent_requests_count, 128),
                    ),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { NetworkConfig.applyConfig(maxRequests = it) }
                    },
                )
            }
            prefsItem {
                // 单域名最大并发数（突破 OkHttp 默认 5 并发限制，显著提升贴吧多图与头像加载速度）。
                ListPref(
                    key = NetworkConfig.PREF_KEY_MAX_REQUESTS_PER_HOST,
                    title = stringResource(id = R.string.title_max_concurrent_requests_per_host),
                    summary = stringResource(id = R.string.summary_max_concurrent_requests_per_host),
                    defaultValue = NetworkConfig.DEFAULT_MAX_REQUESTS_PER_HOST.toString(),
                    useSelectedAsSummary = true,
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.Tune,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    entries = mapOf(
                        "5" to stringResource(id = R.string.entry_concurrent_per_host_conservative, 5),
                        "8" to stringResource(id = R.string.entry_concurrent_requests_count, 8),
                        "16" to stringResource(id = R.string.entry_concurrent_requests_recommended, 16),
                        "24" to stringResource(id = R.string.entry_concurrent_requests_count, 24),
                        "32" to stringResource(id = R.string.entry_concurrent_requests_count, 32),
                    ),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { NetworkConfig.applyConfig(maxRequestsPerHost = it) }
                    },
                )
            }
            prefsItem {
                // 网络请求超时（秒）：缩短弱网假死等待时间，切换后立即对新请求生效。
                ListPref(
                    key = NetworkConfig.PREF_KEY_TIMEOUT_SECONDS,
                    title = stringResource(id = R.string.title_network_timeout),
                    summary = stringResource(id = R.string.summary_network_timeout),
                    defaultValue = NetworkConfig.DEFAULT_TIMEOUT_SECONDS.toString(),
                    useSelectedAsSummary = true,
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.Timer,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    entries = mapOf(
                        "10" to stringResource(id = R.string.entry_network_timeout_seconds, 10),
                        "15" to stringResource(id = R.string.entry_network_timeout_recommended, 15),
                        "30" to stringResource(id = R.string.entry_network_timeout_seconds, 30),
                        "60" to stringResource(id = R.string.entry_network_timeout_seconds, 60),
                    ),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { NetworkConfig.applyConfig(timeoutSec = it) }
                    },
                )
            }
            prefsItem {
                TextPref(
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.OfflineBolt,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    enabled = true,
                    title = stringResource(id = R.string.title_clear_picture_cache),
                    onClick = {
                        coroutineScope.launch {
                            ImageCacheUtil.clearImageAllCache(context)
                            cacheSize = "0.0B"
                            snackbarHostState.showSnackbar(context.getString(R.string.toast_clear_picture_cache_success))
                        }
                    },
                    summary = stringResource(id = R.string.tip_cache, cacheSize)
                )
            }
            prefsItem {
                TextPref(
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.Info,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    enabled = true,
                    title = stringResource(id = R.string.title_about),
                    onClick = {
                        navigator.navigate("settings/about")
                    },
                    summary = stringResource(id = R.string.tip_about, BuildConfig.VERSION_NAME)
                )
            }
        }
    }
}