package uk.me.parabola.splitter.args;

import java.util.List;

/**
 * Thrown when the command line arguments could not be successfully parsed.
 *
 * @author Chris Miller
 */
public class ParseException extends Exception {
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private final List<String> errors;

	public ParseException(String message, List<String> errors) {
		this(message, null, errors);
	}

	public ParseException(String message, Exception cause, List<String> errors) {
		super(message, cause);
		this.errors = errors;
	}

	public List<String> getErrors() {
		return errors;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(500);
		buf.append(super.toString());
		buf.append(LINE_SEPARATOR);
		for (String error : errors) {
			buf.append(error).append(LINE_SEPARATOR);
		}
		return buf.toString();
	}
}
