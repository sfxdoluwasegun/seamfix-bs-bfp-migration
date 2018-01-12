/**
 *
 */
package com.seamfix.kyc.bfp.validator;

import java.util.Map;

import com.seamfix.kyc.bfp.contract.IValidator;
import java.util.LinkedHashMap;

/**
 * @author dawuzi
 *
 * <p/>
 * this enum defines the order of applying the validators.
 *
 * <p/>
 * Recommended order
 * <p/>
 * <ul>
 *
 * <li>The group of validations (eg bad app version) which leads to the
 * rejection of the whole record should come first before those that can
 * partially reject msisdns records (eg Msisdn check). This is because there is
 * no need partially rejecting a record only for it to be fully rejected once it
 * fails a later applied validation
 *
 * <li>Once the previous condition is observed, validations that do not need to
 * hit the database (eg blacklisted sync marker) should come before those that
 * do (eg blacklisted agent). This is obviously for performance reasons
 *
 * </ul>
 *
 * <p/>
 * NB: These are just recommendations that can be modified based on any new
 * superior developments
 *
 */
public enum ValidatorOrder {

    
    FIELD_DECRYPTION(FieldDecryptionValidator.class),
    UNIQUE_ID(UniqueIdValidator.class),
    NULL_METADATA(NullMetadataValidator.class),
    NULL_ENROLMENT_REF(EnrolmentLogValidator.class),
    APP_VERSION(AppVersionValidator.class),
    DEVICE_ID_CHECK(DeviceIdValidator.class),
    WSQ_CHECK(WSQValidator.class),
    CLIENT_MAC_ADDRESS_MISMATCH(ClientMacAddressMismatchValidator.class),
    CLIENT_DEVICE_ID_MISMATCH(DeviceIdMismatchValidator.class),
    BLACKLISTED_KIT(BlacklistedKitValidator.class),
    MARKER_CHECK(MarkerValidator.class),
    TAGGED_KIT_CHECK(TaggedKitValidator.class),
    MAC_ADDRESS_CHECK(MacAddressValidator.class),
    BLACKLISTED_AGENT(BlacklistedAgentValidator.class),
    CAPTURE_TIME(CaptureTimeValidator.class),
    
    NULL_MSISDN_AND_SERIAL(NullMsisdnAndSerialValidator.class),
    NULL_ALLOWABLE_REGISTRATION(NullAllowableMsisdnRegistration.class),
    UNIQUE_SIM_SERIAL_CHECK(UniqueSimSerialValidator.class),
    SIM_SERIAL_CHECK(SimSerialValidator.class),
    MSISDN_CHECK(AuthenticMsisdnValidator.class)
    ;

    private final static Map<Class<? extends IValidator>, ValidatorOrder> mapping = new LinkedHashMap<>();
    private Class<? extends IValidator> validatorClass;

    private ValidatorOrder(Class<? extends IValidator> validatorClass) {
        this.validatorClass = validatorClass;
    }

    public Class<? extends IValidator> getValidatorClass() {
        return validatorClass;
    }

    static {
        for (ValidatorOrder order : ValidatorOrder.values()) {

            if (order.getValidatorClass() == null) {
                throw new IllegalStateException("no validator class specified for " + order);
            }

            ValidatorOrder prev = mapping.put(order.getValidatorClass(), order);
            if (prev != null) {
                throw new IllegalStateException("duplicate class : " + prev.getValidatorClass().getName());
            }
        }
    }

}
