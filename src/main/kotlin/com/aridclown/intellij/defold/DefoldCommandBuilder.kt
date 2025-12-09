package com.aridclown.intellij.defold

import com.aridclown.intellij.defold.DefoldConstants.GAME_PROJECT_FILE
import com.aridclown.intellij.defold.Platform.*
import com.intellij.execution.configurations.GeneralCommandLine
import kotlin.io.path.Path
import kotlin.io.path.pathString

/**
 * Builds platform-specific commands to launch the Defold editor.
 */
class DefoldCommandBuilder {
    fun createLaunchCommand(projectPath: String): GeneralCommandLine = when (val platform = Platform.current()) {
        WINDOWS -> createWindowsCommand(projectPath)
        MACOS -> createMacCommand(projectPath)
        LINUX -> createLinuxCommand(projectPath)
        UNKNOWN -> error("Unknown platform: $platform")
    }

    private fun createWindowsCommand(projectPath: String): GeneralCommandLine {
        val defoldProgram = Path(DefoldDefaults.getDefoldInstallPath(), DefoldDefaults.getDefoldProcess()).pathString
        val gameProjectFile = Path(projectPath, GAME_PROJECT_FILE).pathString
        return GeneralCommandLine(defoldProgram, gameProjectFile)
    }

    private fun createMacCommand(projectPath: String): GeneralCommandLine = if (isDefoldProcessRunning()) {
        GeneralCommandLine("osascript", "-e", "activate application \"Defold\"")
    } else {
        val gameProjectFile = Path(projectPath, GAME_PROJECT_FILE).pathString
        GeneralCommandLine("open", "-a", DefoldDefaults.getDefoldProcess(), gameProjectFile)
    }

    private fun createLinuxCommand(projectPath: String): GeneralCommandLine {
        val gameProjectFile = Path(projectPath, GAME_PROJECT_FILE).pathString
        return GeneralCommandLine("xdg-open", gameProjectFile)
    }

    private fun isDefoldProcessRunning(): Boolean = runCatching {
        GeneralCommandLine("pgrep", "-x", "Defold")
            .createProcess()
            .waitFor() == 0
    }.getOrDefault(false)
}
