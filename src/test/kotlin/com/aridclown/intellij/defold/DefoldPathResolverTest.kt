package com.aridclown.intellij.defold

import com.aridclown.intellij.defold.settings.DefoldSettings
import com.aridclown.intellij.defold.settings.DefoldSettingsConfigurable
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class DefoldPathResolverTest {

    private val project = mockk<Project>(relaxed = true)
    private val application = mockk<Application>(relaxed = true)
    private val settings = mockk<DefoldSettings>(relaxed = true)
    private val notificationGroupManager = mockk<NotificationGroupManager>(relaxed = true)
    private val notificationGroup = mockk<NotificationGroup>(relaxed = true)
    private val notification = mockk<Notification>(relaxed = true)
    private val showSettingsUtil = mockk<ShowSettingsUtil>(relaxed = true)

    @BeforeEach
    fun setUp() {
        // Mock ApplicationManager
        mockkStatic(ApplicationManager::class)
        every { ApplicationManager.getApplication() } returns application

        // Mock DefoldSettings
        mockkObject(DefoldSettings.Companion)
        every { DefoldSettings.getInstance() } returns settings

        // Mock Notification manager
        mockkStatic(NotificationGroupManager::class)
        every { NotificationGroupManager.getInstance() } returns notificationGroupManager
        every { notificationGroupManager.getNotificationGroup("Defold") } returns notificationGroup
        every {
            notificationGroup.createNotification(
                any<String>(),
                any<String>(),
                any<NotificationType>()
            )
        } returns notification

        // Mock DefoldEditorConfig
        mockkObject(DefoldEditorConfig.Companion)

        // Mock Platform
        mockkObject(Platform.Companion)

        // Mock Messages dialog
        mockkStatic(Messages::class)

        // Mock ShowSettingsUtil
        mockkStatic(ShowSettingsUtil::class)
        every { ShowSettingsUtil.getInstance() } returns showSettingsUtil

        // Default: invokeAndWait executes immediately
        every { application.invokeAndWait(any<Runnable>()) } answers {
            firstArg<Runnable>().run()
        }

        // Default mock for project.messageBus (used by notifications)
        every { project.messageBus } returns mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Nested
    inner class `Config Loading` {

        @Test
        fun `returns config when valid`() {
            val expectedConfig = mockk<DefoldEditorConfig>()
            every { DefoldEditorConfig.loadEditorConfig() } returns expectedConfig

            val result = DefoldPathResolver.ensureEditorConfig(project)

            assertThat(result).isEqualTo(expectedConfig)
        }

        @Test
        fun `returns null when config invalid and user cancels`() {
            every { DefoldEditorConfig.loadEditorConfig() } returns null
            every { settings.installPath() } returns "/some/path"
            every { Platform.current() } returns Platform.MACOS
            every {
                Messages.showOkCancelDialog(
                    any<Project>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any()
                )
            } returns Messages.CANCEL

            val result = DefoldPathResolver.ensureEditorConfig(project)

            assertThat(result).isNull()
            verify(exactly = 0) {
                showSettingsUtil.showSettingsDialog(
                    any<Project>(),
                    any<Class<DefoldSettingsConfigurable>>()
                )
            }
        }

        @Test
        fun `loads config after user fixes path in settings`() {
            val expectedConfig = mockk<DefoldEditorConfig>()
            var callCount = 0
            every { DefoldEditorConfig.loadEditorConfig() } answers {
                if (callCount++ == 0) null else expectedConfig
            }
            every { settings.installPath() } returns "/some/path"
            every { Platform.current() } returns Platform.MACOS
            every {
                Messages.showOkCancelDialog(
                    any<Project>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any()
                )
            } returns Messages.YES
            every {
                showSettingsUtil.showSettingsDialog(
                    any<Project>(),
                    eq(DefoldSettingsConfigurable::class.java)
                )
            } just Runs

            val result = DefoldPathResolver.ensureEditorConfig(project)

            assertThat(result).isEqualTo(expectedConfig)
            verify(exactly = 1) { showSettingsUtil.showSettingsDialog(project, DefoldSettingsConfigurable::class.java) }
            verify(exactly = 2) { DefoldEditorConfig.loadEditorConfig() }
        }
    }

    @Nested
    inner class `Dialog Interaction` {

        @Test
        fun `shows dialog with attempted path when config missing`() {
            every { DefoldEditorConfig.loadEditorConfig() } returns null
            every { settings.installPath() } returns "/custom/path"
            every { Platform.current() } returns Platform.MACOS
            every {
                Messages.showOkCancelDialog(
                    any<Project>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any()
                )
            } returns Messages.CANCEL

            DefoldPathResolver.ensureEditorConfig(project)

            verify(exactly = 1) {
                Messages.showOkCancelDialog(
                    eq(project),
                    eq(
                        "The Defold installation path could not be located.\n" +
                                "Current location: /custom/path\n" +
                                "\n" +
                                "Would you like to update the path now?"
                    ),
                    eq("Defold"),
                    eq("Open Settings"),
                    eq("Cancel"),
                    any()
                )
            }
        }

        @Test
        fun `shows dialog without path when none available`() {
            every { DefoldEditorConfig.loadEditorConfig() } returns null
            every { settings.installPath() } returns null
            every { Platform.current() } returns Platform.UNKNOWN
            every {
                Messages.showOkCancelDialog(
                    any<Project>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any()
                )
            } returns Messages.CANCEL

            DefoldPathResolver.ensureEditorConfig(project)

            verify(exactly = 1) {
                Messages.showOkCancelDialog(
                    eq(project),
                    match {
                        it.contains("The Defold installation path could not be located.")
                                && !it.contains("Current location:")
                                && it.contains("Would you like to update the path now?")
                    },
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any()
                )
            }
        }

        @Test
        fun `opens settings when user clicks OK`() {
            every { DefoldEditorConfig.loadEditorConfig() } returns null
            every { settings.installPath() } returns "/some/path"
            every { Platform.current() } returns Platform.MACOS
            every {
                Messages.showOkCancelDialog(
                    any<Project>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any()
                )
            } returns Messages.YES
            every {
                showSettingsUtil.showSettingsDialog(
                    any<Project>(),
                    eq(DefoldSettingsConfigurable::class.java)
                )
            } just Runs

            DefoldPathResolver.ensureEditorConfig(project)

            verify(exactly = 1) { application.invokeAndWait(any<Runnable>()) }
            verify(exactly = 1) { showSettingsUtil.showSettingsDialog(project, DefoldSettingsConfigurable::class.java) }
            verify(exactly = 1) { notification.notify(any()) }
        }

        @Test
        fun `skips settings when user clicks Cancel`() {
            every { DefoldEditorConfig.loadEditorConfig() } returns null
            every { settings.installPath() } returns "/some/path"
            every { Platform.current() } returns Platform.MACOS
            every {
                Messages.showOkCancelDialog(
                    any<Project>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any()
                )
            } returns Messages.CANCEL

            DefoldPathResolver.ensureEditorConfig(project)

            verify(exactly = 0) {
                showSettingsUtil.showSettingsDialog(
                    any<Project>(),
                    any<Class<DefoldSettingsConfigurable>>()
                )
            }
        }
    }

    @Nested
    inner class `Notification Handling` {

        @Test
        fun `shows notification when config still invalid after settings`() {
            every { DefoldEditorConfig.loadEditorConfig() } returns null
            every { settings.installPath() } returns "/some/path"
            every { Platform.current() } returns Platform.MACOS
            every {
                Messages.showOkCancelDialog(
                    any<Project>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any()
                )
            } returns Messages.YES
            every {
                showSettingsUtil.showSettingsDialog(
                    any<Project>(),
                    eq(DefoldSettingsConfigurable::class.java)
                )
            } just Runs

            val result = DefoldPathResolver.ensureEditorConfig(project)

            assertThat(result).isNull()
            // Notification should be triggered - config is checked twice (before and after settings)
            verify(exactly = 2) { DefoldEditorConfig.loadEditorConfig() }
            verify(exactly = 1) { application.invokeAndWait(any<Runnable>()) }
            verify(exactly = 1) { notification.addAction(any()) }
            verify(exactly = 1) { notification.notify(any()) }
        }

        @Test
        fun `notification has Configure action that reopens settings`() {
            every { DefoldEditorConfig.loadEditorConfig() } returns null
            every { settings.installPath() } returns "/some/path"
            every { Platform.current() } returns Platform.MACOS
            every {
                Messages.showOkCancelDialog(
                    any<Project>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any()
                )
            } returns Messages.YES
            every {
                showSettingsUtil.showSettingsDialog(
                    any<Project>(),
                    eq(DefoldSettingsConfigurable::class.java)
                )
            } just Runs

            val result = DefoldPathResolver.ensureEditorConfig(project)

            assertThat(result).isNull()
            verify(exactly = 1) {
                notification.addAction(match {
                    it.templateText == "Configure"
                })
            }
        }
    }

    @Nested
    inner class `Platform Detection` {

        @Test
        fun `uses configured path when set`() {
            every { DefoldEditorConfig.loadEditorConfig() } returns null
            every { settings.installPath() } returns "/custom/defold"
            every { Platform.current() } returns Platform.MACOS
            every {
                Messages.showOkCancelDialog(
                    any<Project>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any()
                )
            } returns Messages.CANCEL

            DefoldPathResolver.ensureEditorConfig(project)

            verify(exactly = 1) {
                Messages.showOkCancelDialog(
                    any<Project>(),
                    match { it.contains("/custom/defold") },
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any()
                )
            }
        }

        @ParameterizedTest(name = "falls back to platform default for {0}")
        @CsvSource(
            "MACOS, /Applications/Defold.app",
            "WINDOWS, C:\\Program Files\\Defold",
            "LINUX, /usr/bin/Defold"
        )
        fun `falls back to platform default when path not configured`(platform: Platform, expectedPath: String) {
            every { DefoldEditorConfig.loadEditorConfig() } returns null
            every { settings.installPath() } returns null
            every { Platform.current() } returns platform
            every {
                Messages.showOkCancelDialog(
                    any<Project>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any()
                )
            } returns Messages.CANCEL

            DefoldPathResolver.ensureEditorConfig(project)

            verify(exactly = 1) {
                Messages.showOkCancelDialog(
                    any<Project>(),
                    match { it.contains(expectedPath) },
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any()
                )
            }
        }

        @Test
        fun `handles unknown platform gracefully`() {
            every { DefoldEditorConfig.loadEditorConfig() } returns null
            every { settings.installPath() } returns null
            every { Platform.current() } returns Platform.UNKNOWN
            every {
                Messages.showOkCancelDialog(
                    any<Project>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any<String>(),
                    any()
                )
            } returns Messages.CANCEL

            val result = DefoldPathResolver.ensureEditorConfig(project)

            assertThat(result).isNull()
            // Should not crash, just shows dialog without suggested path
        }
    }
}
