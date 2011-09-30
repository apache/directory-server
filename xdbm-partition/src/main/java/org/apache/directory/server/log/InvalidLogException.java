
package org.apache.directory.server.log;

/** 
 * An exception used when the log content could be invalid.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class InvalidLogException extends Exception
{
    public InvalidLogException() {}

    public InvalidLogException(String s) 
    {
        super(s);
    }

    public InvalidLogException(Throwable cause) 
    {
        super(cause);
    }

    public InvalidLogException(String s, Throwable cause) 
    {
        super(s, cause);
    }

}
