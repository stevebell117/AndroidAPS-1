package info.nightscout.interfaces.sync

import android.text.Spanned
import org.json.JSONObject

interface NsClient : Sync {
    enum class Version {
        NONE, V1, V3
    }

    val version: Version
    val address: String
    val nsClientService: NSClientService?

    fun pause(newState: Boolean)
    fun resend(reason: String)
    fun textLog(): Spanned
    fun clearLog()

    fun updateLatestBgReceivedIfNewer(latestReceived: Long)
    fun updateLatestTreatmentReceivedIfNewer(latestReceived: Long)

    fun resetToFullSync()

    interface NSClientService {

        fun dbAdd(collection: String, data: JSONObject, originalObject: Any, progress: String)
        fun dbUpdate(collection: String, _id: String?, data: JSONObject?, originalObject: Any, progress: String)
    }
}