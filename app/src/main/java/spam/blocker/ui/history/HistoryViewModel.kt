package spam.blocker.ui.history

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import spam.blocker.db.CallTable
import spam.blocker.db.HistoryRecord
import spam.blocker.db.HistoryTable
import spam.blocker.db.SmsTable
import spam.blocker.util.SharedPref.HistoryOptions

/*
  To simplify the code, this view model is used in GlobalVariables instead of viewModel<...>().
 */
open class HistoryViewModel(
    val table: HistoryTable,
) : ViewModel() {
    val records = mutableStateListOf<HistoryRecord>()

    fun reload(ctx: Context) {
        records.clear()

        val spf = HistoryOptions(ctx)
        val showPassed = spf.getShowPassed()
        val showBlocked = spf.getShowBlocked()

        records.addAll(table.listRecords(ctx).filter {
            (showPassed && it.isNotBlocked()) || (showBlocked && it.isBlocked())
        })
    }
}

class CallViewModel : HistoryViewModel(CallTable())

class SmsViewModel : HistoryViewModel(SmsTable())
