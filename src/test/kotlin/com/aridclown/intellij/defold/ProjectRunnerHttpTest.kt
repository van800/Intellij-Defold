package com.aridclown.intellij.defold

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProjectRunnerHttpTest {
    @Test
    fun `defaults to build`() {
        val command = ProjectRunner.editorCommandFor(listOf("build"), enableDebugScript = false)

        assertThat(command).isEqualTo("build")
    }

    @Test
    fun `uses rebuild when distclean requested`() {
        val command = ProjectRunner.editorCommandFor(listOf("distclean", "build"), enableDebugScript = false)

        assertThat(command).isEqualTo("rebuild")
    }

    @Test
    fun `uses debugger start when debug script enabled`() {
        val command = ProjectRunner.editorCommandFor(listOf("build"), enableDebugScript = true)

        assertThat(command).isEqualTo("debugger-start")
    }

    @Test
    fun `delegation is disabled by default`() {
        val request = RunRequest(
            project = mockk(relaxed = true),
            config = mockk(relaxed = true),
            console = mockk(relaxed = true)
        )

        assertThat(request.delegateToEditor).isFalse()
    }
}
