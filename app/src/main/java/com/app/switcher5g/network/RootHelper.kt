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
        val modeId = when (mode) {
            NetworkMode.NR_ONLY -> 28 // NETWORK_MODE_NR_ONLY (5G SA)
            NetworkMode.NR_LTE -> 26  // NETWORK_MODE_NR_LTE_GSM_WCDMA (5G NSA)
            NetworkMode.LTE_ONLY -> 11 // NETWORK_MODE_LTE_ONLY (4G LTE)
        }

        val allowedMask = when (mode) {
            NetworkMode.NR_ONLY -> 524288L // BITMASK_NR (1L shl 19)
            NetworkMode.NR_LTE -> 528391L  // NR + LTE + UMTS + GSM
            NetworkMode.LTE_ONLY -> 4096L   // BITMASK_LTE (1L shl 12)
        }

        AppLogger.i("RootHelper", "Attempting Root switch to $mode (modeId=$modeId, mask=$allowedMask, subId=$subId)")

        // 1. Try cmd phone set-preferred-network-mode (-s subId modeId)
        val cmd1 = runSuCommand("cmd phone set-preferred-network-mode -s $subId $modeId")
        if (cmd1.isSuccess) {
            AppLogger.i("RootHelper", "Root switch succeeded with cmd phone set-preferred-network-mode -s $subId")
            return@withContext SwitchResult.Success("Applied ${mode.name} via Root (cmd phone)")
        }

        // 2. Try cmd phone set-allowed-network-types (Android 11+ preferred API)
        val cmdAllowed = runSuCommand("cmd phone set-allowed-network-types -s $subId -r 0 $allowedMask")
        if (cmdAllowed.isSuccess) {
            AppLogger.i("RootHelper", "Root switch succeeded with cmd phone set-allowed-network-types -s $subId")
            return@withContext SwitchResult.Success("Applied ${mode.name} via Root (allowed network types)")
        }

        val cmdAllowedDefault = runSuCommand("cmd phone set-allowed-network-types -r 0 $allowedMask")
        if (cmdAllowedDefault.isSuccess) {
            AppLogger.i("RootHelper", "Root switch succeeded with cmd phone set-allowed-network-types default")
            return@withContext SwitchResult.Success("Applied ${mode.name} via Root (allowed network types default)")
        }

        // 3. Try cmd phone set-preferred-network-mode (default sub)
        val cmd2 = runSuCommand("cmd phone set-preferred-network-mode $modeId")
        if (cmd2.isSuccess) {
            AppLogger.i("RootHelper", "Root switch succeeded with cmd phone set-preferred-network-mode default")
            return@withContext SwitchResult.Success("Applied ${mode.name} via Root (cmd phone default)")
        }

        // 4. Fallback: settings put global preferred_network_mode
        val cmd3 = runSuCommand("settings put global preferred_network_mode$subId $modeId && settings put global preferred_network_mode $modeId")
        if (cmd3.isSuccess) {
            AppLogger.i("RootHelper", "Root switch succeeded with settings put global")
            return@withContext SwitchResult.Success("Applied ${mode.name} via Root settings")
        }

        AppLogger.w("RootHelper", "Root switch commands failed. Err: ${cmd1.error}")
        SwitchResult.Failure("Root switch failed: ${cmd1.error.ifBlank { "Exit code ${cmd1.exitCode}" }}")
    }

    data class CommandResult(
        val exitCode: Int,
        val output: String,
        val error: String,
        val isSuccess: Boolean,
    )
}
