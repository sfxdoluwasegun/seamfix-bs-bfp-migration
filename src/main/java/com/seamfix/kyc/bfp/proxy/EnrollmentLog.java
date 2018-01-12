package com.seamfix.kyc.bfp.proxy;

import java.io.Serializable;
import java.util.Date;

import com.sf.biocapture.entity.EnrollmentRef;

public class EnrollmentLog implements Serializable{

	private static final long serialVersionUID = 8628750446750288552L;

	private Date date;

	private Date time;

	private EnrollmentRef enrollmentRef;

	public Date getDate() {
		return this.date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Date getTime() {
		return this.time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public EnrollmentRef getEnrollmentRef() {
		return this.enrollmentRef;
	}

	public void setEnrollmentRef(EnrollmentRef enrollmentRef) {
		this.enrollmentRef = enrollmentRef;
	}

	public com.sf.biocapture.entity.EnrollmentLog to(){
		com.sf.biocapture.entity.EnrollmentLog log = new com.sf.biocapture.entity.EnrollmentLog();
		log.setDate(getDate());
		log.setTime(getTime());
		log.setEnrollmentRef(getEnrollmentRef());

		return log;
	}

}
