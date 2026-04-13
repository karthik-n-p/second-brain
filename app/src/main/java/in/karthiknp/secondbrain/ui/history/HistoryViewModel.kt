package `in`.karthiknp.secondbrain.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.karthiknp.secondbrain.data.local.AppDatabase
import `in`.karthiknp.secondbrain.data.model.JournalEntry
import kotlinx.coroutines.flow.*

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val journalDao = db.journalDao()

    val journalEntries: StateFlow<List<JournalEntry>> = journalDao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
