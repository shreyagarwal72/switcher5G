package com.app.switcher5g.network

import com.app.switcher5g.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Native Root Shell (`su`) Facility for Rooted Android Devices.
 * Enables root checking, root permission requests, and execution of privileged `cmd phone`
 * network mode switches without requiring Shizuku daemon.
 */
object RootHelper {

    /**
     * Checks if the `su` binary exists on the system PATH or standard binary locations.
     */
    fun isRootAvailable(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return runSuCommand("which su").isSuccess
    }

    /**
     * Checks if root permission (`su`) has been granted to the app.
     */
    suspend fun isRootGranted(): Boolean = withContext(Dispatchers.IO) {
        val result = runSuCommand("id")
        result.isSuccess && result.output.contains("uid=0")
    }

    /**
     * Prompts the SuperUser manager (Magisk / KernelSU / APatch / SuperSU) for root access.
     */
    suspend fun requestRootAccess(): Boolean = withContext(Dispatchers.IO) {
        AppLogger.i("RootHelper", "Requesting Root su permission…")
        val result = runSuCommand("id")
        val granted = result.isSuccess && result.output.contains("uid=0")
        AppLogger.i("RootHelper", "Root access granted: $granted")
        granted
    }

    /**
     * Executes a command via root shell (`su -c <command>`).
     */
    fun runSuCommand(command: String): CommandResult {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            CommandResult(
                exitCode = exitCode,
                output = output.trim(),
                error = error.trim(),
                isSuccess = exitCode == 0,
            )
        } catch (t: Throwable) {
            CommandResult(
                exitCode = -1,
                output = "",
                error = t.localizedMessage ?: "Failed to execute su",
                isSuccess = false,
            )
        }
    }

    /**
     * Executes network mode switch via root `cmd phone` or `settings put` APIs.
     */
    suspend fun switchNetworkMode(mode: NetworkMode, subId: Int): SwitchResult = withContext(Dispatchers.IO) {
        val modeIds = when (mode) {
            NetworkMode.NR_ONLY -> listOf(28) // NETWORK_MODE_NR_ONLY (5G SA)
            NetworkMode.NR_LTE -> listOf(27, 26, 24) // 5G NSA Global (NR+LTE+CDMA+GSM+WCDMA), NR+LTE+GSM+WCDMA, NR+LTE
            NetworkMode.LTE_ONLY -> listOf(11, 10, 9) // 4G LTE Only, 4G Global, LTE+GSM+WCDMA
        }

        // Full global legacy network bitmask covering all 2G/3G/4G standards
        // NR (1L shl 19 = 524288L) | LTE (4096L) | LTE_CA (262144L) | UMTS/GSM/CDMA/EvDo/TDSCDMA etc.
        val allowedMask = when (mode) {
            NetworkMode.NR_ONLY -> 524288L // BITMASK_NR (1L shl 19)
            NetworkMode.NR_LTE -> 524288L or 262144L or 65536L or 32768L or 16384L or 4096L or 2048L or 512L or 256L or 128L or 64L or 32L or 16L or 8L or 4L or 2L or 1L // NR + ALL LEGACY
            NetworkMode.LTE_ONLY -> 4096L or 262144L // BITMASK_LTE | BITMASK_LTE_CA
        }

        AppLogger.i("RootHelper", "Attempting Root switch to $mode (modeIds=$modeIds, mask=$allowedMask, subId=$subId)")

        // 1. Try cmd phone set-preferred-network-mode (-s subId modeId)
        for (mId in modeIds) {
            val cmd1 = runSuCommand("cmd phone set-preferred-network-mode -s $subId $mId")
            if (cmd1.isSuccess) {
                AppLogger.i("RootHelper", "Root switch succeeded with cmd phone set-preferred-network-mode -s $subId $mId")
                return@withContext SwitchResult.Success("Applied ${mode.name} via Root (cmd phone)")
            }
        }

        // 2. Try cmd phone set-allowed-network-types across REASON_USER (0) and REASON_CARRIER (2)
        for (reason in intArrayOf(0, 2)) {
            val cmdAllowed = runSuCommand("cmd phone set-allowed-network-types -s $subId -r $reason $allowedMask")
            if (cmdAllowed.isSuccess) {
                AppLogger.i("RootHelper", "Root switch succeeded with cmd phone set-allowed-network-types -s $subId -r $reason")
                return@withContext SwitchResult.Success("Applied ${mode.name} via Root (allowed network types reason=$reason)")
            }

            val cmdAllowedDefault = runSuCommand("cmd phone set-allowed-network-types -r $reason $allowedMask")
            if (cmdAllowedDefault.isSuccess) {
                AppLogger.i("RootHelper", "Root switch succeeded with cmd phone set-allowed-network-types default -r $reason")
                return@withContext SwitchResult.Success("Applied ${mode.name} via Root (allowed network types default reason=$reason)")
            }
        }

        // 3. Try cmd phone set-preferred-network-mode (default sub)
        for (mId in modeIds) {
            val cmd2 = runSuCommand("cmd phone set-preferred-network-mode $mId")
            if (cmd2.isSuccess) {
                AppLogger.i("RootHelper", "Root switch succeeded with cmd phone set-preferred-network-mode default $mId")
                return@withContext SwitchResult.Success("Applied ${mode.name} via Root (cmd phone default)")
            }
        }

        // 4. Try service call phone (legacy Android root service call)
        for (mId in modeIds) {
            val cmdServiceCall = runSuCommand("service call phone 143 i32 $subId i32 $mId")
            if (cmdServiceCall.isSuccess && !cmdServiceCall.output.contains("Parcel")) {
                AppLogger.i("RootHelper", "Root switch succeeded with service call phone $mId")
                return@withContext SwitchResult.Success("Applied ${mode.name} via Root (service call)")
            }
        }

        // 5. Fallback: settings put global preferred_network_mode
        val primaryModeId = modeIds.first()
        val cmd3 = runSuCommand("settings put global preferred_network_mode$subId $primaryModeId && settings put global preferred_network_mode $primaryModeId")
        if (cmd3.isSuccess) {
            AppLogger.i("RootHelper", "Root switch succeeded with settings put global $primaryModeId")
            return@withContext SwitchResult.Success("Applied ${mode.name} via Root settings")
        }

        AppLogger.w("RootHelper", "Root switch commands failed.")
        SwitchResult.Failure("Root switch failed across all candidate methods.")
    }

    data class CommandResult(
        val exitCode: Int,
        val output: String,
        val error: String,
        val isSuccess: Boolean,
    )
}
