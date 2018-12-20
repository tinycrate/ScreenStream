package info.dvkr.screenstream.receiver


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.elvishew.xlog.XLog
import info.dvkr.screenstream.data.other.getLog
import info.dvkr.screenstream.data.settings.SettingsReadOnly
import info.dvkr.screenstream.service.AppService
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val settingsReadOnly: SettingsReadOnly by inject()

    override fun onReceive(context: Context, intent: Intent) {
        XLog.d(getLog("onReceive", "Invoked"))

        if (settingsReadOnly.startOnBoot.not()) Runtime.getRuntime().exit(0)

        if (
            intent.action == "android.intent.action.BOOT_COMPLETED" ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            AppService.startForegroundService(context, AppService.IntentAction.StartOnBoot)
        }
    }
}