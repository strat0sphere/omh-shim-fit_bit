package org.openmhealth.shim.exception;

/**
 * <p> 
 * An exception for problems reading a schema.
 * </p>
 *
 * @author John Jenkins
 */
public class ShimSchemaException extends ShimException {
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
	public ShimSchemaException(final String reason) {
		super(reason);
	}

	/**
	 * Creates a new exception with a reason and an underlying cause.
	 * 
	 * @param reason
	 *        The reason this exception is being thrown.
	 * 
	 * @param cause
	 *        The Throwable that caused this exception to be thrown.
	 */
	public ShimSchemaException(final String reason, final Throwable cause) {
		super(reason, cause);
	}
}