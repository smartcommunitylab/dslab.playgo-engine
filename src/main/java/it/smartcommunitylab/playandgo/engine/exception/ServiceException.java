package it.smartcommunitylab.playandgo.engine.exception;

public class ServiceException extends PlayAndGoException {
	private static final long serialVersionUID = -6197169937762469426L;

	public ServiceException() {
		super();
	}
	
	public ServiceException(String message, String code) {
		super(message, code);
	}

	public ServiceException(String message) {
		super(message);
	}
}
