package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn

lateinit var LocalPrefsDataStore: ProvidableCompositionLocal<DataStore<Preferences>>

/**
 * Main preference screen which holds [PrefsListItem]s
 *
 * @param dataStore DataStore which will be used to save all the preferences
 * @param modifier Modifier applied to the [MyLazyColumn] holding the list of Prefs
 *
 */
@Composable
fun PrefsScreen(
    dataStore: DataStore<Preferences>,
    modifier: Modifier = Modifier,
    dividerThickness: Dp = 1.dp, // 0 for no divider
    dividerIndent: Dp = 0.dp, // indents on both sides
    content: PrefsScope.() -> Unit
) {
    LocalPrefsDataStore = staticCompositionLocalOf { dataStore }
    val prefsScope = PrefsScopeImpl().apply(content)

    // Now the dataStore can be accessed by calling LocalPrefsDataStore.current from any child Pref
    CompositionLocalProvider(LocalPrefsDataStore provides dataStore) {
        Container {
            // 用 MyLazyColumn（而不是裸 LazyColumn）：它会自动合并 LocalBarsContentPadding，
            // 设置项因此能滚进悬浮顶栏 / 胶囊底栏所在区域，而不是被整页内缩。
            MyLazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp),
            ) {
                items(prefsScope.prefsItems.size) { index ->
                    prefsScope.getPrefsItem(index)()

                    if (dividerThickness != 0.dp
                        && index != prefsScope.prefsItems.size - 1
                        && !prefsScope.headerIndexes.contains(index)
                        && !prefsScope.headerIndexes.contains(index + 1)
                        && !prefsScope.footerIndexes.contains(index)
                    ) {
                        Divider(
                            thickness = dividerThickness,
                            indent = dividerIndent
                        )
                    }
                }
            }
        }
    }
}
