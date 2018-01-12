/**
 * 
 */
package com.seamfix.kyc.bfp.exception;

/**
 * @author dawuzi
 * 
 * This exception is signifies that the file name is invalid (usually invalid base 64 encoded name). 
 *
 */
public class DigestException extends RuntimeException {

	private static final long serialVersionUID = -5676755832229046986L;

	public DigestException(String message, Throwable cause) {
		super(message, cause);
	}

	public DigestException(String message) {
		super(message);
	}

	public DigestException() {
		super();
	}
}
