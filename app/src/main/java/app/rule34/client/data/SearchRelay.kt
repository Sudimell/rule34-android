package app.rule34.client.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SearchRelay {
    private val pending = ArrayDeque<String>()
    private val signal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val updates = signal.asSharedFlow()

    fun push(tag: String) {
        synchronized(pending) { pending.addLast(tag) }
        signal.tryEmit(Unit)
    }

    fun drain(): List<String> = synchronized(pending) {
        if (pending.isEmpty()) {
            emptyList()
        } else {
            pending.toList().also { pending.clear() }
        }
    }
}
