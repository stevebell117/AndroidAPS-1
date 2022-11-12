package info.nightscout.androidaps.di

import android.content.Context
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.implementations.ActivityNamesImpl
import info.nightscout.androidaps.interfaces.stats.DexcomTirCalculator
import info.nightscout.androidaps.interfaces.stats.TddCalculator
import info.nightscout.androidaps.interfaces.stats.TirCalculator
import info.nightscout.androidaps.plugins.aps.loop.LoopPlugin
import info.nightscout.androidaps.plugins.configBuilder.ConfigBuilderPlugin
import info.nightscout.androidaps.plugins.configBuilder.PluginStore
import info.nightscout.androidaps.plugins.configBuilder.ProfileFunctionImpl
import info.nightscout.androidaps.plugins.general.maintenance.ImportExportPrefsImpl
import info.nightscout.androidaps.plugins.general.maintenance.PrefFileListProvider
import info.nightscout.androidaps.plugins.iob.iobCobCalculator.IobCobCalculatorPlugin
import info.nightscout.androidaps.utils.androidNotification.NotificationHolderImpl
import info.nightscout.androidaps.utils.buildHelper.BuildHelperImpl
import info.nightscout.androidaps.utils.buildHelper.ConfigImpl
import info.nightscout.androidaps.utils.resources.IconsProviderImplementation
import info.nightscout.core.fabric.FabricPrivacy
import info.nightscout.database.impl.AppRepository
import info.nightscout.implementation.AndroidPermissionImpl
import info.nightscout.implementation.BolusTimerImpl
import info.nightscout.implementation.CarbTimerImpl
import info.nightscout.implementation.LocalAlertUtilsImpl
import info.nightscout.implementation.TrendCalculatorImpl
import info.nightscout.implementation.XDripBroadcastImpl
import info.nightscout.implementation.constraints.ConstraintsImpl
import info.nightscout.implementation.pump.PumpSyncImplementation
import info.nightscout.implementation.queue.CommandQueueImplementation
import info.nightscout.implementation.stats.DexcomTirCalculatorImpl
import info.nightscout.implementation.stats.TddCalculatorImpl
import info.nightscout.implementation.stats.TirCalculatorImpl
import info.nightscout.interfaces.AndroidPermission
import info.nightscout.interfaces.BolusTimer
import info.nightscout.interfaces.BuildHelper
import info.nightscout.interfaces.CarbTimer
import info.nightscout.interfaces.Config
import info.nightscout.interfaces.ConfigBuilder
import info.nightscout.interfaces.LocalAlertUtils
import info.nightscout.interfaces.NotificationHolder
import info.nightscout.interfaces.XDripBroadcast
import info.nightscout.interfaces.aps.Loop
import info.nightscout.interfaces.autotune.Autotune
import info.nightscout.interfaces.constraints.Constraints
import info.nightscout.interfaces.iob.IobCobCalculator
import info.nightscout.interfaces.maintenance.ImportExportPrefs
import info.nightscout.interfaces.plugin.ActivePlugin
import info.nightscout.interfaces.plugin.PluginBase
import info.nightscout.interfaces.profile.ProfileFunction
import info.nightscout.interfaces.pump.PumpSync
import info.nightscout.interfaces.queue.CommandQueue
import info.nightscout.interfaces.smsCommunicator.SmsCommunicator
import info.nightscout.interfaces.storage.FileStorage
import info.nightscout.interfaces.storage.Storage
import info.nightscout.interfaces.sync.DataSyncSelector
import info.nightscout.interfaces.ui.ActivityNames
import info.nightscout.interfaces.ui.IconsProvider
import info.nightscout.interfaces.utils.HardLimits
import info.nightscout.interfaces.utils.TrendCalculator
import info.nightscout.plugins.general.autotune.AutotunePlugin
import info.nightscout.plugins.general.smsCommunicator.SmsCommunicatorPlugin
import info.nightscout.plugins.sync.nsclient.DataSyncSelectorImplementation
import info.nightscout.plugins.sync.nsclient.data.ProcessedDeviceStatusData
import info.nightscout.rx.AapsSchedulers
import info.nightscout.rx.bus.RxBus
import info.nightscout.rx.logging.AAPSLogger
import info.nightscout.shared.interfaces.ResourceHelper
import info.nightscout.shared.sharedPreferences.SP
import info.nightscout.shared.utils.DateUtil
import javax.inject.Singleton

@Suppress("unused")
@Module(
    includes = [
        AppModule.AppBindings::class
    ]
)
open class AppModule {

