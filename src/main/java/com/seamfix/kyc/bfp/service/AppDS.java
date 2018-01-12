package com.seamfix.kyc.bfp.service;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.seamfix.kyc.bfp.BioCache;
import com.seamfix.kyc.bfp.enums.BfpProperty;
import com.seamfix.kyc.bfp.enums.ProxyKeyEnum;
import com.seamfix.kyc.bfp.proxy.DynamicData;
import com.seamfix.kyc.bfp.proxy.MetaData;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.proxy.MsisdnStatusHolder;
import com.seamfix.kyc.bfp.proxy.Passport;
import com.seamfix.kyc.bfp.proxy.PassportDetail;
import com.seamfix.kyc.bfp.proxy.RegistrationSignature;
import com.seamfix.kyc.bfp.proxy.Signature;
import com.seamfix.kyc.bfp.proxy.SpecialData;
import com.seamfix.kyc.bfp.proxy.WsqImage;
import com.seamfix.kyc.bfp.syncs.SyncUtils;
import com.seamfix.kyc.bfp.util.LocationUtil;
import com.sf.biocapture.entity.BasicData;
import com.sf.biocapture.entity.EnrollmentLog;
import com.sf.biocapture.entity.EnrollmentRef;
import com.sf.biocapture.entity.KycAgilityMapper;
import com.sf.biocapture.entity.Node;
import com.sf.biocapture.entity.NodeAssignment;
import com.sf.biocapture.entity.Outlet;
import com.sf.biocapture.entity.PassportData;
import com.sf.biocapture.entity.PhoneNumberStatus;
import com.sf.biocapture.entity.Setting;
import com.sf.biocapture.entity.SignatureData;
import com.sf.biocapture.entity.SmsActivationRequest;
import com.sf.biocapture.entity.State;
import com.sf.biocapture.entity.TelecomMasterRecords;
import com.sf.biocapture.entity.UserId;
import com.sf.biocapture.entity.audit.BfpFailureLog;
import com.sf.biocapture.entity.audit.BfpSyncLog;
import com.sf.biocapture.entity.enums.ProcessingStatusCode;
import com.sf.biocapture.entity.enums.SarMsisdnUpdateStatusCode;
import com.sf.biocapture.entity.enums.StatusType;
import com.sf.biocapture.entity.security.KMUser;
import com.sf.biocapture.entity.temp.SubscriberDetail;
import com.sf.biocapture.entity.validation.ValidationResult;

import nw.commons.StopWatch;
import nw.orm.core.exception.NwormQueryException;
import nw.orm.core.query.QueryAlias;
import nw.orm.core.query.QueryModifier;

@Stateless
public class AppDS extends DataService {

	@Inject
	BioCache cache;
	
	@Inject
	LocationUtil locationUtil;
	
	private final String BLACKLIST_KEY = "XLIST-";
	private final String BLACKLIST_AGENT_KEY = "X-AGENT-LIST-";
	private final Pattern SPACE_PATTERN = Pattern.compile(" ");
	
	private final SyncUtils syncUtils = new SyncUtils();
        
