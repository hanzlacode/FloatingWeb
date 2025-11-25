//package com.example.floatingweb
//
//
//import android.app.Application
//import android.content.Context
//import android.content.Intent
//import android.widget.Toast
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.floatingweb.data.LinkStorage
//import com.example.floatingweb.data.PersistedLink
//import com.example.floatingweb.services.FloatingBrowserService
//import kotlinx.coroutines.flow.SharingStarted
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.map
//import kotlinx.coroutines.flow.stateIn
//import kotlinx.coroutines.launch
//
//class FloatingWebViewModel(application: Application) : AndroidViewModel(application) {
//    private val storage = LinkStorage(application.applicationContext)
//
//    // expose links as StateFlow for Compose
//    val links: StateFlow<List<PersistedLink>> = storage.linksFlow
//        .map { it.sortedByDescending { l -> l.createdAt } }
//        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
//
//    private val _isServiceRunning = kotlinx.coroutines.flow.MutableStateFlow(false)
//    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning
//
//    // UI actions
//    fun addLink(url: String, title: String = "") {
//        viewModelScope.launch {
//            storage.addLink(url, title)
//        }
//    }
//
//    fun updateLink(link: PersistedLink) {
//        viewModelScope.launch {
//            storage.updateLink(link)
//        }
//    }
//
//    fun removeLink(id: String) {
//        viewModelScope.launch {
//            storage.removeLink(id)
//        }
//    }
//
//    fun toggleEnabled(link: PersistedLink) {
//        val toggled = link.copy(enabled = !link.enabled)
//        updateLink(toggled)
//    }
//
//    fun startOrStopService(context: Context) {
//        viewModelScope.launch {
//            val currently = _isServiceRunning.value
//            val intent = Intent(context, FloatingBrowserService::class.java)
//            if (!currently) {
//                // only send enabled links
//                val enabled = storage.linksFlow.firstOrNull()?.filter { it.enabled } ?: emptyList()
//                if (enabled.isEmpty()) {
//                    Toast.makeText(context, "Enable at least one link", Toast.LENGTH_SHORT).show()
//                    return@launch
//                }
//                intent.putStringArrayListExtra("links", ArrayList(enabled.map { it.url }))
//                context.startService(intent)
//                _isServiceRunning.value = true
//                Toast.makeText(context, "Floating browser started", Toast.LENGTH_SHORT).show()
//            } else {
//                context.stopService(intent)
//                _isServiceRunning.value = false
//                Toast.makeText(context, "Floating browser stopped", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//}