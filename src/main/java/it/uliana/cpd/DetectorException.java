package it.uliana.cpd;

public class DetectorException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5185475380976614370L;

	public DetectorException() {
	}

	public DetectorException(String message) {
		super(message);
	}

	public DetectorException(Throwable cause) {
		super(cause);
	}

	public DetectorException(String message, Throwable cause) {
		super(message, cause);
	}

	public DetectorException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
