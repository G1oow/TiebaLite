@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)

package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.EditTextPref
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 设置项弹窗的草稿重置：修改后取消，再次打开必须回到**已保存**的值。
 *
 * 曾经的实现把重置写在 `if (dialogState.show) { LaunchedEffect(null) { … } }` 里，
 * 而 `PromptDialog` 会在首次组合时用 `initialValue` 初始化自己的输入状态——
 * 于是「修改 → 取消 → 再打开」会把上一次没保存的草稿带回来。
 */
@RunWith(AndroidJUnit4::class)
class PrefsDraftTest {
    @get:Rule
    val compose = createComposeRule()

    private fun newDataStore(): DataStore<Preferences> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { File(context.cacheDir, "prefs-draft-${System.nanoTime()}.preferences_pb") },
        )
    }

    @Test
    fun editTextPrefReopensWithSavedValueAfterCancel() {
        val store = newDataStore()
        // LocalPrefsDataStore 是 lateinit 顶层变量，由 PrefsScreen 赋值；这里单独渲染一个设置项，
        // 所以自行装一个只指向本用例临时 DataStore 的 composition local。
        LocalPrefsDataStore = staticCompositionLocalOf { store }
        compose.setContent {
            TiebaLiteTheme {
                CompositionLocalProvider(LocalPrefsDataStore provides store) {
                    EditTextPref(
                        key = "pi_test_draft",
                        title = "Draft",
                        defaultValue = "SAVED",
                        dialogTitle = "Edit draft",
                    )
                }
            }
        }
        compose.waitForIdle()

        // 打开：输入框应是已保存的值
        compose.onNodeWithText("Draft").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("SAVED").fetchSemanticsNodes().isNotEmpty()
        }

        // 改成未保存的草稿，然后取消
        compose.onNodeWithText("SAVED").performTextReplacement("DRAFT")
        compose.onNodeWithText("取消").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("DRAFT").fetchSemanticsNodes().isEmpty()
        }

        // 重新打开：必须回到已保存的值，草稿不能残留
        compose.onNodeWithText("Draft").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("SAVED").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("DRAFT").assertDoesNotExist()
        compose.onNodeWithText("取消").performClick()
    }
}