        /**
         * query attribute to adhere to PMD rules
         */        
        private final String BASIC_DATA = "basicData";
        /**
         * query attribute to adhere to PMD rules
         */
        private final String MSISDN = "msisdn";
	
	
	/**
	 * 
	 * @param name
	 * @param defaultDescription
	 * @param defaultValue
	 * @param createIfNotExist
	 * @return
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public String getSettingValue(String name, String defaultValue, String defaultDescription, boolean createIfNotExist) {
		
		Setting setting = getSetting(name);
		
		if(setting != null){
			return setting.getValue();
		}
		
		if(createIfNotExist){
			
			setting = new Setting();
			
			setting.setName(name);
			setting.setDescription(defaultDescription);
			setting.setValue(defaultValue);
			
			dbService.create(setting);
		}
		
		return defaultValue;
	}
	public Timestamp getMinSyncTimestamp(String uniqueId) {
		// String hql = "SELECT MIN(s.receiptTimestamp) FROM
		// SmsActivationRequest s WHERE s.uniqueId = :uniqueId";
		QueryModifier qm = new QueryModifier(SmsActivationRequest.class);
		qm.addProjection(Projections.min("receiptTimestamp"));
		qm.transformResult(false);
		return dbService.getByCriteria(Timestamp.class, qm, Restrictions.eq(RECORD_UNIQUE_ID_FIELD, uniqueId));
	}

	// Remember everything in this method is transactional
	@SuppressWarnings("unchecked")
	public boolean saveRecord(Map<ProxyKeyEnum, Object> items) {
		StopWatch sw = new StopWatch(true);
		if (items == null) {
			return false;
		}

		MsisdnStatusHolder statusHolder = new MsisdnStatusHolder();
		UserId userId = (UserId) items.get(ProxyKeyEnum.USER_ID);
		if (userId == null) {
			return false;
		}
		statusHolder.setUniqueId(userId.getUniqueId());

		com.seamfix.kyc.bfp.proxy.BasicData bada = (com.seamfix.kyc.bfp.proxy.BasicData) items.get(ProxyKeyEnum.BD);
		BasicData bd = bada.to();
		if (bd == null) {
			logger.debug("Null Basic Data: " + userId.getUniqueId());
			return false;
		}

		com.seamfix.kyc.bfp.proxy.EnrollmentLog loga = (com.seamfix.kyc.bfp.proxy.EnrollmentLog) items.get(ProxyKeyEnum.EL);
		EnrollmentLog log = loga.to();
		if (log == null) {
			logger.debug("Null Enrollment Log: " + userId.getUniqueId());
			return false;
		}
		MetaData md = (MetaData) items.get(ProxyKeyEnum.META);
		if (md == null) {
			logger.debug("Null Metadata " + userId.getUniqueId());
			return false;
		}
		EnrollmentRef ref = null;
		if (log.getEnrollmentRef() == null) {
			ref = getEnrollmentRefByMacAddressOrDeviceId(md.getCaptureMachineId(), null);
		} else {
			ref = getEnrollmentRefByMacAddressOrDeviceId(log.getEnrollmentRef().getMacAddress(), log.getEnrollmentRef().getDeviceId());
		}		
		if(ref == null){
			throw new IllegalStateException("Kit not tagged");
		}

                items.put(ProxyKeyEnum.SAVED_EF, ref); //this is added here to be used in sync backup thread.
		log.setEnrollmentRef(ref);
		DynamicData dd = (DynamicData) items.get(ProxyKeyEnum.DD);
		if (dd == null) {
			logger.debug("Null Dynamic Data: " + userId.getUniqueId());
			return false;
		}
		com.sf.biocapture.entity.DynamicData dynamicData = dd.toDynamicData();

		// fixing birthday issues on droid and windows
		Timestamp birthday = bd.getBirthday();
		if ((bd.getBirthday() != null) && (dynamicData.getDa1() != null) && (ref.getCode() != null)) {
			try {
				if (ref.getCode().trim().toUpperCase().contains("DROID")) {
					bd.setBirthday(new Timestamp(
							new SimpleDateFormat("dd-MM-yyyy").parse(dynamicData.getDa1().trim()).getTime()));
					logger.debug("Setting birthday in basic data for droid devices dd-MM-yyyy... {old: " + birthday
							+ ", new: " + bd.getBirthday() + "}");
				} else // if(
						// ref.getCode().trim().toUpperCase().contains("KYC"))
				{
					bd.setBirthday(new Timestamp(
							new SimpleDateFormat("yyyy-MM-dd").parse(dynamicData.getDa1().trim()).getTime()));
					logger.debug("Setting birthday in basic data for window devices yyyy-MM-dd ... {old: " + birthday
							+ ", new: " + bd.getBirthday() + "}");
				}
			} catch (ParseException e) {
				logger.info("Could not parse subscriber's birthday " + dynamicData.getDa1());
			}
		}

		com.sf.biocapture.entity.MetaData metaData = md.toMetaData();
		metaData.setWithinGeoFence(locationUtil.isWithinGeofence(md.getLatitude(), md.getLongitude(), md.getCaptureMachineId(), md.getRealtimeDeviceId()));
		State state = getState(bada.getStateOfRegistration().getCode());
		if (state == null) {
			logger.debug("null state : "+userId.getUniqueId()+" state code : "+bada.getStateOfRegistration().getCode());
			return false;
		}

		Passport pp = (Passport) items.get(ProxyKeyEnum.PP);
		if (pp == null) {
			logger.debug("Null Passport: " + userId.getUniqueId());
			return false;
		}
		PassportData passport = pp.toPassport();
		com.sf.biocapture.entity.PassportDetail passportDetail = null;
		PassportDetail pd = (PassportDetail) items.get(ProxyKeyEnum.PD);
		if (pd != null) {
			passportDetail = pd.to();
			Signature sign = (Signature) items.get(ProxyKeyEnum.SIGN);
			SignatureData sd = sign.to();
			passportDetail.setSignature(sd);
		}

		logger.debug(userId.getUniqueId() + " Starting Persistence transaction: Reading stuff from map: ["
				+ sw.elapsedTime() + "]");
		dbService.create(userId);
		logger.debug(userId.getUniqueId() + " UserId: [" + sw.elapsedTime() + "]");
		bd.setUserId(userId);
		bd.setState(state);
		dbService.create(bd);
		logger.debug(userId.getUniqueId() + " Basic Data: [" + sw.elapsedTime() + "]");
		dynamicData.setBasicData(bd);
		dbService.create(dynamicData);
		logger.debug(userId.getUniqueId() + " Dynamic Data: [" + sw.elapsedTime() + "]");
		log.setBasicData(bd);
		dbService.create(log);
		logger.debug(userId.getUniqueId() + " Enrollment Log: [" + sw.elapsedTime() + "]");
		passport.setBasicData(bd);
		dbService.create(passport);
		logger.debug(userId.getUniqueId() + " Passport: [" + sw.elapsedTime() + "]");
		metaData.setBasicData(bd);
                metaData.setEnrollmentRef(ref);
		dbService.create(metaData);
		logger.debug(userId.getUniqueId() + " Meta Data: [" + sw.elapsedTime() + "]");
		statusHolder.setPortrait(passport);

		statusHolder.setPortrait(passport);

		List<MsisdnDetail> msisdns = (List<MsisdnDetail>) items.get(ProxyKeyEnum.MSISDNS);
		if (msisdns.isEmpty()) {
			throw new IllegalArgumentException("Empty MSISDN List: " + userId.getUniqueId());
		}
		
		for (MsisdnDetail msisdn : msisdns) {
			com.sf.biocapture.entity.MsisdnDetail m = msisdn.to();
			m.setBasicData(bd);
			dbService.create(m);
			
//			we pass in dynamic data now so we can include the previous unique id and reg type to sar
			createSmsActivationRequest(bd, log, m.getMsisdn(), userId, m.getSerial(), dynamicData, null);

			statusHolder.addMsisdnDetails(m);
		}

		logger.debug(userId.getUniqueId() + " Msisdn Detail: [" + sw.elapsedTime() + "]");
		List<SpecialData> sp = (List<SpecialData>) items.get(ProxyKeyEnum.SPECIALS);
		List<WsqImage> wsqs = (List<WsqImage>) items.get(ProxyKeyEnum.WSQS);
		if ((wsqs.size() < 4 && sp.isEmpty()) || wsqs.size() > 10) {
			throw new IllegalArgumentException("Invalid WSQ count: [" + wsqs.size() + "] " + userId.getUniqueId());
		}
		for (WsqImage wsq : wsqs) {
			com.sf.biocapture.entity.WsqImage w = wsq.to();
			w.setBasicData(bd);
			dbService.create(w);

			statusHolder.addWsqImage(w);
		}
		logger.debug(userId.getUniqueId() + " WSQ: [" + sw.elapsedTime() + "]");
		if (passportDetail != null) {
			SignatureData s = passportDetail.getSignature();
			s.setBasicData(bd);
			dbService.create(s);
			passportDetail.setSignature(s);
			dbService.create(passportDetail);

			statusHolder.setPassport(passportDetail);
		}
		logger.debug(userId.getUniqueId() + " Passport Detail: [" + sw.elapsedTime() + "]");
		for (SpecialData sdata : sp) {
			com.sf.biocapture.entity.SpecialData specialData = sdata.to();
			specialData.setBasicData(bd);
			dbService.create(specialData);

			statusHolder.addSpecialData(specialData);
		}
		logger.debug(userId.getUniqueId() + " Special Data: [" + sw.elapsedTime() + "]");
		statusHolder.createMsisdnStatus(dbService);

		createKycAgilityMapper(bd);
		logger.debug(userId.getUniqueId() + " Agility: [" + sw.elapsedTime() + "]");
		
		Boolean createValidationRecord = getBool(BfpProperty.CREATE_VALIDATION_RECORD); //to enable real time validation
		if(createValidationRecord){
			createValidationData(bd, md, dd, userId.getUniqueId());
			logger.debug(userId.getUniqueId() + " Validation Data: [" + sw.elapsedTime() + "]");
		}
                
		RegistrationSignature proxyRs = (RegistrationSignature) items.get(ProxyKeyEnum.RS);
                if (proxyRs != null) {
                    //captures are not always signed
                    com.sf.biocapture.entity.RegistrationSignature rs = proxyRs.to();
                    rs.setBasicData(bd);
                    dbService.create(rs);
                    logger.debug(userId.getUniqueId() + " Registration Signature: [" + sw.elapsedTime() + "]");
                }
                
		return true;
	}
	
	public void createValidationData(BasicData bd, MetaData md, DynamicData dd, String uniqueId){
		ValidationResult vr = new ValidationResult();
		vr.setCaptureVersion(md.getAppVersion());
		vr.setDeleted(false);
		vr.setRecordId(bd.getId());
		vr.setRevalidate(false);
		vr.setThresholdVersion(dd.getDda4());
		vr.setUniqueId(uniqueId);
		
		//check for portrait capture override
		if(dd.getDda20() != null && !dd.getDda20().isEmpty()){
			logger.debug("===========PORTRAIT CAPTURE OVERRIDE DETECTED FOR " + uniqueId);
			vr.setPassportValid(StatusType.OVERRIDE);
		}
		
		dbService.create(vr);
	}

	public void createKycAgilityMapper(BasicData bd){
		KycAgilityMapper kam = new KycAgilityMapper();
		kam.setAgilityStatus(false);
		kam.setOwcStatus(false);
		kam.setAgilityTimestamp(null);
		kam.setOwcTimestamp(null);
		kam.setBasicData(bd);
		dbService.create(kam);
	}

	public void createSmsActivationRequest(BasicData basicData, EnrollmentLog log, String phoneNumber, UserId userId,
			String serial, com.sf.biocapture.entity.DynamicData dynamicData, SmsActivationRequest parentSAR) {

		SmsActivationRequest smsActivationRequest = new SmsActivationRequest();
		smsActivationRequest.setCustomerName(basicData.getSurname() + " " + basicData.getFirstname());
		smsActivationRequest.setEnrollmentRef(log.getEnrollmentRef().getCode());
		smsActivationRequest.setPhoneNumber(phoneNumber);
                if (parentSAR == null) {
                    smsActivationRequest.setReceiptTimestamp(new Timestamp(new Date().getTime()));
                } else {                    
                    //MK-1624
                    //SAR created for telecommaster record is assumed to have passed through agility processes and thus should not be reprocessed.
                    smsActivationRequest.setReceiptTimestamp(parentSAR.getReceiptTimestamp());
                    smsActivationRequest.setConfirmationStatus(Boolean.TRUE);
                }
		smsActivationRequest.setSenderNumber("BIOMETRICS");
		smsActivationRequest.setStatus(userId.getDescription() == null ? "UNACTIVATED" : userId.getDescription());
		smsActivationRequest.setSerialNumber(serial);
		smsActivationRequest.setActivationTimestamp(new Timestamp(new Date().getTime()));
		
//		newly added
		smsActivationRequest.setPreviousUniqueId(dynamicData.getDda1());
		smsActivationRequest.setRegistrationType(dynamicData.getDda11());
		
		syncUtils.setRegistrationTimestamp(smsActivationRequest, log);

		smsActivationRequest.setUniqueId(basicData.getUserId().getUniqueId());
		smsActivationRequest.setStateId(basicData.getState().getId());

		SmsActivationRequest sms = getSmsActivationRequest(phoneNumber);
		if (sms == null) {
			smsActivationRequest.setIsInitiator(true);
			PhoneNumberStatus phoneNumStatus = new PhoneNumberStatus();
			phoneNumStatus.setInitTimestamp(smsActivationRequest.getReceiptTimestamp());
			phoneNumStatus.setStatus("ACTIVATION_PENDING");
			phoneNumStatus.setRemarks("");
			phoneNumStatus.setFinalTimestamp(null);

			dbService.create(phoneNumStatus);
			smsActivationRequest.setPhoneNumberStatus(phoneNumStatus);
			dbService.create(smsActivationRequest);
		} else {
			smsActivationRequest.setIsInitiator(false);
			smsActivationRequest.setPhoneNumberStatus(sms.getPhoneNumberStatus());
			dbService.create(smsActivationRequest);
		}

	}

	public boolean saveSignatureData(Map<ProxyKeyEnum, Object> items) {

		if (items == null) {
			return false;
		}

		UserId userId = (UserId) items.get(ProxyKeyEnum.USER_ID);
		if (userId == null) {
			return false;
		}

		QueryModifier qm = new QueryModifier(BasicData.class);
		qm.addAlias(new QueryAlias("userId", "uid"));

		BasicData bd = dbService.getByCriteria(BasicData.class, qm,
				Restrictions.eq("uid.uniqueId", userId.getUniqueId()));
		// logger.debug(bd + " uid: " + userId.getUniqueId());
		if (bd != null) {
			QueryModifier qms = new QueryModifier(SignatureData.class);
			qms.addAlias(new QueryAlias(BASIC_DATA, "bd"));
			SignatureData sd = dbService.getByCriteria(SignatureData.class, qms, Restrictions.eq("bd.id", bd.getId()));
			if (sd == null) {
				// save signature data
				PassportDetail pd = (PassportDetail) items.get(ProxyKeyEnum.PD);
				if (pd != null) {
					com.sf.biocapture.entity.PassportDetail passportDetail = pd.to();
					Signature sign = (Signature) items.get(ProxyKeyEnum.SIGN);
					sd = sign.to();
					passportDetail.setSignature(sd);
					sd.setBasicData(bd);
					dbService.create(sd);
					dbService.create(passportDetail);
				}
			} else {
				logger.debug("Signature Data exist for: " + userId.getUniqueId());
			}
			return true;
		}
		return false;
	}
        
    public boolean checkBlacklistStatusByDeviceId(String macAddress, String deviceId) {        
        Boolean blacklisted = true;
        Session session = null;
        try {
            session = dbService.getSessionService().getManagedSession();
            Criteria criteria = session.createCriteria(Node.class, "n");
            criteria.createAlias("n.enrollmentRef", "ref");
            //use only device id when available if not use only mac address                    
            if (deviceId != null && !deviceId.isEmpty()) {
                //consider this only when it is made available
                criteria.add(Restrictions.eq("ref.deviceId", deviceId.toUpperCase()));
            } else if (macAddress != null && !macAddress.isEmpty()) {
                //use mac address if available                
                criteria.add(Restrictions.eq("n.macAddress", macAddress.toUpperCase()));
            } else {
                logger.debug("Both mac address and device id cannot be empty or null");
                return blacklisted;
            }
            criteria.setProjection(Projections.property("n.blacklisted"));
            Object answer = criteria.uniqueResult();
            return answer == null ? false : (Boolean) answer;
        } catch (HibernateException he) {
            logger.error("", he);
        } finally {
            dbService.getSessionService().closeSession(session);
        }
        return blacklisted;
    }
	
//	this is just a stripped down version of the saveRecord method above. It pull existing data it needs from the db
	public ProcessingStatusCode saveRecordTelecomMaster(TelecomMasterRecords telecomMasterRecord) {
		
		String primaryMsisdn = telecomMasterRecord.getPrimaryMsisdn();
		String newMsisdn = telecomMasterRecord.getMsisdn();
		String targetUniqueId = telecomMasterRecord.getPinRef();
		
		UserId userId = dbService.getByCriteria(UserId.class, Restrictions.eq(RECORD_UNIQUE_ID_FIELD, targetUniqueId));
		
//		a userId must exist 
		if(userId == null){
			return ProcessingStatusCode.USER_ID_NOT_FOUND;
		}
		
		BasicData targetBasicData = dbService.getByCriteria(BasicData.class, Restrictions.eq("userId", userId));
		
		if(targetBasicData == null){
			return ProcessingStatusCode.BASIC_DATA_NOT_FOUND;
		}
		
		PassportData passport = dbService.getByCriteria(PassportData.class, Restrictions.eq(BASIC_DATA, targetBasicData));
		
		if(passport == null){
			return ProcessingStatusCode.PASSPORT_NOT_FOUND;
		}
		
		QueryModifier qmPassportDetail = new QueryModifier(com.sf.biocapture.entity.PassportDetail.class);
		
		qmPassportDetail.addAlias(new QueryAlias("signature", "sig"));
		
		com.sf.biocapture.entity.PassportDetail passportDetail = dbService.getByCriteria(com.sf.biocapture.entity.PassportDetail.class, 
				qmPassportDetail, Restrictions.eq("sig.basicData", targetBasicData));
		
		MsisdnStatusHolder statusHolder = new MsisdnStatusHolder();
		
		statusHolder.setUniqueId(targetUniqueId);
		statusHolder.setPassport(passportDetail); 
		
		EnrollmentLog log = dbService.getByCriteria(EnrollmentLog.class, Restrictions.eq(BASIC_DATA, targetBasicData));
		
		if(log == null){
			return ProcessingStatusCode.ENROLLMENT_LOG_NOT_FOUND;
		}
		
		statusHolder.setPortrait(passport);
	
		com.sf.biocapture.entity.DynamicData dd = dbService.getByCriteria(com.sf.biocapture.entity.DynamicData.class, 
				Restrictions.eq(BASIC_DATA, targetBasicData));

		com.sf.biocapture.entity.MsisdnDetail primaryMsisdnDetail = getMsisnDetail(primaryMsisdn, targetBasicData);
		
		if(primaryMsisdnDetail == null){
			return ProcessingStatusCode.PRIMARY_MSISDN_DETAIL_NOT_FOUND;
		}
		
		com.sf.biocapture.entity.MsisdnDetail newMsisdnDetail = new com.sf.biocapture.entity.MsisdnDetail();
		
		newMsisdnDetail.setMsisdn(newMsisdn);
		newMsisdnDetail.setBasicData(targetBasicData);
		newMsisdnDetail.setSubscriberType(primaryMsisdnDetail.getSubscriberType());
		newMsisdnDetail.setSerial("NA");
		newMsisdnDetail.setZap(primaryMsisdnDetail.getZap());
		newMsisdnDetail.setNewSubscriber(primaryMsisdnDetail.getNewSubscriber());
		
		dbService.create(newMsisdnDetail);
                SmsActivationRequest parentSAR = getSmsActivationRequest(targetUniqueId, primaryMsisdn);
		createSmsActivationRequest(targetBasicData, log, newMsisdnDetail.getMsisdn(), userId, newMsisdnDetail.getSerial(), dd, parentSAR);
		
		statusHolder.addMsisdnDetails(newMsisdnDetail);
		
		List<com.sf.biocapture.entity.WsqImage> wsqImages = dbService.getListByCriteria(com.sf.biocapture.entity.WsqImage.class, Restrictions.eq(BASIC_DATA, targetBasicData));
		
		for(com.sf.biocapture.entity.WsqImage anImage : wsqImages){
			statusHolder.addWsqImage(anImage);
		}
		
		List<com.sf.biocapture.entity.SpecialData> specialDatas = dbService.getListByCriteria(com.sf.biocapture.entity.SpecialData.class, Restrictions.eq(BASIC_DATA, targetBasicData));
		
		for (com.sf.biocapture.entity.SpecialData specialData : specialDatas) {
			statusHolder.addSpecialData(specialData);
		}
		
		statusHolder.createMsisdnStatus(dbService);
		
		return ProcessingStatusCode.PROCESSED;
	}

	private com.sf.biocapture.entity.MsisdnDetail getMsisnDetail(String msisdn, BasicData basicData) {

		List<com.sf.biocapture.entity.MsisdnDetail> msisdnDetails = dbService.getListByCriteria(com.sf.biocapture.entity.MsisdnDetail.class, 
				Restrictions.eq(MSISDN, msisdn), Restrictions.eq(BASIC_DATA, basicData));
		
		if(msisdnDetails == null || msisdnDetails.isEmpty()){
			return null;
		}
		
		if(msisdnDetails.size() > 1){
			logger.warn("More than one msisdn detail for basic data id : "+basicData.getId()+", msisdn : "+msisdn);
		}
		
		return msisdnDetails.get(0);
	}

	public List<TelecomMasterRecords> getUnprocessedRecords(int pageOffSet, int pageSize) {
		
		QueryModifier qm = new QueryModifier(TelecomMasterRecords.class);
		qm.setPaginated(pageOffSet, pageSize);
		
		Criterion statusCriterion = Restrictions.or(Restrictions.isNull("processingStatus"), 
				Restrictions.eq("processingStatus", ProcessingStatusCode.PENDING.getCode()));
		
		List<TelecomMasterRecords> records = dbService.getListByCriteria(TelecomMasterRecords.class, qm, statusCriterion);
		
		return records;
	}

	public SmsActivationRequest getSmsActivationRequest(String uniqueId, String msisdn) {
		
		List<SmsActivationRequest> requests = dbService.getListByCriteria(SmsActivationRequest.class, 
				Restrictions.eq(RECORD_UNIQUE_ID_FIELD, uniqueId), Restrictions.eq("phoneNumber", msisdn));
		
		if(requests == null || requests.isEmpty()){
			return null;
		}
		
		if(requests.size() > 1){
			logger.warn("More than one sms activation request for unique id : "+uniqueId+", msisdn : "+msisdn);
		}
		
		return requests.get(0);
	}

	public boolean updateEntity(Object entity){
		return dbService.update(entity);
	}
	
	public Object createEntity(Object entity){
		return dbService.create(entity);
	}

	public List<SmsActivationRequest> getMissingMsisdnRecords(int pageOffSet, int pageSize) {
		
		QueryModifier qm = new QueryModifier(SmsActivationRequest.class);
		qm.setPaginated(pageOffSet, pageSize);
		
		Criterion statusCriterion = Restrictions.or(
				Restrictions.eq("msisdnUpdateStatus", SarMsisdnUpdateStatusCode.PENDING.getCode()), 
				Restrictions.isNull("msisdnUpdateStatus")
				);

		return dbService.getListByCriteria(SmsActivationRequest.class, qm, Restrictions.isNotNull("msisdnUpdateTimestamp"), statusCriterion);
	}

	public List<com.sf.biocapture.entity.MsisdnDetail> getMsisdnDetail(String uniqueId, String serial) {

		QueryModifier qm = new QueryModifier(com.sf.biocapture.entity.MsisdnDetail.class);
		
		qm.addAlias(new QueryAlias(BASIC_DATA, "bd"));
		qm.addAlias(new QueryAlias("bd.userId", "uId"));
		
		List<com.sf.biocapture.entity.MsisdnDetail> results = dbService.getListByCriteria(com.sf.biocapture.entity.MsisdnDetail.class, qm,
				Restrictions.eq("uId.uniqueId", uniqueId), Restrictions.eq("serial", serial));
		
		return results;
	}

	public String checkAgentBlacklistStatus(MetaData meta, com.seamfix.kyc.bfp.proxy.BasicData basicData) {
		
		String yes = "Y";
		String no = "N";
		
		String agentEmail = basicData.getBiometricCaptureAgent();
		
		logger.debug("agent email : "+agentEmail);
		
//		probably an old app verion for it not match the old format 
		if(!agentEmail.contains("@")){
			logger.debug("not an email : "+agentEmail);
			return no;
		}
		
		String trimmedEmail = agentEmail.trim();
		
		Integer cacheTime = appProps.getInt("blacklist-agent-cache-time", 10);
		
//		check with email first
		
		String bItem = cache.getItem(BLACKLIST_AGENT_KEY + trimmedEmail, String.class);
		
		logger.debug("bItem = "+bItem);
		
		if(bItem != null){
			return bItem;
		}
		
		KMUser kmUser;
		
		kmUser = dbService.getByCriteria(KMUser.class, Restrictions.eq("emailAddress", trimmedEmail).ignoreCase()); 
		
//		logger.debug("kmuser : "+kmUser);
		
		boolean blackListed = true;
		
		if(kmUser != null){
			blackListed = !kmUser.isActive();
			logger.debug("blackListed : "+blackListed);
		} else {
			blackListed = true;
		}
		
		if(blackListed){
			cache.setItem(BLACKLIST_AGENT_KEY + trimmedEmail, yes, cacheTime * 60);
			return yes;
		} else {
			cache.setItem(BLACKLIST_AGENT_KEY + trimmedEmail, no, cacheTime * 60);
			return no;
		}
	}

	/**
	 * @param serial
	 * @param pageSize
	 * @param nullMsisdnVal 
	 * @return
	 */
	public List<SmsActivationRequest> getSmsActivationRequest(String serial, int pageSize, String excludeSerial) {
		
		return getSmsActivationRequest(serial, pageSize, excludeSerial, 0);
	}
	
