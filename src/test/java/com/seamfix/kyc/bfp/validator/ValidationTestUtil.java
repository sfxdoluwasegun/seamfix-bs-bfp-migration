/**
 *
 */
package com.seamfix.kyc.bfp.validator;

import java.io.File;
import java.util.ArrayList;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.seamfix.kyc.bfp.BioCache;
import com.seamfix.kyc.bfp.BsClazz;
import com.seamfix.kyc.bfp.enums.ProxyKeyEnum;
import com.seamfix.kyc.bfp.proxy.BasicData;
import com.seamfix.kyc.bfp.proxy.DynamicData;
import com.seamfix.kyc.bfp.proxy.EnrollmentLog;
import com.seamfix.kyc.bfp.proxy.FileSyncNewEntryMarker;
import com.seamfix.kyc.bfp.proxy.MetaData;
import com.seamfix.kyc.bfp.proxy.Passport;
import com.seamfix.kyc.bfp.proxy.PassportDetail;
import com.seamfix.kyc.bfp.proxy.Signature;
import com.seamfix.kyc.bfp.queue.JmsQueue;
import com.seamfix.kyc.bfp.service.AppDS;
import com.seamfix.kyc.bfp.syncs.SyncFile;
import com.seamfix.kyc.bfp.syncs.SyncUtils;
import com.seamfix.kyc.bfp.syncs.SyncWorker;
import com.sf.biocapture.entity.EnrollmentRef;
import com.sf.biocapture.entity.UserId;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.keyczar.Crypter;
import org.keyczar.exceptions.KeyczarException;
import sfx.crypto.CryptoReader;

/**
 * @author dawuzi
 *
 */
public class ValidationTestUtil extends BsClazz {

    public static SyncFile getTestSyncFile() {

        SyncFile syncFile = new SyncFile();

        syncFile.setFile(new File("BIO-!3eb0RUgpFCHqUV1GWJ3QA==.sv2.sync"));

        MetaData metaData = new MetaData();
        syncFile.addProxyItem(ProxyKeyEnum.META, metaData);

        EnrollmentLog enrollmentLog = new EnrollmentLog();

        enrollmentLog.setEnrollmentRef(new EnrollmentRef());

        syncFile.addProxyItem(ProxyKeyEnum.EL, enrollmentLog);

        FileSyncNewEntryMarker marker = new FileSyncNewEntryMarker();
        syncFile.addProxyItem(ProxyKeyEnum.SYNC_MARKER, marker);

        UserId userId = new UserId();
        syncFile.addProxyItem(ProxyKeyEnum.USER_ID, userId);

        BasicData basicData = new BasicData();
        syncFile.addProxyItem(ProxyKeyEnum.BD, basicData);

        DynamicData dynamicData = new DynamicData();
        syncFile.addProxyItem(ProxyKeyEnum.DD, dynamicData);

        Passport passport = new Passport();
        syncFile.addProxyItem(ProxyKeyEnum.PP, passport);

        PassportDetail passportDetail = new PassportDetail();
        syncFile.addProxyItem(ProxyKeyEnum.PD, passportDetail);

        Signature signature = new Signature();
        syncFile.addProxyItem(ProxyKeyEnum.SIGN, signature);

        syncFile.addProxyItem(ProxyKeyEnum.WSQS, new ArrayList<>());
        syncFile.addProxyItem(ProxyKeyEnum.MSISDNS, new ArrayList<>());
        syncFile.addProxyItem(ProxyKeyEnum.SPECIALS, new ArrayList<>());

        return syncFile;
    }

    public static ValidatorContext getTestValidatorContext() {

        try {
            BioCache bioCache = Mockito.mock(BioCache.class);
            JmsQueue jmsQueue = Mockito.mock(JmsQueue.class);
            AppDS appDS = Mockito.mock(AppDS.class, new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    if (invocation.getMethod().getName().equals("getSettingValue")) {
                        return "6:30";
                    }
                    // Delegate to the default answer.
                    return Mockito.RETURNS_DEFAULTS.answer(invocation);
                }
            });

            ValidatorContext context = new ValidatorContext();

            context.setAppDS(appDS);
            context.setBioCache(bioCache);

            SyncWorker syncWorker = new SyncWorker(appDS, bioCache, jmsQueue);
            Crypter kycCrypter = new Crypter(new CryptoReader("map"));
            syncWorker.setCrypto(kycCrypter);
            File[] files = new File("samplesync").listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith("sync");
                }
            });
            PrintWriter pw = null;
            try {
                File logFile = new File("bfp.log");
                FileWriter fw = new FileWriter(logFile);
                pw = new PrintWriter(fw);
                for (File file : files) {
                    SyncFile syncFile = new SyncFile();
                    syncFile.setFile(file);
                    syncWorker.deserialize(syncFile);
                    syncWorker.validate(syncFile);
                    SyncUtils syncUtils = new SyncUtils();
                    String result = "";
                    for (ProxyKeyEnum key : syncFile.getProxyItems().keySet()) {
                        if (key.equals(ProxyKeyEnum.PP) | key.equals(ProxyKeyEnum.WSQS) | key.equals(ProxyKeyEnum.SPECIALS)) {
                            System.out.println(key + " was excluded.");
                            continue;
                        }
                        result += "\n" + key.name() + "    " + syncUtils.log(syncFile.getProxyItem(key));
                    }
//                    System.out.println(result);
                    pw.println(result);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (pw != null) {
                    pw.close();
                }
            }
            return context;
        } catch (KeyczarException ex) {
            Logger.getLogger(ValidationTestUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
