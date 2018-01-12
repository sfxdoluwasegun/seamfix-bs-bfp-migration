/**
 *
 */
package com.seamfix.kyc.bfp.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import com.seamfix.kyc.bfp.enums.ProxyKeyEnum;
import com.seamfix.kyc.bfp.proxy.EnrollmentLog;
import com.seamfix.kyc.bfp.proxy.MetaData;
import com.seamfix.kyc.bfp.syncs.SyncFile;

/**
 * @author dawuzi
 *
 */
public class TestValidation {

    @Test
    public void testValidatorList() {

        ValidatorContext validatorContext = ValidationTestUtil.getTestValidatorContext();

//		some internal checks has been coded in this class's constructor. If this instantiation is possible, it means they passed
        new Validators(validatorContext);
    }

    @Test
    public void testAppVersionValidator() {

        SyncFile syncFile = ValidationTestUtil.getTestSyncFile();
        ValidatorContext validatorContext = ValidationTestUtil.getTestValidatorContext();
        

        String droidKit = "DROIDT0005885AG05";
        String nonDroidKit = "KYCNCC1052566";

        MetaData metaData = (MetaData) syncFile.getProxyItem(ProxyKeyEnum.META);
        EnrollmentLog enrollmentLog = (EnrollmentLog) syncFile.getProxyItem(ProxyKeyEnum.EL);

        AppVersionValidator appVersionValidator = new AppVersionValidator();
        
        appVersionValidator.init(validatorContext);

        appVersionValidator.setAllowedDroidVersions("1.34 1.35 1.36 1.37");
        appVersionValidator.setAllowedWindowsVersion("2.0 2.1 1.0 1.8 2.11 2.11 2.12");

        enrollmentLog.getEnrollmentRef().setCode(nonDroidKit);
        metaData.setAppVersion("2.0");
        appVersionValidator.validate(syncFile);
        assertEquals("FIRST TEST", ValidationStatusEnum.VALID, syncFile.getValidationStatusEnum());

        enrollmentLog.getEnrollmentRef().setCode(nonDroidKit);
        metaData.setAppVersion("3.0");
        appVersionValidator.validate(syncFile);
        assertNotEquals("SECOND TEST", ValidationStatusEnum.VALID, syncFile.getValidationStatusEnum());

        enrollmentLog.getEnrollmentRef().setCode(droidKit);
        metaData.setAppVersion("1.34");
        appVersionValidator.validate(syncFile);
        assertEquals("THIRD TEST", ValidationStatusEnum.VALID, syncFile.getValidationStatusEnum());

        enrollmentLog.getEnrollmentRef().setCode(nonDroidKit);
        metaData.setAppVersion("1.38");
        appVersionValidator.validate(syncFile);
        assertNotEquals("FOURTH TEST", ValidationStatusEnum.VALID, syncFile.getValidationStatusEnum());

    }
}
