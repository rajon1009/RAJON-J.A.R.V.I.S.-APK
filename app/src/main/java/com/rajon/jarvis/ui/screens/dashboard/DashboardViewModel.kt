package com.rajon.jarvis.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.jarvis.data.local.PreferencesManager
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
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val ownerName: String = "Rajon",
    val greeting: String = "Good day, Sir",
    val deviceInfo: DeviceInfo = DeviceInfo(),
    val storageInfo: StorageInfo = StorageInfo(0f, 0f, 0f),
    val ramInfo: RamInfo = RamInfo(0, 0, 0),
    val networkInfo: NetworkInfo = NetworkInfo(false, false, false, "Offline"),
    val isRefreshing: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val deviceController: DeviceController,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _deviceInfo = MutableStateFlow(deviceController.getFullDeviceInfo())
    private val _storageInfo = MutableStateFlow(deviceController.getStorageInfo())
    private val _ramInfo = MutableStateFlow(deviceController.getRamInfo())
    private val _networkInfo = MutableStateFlow(deviceController.getNetworkInfo())

    val uiState: StateFlow<DashboardUiState> = combine(
        preferencesManager.ownerName,
        preferencesManager.language,
        _deviceInfo,
        _storageInfo,
        _ramInfo,
        _networkInfo,
        _isRefreshing
    ) { owner, lang, devInfo, storage, ram, net, refreshing ->
        DashboardUiState(
            ownerName = owner,
            greeting = computeGreeting(owner, lang),
            deviceInfo = devInfo,
            storageInfo = storage,
            ramInfo = ram,
            networkInfo = net,
            isRefreshing = refreshing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
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
            _isRefreshing.value = false
        }
    }

    private fun computeGreeting(owner: String, lang: String): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isBengali = lang == "bn"

        return when (hour) {
            in 5..11 -> if (isBengali) "শুভ সকাল, $owner স্যার" else "Good morning, $owner Sir"
            in 12..16 -> if (isBengali) "শুভ দুপুর, $owner স্যার" else "Good afternoon, $owner Sir"
            in 17..20 -> if (isBengali) "শুভ সন্ধ্যা, $owner স্যার" else "Good evening, $owner Sir"
            else -> if (isBengali) "শুভ রাত্রি, $owner স্যার" else "Good night, $owner Sir"
        }
    }
}
