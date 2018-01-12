package com.seamfix.kyc.bfp.proxy;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

public class BasicData implements Serializable{

	private static final long serialVersionUID = 577896595295935679L;

	private State stateOfRegistration;

	private String surname;

	private String firstname;

	private String othername;

	private String gender;

	private Boolean matchFound = false;

	private String matchId;

	private String syncStatus;

	private String smsStatus;

	private String biometricCaptureAgent;

	private String lastBasicDataEditAgent;

	private Boolean isProcessed;

	private String lastBasicDataEditLoginId;

	private Timestamp birthday;

	private Timestamp createDate = new Timestamp(new Date().getTime());




	public BasicData() {
		createDate = new Timestamp(new Date().getTime());
	}


	public String getSurname() {
		return this.surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}
	public String getFirstname() {
		return this.firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}
	public String getOthername() {
		return this.othername;
	}

	public void setOthername(String othername) {
		this.othername = othername;
	}
	public String getGender() {
		return this.gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}
	public boolean isMatchFound() {
		return this.matchFound;
	}

	public void setMatchFound(boolean matchFound) {
		this.matchFound = matchFound;
	}
	public String getMatchId() {
		return this.matchId;
	}

	public void setMatchId(String matchId) {
		this.matchId = matchId;
	}
	public String getSyncStatus() {
		return this.syncStatus;
	}

	public void setSyncStatus(String syncStatus) {
		this.syncStatus = syncStatus;
	}
	public String getSmsStatus() {
		return this.smsStatus;
	}

	public void setSmsStatus(String smsStatus) {
		this.smsStatus = smsStatus;
	}
	public String getBiometricCaptureAgent() {
		return this.biometricCaptureAgent;
	}

	public void setBiometricCaptureAgent(String biometricCaptureAgent) {
		this.biometricCaptureAgent = biometricCaptureAgent;
	}
	public String getLastBasicDataEditAgent() {
		return this.lastBasicDataEditAgent;
	}

	public void setLastBasicDataEditAgent(String lastBasicDataEditAgent) {
		this.lastBasicDataEditAgent = lastBasicDataEditAgent;
	}
	public boolean isIsProcessed() {
		return this.isProcessed;
	}

	public void setIsProcessed(boolean isProcessed) {
		this.isProcessed = isProcessed;
	}
	public String getLastBasicDataEditLoginId() {
		return this.lastBasicDataEditLoginId;
	}

	public void setLastBasicDataEditLoginId(String lastBasicDataEditLoginId) {
		this.lastBasicDataEditLoginId = lastBasicDataEditLoginId;
	}
	public Timestamp getBirthday() {
		return this.birthday;
	}

	public void setBirthday(Timestamp birthday) {
		this.birthday = birthday;
	}

	public Timestamp getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Timestamp createDate) {
		this.createDate = createDate;
	}


	public State getStateOfRegistration() {
		return stateOfRegistration;
	}


	public void setStateOfRegistration(State stateOfRegistration) {
		this.stateOfRegistration = stateOfRegistration;
	}

	public com.sf.biocapture.entity.BasicData to() {
		com.sf.biocapture.entity.BasicData bd = new com.sf.biocapture.entity.BasicData();
		bd.setBiometricCaptureAgent(getBiometricCaptureAgent());
		bd.setBirthday(getBirthday());
		bd.setFirstname(getFirstname());
		bd.setGender(getGender());
		bd.setLastBasicDataEditAgent(getLastBasicDataEditAgent());
		bd.setMatchFound(isMatchFound());
		bd.setOthername(getOthername());
		bd.setSurname(getSurname());
		bd.setIsProcessed(isIsProcessed());
		bd.setSmsStatus(getSmsStatus());
		bd.setSyncStatus(getSyncStatus());
		bd.setLastBasicDataEditLoginId(getLastBasicDataEditLoginId());
		return bd;

	}

}
