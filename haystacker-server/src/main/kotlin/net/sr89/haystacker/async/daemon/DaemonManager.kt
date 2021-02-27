package net.sr89.haystacker.async.daemon

import net.sr89.haystacker.async.daemon.DaemonProcessExecutionState.NOT_FOUND
import java.util.UUID

data class DaemonProcessID(val id: UUID)

data class DaemonProcessProgress(val id: DaemonProcessID, val state: DaemonProcessExecutionState, val description: String)

enum class DaemonProcessExecutionState {
    NOT_FOUND, RUNNING, COMPLETED
}

class DaemonManager {
    fun submit(process: DaemonProcess): DaemonProcessID {
        return DaemonProcessID(UUID.randomUUID())
    }

    fun status(id: DaemonProcessID): DaemonProcessProgress {
        return DaemonProcessProgress(id, NOT_FOUND, "Not found")
    }
}