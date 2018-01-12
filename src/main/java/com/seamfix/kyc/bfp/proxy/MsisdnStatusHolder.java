
package com.seamfix.kyc.bfp.proxy;

import com.sf.biocapture.entity.PassportData;
import com.sf.biocapture.entity.PassportDetail;
import com.sf.biocapture.entity.WsqImage;
import com.sf.biocapture.entity.SpecialData;
import com.sf.biocapture.entity.CrmPushStatus;
import com.sf.biocapture.entity.MsisdnDetail;
import com.sf.biocapture.entity.enums.CrmStatus;
import com.sf.biocapture.entity.enums.SfxCrmTypes;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import nw.orm.core.service.Nworm;

/**
 *
 * @author @wizzyclems
 */
public class MsisdnStatusHolder {
	private String uniqueId;
	private List<MsisdnDetail> msisdns;
	private List<SpecialData> specialData;
	private List<WsqImage> wsqImage;
	private PassportData portrait;
	private PassportDetail passport;

	public MsisdnStatusHolder() {
		uniqueId = "";
		msisdns = new ArrayList<>();
		specialData = new ArrayList<>();
		wsqImage = new ArrayList<>();
	}

	public void addSpecialData(SpecialData sd) {
		specialData.add(sd);
	}

	public void addMsisdnDetails(MsisdnDetail md) {
		msisdns.add(md);
	}

	public void addWsqImage(WsqImage wi) {
		wsqImage.add(wi);
	}

	public List<WsqImage> getWsqImage() {
		return wsqImage;
	}