	public List<SmsActivationRequest> getSmsActivationRequest(String serial, int pageSize, String excludeSerial, int pageOffSet) {
		
		QueryModifier qm = new QueryModifier(SmsActivationRequest.class);
		
		qm.setPaginated(pageOffSet, pageSize);
		
		Criterion serialCriterion;
		
		if(excludeSerial == null){
			serialCriterion = Restrictions.eq("serialNumber", serial);
		} else {
			serialCriterion = Restrictions.and(
					Restrictions.eq("serialNumber", serial), 
					Restrictions.not(Restrictions.eq("serialNumber", excludeSerial))
					);
		}
		
		List<SmsActivationRequest> result = dbService.getListByCriteria(SmsActivationRequest.class, qm, serialCriterion);
		
		return result;
	}

	/**
	 * @param macAddress
	 * @return list of enrollment refs with that mac address
	 */
	public List<EnrollmentRef> getEnrollmentRefsByMacAddress(String macAddress, int pageIndex, int pageSize) {
		
		QueryModifier modifier = new QueryModifier(EnrollmentRef.class);
		
		modifier.setPaginated(pageIndex, pageSize);
		
		List<EnrollmentRef> list = dbService.getListByCriteria(EnrollmentRef.class, Restrictions.eq("macAddress", macAddress).ignoreCase()); 
		
		return list;
	}
        
