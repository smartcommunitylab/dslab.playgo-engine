package it.smartcommunitylab.playandgo.engine.exception;

public class UnauthorizedException extends PlayAndGoException {

	public UnauthorizedException() {
		super();
	}
	
	public UnauthorizedException(String message, String code) {
		super(message, code);
	}

	public UnauthorizedException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnauthorizedException(String message) {
		super(message);
	}

	public UnauthorizedException(Throwable cause) {
		super(cause);
	}

}
