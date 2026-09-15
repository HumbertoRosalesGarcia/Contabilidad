package com.xxcamixx.contabilidad.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModelProvider

class FinanceViewModelFactory(private val application: Application, private val userId: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return FinanceViewModel(application, userId) as T
    }
}
