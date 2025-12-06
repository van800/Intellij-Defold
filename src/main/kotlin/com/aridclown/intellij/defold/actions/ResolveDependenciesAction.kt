package com.aridclown.intellij.defold.actions

import com.aridclown.intellij.defold.BuildRequest
import com.aridclown.intellij.defold.DefoldCoroutineService.Companion.launch
import com.aridclown.intellij.defold.DefoldPathResolver
import com.aridclown.intellij.defold.DefoldProjectService.Companion.isDefoldProject
import com.aridclown.intellij.defold.ProjectBuilder
import com.aridclown.intellij.defold.process.ProcessExecutor
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.ActionUpdateThread.BGT
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class ResolveDependenciesAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project.isDefoldProject
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (!project.isDefoldProject) return

        val config = DefoldPathResolver.ensureEditorConfig(project) ?: return
        val builder = ProjectBuilder(ProcessExecutor())

        project.launch {
            builder.buildProject(
                BuildRequest(
                    project = project,
                    config = config,
                    commands = listOf("resolve")
                )
            )
        }
    }
}
