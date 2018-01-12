/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.validator;

import com.seamfix.kyc.bfp.contract.IValidator;
import com.seamfix.kyc.bfp.contract.SyncItem;
import com.seamfix.kyc.bfp.enums.BfpProperty;
import com.seamfix.kyc.bfp.enums.ProxyKeyEnum;
import com.seamfix.kyc.bfp.exception.EncryptionException;
import com.seamfix.kyc.bfp.proxy.BasicData;
import com.seamfix.kyc.bfp.proxy.EnrollmentLog;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.service.AppDS;
import com.seamfix.kyc.bfp.syncs.DesEncrypter;
import com.seamfix.kyc.bfp.syncs.SyncFile;
import com.sf.biocapture.entity.EnrollmentRef;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Marcel
 * @since 31-Oct-2016, 21:17:27
 */
public class FieldDecryptionValidator extends AbstractSyncFileValidator implements IValidator {

    private boolean prerequisite;
    private boolean skip;
    @SuppressWarnings("unused")
    private AppDS appDS;
    private DesEncrypter desEncrypter = new DesEncrypter();
    @SuppressWarnings("unused")
    private ValidatorContext validatorContext;

    public FieldDecryptionValidator() {
        defineConditions();
    }

    private void defineConditions() {
        skip = getBool(BfpProperty.SKIP_CLIENT_DECRYPTION);
    }

    @Override
    public SyncItem validate(SyncItem syncItem) {
        if (syncItem == null) {
            logger.debug(validatorContext.getWorkerId() + " sync item is null");
            return syncItem;
        }
        SyncFile syncFile = (SyncFile) syncItem;
        if (skip()) {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
            return syncFile;
        }

        Object enrollmentLog = syncFile.getProxyItem(ProxyKeyEnum.EL);
        if (enrollmentLog != null) {
            EnrollmentLog log = (EnrollmentLog) enrollmentLog;
            EnrollmentRef ref = log.getEnrollmentRef();
            if (ref != null) {
                try {
                    //attempt decrypting enrollment reference before checking if entry is droid or windows
                    desEncrypter.decryptFields(ref);
                } catch (EncryptionException ex) {
                    logger.error(validatorContext.getWorkerId() + " ", ex);
                }
                if (ref.getCode().toLowerCase().contains("droid")) {
                    //skip decryption check for files coming from android devices
                    syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
                    return syncFile;
                }
            }
        }

        ProxyKeyEnum pke[] = {ProxyKeyEnum.BD, ProxyKeyEnum.DD, ProxyKeyEnum.META, ProxyKeyEnum.USER_ID, ProxyKeyEnum.PD};
        for (ProxyKeyEnum keyEnum : pke) {
            try {
                Object object = syncFile.getProxyItem(keyEnum);
                if (object != null) {
                    desEncrypter.decryptFields(object);
                }
            } catch (EncryptionException ex) {
                logger.error(validatorContext.getWorkerId(), ex);
            }
        }
        List<MsisdnDetail> msisdnDetails = (List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS);
        if (msisdnDetails != null) {
            for (MsisdnDetail md : msisdnDetails) {
                try {
                    desEncrypter.decryptFields(md);
                } catch (EncryptionException ex) {
                    logger.error(validatorContext.getWorkerId() + " ", ex);
                }
            }
        }
        syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
        return syncFile;
    }

    @Override
    public boolean isPrerequisite() {
        return prerequisite;
    }

    @Override
    public boolean skip() {
        return skip;
    }

    @Override
    public void init(ValidatorContext validatorContext) {
        this.validatorContext = validatorContext;
        this.appDS = validatorContext.getAppDS();
    }

    public static void main(String arg[]) {
        try {
            FieldDecryptionValidator decryptionValidator = new FieldDecryptionValidator();
//        DynamicData data = new DynamicData();
            BasicData basicData = new BasicData();
            basicData.setBiometricCaptureAgent("RFNCeSwTjwHCxQJEIE+GALCMW4h5lfZRtDYHWd1RsACcdYkDYPMAy2OcKIkjNv5+4cnQiH8Q8ClJw96Q1OBuTv9K1rBf42ynhlhsu1lviYapZGeCHcISRpdHiT4vX+fd4iS3pSn3/MlLbTHiklrfZMYax43VK49C3xsTr188mkfdcU8jJOFYu1YMrdHbI9gwUzEneMw7s9q6sHioOMeEQhpo7cUBs2ZwczYJ+eAHmSvF7dBI5xXXGWdJ9CaF9n82");
            decryptionValidator.desEncrypter.decryptFields(basicData);
            System.out.println("capture agent: " + basicData.getBiometricCaptureAgent());
        } catch (EncryptionException ex) {
            Logger.getLogger(FieldDecryptionValidator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
