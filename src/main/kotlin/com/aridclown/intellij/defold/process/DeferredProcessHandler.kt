package com.aridclown.intellij.defold.process

import com.intellij.execution.process.*
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lightweight process handler that is returned to the platform immediately while the Defold build runs.
 * Once the engine process starts we "attach" the real [OSProcessHandler] so lifecycle events propagate
 * correctly to the Run tool window actions (start, stop, termination, etc.).
 */
internal class DeferredProcessHandler : ProcessHandler() {
    private var attachedHandler: OSProcessHandler? = null
    private val terminated = AtomicBoolean(false)

    fun attach(handler: OSProcessHandler) {
        if (terminated.get()) {
            handler.destroyProcess()
            return
        }

        attachedHandler = handler
        if (handler.isProcessTerminated) {
            terminate(handler.exitCode ?: -1)
            return
        }

        handler.notifyTextAvailable(
            "Defold engine started with PID ${handler.process.pid()}\n",
            ProcessOutputTypes.STDOUT
        )

        handler.addProcessListener(
            object : ProcessListener {
                override fun processTerminated(event: ProcessEvent) {
                    terminate(event.exitCode)
                }
            }
        )
    }

    fun terminate(exitCode: Int = -1) {
        if (terminated.compareAndSet(false, true)) {
            notifyProcessTerminated(exitCode)
        }
    }

    override fun destroyProcessImpl() {
        when (attachedHandler) {
            null -> terminate()
            else -> attachedHandler?.destroyProcess()
        }
    }

    override fun detachProcessImpl() {
        attachedHandler?.detachProcess()
    }

    override fun detachIsDefault(): Boolean = false

    override fun getProcessInput(): OutputStream? = attachedHandler?.processInput
}
