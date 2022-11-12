package info.nightscout.plugins.ui

import android.widget.TextView
import androidx.annotation.StringRes
import info.nightscout.androidaps.utils.DecimalFormatter
import info.nightscout.androidaps.utils.WarnColors
import info.nightscout.database.entities.TherapyEvent
import info.nightscout.database.impl.AppRepository
import info.nightscout.database.impl.ValueWrapper
import info.nightscout.interfaces.Config
import info.nightscout.interfaces.plugin.ActivePlugin
import info.nightscout.interfaces.pump.defs.PumpType
import info.nightscout.plugins.R
import info.nightscout.plugins.sync.nsclient.extensions.age
import info.nightscout.shared.interfaces.ResourceHelper
import info.nightscout.shared.sharedPreferences.SP
import info.nightscout.shared.utils.DateUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusLightHandler @Inject constructor(
    private val rh: ResourceHelper,
    private val sp: SP,
    private val dateUtil: DateUtil,
    private val activePlugin: ActivePlugin,
    private val warnColors: WarnColors,
    private val config: Config,
    private val repository: AppRepository
) {

    /**
     * applies the extended statusLight subview on the overview fragment
     */
    fun updateStatusLights(
        careportal_cannula_age: TextView?,
        careportal_insulin_age: TextView?,
        careportal_reservoir_level: TextView?,
        careportal_sensor_age: TextView?,
        careportal_sensor_battery_level: TextView?,
        careportal_pb_age: TextView?,
        careportal_battery_level: TextView?
    ) {
        val pump = activePlugin.activePump
        val bgSource = activePlugin.activeBgSource
        handleAge(careportal_cannula_age, TherapyEvent.Type.CANNULA_CHANGE, R.string.key_statuslights_cage_warning, 48.0, R.string.key_statuslights_cage_critical, 72.0)
        handleAge(careportal_insulin_age, TherapyEvent.Type.INSULIN_CHANGE, R.string.key_statuslights_iage_warning, 72.0, R.string.key_statuslights_iage_critical, 144.0)
        handleAge(careportal_sensor_age, TherapyEvent.Type.SENSOR_CHANGE, R.string.key_statuslights_sage_warning, 216.0, R.string.key_statuslights_sage_critical, 240.0)
        if (pump.pumpDescription.isBatteryReplaceable || pump.isBatteryChangeLoggingEnabled()) {
            handleAge(careportal_pb_age, TherapyEvent.Type.PUMP_BATTERY_CHANGE, R.string.key_statuslights_bage_warning, 216.0, R.string.key_statuslights_bage_critical, 240.0)
        }

        val insulinUnit = rh.gs(R.string.insulin_unit_shortname)
        if (pump.pumpDescription.isPatchPump)
            handlePatchReservoirLevel(
                careportal_reservoir_level,
                R.string.key_statuslights_res_critical,
                10.0,
                R.string.key_statuslights_res_warning,
                80.0,
                pump.reservoirLevel,
                insulinUnit,
                pump.pumpDescription.maxResorvoirReading.toDouble()
            )
        else
            handleLevel(careportal_reservoir_level, R.string.key_statuslights_res_critical, 10.0, R.string.key_statuslights_res_warning, 80.0, pump.reservoirLevel, insulinUnit)

        if (!config.NSCLIENT) {
            if (bgSource.sensorBatteryLevel != -1)
                handleLevel(careportal_sensor_battery_level, R.string.key_statuslights_sbat_critical, 5.0, R.string.key_statuslights_sbat_warning, 20.0, bgSource.sensorBatteryLevel.toDouble(), "%")
            else
                careportal_sensor_battery_level?.text = ""
        }

        if (!config.NSCLIENT) {
            // The Omnipod Eros does not report its battery level. However, some RileyLink alternatives do.
            // Depending on the user's configuration, we will either show the battery level reported by the RileyLink or "n/a"
            // Pump instance check is needed because at startup, the pump can still be VirtualPumpPlugin and that will cause a crash
            val erosBatteryLinkAvailable = pump.model() == PumpType.OMNIPOD_EROS && pump.isUseRileyLinkBatteryLevel()

            if (pump.model().supportBatteryLevel || erosBatteryLinkAvailable) {
                handleLevel(careportal_battery_level, R.string.key_statuslights_bat_critical, 26.0, R.string.key_statuslights_bat_warning, 51.0, pump.batteryLevel.toDouble(), "%")
            } else {
                careportal_battery_level?.text = rh.gs(R.string.value_unavailable_short)
                careportal_battery_level?.setTextColor(rh.gac(careportal_battery_level.context, R.attr.defaultTextColor))
            }
        }
    }

    private fun handleAge(view: TextView?, type: TherapyEvent.Type, @StringRes warnSettings: Int, defaultWarnThreshold: Double, @StringRes urgentSettings: Int, defaultUrgentThreshold: Double) {
        val warn = sp.getDouble(warnSettings, defaultWarnThreshold)
        val urgent = sp.getDouble(urgentSettings, defaultUrgentThreshold)
        val therapyEvent = repository.getLastTherapyRecordUpToNow(type).blockingGet()
        if (therapyEvent is ValueWrapper.Existing) {
            warnColors.setColorByAge(view, therapyEvent.value, warn, urgent)
            view?.text = therapyEvent.value.age(rh.shortTextMode(), rh, dateUtil)
        } else {
            view?.text = if (rh.shortTextMode()) "-" else rh.gs(R.string.value_unavailable_short)
        }
    }

    private fun handleLevel(view: TextView?, criticalSetting: Int, criticalDefaultValue: Double, warnSetting: Int, warnDefaultValue: Double, level: Double, units: String) {
        val resUrgent = sp.getDouble(criticalSetting, criticalDefaultValue)
        val resWarn = sp.getDouble(warnSetting, warnDefaultValue)
        @Suppress("SetTextI18n")
        view?.text = " " + DecimalFormatter.to0Decimal(level) + units
        warnColors.setColorInverse(view, level, resWarn, resUrgent)
    }

    // Omnipod only reports reservoir level when it's 50 units or less, so we display "50+U" for any value > 50
    @Suppress("SameParameterValue")
    private fun handlePatchReservoirLevel(
        view: TextView?, criticalSetting: Int, criticalDefaultValue: Double, warnSetting: Int,
        warnDefaultValue: Double, level: Double, units: String, maxReading: Double
    ) {
        if (level >= maxReading) {
            @Suppress("SetTextI18n")
            view?.text = " ${maxReading.toInt()}+$units"
            view?.setTextColor(rh.gac(view.context, R.attr.defaultTextColor))
        } else {
            handleLevel(view, criticalSetting, criticalDefaultValue, warnSetting, warnDefaultValue, level, units)
        }
    }
}