        public EnrollmentRef getEnrollmentRefByDeviceId(String deviceId) {
            if (deviceId == null || deviceId.isEmpty()) {
                logger.debug("Provided device id is empty or null");
                return null;
            }
            return dbService.getByCriteria(EnrollmentRef.class, Restrictions.eq("deviceId", deviceId.toUpperCase()));
        }
        
        public EnrollmentRef getEnrollmentRefByMacAddressOrDeviceId(String macAddress, String deviceId) {
            Conjunction conjunction = Restrictions.conjunction();
            if (deviceId != null && !deviceId.isEmpty()) {
                conjunction.add(Restrictions.eq("deviceId", deviceId.toUpperCase()));
            } else if (macAddress != null && !macAddress.isEmpty()) {
                conjunction.add(Restrictions.eq("macAddress", macAddress).ignoreCase());
            } else {
                return null;
            }
            return dbService.getByCriteria(EnrollmentRef.class, conjunction);
        }

	/**
	 * @param pinRef
	 * @param msisdn
	 * @param unfixedMsisdn
	 * @param pageSize
	 * @return
	 */
	public List<TelecomMasterRecords> getTelecomMasterRecordsWithLowestId(String pinRef, String msisdn,
			String unfixedMsisdn, int pageIndex, int pageSize) {
		
		QueryModifier qm = new QueryModifier(TelecomMasterRecords.class);
		
		qm.setPaginated(pageIndex, pageSize); 
		qm.addOrderBy(Order.asc("id"));
		
		Criterion msisdnCriterion = Restrictions.or(Restrictions.eq(MSISDN, msisdn), Restrictions.eq(MSISDN, unfixedMsisdn));
		
		List<TelecomMasterRecords> list = dbService.getListByCriteria(TelecomMasterRecords.class, qm, msisdnCriterion, Restrictions.eq("pinRef", pinRef));
		
		return list;
	}

