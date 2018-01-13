package com.firefly.codec.http2.model.servlet;

import javax.servlet.ServletException;


/* ------------------------------------------------------------ */

/** A ServletException that is logged less verbosely than
 * a normal ServletException.
 * <p>
 * Used for container generated exceptions that need only a message rather
 * than a stack trace.
 * </p>
 */
public class QuietServletException extends ServletException implements QuietException
{
    public QuietServletException()
    {
        super();
    }

    public QuietServletException(String message, Throwable rootCause)
    {
        super(message,rootCause);
    }

    public QuietServletException(String message)
    {
        super(message);
    }

    public QuietServletException(Throwable rootCause)
    {
        super(rootCause);
    }
}
