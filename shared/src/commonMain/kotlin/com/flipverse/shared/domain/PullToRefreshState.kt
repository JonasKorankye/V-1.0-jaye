import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PullToRefreshState(
    val isRefreshing: Boolean = false,
    val pullOffset: Float = 0f,
    val canRefresh: Boolean = true
)

class PullToRefreshController {
    private val _state = MutableStateFlow(PullToRefreshState())
    val state: StateFlow<PullToRefreshState> = _state.asStateFlow()

    private var onRefreshCallback: (suspend () -> Unit)? = null

    fun setOnRefresh(callback: suspend () -> Unit) {
        onRefreshCallback = callback
    }

    suspend fun refresh() {
        if (_state.value.isRefreshing) return

        _state.value = _state.value.copy(isRefreshing = true)
        try {
            onRefreshCallback?.invoke()
        } finally {
            _state.value = _state.value.copy(isRefreshing = false, pullOffset = 0f)
        }
    }

    fun updatePullOffset(offset: Float) {
        if (!_state.value.isRefreshing) {
            _state.value = _state.value.copy(pullOffset = offset)
        }
    }

    fun setCanRefresh(canRefresh: Boolean) {
        _state.value = _state.value.copy(canRefresh = canRefresh)
    }

    fun stopRefreshing() {
        _state.value = _state.value.copy(isRefreshing = false, pullOffset = 0f)
    }
}