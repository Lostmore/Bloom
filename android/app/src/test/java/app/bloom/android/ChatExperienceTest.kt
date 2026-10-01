package app.bloom.android

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import app.bloom.android.core.model.ChatMessage
import app.bloom.android.core.ui.BloomTheme
import app.bloom.android.feature.chat.*
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChatExperienceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun stickerTabCanOpenSelectAndReturnToEmoji() {
        var draft by mutableStateOf(TextFieldValue())
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            BloomTheme("dark") {
                Surface { MessageComposer(draft, { draft = it }, true, true, {}, {}) }
            }
        }
        compose.onNodeWithContentDescription("Эмодзи").performClick()
        compose.onNodeWithTag("expression-categories").performScrollToIndex(5)
        compose.onNodeWithText("Стикеры Bloom").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Привет, это я").assertIsDisplayed()
        compose.onNodeWithText("Привет, это я").performClick()
        assertEquals(BloomStickers.first().wire, draft.text)
        compose.onNodeWithContentDescription("Убрать стикер").performClick()
        compose.onNodeWithContentDescription("Эмодзи").performClick()
        compose.onNodeWithText("😊").performClick()
        assertEquals("😊", draft.text)
    }

    @Test
    fun ownStickerShowsReadInformationOnHold() {
        val graph = AppGraph(compose.activity)
        val message =
            ChatMessage(9, 7, "me", BloomStickers.first().wire, "2026-09-30T10:00:00Z", readAt = "2026-09-30T10:01:00Z")
        compose.setContent { BloomTheme("dark") { Surface { MessageBubble(graph, message, true, false) } } }
        compose.onNodeWithContentDescription("Прочитано").assertExists()
        compose.onNodeWithText("Привет, это я").performTouchInput { longClick() }
        compose.onNodeWithText("О сообщении").assertIsDisplayed()
        compose.onNodeWithText("Понятно").performClick()
        compose.onNodeWithText("О сообщении").assertDoesNotExist()
    }

    @Test
    fun composerInsertsEmojiAndKeepsAttachmentAction() {
        var draft by mutableStateOf(TextFieldValue("Привет", TextRange(6)))
        var attached = false
        var sent = false
        compose.setContent {
            BloomTheme("dark") {
                Surface { MessageComposer(draft, { draft = it }, true, true, { sent = true }, { attached = true }) }
            }
        }
        compose.onNodeWithContentDescription("Эмодзи").performClick()
        compose.onNodeWithText("😊").performClick()
        assertEquals("Привет😊", draft.text)
        compose.onNodeWithContentDescription("Прикрепить фото").performClick()
        assertTrue(attached)
        compose.onNodeWithContentDescription("Отправить сообщение").performClick()
        assertTrue(sent)
    }

    @Test
    fun messageActionsSendEditedTextAndDeletedMessageHidesContent() {
        val graph = AppGraph(compose.activity)
        var message by mutableStateOf(ChatMessage(10, 7, "me", "Original", "2026-10-01T10:00:00Z"))
        var edited: String? = null
        var deleted = false
        compose.setContent {
            BloomTheme("dark") {
                Surface {
                    MessageBubble(
                        graph,
                        message,
                        true,
                        false,
                        edit = {
                            edited = it
                            true
                        },
                        delete = {
                            deleted = true
                            true
                        },
                    )
                }
            }
        }
        compose.onNodeWithText("Original").performTouchInput { longClick() }
        compose.onNodeWithText("Редактировать").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Changed")
        compose.onNodeWithText("Сохранить").performClick()
        assertEquals("Changed", edited)
        compose.onNodeWithText("Original").performTouchInput { longClick() }
        compose.onNodeWithText("Удалить").performClick()
        compose.onNodeWithText("Удалить").performClick()
        assertTrue(deleted)
        compose.runOnIdle { message = message.copy(deletedAt = "2026-10-01T10:01:00Z") }
        compose.onNodeWithText("Original").assertDoesNotExist()
        compose.onNodeWithText("Сообщение удалено").assertIsDisplayed()
    }

    @Test
    fun globalSearchOpensExactMessage() {
        var opened: Pair<Long, Long>? = null
        compose.setContent {
            BloomTheme("dark") {
                Surface {
                    ChatsContent(
                        listOf(ChatRowItem(7, "Анна")),
                        false,
                        null,
                        {},
                        {},
                        searchResults =
                            listOf(ChatMessage(42, 7, "partner", "Давай зайдём на кофе", "2026-09-30T10:00:00Z")),
                        openMessage = { room, id -> opened = room to id },
                    )
                }
            }
        }
        compose.onNodeWithTag("chat-search").performTextInput("кофе")
        compose.onNodeWithText("Давай зайдём на кофе").performClick()
        assertEquals(7L to 42L, opened)
        capture("chat-search-dark")
    }

    @Test
    fun albumOpensFullScreenViewer() {
        val graph = AppGraph(compose.activity)
        val source = PhotoSource("android.resource://app.bloom.android/${R.drawable.welcome_sunset}", true)
        compose.setContent {
            BloomTheme("dark") {
                Surface {
                    Column(Modifier.fillMaxSize().padding(20.dp)) {
                        PhotoAlbum(graph, listOf(source, source, source), Modifier.width(310.dp))
                    }
                }
            }
        }
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Фото").fetchSemanticsNodes().size == 3 }
        capture("chat-album-dark")
        compose.onNode(hasClickLabel("Открыть фото 1")).performClick()
        compose.onNodeWithText("1 / 3").assertIsDisplayed()
        compose.onNodeWithContentDescription("Закрыть фото").performClick()
        compose.onNodeWithText("1 / 3").assertDoesNotExist()
    }

    private fun hasClickLabel(label: String) =
        SemanticsMatcher("click label $label") {
            it.config.getOrNull(androidx.compose.ui.semantics.SemanticsActions.OnClick)?.label == label
        }

    private fun capture(name: String) {
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val file = File("build/design-previews/$name.png")
            file.parentFile!!.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
