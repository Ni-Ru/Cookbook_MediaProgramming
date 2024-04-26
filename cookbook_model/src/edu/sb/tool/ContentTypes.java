package edu.sb.tool;


/**
 * Facade for content type (MIME type) related operations.
 */
@Copyright(year = 2023, holders = "Sascha Baumeister")
public class ContentTypes {

	/**
	 * Prevents external instantiation.
	 */
	private ContentTypes () {}


	/**
	 * Returns whether or not the given content type is compatible to at least one of the given
	 * acceptable types.
	 * @param contentType the content type
	 * @param acceptHeader the accept header value
	 * @return true if the given content type is compatible to at least one of the types
	 * 		contained within the given accept header
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	static public boolean isAcceptable (final String contentType, final String acceptHeader) throws NullPointerException {
		for (String acceptableType : acceptHeader.trim().split("\\s*,\\s*")) {
			acceptableType = acceptableType.trim();
			if (acceptableType.contains(";")) acceptableType = acceptableType.split("\\s*;\\s*")[0];

			if (acceptableType.equals(contentType) || acceptableType.equals("*/*")) return true;
			if (acceptableType.length() < 2) continue;

			if (acceptableType.startsWith("*/") && contentType.endsWith(acceptableType.substring(1))) return true;
			if (acceptableType.endsWith("/*") && contentType.startsWith(acceptableType.substring(0, acceptableType.length() - 1))) return true;
		}

		return false;
	}
}
