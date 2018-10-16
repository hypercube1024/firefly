package com.firefly.db.init;

import com.firefly.utils.io.EncodedResource;

/**
 * Thrown by {@link ScriptUtils} if an SQL script cannot be properly parsed.
 */
@SuppressWarnings("serial")
public class ScriptParseException extends ScriptException {

    /**
     * Construct a new {@code ScriptParseException}.
     *
     * @param message  detailed message
     * @param resource the resource from which the SQL script was read
     */
    public ScriptParseException(String message, EncodedResource resource) {
        super(buildMessage(message, resource));
    }

    /**
     * Construct a new {@code ScriptParseException}.
     *
     * @param message  detailed message
     * @param resource the resource from which the SQL script was read
     * @param cause    the underlying cause of the failure
     */
    public ScriptParseException(String message, EncodedResource resource, Throwable cause) {
        super(buildMessage(message, resource), cause);
    }

    private static String buildMessage(String message, EncodedResource resource) {
        return String.format("Failed to parse SQL script from resource [%s]: %s", (resource == null ? "<unknown>"
                : resource), message);
    }

}
