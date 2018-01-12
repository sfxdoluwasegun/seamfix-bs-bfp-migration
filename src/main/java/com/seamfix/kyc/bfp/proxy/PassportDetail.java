package com.seamfix.kyc.bfp.proxy;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;

public class PassportDetail implements Serializable{

	private static final long serialVersionUID = 345632423744L;

	@Column(name = "ISSUE_COUNTRY", nullable = false)
	private String issueCountry;

	@Column(name = "PASSPORT_NUMBER", nullable = false)
	private String passportNumber;

	@Column(name = "RESIDENCY_STATUS")
	private Boolean residencyStatus;

	@Column(name = "EXPIRY_DATE", nullable = false)
	private Date expiryDate;

	public String getIssueCountry() {
		return this.issueCountry;
	}

	public void setIssueCountry(String issueCountry) {
		this.issueCountry = issueCountry;
	}

	public String getPassportNumber() {
		return this.passportNumber;
	}

	public void setPassportNumber(String passportNumber) {
		this.passportNumber = passportNumber;
	}

	public Boolean getResidencyStatus() {
		return this.residencyStatus;
	}

	public void setResidencyStatus(Boolean residencyStatus) {
		this.residencyStatus = residencyStatus;
	}

	public Date getExpiryDate() {
		return this.expiryDate;
	}

	public void setExpiryDate(Date expiryDate) {
		this.expiryDate = expiryDate;
	}

	public com.sf.biocapture.entity.PassportDetail to(){
		com.sf.biocapture.entity.PassportDetail p = new com.sf.biocapture.entity.PassportDetail();
		p.setExpiryDate(expiryDate);
		p.setIssueCountry(issueCountry);
		p.setPassportNumber(passportNumber);
		p.setResidencyStatus(residencyStatus);
		return p;
	}

}
