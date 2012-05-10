package org.apache.directory.server.hub.api.exception;


@SuppressWarnings("serial")
public class ComponentInstantiationException extends Exception
{
    public ComponentInstantiationException( String msg, Throwable cause )
    {
        super( msg, cause );
    }
}