    @Provides
    fun providesPlugins(
        config: Config, buildHelper: BuildHelper,
        @PluginsListModule.AllConfigs allConfigs: Map<@JvmSuppressWildcards Int, @JvmSuppressWildcards PluginBase>,
        @PluginsListModule.PumpDriver pumpDrivers: Lazy<Map<@JvmSuppressWildcards Int, @JvmSuppressWildcards PluginBase>>,
        @PluginsListModule.NotNSClient notNsClient: Lazy<Map<@JvmSuppressWildcards Int, @JvmSuppressWildcards PluginBase>>,
        @PluginsListModule.APS aps: Lazy<Map<@JvmSuppressWildcards Int, @JvmSuppressWildcards PluginBase>>,
        @PluginsListModule.Unfinished unfinished: Lazy<Map<@JvmSuppressWildcards Int, @JvmSuppressWildcards PluginBase>>
    )
        : List<@JvmSuppressWildcards PluginBase> {
        val plugins = allConfigs.toMutableMap()
        if (config.PUMPDRIVERS) plugins += pumpDrivers.get()
        if (config.APS) plugins += aps.get()
        if (!config.NSCLIENT) plugins += notNsClient.get()
        if (buildHelper.isUnfinishedMode()) plugins += unfinished.get()
        return plugins.toList().sortedBy { it.first }.map { it.second }
    }

    @Provides
    @Singleton
    fun provideStorage(): Storage = FileStorage()

    @Provides
    @Singleton
    fun provideBuildHelper(config: Config, fileListProvider: PrefFileListProvider): BuildHelper = BuildHelperImpl(config, fileListProvider)

    @Provides
    @Singleton
    fun provideProfileFunction(
        aapsLogger: AAPSLogger, sp: SP, rxBus: RxBus, rh:
        ResourceHelper, activePlugin:
        ActivePlugin, repository: AppRepository, dateUtil: DateUtil, config: Config, hardLimits: HardLimits,
        aapsSchedulers: AapsSchedulers, fabricPrivacy: FabricPrivacy, processedDeviceStatusData: ProcessedDeviceStatusData
    ): ProfileFunction =
        ProfileFunctionImpl(
            aapsLogger, sp, rxBus, rh, activePlugin, repository, dateUtil,
            config, hardLimits, aapsSchedulers, fabricPrivacy, processedDeviceStatusData
        )

    @Provides
    @Singleton
    internal fun provideConstraints(activePlugin: ActivePlugin): Constraints = ConstraintsImpl(activePlugin)

    @Module
    interface AppBindings {

        @Binds fun bindContext(mainApp: MainApp): Context
        @Binds fun bindInjector(mainApp: MainApp): HasAndroidInjector
        @Binds fun bindActivePlugin(pluginStore: PluginStore): ActivePlugin
        @Binds fun bindCommandQueue(commandQueue: CommandQueueImplementation): CommandQueue
        @Binds fun bindConfigInterface(config: ConfigImpl): Config

        @Binds fun bindConfigBuilderInterface(configBuilderPlugin: ConfigBuilderPlugin): ConfigBuilder
        @Binds fun bindNotificationHolderInterface(notificationHolder: NotificationHolderImpl): NotificationHolder
        @Binds fun bindImportExportPrefsInterface(importExportPrefs: ImportExportPrefsImpl): ImportExportPrefs
        @Binds fun bindIconsProviderInterface(iconsProvider: IconsProviderImplementation): IconsProvider
        @Binds fun bindLoopInterface(loopPlugin: LoopPlugin): Loop
        @Binds fun bindAutotuneInterface(autotunePlugin: AutotunePlugin): Autotune
        @Binds fun bindIobCobCalculatorInterface(iobCobCalculatorPlugin: IobCobCalculatorPlugin): IobCobCalculator
        @Binds fun bindSmsCommunicatorInterface(smsCommunicatorPlugin: SmsCommunicatorPlugin): SmsCommunicator
        @Binds fun bindDataSyncSelectorInterface(dataSyncSelectorImplementation: DataSyncSelectorImplementation): DataSyncSelector
        @Binds fun bindPumpSyncInterface(pumpSyncImplementation: PumpSyncImplementation): PumpSync
        @Binds fun bindXDripBroadcastInterface(xDripBroadcastImpl: XDripBroadcastImpl): XDripBroadcast
        @Binds fun bindCarbTimerInterface(carbTimer: CarbTimerImpl): CarbTimer
        @Binds fun bindBolusTimerInterface(bolusTimer: BolusTimerImpl): BolusTimer
        @Binds fun bindAndroidPermissionInterface(androidPermission: AndroidPermissionImpl): AndroidPermission
        @Binds fun bindLocalAlertUtilsInterface(localAlertUtils: LocalAlertUtilsImpl): LocalAlertUtils
        @Binds fun bindActivityNamesInterface(activityNames: ActivityNamesImpl): ActivityNames
        @Binds fun bindTrendCalculatorInterface(trendCalculator: TrendCalculatorImpl): TrendCalculator
        @Binds fun bindTddCalculatorInterface(tddCalculator: TddCalculatorImpl): TddCalculator
        @Binds fun bindTirCalculatorInterface(tirCalculator: TirCalculatorImpl): TirCalculator
        @Binds fun bindDexcomTirCalculatorInterface(dexcomTirCalculator: DexcomTirCalculatorImpl): DexcomTirCalculator
    }
}

