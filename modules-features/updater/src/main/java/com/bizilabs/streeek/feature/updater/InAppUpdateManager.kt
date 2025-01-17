import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import timber.log.Timber

class InAppUpdateManager(
    private val activity: Activity,
) {
    companion object {
        private const val TAG = "InAppUpdateManager"
        private const val UPDATE_REQUEST_CODE = 100
    }

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)

    // Listener to monitor install state updates
    private val installStateUpdatedListener =
        InstallStateUpdatedListener { state ->
            handleInstallState(state)
        }

    init {
        // Register the listener when the manager is created
        appUpdateManager.registerListener(installStateUpdatedListener)
    }

    /**
     * Initiates the update flow based on availability and update type.
     */
    fun checkForUpdates() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            when {
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> {
                    log("Immediate update available. Starting update...")
                    startUpdateFlow(appUpdateInfo, AppUpdateType.IMMEDIATE)
                }

                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                    log("Flexible update available. Starting update...")
                    startUpdateFlow(appUpdateInfo, AppUpdateType.FLEXIBLE)
                }

                else -> {
                    log("No updates available.")
                }
            }
        }.addOnFailureListener { exception ->
            logError("Failed to check for updates: ${exception.localizedMessage}")
        }
    }

    /**
     * Completes the flexible update if required.
     */
    fun completeUpdateIfNeeded() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                log("Update downloaded. Completing the update...")
                appUpdateManager.completeUpdate()
            }
        }.addOnFailureListener { exception ->
            logError("Failed to complete the update: ${exception.localizedMessage}")
        }
    }

    /**
     * Unregisters the listener and cleans up resources.
     */
    fun cleanup() {
        appUpdateManager.unregisterListener(installStateUpdatedListener)
        log("Listener unregistered and resources cleaned up.")
    }

    /**
     * Starts the update flow with the specified update type.
     */
    private fun startUpdateFlow(
        appUpdateInfo: com.google.android.play.core.appupdate.AppUpdateInfo,
        updateType: Int,
    ) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.defaultOptions(updateType),
                UPDATE_REQUEST_CODE,
            )
        } catch (exception: Exception) {
            logError("Failed to start update (type: $updateType): ${exception.localizedMessage}")
        }
    }

    private fun handleInstallState(state: InstallState) {
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                log("Update has been downloaded. Prompting user to complete the update.")
                appUpdateManager.completeUpdate()
            }
            InstallStatus.INSTALLED -> log("Update installed successfully.")
            InstallStatus.FAILED -> logError("Update installation failed.")
            else -> log("Update state: ${state.installStatus()}")
        }
    }

    private fun log(message: String) {
        Timber.tag(TAG).d(message)
    }

    private fun logError(message: String) {
        Timber.tag(TAG).e(message)
    }
}