	/**
	 * 
	 * @param uniqueId
	 * @param msisdn
	 * @param serial
	 * @param pageIndex
	 * @param pageSize
	 * @return
	 */
	public List<BfpFailureLog> getBfpFailureLog(String uniqueId, String msisdn, String serial, int pageIndex, int pageSize) {
		
		QueryModifier qm = new QueryModifier(BfpFailureLog.class);
		
		qm.setPaginated(pageIndex, pageSize); 
		
		
		Conjunction conjunction = Restrictions.conjunction();
		
		if(uniqueId != null){
			conjunction.add(Restrictions.eq(RECORD_UNIQUE_ID_FIELD, uniqueId).ignoreCase());
		}
		
		if(msisdn != null){
			conjunction.add(Restrictions.eq(MSISDN, msisdn).ignoreCase());
		}
		
		if(serial != null){
			conjunction.add(Restrictions.eq("simSerial", serial).ignoreCase());
		}
		
		List<BfpFailureLog> list = dbService.getListByCriteria(BfpFailureLog.class, qm, conjunction);
		
		return list;
	}

    public BfpSyncLog getBfpSyncLog(String uniqueId, String msisdn, String simSerial) {
        Disjunction conjunction = Restrictions.disjunction();
        conjunction.add(Restrictions.eq("simSerial", simSerial));
        conjunction.add(Restrictions.eq("msisdn", msisdn));
        return dbService.getByCriteria(BfpSyncLog.class, Restrictions.eq("uniqueId", uniqueId), conjunction);
    }
    
