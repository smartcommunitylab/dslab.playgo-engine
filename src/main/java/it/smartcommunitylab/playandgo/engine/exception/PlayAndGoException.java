package it.smartcommunitylab.playandgo.engine.exception;

public class PlayAndGoException extends Exception {
	private String code;

	public PlayAndGoException() {
		super();
	}

	public PlayAndGoException(String message, String code) {
		super(message);
		this.code = code;
	}
	
	public PlayAndGoException(String message, Throwable cause) {
		super(message, cause);
	}

	public PlayAndGoException(String message) {
		super(message);
	}

	public PlayAndGoException(Throwable cause) {
		super(cause);
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

}
