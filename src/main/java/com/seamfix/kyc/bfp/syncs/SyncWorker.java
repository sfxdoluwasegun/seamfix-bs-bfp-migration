package com.seamfix.kyc.bfp.syncs;

import com.seamfix.kyc.bfp.validator.ValidationStatusEnum;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import javax.inject.Inject;

import org.keyczar.Crypter;
import org.keyczar.exceptions.KeyczarException;

import com.seamfix.kyc.bfp.BioCache;
import com.seamfix.kyc.bfp.BsClazz;
import com.seamfix.kyc.bfp.contract.IBlockingBuffer;
import com.seamfix.kyc.bfp.contract.IValidator;
import com.seamfix.kyc.bfp.contract.IWorker;
import com.seamfix.kyc.bfp.contract.SyncItem;
import com.seamfix.kyc.bfp.enums.BfpProperty;
import com.seamfix.kyc.bfp.enums.BfpRejectionReason;
import com.seamfix.kyc.bfp.enums.ProxyKeyEnum;
import com.seamfix.kyc.bfp.enums.RegistrationUsecaseEnum;
import com.seamfix.kyc.bfp.enums.UsecaseEnum;
import com.seamfix.kyc.bfp.exception.DigestException;
import com.seamfix.kyc.bfp.exception.EncryptionException;
import com.seamfix.kyc.bfp.exception.InvalidSyncFileNameException;
import com.seamfix.kyc.bfp.proxy.BasicData;
import com.seamfix.kyc.bfp.proxy.DynamicData;
import com.seamfix.kyc.bfp.proxy.EnrollmentLog;
import com.seamfix.kyc.bfp.proxy.FileSyncNewEntryMarker;
import com.seamfix.kyc.bfp.proxy.MetaData;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.proxy.Passport;
import com.seamfix.kyc.bfp.proxy.PassportDetail;
import com.seamfix.kyc.bfp.proxy.RegistrationSignature;
import com.seamfix.kyc.bfp.proxy.Signature;
import com.seamfix.kyc.bfp.proxy.SpecialData;
import com.seamfix.kyc.bfp.proxy.WsqImage;
import com.seamfix.kyc.bfp.queue.JmsQueue;
import com.seamfix.kyc.bfp.service.AppDS;
import com.seamfix.kyc.bfp.syncs.BiocaptureObjectInputStream.BiocaptureStreaException;
import com.seamfix.kyc.bfp.syncs.BiocaptureObjectInputStream.DummyObject;
import com.seamfix.kyc.bfp.validator.ValidatorContext;
import com.seamfix.kyc.bfp.validator.Validators;
import com.sf.biocapture.entity.UserId;
import com.sf.biocapture.entity.audit.BfpFailureLog;
import com.sf.biocapture.entity.audit.BfpSyncLog;
import java.security.NoSuchAlgorithmException;

/**
 * Handles sync file processing actions
 *
 * @author Ogwara O. Rowland
 * @author dawuzi
 * @author Marcel Ugwu
 */
public class SyncWorker extends BsClazz implements IWorker {

    /**
     * The following flags are used for the various reg type. Those ending with
     * I is for the individual and those ending with C for company
     *
     * New Registration (Sim Serial) NSI NSC Biometric Update BUI BUC Additional
     * Registration ARI ARC Re-Registration RRI RRC New Registration (MSISDN)
     * NMI NMC
     *
     */
    private Crypter crypto;
    private Base64Coder base64Coder;

    /**
     * Buffer for all unprocessed records
     */
    private IBlockingBuffer<SyncItem> buffer;
    protected String queueKey = "BFPT3YXW";

    /**
     * Cache for most recently processed records
     */
    protected BioCache cache;

    protected AppDS appDs;
    private JmsQueue jmsQueue;

    protected String workerId;
    protected Integer cacheTimeout;
    /**
     * an engine that controls validation processes
     */
    private Validators validators;
    /**
     * defines prerequisite values used by validation engine
     */
    private ValidatorContext validatorContext;
    /**
     * determines whether all validation should be skipped
     */
    private boolean skippAllValidation;

    public SyncWorker(AppDS appDs, BioCache cache, JmsQueue jmsQueue) {
        this.cache = cache;
        this.appDs = appDs;
        this.jmsQueue = jmsQueue;
        init();
    }

    @Inject
    public SyncWorker() {
        init();
    }