    public Outlet getOutlet(String mac, String deviceId) {
    	if(StringUtils.isBlank(mac) && StringUtils.isBlank(deviceId)) {
    		return null;
    	}
		String hql = "SELECT a FROM NodeAssignment a left join fetch a.outlet o left join a.targetNode.enrollmentRef e WHERE (";
		if(StringUtils.isNotBlank(deviceId)) {
			hql += " e.deviceId = :deviceId ";
		}
		if(StringUtils.isNotBlank(mac)) {
			if (StringUtils.isNotBlank(deviceId))
				hql += " OR ";
			hql += " e.macAddress = :mac ";
		}
		hql += ") and a.deleted = :deleted order by a.createDate desc";
		
		Session session = null;
		try {
			session = dbService.getSessionService().getManagedSession();
			Query query = session.createQuery(hql);
			if(StringUtils.isNotBlank(deviceId)) {
				query.setParameter("deviceId", deviceId);
			}
			if(StringUtils.isNotBlank(mac)) {
				query.setParameter("mac", mac.toUpperCase());
			}
			query.setParameter("deleted", false);
			List<NodeAssignment> nA = (List<NodeAssignment>)query.list();
			if (nA != null && !nA.isEmpty()) {
				return nA.get(0).getOutlet();
			}
		} catch (HibernateException | NwormQueryException e) {
			logger.error("Something went wrong while getting outlet: ", e);
		} finally {
			dbService.getSessionService().closeSession(session);
		}
		return null;
	}
    
    public SubscriberDetail getSubscriberDetail(String msisdn) {
        Conjunction conjunction = Restrictions.conjunction();
        if (Boolean.valueOf(getSettingValue("CHECK-TEMP-SUBSCRIBER-VAL-STATUS", "true", "determines whether to check temp subscriber's validation status during rereg", true))) {
            conjunction.add(Restrictions.eq("validated", true));
        }

        conjunction.add(Restrictions.eq("msisdn", msisdn));
        conjunction.add(Restrictions.eq("biometricsUpdated", false));
        return dbService.getByCriteria(SubscriberDetail.class, conjunction);
    }
}