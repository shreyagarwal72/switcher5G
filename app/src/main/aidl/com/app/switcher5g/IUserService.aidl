package com.app.switcher5g;

// Runs inside a separate process spawned by Shizuku, executing as the `shell`
// UID. That's what lets setAllowedNetworkTypesForReason() pass its permission
// check without our app holding MODIFY_PHONE_STATE itself.
interface IUserService {
    // mode is one of: "NR_ONLY", "NR_LTE", "LTE_ONLY"
    // Returns a human-readable result string; check for a "OK:" prefix for success.
    String setNetworkMode(int subId, String mode);

    // subId of the default data SIM, or -1 if it couldn't be determined.
    int getDefaultDataSubId();

    // Returns array of active subscription IDs detected on the device.
    int[] getAvailableSubIds();

    void destroy();
}