    public SyncWorker(BioCache cache) {
        this.cache = cache;
        init();
    }

    private void init() {
        this.workerId = UUID.randomUUID().toString();
        validatorContext = new ValidatorContext();
        validatorContext.setAppDS(appDs);
        validatorContext.setBioCache(cache);
        validatorContext.setWorkerId(workerId);
        validators = new Validators(validatorContext);
        base64Coder = new Base64Coder();
        cacheTimeout = appProps.getInt("sync-cache-timeout", 300);

        skippAllValidation = getBool(BfpProperty.SKIP_ALL_VALIDATION);
    }

    @Override
    public SyncItem getSyncItem() {
        return buffer.get();
    }

    @Override
    public SyncItem process(SyncItem syncItem) {
        SyncFile syncFile = (SyncFile) syncItem;
        Map<ProxyKeyEnum, Object> items = syncFile.getProxyItems();
        if (appDs.saveRecord(items)) {
            UserId userId = (UserId) syncFile.getProxyItem(ProxyKeyEnum.USER_ID);
            wrapUpSaveRecord(userId, syncItem, items);
            syncFile.setValidationStatusEnum(ValidationStatusEnum.PROCESSED);

            BfpSyncLog bfpSyncLog = new BfpSyncLog();
            bfpSyncLog.setUniqueId(userId.getUniqueId());
            bfpSyncLog.setFileName(syncFile.getFile().getName());
            bfpSyncLog.setSourcePath(syncFile.getFile().getAbsolutePath());
            syncFile.addBfpSyncLog(bfpSyncLog);

            boolean skipCustomerInfoUpdate = getBool(BfpProperty.SKIP_CUSTOMER_INFORMATION_UPDATE);
            if (!skipCustomerInfoUpdate) {
                DynamicData dynamicData = (DynamicData) syncFile.getProxyItem(ProxyKeyEnum.DD);
                String regType = dynamicData.getDda11();
                if (regType != null) {
                    if (isRereg(regType)) {
                        List<MsisdnDetail> msisdns = (List<MsisdnDetail>) items.get(ProxyKeyEnum.MSISDNS);
                        if (!msisdns.isEmpty()) {
                            msisdns.stream().forEach((msisdn) -> {
                                jmsQueue.queueSubscriberMsisdn(msisdn.getMsisdn());
                            });
                        }
                    }
                }
            }

            boolean callActivationService = getBool(BfpProperty.MOCK_ACTIVATION_SERVICE);
            if(callActivationService){
                List<MsisdnDetail> msisdns = (List<MsisdnDetail>) items.get(ProxyKeyEnum.MSISDNS);
                    if (!msisdns.isEmpty()) {
                        msisdns.stream().forEach((msisdn) -> {
                            UsecaseEnum ue = msisdn.getMsisdn() == null || msisdn.getMsisdn().isEmpty() ? UsecaseEnum.NS : UsecaseEnum.NM;
                            String subscriberInfo = msisdn.getMsisdn();
                            switch (ue) {
                                case NM:
                                    subscriberInfo = msisdn.getMsisdn();
                                    break;
                                case NS:
                                    subscriberInfo = msisdn.getSerial();
                                    break;
                            }
                            MockActivationRequest mar = new MockActivationRequest();
                            mar.setUsecase(ue.name());
                            mar.setMsisdn(subscriberInfo);
                            mar.setUniqueId(userId.getUniqueId());
                            jmsQueue.queuemockActivation(mar);
                        });
                    }
            }
        } else {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.NOT_SAVED);
        }
        return syncFile;
    }

    private boolean isRereg(String regType) {
        if (regType != null) {
            if (RegistrationUsecaseEnum.RRI.name().equalsIgnoreCase(regType) || RegistrationUsecaseEnum.RRC.name().equalsIgnoreCase(regType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void postProcessing(SyncItem item) {

    }

    @Override
    public void setBuffer(IBlockingBuffer<SyncItem> buffer) {
        this.buffer = buffer;
    }

    @Override
    public boolean wasProcessed(String itemId) {
        return cache.getItem(queueKey + itemId) != null;
    }

    protected String getExtension(File file) {
        String[] nameItems = file.getName().split("\\.");
        if (nameItems.length < 3) {
            return ".smart.sync";
        }
        return "." + nameItems[1] + "." + nameItems[2];
    }

    protected boolean checkDigest(File file) {

        try {
            // Open a stream to read the file contents for digest computation
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream syncFis = new BufferedInputStream(fis);

            // Computes digest
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update("SeamfixBioCapture".getBytes(), 0, "SeamfixBioCapture".getBytes().length);
            byte[] buffer = new byte[255];
            int bytesRead = 0;
            while (true) {
                bytesRead = syncFis.read(buffer);

                if (bytesRead == -1) {
                    break;
                }
                digest.update(buffer, 0, bytesRead);
            }

            byte[] processedDigestArray = digest.digest();
            // Close stream opened for digest computation
            syncFis.close();
            fis.close();
            // Validate digest
            int endIndex = file.getName().indexOf(getExtension(file));
            String encodedDigest = file.getName().substring(4, endIndex);
            byte[] selectedDigestArray = null;
            try {
                selectedDigestArray = base64Coder.decode(encodedDigest);
            } catch (IllegalArgumentException iae) {
                logger.error(workerId, iae);
                throw new InvalidSyncFileNameException("Invalid file name : " + file.getName());
            }

            if (!Arrays.equals(processedDigestArray, selectedDigestArray)) {
                logger.debug(workerId + " Digests do not equal and file too old");
                throw new DigestException("Digest does not match");
            } else {
                return true;
            }
        } catch (NoSuchAlgorithmException e) {
            logger.debug(workerId, e);
        } catch (IOException e) {
            logger.debug(workerId, e);
        }

        return false;
    }

    protected SyncFile readDeserializedData(BiocaptureObjectInputStream objectIn, SyncFile syncFile)
            throws ClassNotFoundException, IOException, EncryptionException {
        // item store

        List<WsqImage> wsqImages = new ArrayList<>();
        List<MsisdnDetail> msisdns = new ArrayList<>();
        List<SpecialData> specials = new ArrayList<>();
        while (true) {

            Object readObject = objectIn.waitForObject();
            if (readObject instanceof FileSyncNewEntryMarker) {
                FileSyncNewEntryMarker marker = (FileSyncNewEntryMarker) readObject;
                syncFile.addProxyItem(ProxyKeyEnum.SYNC_MARKER, marker);

                break;
            } else if (readObject instanceof DummyObject) {
                continue;

            } else if (readObject instanceof UserId) {
                UserId userId = (UserId) readObject;
                if (userId.getCreateDate() == null) {
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    userId.setCreateDate(timestamp);
                }
                userId.setId(null);
                syncFile.addProxyItem(ProxyKeyEnum.USER_ID, userId);
            } else if (readObject instanceof BasicData) {
                BasicData basicData = (BasicData) readObject;
                basicData.setSurname(basicData.getSurname().trim());
                basicData.setFirstname(basicData.getFirstname().trim());
                basicData.setOthername(basicData.getOthername() != null ? basicData.getOthername().trim() : "");
                syncFile.addProxyItem(ProxyKeyEnum.BD, basicData);

            } else if (readObject instanceof DynamicData) {
                DynamicData dynamicData = (DynamicData) readObject;
                syncFile.addProxyItem(ProxyKeyEnum.DD, dynamicData);

            } else if (readObject instanceof EnrollmentLog) {
                EnrollmentLog enrollmentLog = (EnrollmentLog) readObject;
                syncFile.addProxyItem(ProxyKeyEnum.EL, enrollmentLog);

            } else if (readObject instanceof MetaData) {
                MetaData meta = (MetaData) readObject;
                syncFile.addProxyItem(ProxyKeyEnum.META, meta);

            } else if (readObject instanceof Passport) {
                Passport passport = (Passport) readObject;
                syncFile.addProxyItem(ProxyKeyEnum.PP, passport);

            } else if (readObject instanceof PassportDetail) {
                PassportDetail pd = (PassportDetail) readObject;
                syncFile.addProxyItem(ProxyKeyEnum.PD, pd);
            } else if (readObject instanceof WsqImage) {
                WsqImage wsqPrint = (WsqImage) readObject;
                wsqImages.add(wsqPrint);

            } else if (readObject instanceof MsisdnDetail) {
                MsisdnDetail msisdn = (MsisdnDetail) readObject;
                msisdns.add(msisdn);
            } else if (readObject instanceof Signature) {
                Signature signature = (Signature) readObject;
                syncFile.addProxyItem(ProxyKeyEnum.SIGN, signature);
            } else if (readObject instanceof SpecialData) {
                SpecialData sp = (SpecialData) readObject;
                specials.add(sp);
            } else if (readObject instanceof RegistrationSignature) {
                RegistrationSignature rs = (RegistrationSignature) readObject;
                syncFile.addProxyItem(ProxyKeyEnum.RS, rs);
            }
        }
        syncFile.addProxyItem(ProxyKeyEnum.WSQS, wsqImages);
        syncFile.addProxyItem(ProxyKeyEnum.MSISDNS, msisdns);
        syncFile.addProxyItem(ProxyKeyEnum.SPECIALS, specials);
        syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);

        return syncFile;
    }

    /**
     * @param serial
     * @return true if the serial number already exists
     */
    @SuppressWarnings("unused")
    private boolean finalizeSync(FileSyncNewEntryMarker fs) {
        // fs.isBlacklisted()
        return !fs.isBlacklisted();
    }

    @SuppressWarnings("PMD")
    @Override
    public SyncItem deserialize(SyncItem item) {
        SyncFile syncFile = null;
        try {
            //add to memcache before processing
            cache.setItem(queueKey + item.getItemId(), item.getItemId(), cacheTimeout);
            syncFile = (SyncFile) item;
            if (!checkDigest(item.getFile())) {
                // If the digest did not match it returns a false and removes
                // the file from the cache to allow for reprocessing...
                syncFile.setValidationStatusEnum(ValidationStatusEnum.DIGEST_EXCEPTION);
                return syncFile;
            }

            byte[] bao = decrypt(item.getFile());
            if (bao == null) {
                syncFile.setValidationStatusEnum(ValidationStatusEnum.DECRYPTION_EXCEPTION);
                return syncFile;
            }			// Create streams to read the actual objects from the decrypted
            // files
            BufferedInputStream syncFis = new BufferedInputStream(new ByteArrayInputStream(bao));
            GZIPInputStream gz = null;
            try {
                gz = new GZIPInputStream(syncFis);
                BiocaptureObjectInputStream objectIn = new BiocaptureObjectInputStream(gz);
                syncFile = readDeserializedData(objectIn, syncFile);
            } catch (IOException e) {
                syncFile.setValidationStatusEnum(ValidationStatusEnum.DECRYPTION_EXCEPTION);
                logger.error(workerId, e);
                return syncFile;
            } catch (BiocaptureStreaException e) {
                logger.error(" ", e);
                syncFile.setValidationStatusEnum(ValidationStatusEnum.BFPHOBIA_SYNC_FILES);
                return syncFile;
            }
        } catch (DigestException e) {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.DIGEST_EXCEPTION);
            cache.removeItem(queueKey + item.getItemId()); //allows this file to be retried
            logger.error("", e);
        } catch (IllegalArgumentException | EncryptionException e) {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.ILLEGAL_SYNC_FILE);
            syncFile.addBfpFailureLog(newBfpFailureLog(e.getClass().getName(), e, syncFile.getFile().getName(), BfpRejectionReason.INVALID_SYNC_FILE));
            logger.error(workerId, e.getMessage());
        } catch (InvalidSyncFileNameException e) {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.ILLEGAL_SYNC_FILE);
            syncFile.addBfpFailureLog(newBfpFailureLog("INVALID-SYNC-FILE-NAME", e, syncFile.getFile().getName(), BfpRejectionReason.INVALID_SYNC_FILE));
            logger.error(workerId, e.getMessage());
        } catch (ClassNotFoundException e) {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.ILLEGAL_SYNC_FILE);
            logger.error("", e);
        } catch (Exception e) {
            logger.error(workerId, e);
            syncFile.setValidationStatusEnum(ValidationStatusEnum.EXCEPTION);
            syncFile.addBfpFailureLog(newBfpFailureLog(e.getClass().getName(), e, syncFile.getFile().getName(), BfpRejectionReason.EXCEPTION));
        }
        return syncFile;

    }

    private BfpFailureLog newBfpFailureLog(String uniqueId, Throwable e, String filename, BfpRejectionReason bfpRejectionReason) {
        String exceptionMessage = e.getMessage();
        Throwable cause = e.getCause();
        String causeMessage = null;
        String causeName = null;
        if (cause != null) {
            causeMessage = cause.getMessage();
            causeName = cause.getClass().getName();
        }

        BfpFailureLog bfpFailureLog = new BfpFailureLog();
        bfpFailureLog.setUniqueId(uniqueId);
        bfpFailureLog.setFilename(filename);
        bfpFailureLog.setRejectionReason(bfpRejectionReason.getCode());
        bfpFailureLog.setReason1(exceptionMessage);
        bfpFailureLog.setReason2(causeName);
        bfpFailureLog.setReason3(causeMessage);

        return bfpFailureLog;
    }

    @SuppressWarnings("unchecked")
    protected void wrapUpSaveRecord(UserId uid, SyncItem item, Map<ProxyKeyEnum, Object> items) {
        List<MsisdnDetail> msisdns = (List<MsisdnDetail>) items.get(ProxyKeyEnum.MSISDNS);
        for (MsisdnDetail msisdn : msisdns) {
            item.addMsisdn(msisdn.getMsisdn());
        }
    }

    protected void getKey() {

    }

    @Override
    public boolean backup(SyncItem item) {
        new Thread(new SyncBackup(appDs, item)).start();
        return true;
    }

    @Override
    public SyncItem validate(SyncItem item) {
        SyncFile syncFile = (SyncFile) item;
        if (!skippAllValidation) {
            for (IValidator validator : validators.getValidators()) {
                syncFile = (SyncFile) validator.validate(syncFile);
//                logger.debug(workerId + ", validator: " + validator.getClass().getName() + ", validation status enum: " + syncFile.getValidationStatusEnum());
                if (syncFile.getValidationStatusEnum() != ValidationStatusEnum.VALID && validator.isPrerequisite()) {
                    //validation is halted here
//                    logger.debug(workerId + ", validator: " + validator.getClass().getName() + ", validation status enum: " + syncFile.getValidationStatusEnum());
                    break;
                }
            }
        } else {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
        }

        if (syncFile.getValidationStatusEnum() == ValidationStatusEnum.VALID) {
            //consider this check if validation is successful
            List<MsisdnDetail> msisdns = (List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS);
            String defaultNullMsisdnValue = getProperty(BfpProperty.NULL_MSISDN_VALUE);
            for (MsisdnDetail msisdnDetail : msisdns) {
                if (msisdnDetail.getMsisdn() == null || msisdnDetail.getMsisdn().isEmpty()) {
                    //well the only reason this is done is because msisdn is not nullable on MsisdnDetail
                    msisdnDetail.setMsisdn(defaultNullMsisdnValue);
                }
            }
        }
        return syncFile;
    }

    public static void main(String[] args) {

        File file = new File("xyz.txt");

        System.out.println("file exists : " + file.exists());

        //		Thread t = new Thread(new Runnable() {
        //
        //			@Override
        //			public void run() {
        //				SyncFile sf = new SyncFile();
        //				sf.setFile(new File("BIO-7-Cavo5eZtZMX3v!yjoiUQ==.smart.sync"));
        //				SyncWorker sw = new SyncWorker();
        //				sw.deserialize(sf);
        //
        //			}
        //		});
        //		t.start();
        // sw.backup(sf);
    }

    @Override
    public Crypter getEncryptionEngine() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setCrypto(Crypter crypto) {
        this.crypto = crypto;
    }

    @Override
    public byte[] decrypt(File sync) throws IOException {
        if (sync.getName().endsWith(".smart.sync")) {
            return decryptV1(sync);
        } else if (sync.getName().endsWith("sv2.sync")) {
            return decryptV2(sync);
        }

        return null;
    }

    public byte[] decryptV1(File sync) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BufferedOutputStream tempBos = new BufferedOutputStream(byteArrayOutputStream);

        FileInputStream fis = new FileInputStream(sync);
        BufferedInputStream syncFis = new BufferedInputStream(fis);

        // Decrypt the file
        AesEncrypter aesEncrypter = new AesEncrypter();
        aesEncrypter.decrypt(syncFis, tempBos);

        // Close the streams used for decryption
        syncFis.close();
        tempBos.close();
        fis.close();

        return byteArrayOutputStream.toByteArray();
    }

    public byte[] decryptV2(File sync) throws IOException {
        RandomAccessFile file = new RandomAccessFile(sync, "r");
        byte[] contents = new byte[(int) file.length()];
        file.read(contents);
        file.close();

        try {
            byte[] data = crypto.decrypt(contents);
            return data;
        } catch (KeyczarException e) {
            throw new IOException(e);
        }

    }

}
