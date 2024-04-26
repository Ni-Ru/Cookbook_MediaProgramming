package edu.sb.tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Facade providing JSON parse and stringify (marshal and unmarshal) operations.
 * This class's design is equivalent to JavaScript's JSON singleton.  
 */
@Copyright(year = 2021, holders = "Sascha Baumeister")
public class JSON {

	/**
	 * Prevents external instantiation.
	 */
	private JSON () {}


	/**
	 * Returns the JSON representation for the given object.
	 * @param object the object, or {@code null}
	 * @return the JSON representation
	 * @throws IllegalArgumentException if the given argument, or any of it's constituents, is neither
	 *         {@code null} nor an instance of {@code Boolean}, {@code Number}, {@code CharSequence},
	 *         {@code Map} with {@code CharSequence} keys, or {@code Collection}, nor an {@code array}
	 */
	static public String stringify (final Object object) throws IllegalArgumentException {
		if (object == null) return "null";
		if (object instanceof Boolean | object instanceof Number) return object.toString();
		if (object instanceof CharSequence) return "'" + object.toString().replace("'", "\\'").replace("\"", "\\\"").replace("\t", "\\t").replace("\n", "\\n") + "'";

		final StringBuilder builder = new StringBuilder();
		if (object instanceof Collection | object.getClass().isArray()) {
			if (object instanceof Collection | object instanceof Object[]) {
				final Object[] array = object instanceof Object[]
					? (Object[]) object
					: ((Collection<?>) object).toArray();

				builder.append("[");
				for (int index = 0; index < array.length; ++index) {
					if (index > 0) builder.append(", ");
					builder.append(stringify(array[index]));
				}
				builder.append("]");
			} else if (object instanceof char[]) {
				final char[] array = (char[]) object;
				builder.append("[");
				for (int index = 0; index < array.length; ++index) {
					if (index > 0) builder.append(", ");
					builder.append('"');
					builder.append(array[index]);
					builder.append('"');
				}
				builder.append("]");
			} else if (object instanceof byte[]) {
				builder.append(Arrays.toString((byte[]) object));
			} else if (object instanceof short[]) {
				builder.append(Arrays.toString((short[]) object));
			} else if (object instanceof int[]) {
				builder.append(Arrays.toString((int[]) object));
			} else if (object instanceof long[]) {
				builder.append(Arrays.toString((long[]) object));
			} else if (object instanceof float[]) {
				builder.append(Arrays.toString((float[]) object));
			} else if (object instanceof double[]) {
				builder.append(Arrays.toString((double[]) object));
			} else {
				builder.append(Arrays.toString((boolean[]) object));
			}
		} else if (object instanceof Map) {
			builder.append("{");

			final Map<?,?> map = (Map<?,?>) object;
			for (final Object key : map.keySet()) {
				if (key == null | !(key instanceof CharSequence)) throw new IllegalArgumentException();
				builder.append(stringify(key));
				builder.append(": ");
				builder.append(stringify(map.get(key)));
				builder.append(", ");
			}

			if (!map.isEmpty()) builder.delete(builder.length() - 2, builder.length());
			builder.append("}");
		} else {
			throw new IllegalArgumentException();
		}

		return builder.toString();
	}


	/**
	 * Returns an object parsed from the given JSON representation. Depending on the given JSON,
	 * the resulting object is either {@code null}, or a {@code Boolean} value, or a {@code Double}
	 * value, or a {@code String} value, or a {@code List<Object>} value, or a
	 * {@code Map<String,Object>} value.
	 * @param json the JSON representation
	 * @return the object, or {@code null}
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IllegalArgumentException if the argument is not valid JSON
	 * @throws ClassCastException if the result is assigned to something not compatible to,
	 * 			or cast to something different from classes {@code Object}, {@code Boolean},
	 * 			{@code Double}, {@code String}, {@code Map<String,Object>}, or {@code List<Object>}
	 */
	@SuppressWarnings("unchecked")
	static public <T> T parse (String json) throws NullPointerException, IllegalArgumentException, ClassCastException {
		json = json.trim();

		if (json.isEmpty()) throw new IllegalArgumentException(json);
		switch (json.charAt(0)) {
			case 'n': case 'u':
				return (T) parseVoid(json);				
			case 't': case 'f':
				return (T) parseBoolean(json);				
			case '+': case '-': case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9': case '.': case 'E': case 'N': case 'I':   
				return (T) parseNumber(json);
			case '"': case '\'':
				return (T) parseString(json);
			case '[':
				return (T) parseList(json);
			case '{':
				return (T) parseMap(json);
			default:
				throw new IllegalArgumentException(json);
		}
	}


