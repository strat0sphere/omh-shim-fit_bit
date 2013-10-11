package org.openmhealth.shim;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * <p>
 * The registry of shim implementations.
 * </p>
 *
 * @author John Jenkins
 */
public abstract class ShimRegistry implements ServletContextListener {
	/**
	 * The map of domains to the {@link Shim} that can handle them.
	 */
	private static final Map<String, Shim> REGISTRY =
		new HashMap<String, Shim>();
	
	/**
	 * Default constructor.
	 */
	public ShimRegistry() {
		// Do nothing.
	}
	
	/**
	 * Call into the sub-class to add it to the registry.
	 */
	@Override
	public final void contextInitialized(final ServletContextEvent event) {
		add(getShim());
	}
	
	/**
	 * Prevent sub-classes from interfering with the initialization process.
	 */
	@Override
	public final void contextDestroyed(final ServletContextEvent event) {
		// Do nothing.
	}
	
	/**
	 * Returns the instance of a {@link Shim} to be registered used when a
	 * schema ID for the shim's domain is requested.
	 * 
	 * @return The instance of the {@link Shim} that will be used to fulfill
	 *         requests for its domain.
	 */
	public abstract Shim getShim();
	
	/**
	 * Returns the list of domains known to the registry.
	 * 
	 * @return The list of domains known to the registry.
	 */
	public static Set<String> getDomains() {
		return Collections.unmodifiableSet(REGISTRY.keySet());
	}
	
	/**
	 * Returns whether or not this registry has a shim registered for the given
	 * domain.
	 * 
	 * @param domain
	 *        The domain in question.
	 * 
	 * @return True if this registry has a shim that can handle the given
	 *         domain; false, otherwise.
	 */
	public static boolean hasDomain(final String domain) {
		return REGISTRY.containsKey(domain);
	}
	
	/**
	 * Returns the Shim for the given domain.
	 * 
	 * @param domain
	 *        The domain in question.
	 * 
	 * @return The Shim that can handle payload IDs for a given domain.
	 * 
	 * @throws IllegalArgumentException
	 *         The domain is null.
	 * 
	 * @throws IllegalStateException
	 *         The domain is unknown.
	 */
	public static Shim getShim(
		final String domain)
		throws IllegalArgumentException, IllegalStateException {
		
		// Validate the parameter.
		if(domain == null) {
			throw new IllegalArgumentException("The domain is null.");
		}
		
		// Attempt to retrieve the Shim object.
		Shim result = REGISTRY.get(domain);
		if(result == null) {
			throw new IllegalStateException("The domain is unknown.");
		}
		
		// Return the Shim object
		return result;
	}
	
	/**
	 * Adds a new {@link Shim} to the registry.
	 * 
	 * @param shim
	 *        The shim to add to the registry.
	 * 
	 * @throws IllegalArgumentException
	 *         The shim is null.
	 * 
	 * @throws IllegalStateException
	 *         The shim returned an invalid domain.
	 */
	private static synchronized void add(
		final Shim shim)
		throws IllegalArgumentException, IllegalStateException {

		if(shim == null) {
			throw
				new IllegalArgumentException(
					"A shim attempted to register itself with a null " +
						"instance of itself.");
		}
		
		String domain = shim.getDomain();
		if(domain == null) {
			throw new IllegalStateException("The shim ID is null.");
		}
		if(domain.trim().length() == 0) {
			throw new IllegalStateException("The shim ID is empty.");
		}
		
		REGISTRY.put(domain, shim);
	}
}