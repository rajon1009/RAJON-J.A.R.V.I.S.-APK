package com.rajon.jarvis.ui.screens.system

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.jarvis.data.model.DeviceInfo
import com.rajon.jarvis.data.model.NetworkInfo
import com.rajon.jarvis.data.model.RamInfo
import com.rajon.jarvis.data.model.StorageInfo
import com.rajon.jarvis.device.DeviceController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SystemMonitorUiState(
    val deviceInfo: DeviceInfo = DeviceInfo(),
    val storageInfo: StorageInfo = StorageInfo(0f, 0f, 0f),
    val ramInfo: RamInfo = RamInfo(0, 0, 0),
    val networkInfo: NetworkInfo = NetworkInfo(false, false, false, "Unknown"),
    val cpuCores: Int = 1,
    val uptimeText: String = "0h 00m",
    val isRefreshing: Boolean = false,
    val lastUpdated: Long = 0L
)

@HiltViewModel
class SystemMonitorViewModel @Inject constructor(
    private val deviceController: DeviceController
) : ViewModel() {

    private val _deviceInfo = MutableStateFlow(deviceController.getFullDeviceInfo())
    private val _storageInfo = MutableStateFlow(deviceController.getStorageInfo())
    private val _ramInfo = MutableStateFlow(deviceController.getRamInfo())
    private val _networkInfo = MutableStateFlow(deviceController.getNetworkInfo())
    private val _isRefreshing = MutableStateFlow(false)
    private val _lastUpdated = MutableStateFlow(0L)
    private val _uptimeText = MutableStateFlow(formatUptime())

    val uiState: StateFlow<SystemMonitorUiState> = combine(
        combine(_deviceInfo, _storageInfo) { device, storage -> device to storage },
        combine(_ramInfo, _networkInfo) { ram, network -> ram to network },
        combine(_isRefreshing, _lastUpdated) { refreshing, updated -> refreshing to updated },
        _uptimeText
    ) { (device, storage), (ram, network), (refreshing, updated), uptime ->
        SystemMonitorUiState(
            deviceInfo = device,
            storageInfo = storage,
            ramInfo = ram,
            networkInfo = network,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            uptimeText = uptime,
            isRefreshing = refreshing,
            lastUpdated = updated
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SystemMonitorUiState()
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _deviceInfo.value = deviceController.getFullDeviceInfo()
            _storageInfo.value = deviceController.getStorageInfo()
            _ramInfo.value = deviceController.getRamInfo()
            _networkInfo.value = deviceController.getNetworkInfo()
            _uptimeText.value = formatUptime()
            _lastUpdated.value = System.currentTimeMillis()
            _isRefreshing.value = false
        }
    }

    private fun formatUptime(): String {
        val totalSeconds = SystemClock.elapsedRealtime() / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return "${hours}h ${"%02d".format(minutes)}m"
    }
}
