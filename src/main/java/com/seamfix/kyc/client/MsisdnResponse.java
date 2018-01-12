/**
 * 
 */
package com.seamfix.kyc.client;

//import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * @author dawuzi
 *
 */
//@JsonIgnoreProperties(ignoreUnknown = true)
public class MsisdnResponse {

	private String message; 
	private String status; // "Paired" is the only message that should be allowed to go through
	private int code; // 0 : successful web service call, -2 : network error, -1 : any other error
	private String uniqueId;
	private String phoneNumber;
	private String activationTimestamp;
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public String getUniqueId() {
		return uniqueId;
	}
	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public String getActivationTimestamp() {
		return activationTimestamp;
	}
	public void setActivationTimestamp(String activationTimestamp) {
		this.activationTimestamp = activationTimestamp;
	}
	@Override
	public String toString() {
		return "MsisdnResponse [message=" + message + ", status=" + status + ", code=" + code + ", uniqueId=" + uniqueId
				+ ", phoneNumber=" + phoneNumber + ", activationTimestamp=" + activationTimestamp + "]";
	}

}
