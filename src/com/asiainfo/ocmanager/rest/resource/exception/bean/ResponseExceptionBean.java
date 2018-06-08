package com.asiainfo.ocmanager.rest.resource.exception.bean;

/**
 * 
 * @author zhaoyim
 *
 */
public class ResponseExceptionBean {

	private String exception;

	public String getException() {
		return exception;
	}

	public void setException(String exception) {
		this.exception = exception;
	}

	@Override
	public String toString() {
		return "ResponseExceptionBean: {exception: " + exception + "}";
	}

}
