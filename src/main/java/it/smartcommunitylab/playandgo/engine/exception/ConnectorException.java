package it.smartcommunitylab.playandgo.engine.exception;

public class ConnectorException extends PlayAndGoException {
	private static final long serialVersionUID = 3510890602135252304L;

	public ConnectorException() {
	}
	
	public ConnectorException(String message, String code) {
		super(message, code);
	}
	
	public ConnectorException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConnectorException(String message) {
		super(message);
	}

	public ConnectorException(Throwable cause) {
		super(cause);
	}

}
