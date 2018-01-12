/**
 * 
 */
package com.seamfix.kyc.bfp.exception;

/**
 * @author dawuzi
 *
 */
public class MsisdnNotVerifiedException extends RuntimeException {

	private static final long serialVersionUID = 5509583423328238052L;
	
	private String msisdn;

	public MsisdnNotVerifiedException(String msisdn, Throwable cause) {
		super("Error verifying msisdn : "+msisdn, cause);
		this.msisdn = msisdn;
	}
	
	public MsisdnNotVerifiedException(String msisdn) {
		this.msisdn = msisdn;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}
	
}
