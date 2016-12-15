package com.sap.lvm;

public class CloudClientException extends Exception {

	private static final long serialVersionUID = 4553109132767619859L;

	/**
	 * Constructor for CloudClientException
	 * 
	 * @param message
	 */
	public CloudClientException(String message) {
		super(message);
	}

	/**
	 * Constructor for CloudClientException
	 * 
	 * @param t
	 */
	public CloudClientException(Throwable t) {
		super(t);
	}

	/**
	 * Constructor for CloudClientException
	 * 
	 * @param message
	 */
	public CloudClientException(String message, Throwable t) {
		super(message, t);
	}
}