	public void setWsqImage(List<WsqImage> wsqImage) {
		this.wsqImage = wsqImage;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public List<MsisdnDetail> getMsisdns() {
		return msisdns;
	}

	public void setMsisdns(List<MsisdnDetail> msisdns) {
		this.msisdns = msisdns;
	}

	public List<SpecialData> getSpecialData() {
		return specialData;
	}

	public void setSpecialData(List<SpecialData> specialData) {
		this.specialData = specialData;
	}

	public PassportData getPortrait() {
		return portrait;
	}

	public void setPortrait(PassportData portrait) {
		this.portrait = portrait;
	}

	public PassportDetail getPassport() {
		return passport;
	}

	public void setPassport(PassportDetail passport) {
		this.passport = passport;
	}

	public void createMsisdnStatus(Nworm dbService) {
		
//		this is to ensure that all the create time stamp are equal for all the newly created CrmPushStatus
		Timestamp crmCreateTimestamp = new Timestamp(new Date().getTime());
		
		for (MsisdnDetail msisdnDetail : msisdns) {
			
			handleCreateCrmPushStatus(dbService, msisdnDetail, CrmStatus.NOT_SENT, SfxCrmTypes.PORTRAIT, crmCreateTimestamp); 
			
			if (getPassport() != null) {
				handleCreateCrmPushStatus(dbService, msisdnDetail, CrmStatus.NOT_SENT, SfxCrmTypes.PASSPORT, crmCreateTimestamp); 
			}

			for(SpecialData aSpecialData : specialData){
				handleSpecialData(dbService, msisdnDetail, aSpecialData, crmCreateTimestamp);
			}
			
			for(WsqImage aWsqImage : wsqImage){
				handleWsqImage(dbService, msisdnDetail, aWsqImage, crmCreateTimestamp);
			}
		}
	}
	
	/**
	 * @param dbService
	 * @param msisdnDetail
	 * @param wsqImage
	 * @param crmCreateTimestamp 
	 */
	private void handleWsqImage(Nworm dbService, MsisdnDetail msisdnDetail, WsqImage wsqImage, Timestamp crmCreateTimestamp) {
		
		SfxCrmTypes fingerCrmType = null;
		
		String finger = wsqImage.getFinger();
		
//		this if-else long code checks where done for backward compatibility reasons for old versions
		
		if (finger.equalsIgnoreCase("LEFT_INDEX_FINGER")) {
			fingerCrmType = SfxCrmTypes.LEFT_INDEX_FINGER;
		} else if (finger.equalsIgnoreCase("RIGHT_INDEX_FINGER")) {
			fingerCrmType = SfxCrmTypes.RIGHT_INDEX_FINGER;
		} else if (finger.equalsIgnoreCase("RIGHT_THUMB")) {
			fingerCrmType = SfxCrmTypes.RIGHT_THUMB;
		} else if (finger.equalsIgnoreCase("LEFT_THUMB")) {
			fingerCrmType = SfxCrmTypes.LEFT_THUMB;
		} else if (finger.equalsIgnoreCase("LEFT_RING_FINGER")) {
			fingerCrmType = SfxCrmTypes.LEFT_RING_FINGER;
		} else if (finger.equalsIgnoreCase("RIGHT_RING_FINGER")) {
			fingerCrmType = SfxCrmTypes.RIGHT_RING_FINGER;
		} else if (finger.equalsIgnoreCase("LEFT_MIDDLE_FINGER")) {
			fingerCrmType = SfxCrmTypes.LEFT_MIDDLE_FINGER;
		} else if (finger.equalsIgnoreCase("RIGHT_MIDDLE_FINGER")) {
			fingerCrmType = SfxCrmTypes.RIGHT_MIDDLE_FINGER;
		} else if (finger.equalsIgnoreCase("RIGHT_LITTLE_FINGER")) {
			fingerCrmType = SfxCrmTypes.RIGHT_LITTLE_FINGER;
		} else if (finger.equalsIgnoreCase("LEFT_LITTLE_FINGER")) {
			fingerCrmType = SfxCrmTypes.LEFT_LITTLE_FINGER;
//		NEWLY ADDED
		} else if (finger.equalsIgnoreCase("RIGHT_INDEX")) {
			fingerCrmType = SfxCrmTypes.RIGHT_INDEX_FINGER;
		} else if (finger.equalsIgnoreCase("LEFT_INDEX")) {
			fingerCrmType = SfxCrmTypes.LEFT_INDEX_FINGER;
		} else if (finger.equalsIgnoreCase("RIGHT_THUMB_FINGER")) {
			fingerCrmType = SfxCrmTypes.RIGHT_THUMB;
		} else if (finger.equalsIgnoreCase("RIGHT_RING")) {
			fingerCrmType = SfxCrmTypes.RIGHT_RING_FINGER;
		} else if (finger.equalsIgnoreCase("LEFT_THUMB_FINGER")) {
			fingerCrmType = SfxCrmTypes.LEFT_THUMB;
		} else if (finger.equalsIgnoreCase("LEFT_RING")) {
			fingerCrmType = SfxCrmTypes.LEFT_RING_FINGER;
		} else if (finger.equalsIgnoreCase("LEFT_PINKY")) {
			fingerCrmType = SfxCrmTypes.LEFT_LITTLE_FINGER;
		} else if (finger.equalsIgnoreCase("RIGHT_MIDDLE")) {
			fingerCrmType = SfxCrmTypes.RIGHT_MIDDLE_FINGER;
		} else if (finger.equalsIgnoreCase("RIGHT_PINKY")) {
			fingerCrmType = SfxCrmTypes.RIGHT_LITTLE_FINGER;
		} else if (finger.equalsIgnoreCase("LEFT_MIDDLE")) {
			fingerCrmType = SfxCrmTypes.LEFT_MIDDLE_FINGER;
		} else {
			throw new IllegalStateException("Invalid WSQ Finger Type");
		}
		
		handleCreateCrmPushStatus(dbService, msisdnDetail, CrmStatus.NOT_SENT, fingerCrmType, crmCreateTimestamp);		
	}

	/**
	 * @param dbService
	 * @param msisdnDetail
	 * @param crmCreateTimestamp 
	 * @param aSpecialData
	 */
	private void handleSpecialData(Nworm dbService, MsisdnDetail msisdnDetail, SpecialData sd, Timestamp crmCreateTimestamp) {
		
		String biometricDataType = sd.getBiometricDataType();
		
		SfxCrmTypes specialDataCrmType = null;
		
		if (biometricDataType.equalsIgnoreCase(SfxCrmTypes.REGISTRATION_FORM.name())) {
			specialDataCrmType = SfxCrmTypes.REGISTRATION_FORM;
		} else if (biometricDataType.equalsIgnoreCase(SfxCrmTypes.MISSING_LEFT_HAND.name())) {
			specialDataCrmType = SfxCrmTypes.MISSING_LEFT_HAND;
		} else if (biometricDataType.equalsIgnoreCase(SfxCrmTypes.MISSING_RIGHT_HAND.name())) {
			specialDataCrmType = SfxCrmTypes.MISSING_RIGHT_HAND;
		} else if (biometricDataType.equalsIgnoreCase(SfxCrmTypes.MODE_OF_IDENTIFICATION.name())) {
			specialDataCrmType = SfxCrmTypes.MODE_OF_IDENTIFICATION;
			
		} else if (biometricDataType.equalsIgnoreCase(SfxCrmTypes.CERTIFICATE_OF_INCORPORATION.name())) {
			specialDataCrmType = SfxCrmTypes.CERTIFICATE_OF_INCORPORATION;
		} else if (biometricDataType.equalsIgnoreCase(SfxCrmTypes.CONTACT_PERSON_FORM.name())) {
			specialDataCrmType = SfxCrmTypes.CONTACT_PERSON_FORM;
		} else if (biometricDataType.equalsIgnoreCase(SfxCrmTypes.BANK_GUARANTEE_LETTER.name())) {
			specialDataCrmType = SfxCrmTypes.BANK_GUARANTEE_LETTER;
		} else if (biometricDataType.equalsIgnoreCase(SfxCrmTypes.PASSPORT_PHOTOGRAPHS.name())) {
			specialDataCrmType = SfxCrmTypes.PASSPORT_PHOTOGRAPHS;
		} else if (biometricDataType.equalsIgnoreCase(SfxCrmTypes.SIGNATURE_CARD.name())) {
			specialDataCrmType = SfxCrmTypes.SIGNATURE_CARD;
		} else if (biometricDataType.equalsIgnoreCase(SfxCrmTypes.REQUEST_LETTER.name())) {
			specialDataCrmType = SfxCrmTypes.REQUEST_LETTER;
		} else if (biometricDataType.equalsIgnoreCase(SfxCrmTypes.UTILITY_BILL.name())) {
			specialDataCrmType = SfxCrmTypes.UTILITY_BILL;
		} else if (biometricDataType.equalsIgnoreCase(SfxCrmTypes.EVIDENCE_OF_PAYMENT.name())) {
			specialDataCrmType = SfxCrmTypes.EVIDENCE_OF_PAYMENT;
			
//		handling the mismatch from android. Some versions of android tagged REGISTRATION_FORM as KYC_FORM
		} else if (biometricDataType.equalsIgnoreCase("KYC_FORM")) {
			specialDataCrmType = SfxCrmTypes.REGISTRATION_FORM;
//		this was a case from an erroneous version of the android application. 
//		The missing finger actually maps to either missing left hand or missing right hand
//		there is nothing to push to OWC in this case because either they dont have any wsq entries or they have valid ones already 
		} else if (biometricDataType.equalsIgnoreCase("MISSING_FINGER")) {
			return;
//		this case is redundant. It corresponds to the international passport which is in the signature table and has already been handled earlier
		} else if (biometricDataType.equalsIgnoreCase("PASSPORT")) {
			return;
		
			
		} else {
			throw new IllegalStateException("Invalid Special Date Biometric Data Type");
		}
		
		handleCreateCrmPushStatus(dbService, msisdnDetail, CrmStatus.NOT_SENT, specialDataCrmType, crmCreateTimestamp);		
	}

	/**
	 * 
	 * @param dbService
	 * @param msisdnDetail
	 * @param pushStatus
	 * @param crmType
	 * @param crmCreateTimestamp 
	 */
	private void handleCreateCrmPushStatus(Nworm dbService, MsisdnDetail msisdnDetail, CrmStatus pushStatus, SfxCrmTypes crmType, Timestamp crmCreateTimestamp) {
		CrmPushStatus crmPushStatus = new CrmPushStatus();
		
		crmPushStatus.setUniqueId(uniqueId);
		crmPushStatus.setCrmType(crmType);
		crmPushStatus.setPushStatus(pushStatus);
		crmPushStatus.setMsisdnDetail(msisdnDetail);
		crmPushStatus.setCrmCreateTimestamp(crmCreateTimestamp);
                crmPushStatus.setSkip(Boolean.FALSE);
		
		dbService.create(crmPushStatus);
	}
}
