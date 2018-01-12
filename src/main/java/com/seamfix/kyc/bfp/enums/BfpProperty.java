/**
 *
 */
package com.seamfix.kyc.bfp.enums;

/**
 * @author dawuzi
 *
 */
@SuppressWarnings("PMD")
public enum BfpProperty {

    NULL_MSISDN_VALUE("null-msisdn-value", "NOT_AVAILABLE", "default value for null msisdn registrations with sim serial"),
    MISSING_MSISDN_WORKER_SLEEP_TIME("missing-msisdn-worker-sleep-time", "600", "thread sleep time for the missing msisdn worker process in SECONDS"),
    MISSING_MSISDN_LOADER_SLEEP_TIME("missing-msisdn-loader-sleep-time", "3600", "thread sleep time for the missing msisdn loader process in SECONDS"),
    TELECOM_MASTER_WORKER_SLEEP_TIME("telecom-master-worker-sleep-time", "600", "thread sleep time for the telecom master worker process in SECONDS"),
    TELECOM_MASTER_LOADER_SLEEP_TIME("telecom-master-loader-sleep-time", "3600", "thread sleep time for the telecom master loader process in SECONDS"),
    CREATE_NON_EXISTING_TAG("create-non-existing-tag", "false", "this boolean property determines if a non exisiting enrollment ref/tag should be created"),
    SKIP_TAG_VALIDATION("skip-validate-tag-exists", "false", ""),
    SKIP_ALL_VALIDATION("skip-all-validation", "false", ""),
    SKIP_MAC_ADDRESS_KIT_TAG_MATCH_VALIDATION("skip-validate-mac-address-kit-tag-match", "false", ""),
    SKIP_DEVICE_ID_VALIDATION("skip-device-id-validation", "false", ""),
    SKIP_MARKER_BLACKLIST_VALIDATION("skip-validate-marker-blacklist", "false", ""),
    SKIP_MAC_ADDRESS_MISMATCH_VALIDATION("skip-validate-mac-address-mismatch", "false", ""),
    SKIP_BLACKLIST_STATUS_VALIDATION("skip-validate-blacklist-status", "false", ""),
    SKIP_APP_VERSION_VALIDATION("skip-validate-app-version", "false", ""),
    ANDROID_APP_VERSION("allowed-android-app-versions", "1.34 1.35 1.36 1.37", ""),
    WINDOWS_APP_VERSION("allowed-windows-app-versions", "2.0 2.1 2.11 2.12", ""),
    SKIP_BLACKLISTED_AGENT_VALIDATION("skip-validate-blacklisted-agent", "false", ""),
    SKIP_CAPTURE_TIME_VALIDATION("skip-validate-time-of-capture", "false", ""),
    ALLOW_NULL_MSISDNS("allow-null-msisdns-save", "true", ""),
    SKIP_SIM_SERIAL_VALIDATION("skip-validate-sim-serial", "false", ""),
    SKIP_MSISDN_VALIDATION("skip-validate-msisdn", "false", ""),
    SKIP_CLIENT_DECRYPTION("skip-client-decryption", "true", ""),
    SKIP_NULL_MSISDN_AND_SERIAL_VALIDATION("skip-validate-null-msisdn-serial", "false", ""),
    SKIP_UNIQUE_SIM_SERIAL_VALIDATION("skip-validate-unique-sim-serial", "false", ""),
    SKIP_SIM_SERIAL_ANDROID_APP_VERSION_VALIDATION("unique-sim-serial-skippable-android-app-versions", "1.34", ""),
    SKIP_SIM_SERIAL_WINDOWS_APP_VERSION_VALIDATION("unique-sim-serial-skippable-windows-app-versions", "2.0", ""),
    MAC_ADDRESS_CHECK_CACHE_TIMEOUT("mac-address-check-cache-time-out", "20", ""),
    RESET_VALID_REGISTRATION_CACHE_TIMEOUT("reset-valid-registration-time-cache-timeout", "60", ""),
    CREATE_VALIDATION_RECORD("create-validation-record", "true", "flag to determine whether to create validation record for backend validation"),
    SIM_SWAP_RETRIAL_PERIOD("sim-swap-retrial-period", "20", "time in mins allowed for sim swap sync file to remain in sftp before removing from sync files folder"),
    MSISDN_VERIFICATION_RETRIAL_PERIOD("msisdn-verification-retrial-period", "3600", "Measured in seconds"),
    DELAY_BEFORE_INCOMPLETE_FTP_FILE_DELETE("delay-before-incomplete-file-delete", "300", "Measured in seconds"),
    INCLUDE_DATE_TO_FOLDER_PATH("include-date-to-folder-path", "true", ""),
    DEPRECATED_MAC_ADDRESS_APP_VERSION_WINDOWS("deprecated-mac-address-app-version-windows", "2.3", "The version number when we stopped using mac address as the unique identifier for windows devices"),
    DEPRECATED_MAC_ADDRESS_APP_VERSION_ANDROID("deprecated-mac-address-app-version-android", "1.41", "The version number when we stopped using mac address as the unique identifier for android devices"),
    SKIP_DEVICE_ID_MISMATCH_VALIDATION("skip-validate-mac-address-mismatch", "false", ""),
    SKIP_CUSTOMER_INFORMATION_UPDATE("skip-customer-information-update", "false", "");

    private String key;
    private String defaultValue;
    private String description;

    private BfpProperty(String key, String defaultValue, String description) {
        this.key = key.trim();
        this.defaultValue = defaultValue.trim();
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

}
