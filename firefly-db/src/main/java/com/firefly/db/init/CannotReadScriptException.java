package com.firefly.db.init;

import com.firefly.utils.io.EncodedResource;

/**
 * Thrown by {@link ScriptUtils} if an SQL script cannot be read.
 */
@SuppressWarnings("serial")
public class CannotReadScriptException extends ScriptException {

	/**
	 * Construct a new {@code CannotReadScriptException}.
	 * 
	 * @param resource
	 *            the resource that cannot be read from
	 * @param cause
	 *            the underlying cause of the resource access failure
	 */
	public CannotReadScriptException(EncodedResource resource, Throwable cause) {
		super("Cannot read SQL script from " + resource, cause);
	}

}
