package org.openmhealth.shim.exception;

import org.openmhealth.reference.exception.OmhException;

/**
 * <p>
 * The super-class for all exceptions that should be thrown by the shim.
 * </p>
 *
 * @author John Jenkins
 */
public abstract class ShimException extends OmhException {
	/**
	 * The version of this class for serialization purposes.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new exception with only a reason.
	 * 
	 * @param reason
	 *        The reason this exception is being thrown.
	 */
	public ShimException(final String reason) {
		super(reason);
	}

	/**
	 * Creates a new exception with a reason and an underlying cause.
	 * 
	 * @param reason
	 *        The reason this exception is being thrown.
	 * 
	 * @param cause
	 *        The throwable that caused this exception to be thrown.
	 */
	public ShimException(final String reason, final Throwable cause) {
		super(reason, cause);
	}	
}