package org.apache.directory.server.hub.api.exception;


@SuppressWarnings("serial")
public class ComponentReconfigurationException extends Exception
{
    public ComponentReconfigurationException( String msg )
    {
        this( msg, null );
    }


    public ComponentReconfigurationException( String msg, Throwable cause )
    {
        super( msg, cause );
    }
}
