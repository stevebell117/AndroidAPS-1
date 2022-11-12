package info.nightscout.androidaps.workflow

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.receivers.DataWorkerStorage
import info.nightscout.database.impl.AppRepository
import info.nightscout.interfaces.iob.IobCobCalculator
import info.nightscout.rx.bus.RxBus
import info.nightscout.rx.logging.AAPSLogger
import info.nightscout.shared.utils.DateUtil
import loadBgData
import javax.inject.Inject

class LoadBgDataWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    @Inject lateinit var dataWorkerStorage: DataWorkerStorage
    @Inject lateinit var aapsLogger: AAPSLogger
    @Inject lateinit var dateUtil: DateUtil
    @Inject lateinit var rxBus: RxBus
    @Inject lateinit var repository: AppRepository

    init {
        (context.applicationContext as HasAndroidInjector).androidInjector().inject(this)
    }

    class LoadBgData(
        val iobCobCalculator: IobCobCalculator,
        val end: Long
    )

    override fun doWork(): Result {

        val data = dataWorkerStorage.pickupObject(inputData.getLong(DataWorkerStorage.STORE_KEY, -1)) as LoadBgData?
            ?: return Result.failure(workDataOf("Error" to "missing input data"))

        data.iobCobCalculator.ads.loadBgData(data.end, repository, aapsLogger, dateUtil, rxBus)
        data.iobCobCalculator.clearCache()
        return Result.success()
    }
}