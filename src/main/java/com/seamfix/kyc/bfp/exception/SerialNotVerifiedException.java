/**
 * 
 */
package com.seamfix.kyc.bfp.exception;

/**
 * @author dawuzi
 * 
 * This exception is thrown when a serial number cannot be verified
 *
 */
public class SerialNotVerifiedException extends RuntimeException {

	private static final long serialVersionUID = -4485180276991714565L;

	private String serial;

	public SerialNotVerifiedException(String serial, Throwable cause) {
		super("Error verifying serial number : "+serial, cause);
		this.serial = serial;
	}
	
	public SerialNotVerifiedException(String serial) {
		this.serial = serial;
	}	

	public String getSerial() {
		return serial;
	}

	public void setSerial(String serial) {
		this.serial = serial;
	}
}
