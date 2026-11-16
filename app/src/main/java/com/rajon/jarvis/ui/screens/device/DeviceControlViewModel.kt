package com.rajon.jarvis.ui.screens.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.jarvis.data.model.AppInfo
import com.rajon.jarvis.data.model.DeviceInfo
import com.rajon.jarvis.data.model.NetworkInfo
import com.rajon.jarvis.data.model.RamInfo
import com.rajon.jarvis.data.model.StorageInfo
import com.rajon.jarvis.device.DeviceController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceControlUiState(
    val deviceInfo: DeviceInfo = DeviceInfo(),
    val storageInfo: StorageInfo = StorageInfo(0f, 0f, 0f),
    val ramInfo: RamInfo = RamInfo(0, 0, 0),
    val networkInfo: NetworkInfo = NetworkInfo(false, false, false, "Unknown"),
    val installedApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val isRefreshing: Boolean = false
) {
    val filteredApps: List<AppInfo>
        get() = if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
}

@HiltViewModel
class DeviceControlViewModel @Inject constructor(
    private val deviceController: DeviceController
) : ViewModel() {

    private val _deviceInfo = MutableStateFlow(deviceController.getFullDeviceInfo())
    private val _storageInfo = MutableStateFlow(deviceController.getStorageInfo())
    private val _ramInfo = MutableStateFlow(deviceController.getRamInfo())
    private val _networkInfo = MutableStateFlow(deviceController.getNetworkInfo())
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<DeviceControlUiState> = combine(
        _deviceInfo,
        _storageInfo,
        _ramInfo,
        _networkInfo,
        _installedApps,
        _searchQuery,
        _isRefreshing
    ) { dev, storage, ram, net, apps, query, refreshing ->
        DeviceControlUiState(
            deviceInfo = dev,
            storageInfo = storage,
            ramInfo = ram,
            networkInfo = net,
            installedApps = apps,
            searchQuery = query,
            isRefreshing = refreshing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DeviceControlUiState()
    )

    init {
        refresh()
        loadInstalledApps()
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

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = deviceController.getInstalledApps()
            _installedApps.value = apps
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun launchApp(packageName: String): Boolean {
        return deviceController.launchApp(packageName)
    }

    fun openSettings(action: String): Boolean {
        return deviceController.openSettings(action)
    }

    fun openWhatsApp(): Boolean {
        return deviceController.openWhatsApp()
    }

    fun searchYouTube(query: String): Boolean {
        return deviceController.openYouTubeSearch(query)
    }
}
