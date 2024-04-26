package edu.sb.cookbook.server;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import edu.sb.tool.Copyright;


/**
 * This exception mapper maps {@link WebApplicationException} instances to their respective HTTP responses, while all other
 * exception types are mapped to HTTP 500 Internal Server Error. The exceptions are additionally logged, with a log level
 * appropriate for the exception's severity.
 */
@Provider
@Priority(Priorities.USER)
@Copyright(year = 2013, holders = "Sascha Baumeister")
public class RestResponseCodeProvider implements ExceptionMapper<Throwable> {

	/**
	 * Maps the given exception to a HTTP response. In case of a WebApplicationException instance, it's associated response is
	 * returned. Otherwise, a generic HTTP 500 response is returned. The exceptions are logged using a log level that
	 * corresponds to their severity:
	 * <ul>
	 * <li>code 5xx (server side error): Level.WARNING</li>
	 * <li>code 4xx (client side error): Level.INFO</li>
	 * <li>code 3xx (redirection): Level.FINE</li>
	 * <li>code 2xx (success): Level.FINER</li>
	 * <li>code 1xx (informational): Level.FINEST</li>
	 * </ul>
	 * @param exception the exception to be mapped
	 * @return the mapped response
	 * @throws NullPointerException if the given argument is {@code null}
	 */
	public Response toResponse (final Throwable exception) throws NullPointerException {
		final Response response = exception instanceof WebApplicationException ? ((WebApplicationException) exception).getResponse() : Response.status(INTERNAL_SERVER_ERROR).build();
		Logger.getGlobal().log(logLevel(response.getStatusInfo()), exception.getMessage(), exception);

		final Throwable cause = exception instanceof ConstraintViolationException ? exception : exception.getCause();
		if (cause instanceof ConstraintViolationException) {
			final Set<ConstraintViolation<?>> constraintViolations = ((ConstraintViolationException) cause).getConstraintViolations();
			for (final ConstraintViolation<?> constraintViolation : constraintViolations)
				Logger.getGlobal().log(Level.INFO, constraintViolation.toString());
		}

		return response;
	}


	/**
	 * Returns the log level appropriate for the given HTTP response status.
	 * @param status the HTTP response status
	 * @return the log level
	 * @throws NullPointerException if the given status is {@code null}
	 */
	static private Level logLevel (final Response.StatusType status) throws NullPointerException {
		switch (status.getFamily()) {
			case SERVER_ERROR:
				return Level.WARNING;
			case CLIENT_ERROR:
				return Level.INFO;
			case REDIRECTION:
				return Level.FINE;
			case SUCCESSFUL:
				return Level.FINER;
			case INFORMATIONAL:
				return Level.FINEST;
			default:
				throw new AssertionError();
		}
	}
}