/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.validator;

/**
 *
 * @author Marcel 
 * @since 13-Oct-2016 11:30:37
 */
public enum ValidationStatusEnum {

    ILLEGAL_SYNC_FILE("illegal"), 
    VALID(""), 
    BFPHOBIA_SYNC_FILES("bfphobia"),
    INVALID_MSISDN("invalid_msisdn"),
    BLACK_LISTED_AGENT("blacklisted_agent"), 
    BLACK_LISTED_APP_VERSION("blackedlisted_app_version"),
    NULL_META_DATA("null_metadata"),
    NULL_ENROLMENT_LOG("null_enrolment_log"),
    INVALID_MSISDN_DETAIL("empty_msisdn_list"), 
    DUPLICATE_SIM_SERIAL("duplicate_sim_serial"),
    INVALID_SIM_SERIAL("invalid_sim_serial"),
    CLIENT_MAC_ADDRESS_MISMATCH("client_mac_address_mismatch"),
    CLIENT_DEVICE_ID_MISMATCH("client_device_id_mismatch"),
    BLACKLISTED_MAC_ADDRESS_OR_KIT("blacklisted_mac_address_or_kit"), 
    BLACKLISTED_DEVICE_ID("blacklisted_device_id"), 
    KIT_NOT_TAGGED("kit_not_tagged"),
    UNREGISTERED_MAC_ADDRESS("unregistered_mac_address"), 
    UNREGISTERED_DEVICE_ID("unregistered_device_id"), 
    NULL_MAC_ADDRESS("null_mac_address"), 
    NULL_DEVICE_ID("null_device_id"), 
    MAC_ADDRESS_ASSOCIATED_WITH_MULTIPLE_KITS("mac_address_and_multiple_kits"),
    MAC_ADDRESS_KIT_TAG_MISMATCH("mac_address_kit_tag_mismatch"),
    INVALID_REGISTRATION_TIME("invalid_reg_time"), 
    SIM_SERIAL_VERIFICATION_EXCEPTION("sim_serial_verification_error"),
    MSISDN_VERIFICATION_EXCEPTION("msisdn_verification_error"), 
    EXCEPTION("unidentified_error"),
    DIGEST_EXCEPTION("digest_error"), 
    DECRYPTION_EXCEPTION ("decryption_error"),
    PROCESSED("processed_syncs"), 
    NOT_SAVED("not_saved"),
    PROCESSED_RESYNCS("processed_resyncs"), 
    NULL_MSISDN_AND_SERIAL_NUMBER("null_msisdn_and_serial"),
    SIM_SWAP_EXCEPTION("sim_swap_error");

    private ValidationStatusEnum(String backupLocation) {
        this.backupLocation = backupLocation;
    }
    
    private String backupLocation;

    public String getBackupLocation() {
        return backupLocation;
    }
}