	/**
	 * Converts the given JSON representation into the content of a newly created map.
	 * @param json the JSON representation
	 * @return the map
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IllegalArgumentException if the given JSON does not represent a valid JSON map
	 */
	static private Map<String,Object> parseMap (final String json) throws NullPointerException, IllegalArgumentException {
		if (json.length() < 2 | !json.startsWith("{") | !json.endsWith("}")) throw new IllegalArgumentException(json);

		final Map<String,Object> map = new HashMap<>();
		for (int index = 1; index < json.length(); ++index) {
			while (index < json.length() && Character.isWhitespace(json.charAt(index))) index += 1;
			if (json.charAt(index) == '}') break;
			if (json.charAt(index) != '"' & json.charAt(index) != '\'') throw new IllegalArgumentException(json);
			int startIndex;

			startIndex = index;
			index = matchingDelimiterPosition(json, startIndex) + 1;
			final String key = parseString(json.substring(startIndex, index));

			while (index < json.length() && Character.isWhitespace(json.charAt(index))) index += 1;
			if (json.charAt(index++) != ':') throw new IllegalArgumentException(json);
			while (index < json.length() && Character.isWhitespace(json.charAt(index))) index += 1;

			startIndex = index;
			if (json.charAt(index) == '{' | json.charAt(index) == '[' | json.charAt(index) == '"' | json.charAt(index) == '\'')
				index = matchingDelimiterPosition(json, startIndex) + 1;
			else
				while (index < json.length() && json.charAt(index) != ',' && json.charAt(index) != '}') index += 1;

			final Object value = parse(json.substring(startIndex, index).trim());
			map.put(key, value);

			while (index < json.length() && Character.isWhitespace(json.charAt(index))) index += 1;
			if (index < json.length() && json.charAt(index) != ',' && json.charAt(index) != '}') throw new IllegalArgumentException(json);
		}

		return map;
	}


	/**
	 * Converts the given JSON representation into the content of a newly created list.
	 * @param json the JSON representation
	 * @return the list
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IllegalArgumentException if the given JSON does not represent a valid JSON array
	 */
	static private List<Object> parseList (final String json) throws NullPointerException, IllegalArgumentException {
		if (json.length() < 2 | !json.startsWith("[") | !json.endsWith("]")) throw new IllegalArgumentException(json);

		final List<Object> list = new ArrayList<>();
		for (int index = 1; index < json.length(); ++index) {
			while (index < json.length() && Character.isWhitespace(json.charAt(index))) index += 1;
			if (json.charAt(index) == ']') break;

			final int startIndex = index;
			if (json.charAt(index) == '{' | json.charAt(index) == '[' | json.charAt(index) == '"' | json.charAt(index) == '\'')
				index = matchingDelimiterPosition(json, startIndex) + 1;
			else
				while (index < json.length() && json.charAt(index) != ',' && json.charAt(index) != ']') index += 1;

			final Object element = parse(json.substring(startIndex, index).trim());
			list.add(element);

			while (index < json.length() && Character.isWhitespace(json.charAt(index))) index += 1;
			if (index < json.length() && json.charAt(index) != ',' && json.charAt(index) != ']') throw new IllegalArgumentException(json);
		}

		return list;
	}


	/**
	 * Converts the given JSON representation into the content of a newly created string.
	 * @param json the JSON representation
	 * @return the string
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IllegalArgumentException if the given JSON does not represent a valid JSON string
	 */
	static private String parseString (final String json) throws NullPointerException, IllegalArgumentException {
		if (json.length() < 2 | !((json.startsWith("\"") & json.endsWith("\"")) | (json.startsWith("'") & json.endsWith("'")))) throw new IllegalArgumentException(json);

		return json.substring(1, json.length() - 1).replace("\\'", "'").replace("\\\"", "\"").replace("\\t", "\t").replace("\\n", "\n");
	}


	/**
	 * Converts the given JSON representation into the content of a newly created numeric value.
	 * @param json the JSON representation
	 * @return the numeric value as {@code Double}
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IllegalArgumentException if the given JSON does not represent a valid JSON number
	 */
	static private Double parseNumber (final String json) throws NullPointerException, IllegalArgumentException {
		return Double.parseDouble(json);
	}


	/**
	 * Converts the given JSON representation into the content of a newly created boolean value.
	 * @param json the JSON representation
	 * @return the Boolean value
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IllegalArgumentException if the given JSON does not represent a valid JSON boolean
	 */
	static private Boolean parseBoolean (final String json) throws NullPointerException, IllegalArgumentException {
		if (json.equals("true")) return true;
		if (json.equals("false")) return false;
		throw new IllegalArgumentException(json);
	}


	/**
	 * Converts the given JSON representation into {@code null}.
	 * @param json the JSON representation
	 * @return the {@code null} value
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IllegalArgumentException if the given JSON does not represent a valid JSON void
	 */
	static private Void parseVoid (final String json) throws NullPointerException, IllegalArgumentException {
		if (!json.equals("null") & !json.equals("undefined")) throw new IllegalArgumentException(json);

		return null;
	}


	/**
	 * Returns the position of the stop delimiter matching the start delimiter at the given position.
	 * @param json the JSON representation
	 * @param startPosition the start delimiter position
	 * @return the matching stop delimiter position
	 * @throws NullPointerException if the given JSON argument is {@code null}
	 * @throws IllegalArgumentException if the given start position is out of range,
	 *			or if the given JSON doesn't contain a matching stop delimiter
	 */
	static private int matchingDelimiterPosition (final String json, final int startPosition) throws NullPointerException, IllegalArgumentException {
		if (startPosition < 0 | startPosition >= json.length()) throw new IllegalArgumentException(json);
		final char startDelimiter = json.charAt(startPosition);

		if (startDelimiter == '"' | startDelimiter == '\'') {
			for (int index = startPosition + 1; index < json.length(); ++index) {
				final char character = json.charAt(index);
				if (character == startDelimiter & json.charAt(index - 1) != '\\') return index;
			}
		} else if (startDelimiter == '{' | startDelimiter == '[') {
			for (int index = startPosition + 1; index < json.length(); ++index) {
				final char character = json.charAt(index);
				if ((startDelimiter == '{' & character == '}') | (startDelimiter == '[' & character == ']')) return index;

				if (character == '{' | character == '[' | character == '"' | character == '\'')
					index = matchingDelimiterPosition(json, index);
			}
		}

		throw new IllegalArgumentException(json);
	}
}