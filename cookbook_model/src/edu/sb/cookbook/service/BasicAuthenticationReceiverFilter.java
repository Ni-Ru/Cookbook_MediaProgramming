package edu.sb.cookbook.service;

import java.io.ObjectInputFilter.Status;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

// import java.util.Base64;
import javax.annotation.Priority;
import javax.persistence.EntityManager;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import edu.sb.cookbook.persistence.Person;
import edu.sb.tool.Copyright;
import edu.sb.tool.HashCodes;
import edu.sb.tool.RestJpaLifecycleProvider;



/**
 * JAX-RS filter provider that performs HTTP "basic" authentication on any REST service request. This aspect-oriented
 * design swaps "Authorization" headers for "Requester-Identity" during authentication.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
@Copyright(year = 2017, holders = "Sascha Baumeister")
public class BasicAuthenticationReceiverFilter implements ContainerRequestFilter {

	/**
	 * HTTP request header for the authenticated requester's identity.
	 */
	static public final String REQUESTER_IDENTITY = "X-Requester-Identity";


	/**
	 * Performs HTTP "basic" authentication by calculating a password hash from the password contained in the request's
	 * "Authorization" header, and comparing it to the one stored in the person matching said header's username. The
	 * "Authorization" header is consumed in any case, and upon success replaced by a new "Requester-Identity" header that
	 * contains the authenticated person's identity. The filter chain is aborted in case of a problem.
	 * @param requestContext {@inheritDoc}
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws ClientErrorException (400) if the "Authorization" header is malformed, or if there is a pre-existing
	 *         "Requester-Identity" header
	 */
	public void filter (final ContainerRequestContext requestContext) throws NullPointerException, ClientErrorException {
		final MultivaluedMap<String,String> headers = requestContext.getHeaders();
		// TODO:
		// - Throw a ClientErrorException(Status.BAD_REQUEST) if the given context's headers map already contains a
		//   "Requester-Identity" key, in order to prevent spoofing attacks.
		// - Remove the "Authorization" header from said map and store the first of it's values in a variable
		//   "textCredentials", or null if the header value is either null or empty.
		// - if the "textCredentials" variable is not null, parse it programmatically using Base64.getDecoder().decode(),
		//   use the resulting byte array to create a new String instance, and store the resulting <email>:<password>
		//   combination in variable "credentials". 
		// - Perform the PQL-Query "select p from Person as p where p.email = :email"), using the credentials email
		//   address part. Note that this query will go to the second level cache before hitting the database if the
		//   Person#email field is annotated using @CacheIndex(updateable = true)! 
		// - if the resulting people list contains exactly one element, calculate the hex-string representation
		//   (i.e. 2 digits per byte) of the SHA2-256 hash code of the credential's password part using
		//   HashCodes.sha2HashText(256, text).
		// - if this hash representation is equal to queried person's password hash, add a new "Requester-Identity"
		//   header to the request headers, using the person's identity (converted to String) as value, and return
		//   from this method.
		// - in all other cases, abort the request using requestContext.abortWith() in order to challenging the client
		//   to provide HTTP Basic credentials (i.e. status code 401, and "WWW-Authenticate" header value "Basic").
		//   Note that the alternative of throwing NotAuthorizedException("Basic") comes with the disadvantage that
		//   failed authentication attempts clutter the server log with stack traces.
		if (headers.containsKey(REQUESTER_IDENTITY)) throw new ClientErrorException(Response.Status.BAD_REQUEST);
		final List<String> headerValues = headers.remove(HttpHeaders.AUTHORIZATION);
		if (headerValues != null && !headerValues.isEmpty()) {
			final String textCredentials = headerValues.get(0);
			final String encodedCredentials = textCredentials.substring("Basic ".length());
			final byte[] bytes = Base64.getDecoder().decode(encodedCredentials);
			final String decodedCredentials = new String(bytes, StandardCharsets.ISO_8859_1);
			final int indexOfFirstColon = decodedCredentials.indexOf(":");
			final String email = decodedCredentials.substring(0, indexOfFirstColon);
			final String password = decodedCredentials.substring(indexOfFirstColon + 1);

			final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("secondhand");
			final List<Person> people = entityManager
					.createQuery("SELECT p FROM Person AS p WHERE p.email = :email", Person.class)
					.setParameter("email", email)
					.getResultList();

			if (people.size() == 1) {
				final String passwordHash = HashCodes.sha2HashText(256, password);
				final Person requester = people.get(0);
				if (passwordHash.equals(requester.getPasswordHash())) {
					headers.add(REQUESTER_IDENTITY, Long.toString(requester.getIdentity()));
					return;
				}
			}
		}

		final Response response = Response.status(Response.Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, "Basic").build();
		requestContext.abortWith(response);
	}
}