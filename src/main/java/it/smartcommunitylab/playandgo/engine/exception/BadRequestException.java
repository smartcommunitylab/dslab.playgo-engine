package it.smartcommunitylab.playandgo.engine.exception;

public class BadRequestException extends PlayAndGoException {

	public BadRequestException() {
		super();
	}

	public BadRequestException(String message, String code) {
		super(message, code);
	}
	
	public BadRequestException(String message, Throwable cause) {
		super(message, cause);
	}

	public BadRequestException(String message) {
		super(message);
	}

	public BadRequestException(Throwable cause) {
		super(cause);
	}

}
