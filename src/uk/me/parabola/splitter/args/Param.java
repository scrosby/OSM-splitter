package uk.me.parabola.splitter.args;

/**
 * A single command line parameter.
 *
 * @author Chris Miller
 */
public class Param {
	private final String name;
	private final String description;
	private final String defaultValue;
	private final Class<?> returnType;

	public Param(String name, String description, String defaultValue, Class<?> returnType) {
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
		this.returnType = returnType;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public Class<?> getReturnType() {
		return returnType;
	}
}
