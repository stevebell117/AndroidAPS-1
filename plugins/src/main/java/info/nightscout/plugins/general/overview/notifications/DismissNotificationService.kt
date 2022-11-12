package info.nightscout.plugins.general.overview.notifications

import android.content.Intent
import dagger.android.DaggerIntentService
import info.nightscout.androidaps.plugins.general.overview.events.EventDismissNotification
import info.nightscout.rx.bus.RxBus
import javax.inject.Inject

class DismissNotificationService : DaggerIntentService(DismissNotificationService::class.simpleName) {
    @Inject lateinit var rxBus: RxBus

    override fun onHandleIntent(intent: Intent?) {
        intent?.let {
            rxBus.send(EventDismissNotification(intent.getIntExtra("alertID", -1)))
        }
    }
}