package org.scarlet.android.collect

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HotFlowViewModel : ViewModel() {

    private val _backingFlow = MutableStateFlow(-1)
    lateinit var flow: StateFlow<Int>

    fun startFlow(kind: FlowKind) {
        when (kind) {
            FlowKind.FLOW1 -> {
                flow = _backingFlow.asStateFlow()
                viewModelScope.launch {
                    repeat(Int.MAX_VALUE) {
                        Log.d(TAG, "ViewModel: value = $it")
                        _backingFlow.value = it
                        delay(2000)
                    }
                }
            }
            FlowKind.FLOW2 -> {
                flow = _backingFlow.asStateFlow()
                viewModelScope.launch {
                    repeat(Int.MAX_VALUE) {
                        if (_backingFlow.subscriptionCount.value != 0) {
                            Log.d(TAG, "ViewModel: value = $it")
                            _backingFlow.value = it
                        }
                        delay(2000)
                    }
                }
            }
            FlowKind.FLOW3 -> {
                flow = flow {
                    repeat(Int.MAX_VALUE) {
                        Log.d(TAG, "ViewModel: value = $it")
                        emit(it)
                        delay(2000)
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(),
                    initialValue = -1
                )
            }
        }
    }

    companion object {
        private const val TAG = "Producer"

        enum class FlowKind { FLOW1, FLOW2, FLOW3 }
    }
}