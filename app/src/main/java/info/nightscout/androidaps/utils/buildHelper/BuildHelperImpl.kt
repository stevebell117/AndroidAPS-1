package info.nightscout.androidaps.utils.buildHelper

import info.nightscout.androidaps.BuildConfig
import info.nightscout.androidaps.interfaces.Config
import info.nightscout.androidaps.plugins.general.maintenance.PrefFileListProvider
import java.io.File

class BuildHelperImpl constructor(
    private val config: Config,
    fileListProvider: PrefFileListProvider
) : BuildHelper {

    private var devBranch = false
    private var engineeringMode = false

    init {
        engineeringMode = true
        devBranch = BuildConfig.VERSION.contains("-") || BuildConfig.VERSION.matches(Regex(".*[a-zA-Z]+.*"))
    }

    override fun isEngineeringModeOrRelease(): Boolean =
        if (!config.APS) true else engineeringMode || !devBranch

    override fun isEngineeringMode(): Boolean = engineeringMode

    override fun isDev(): Boolean = devBranch
}