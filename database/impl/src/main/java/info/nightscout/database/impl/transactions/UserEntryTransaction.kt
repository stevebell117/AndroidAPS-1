package info.nightscout.database.impl.transactions

import info.nightscout.database.entities.UserEntry
import info.nightscout.database.entities.UserEntry.Action
import info.nightscout.database.entities.UserEntry.Sources
import info.nightscout.database.entities.ValueWithUnit

class UserEntryTransaction(private val entries: List<Entry>) : Transaction<List<UserEntryTransaction.Entry>>() {

    data class Entry(
        val timestamp: Long,
        val action: Action,
        val source: Sources,
        val note: String,
        val values: List<ValueWithUnit?> = listOf()
    )

    override fun run(): List<Entry> {

        for (entry in entries)
            database.userEntryDao.insert(
                UserEntry(
                    timestamp = entry.timestamp,
                    action = entry.action,
                    source = entry.source,
                    note = entry.note,
                    values = entry.values
                )
            )
        return entries
    }
}