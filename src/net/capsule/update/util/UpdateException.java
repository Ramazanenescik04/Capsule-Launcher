package net.capsule.update.util;

public class UpdateException extends Exception {
	private static final long serialVersionUID = -3429688172968175186L;
	
    public UpdateException(final String message) {
        super(message);
    }

    public UpdateException(final String message, final Throwable cause) {
        super(message, cause);
    }
    
    public UpdateException(final Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
