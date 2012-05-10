package org.apache.directory.server.hub.api.exception;


@SuppressWarnings("serial")
public class HubAbortException extends Exception
{
    public HubAbortException( String msg )
    {
        super( msg );
    }


    public HubAbortException( String msg, Throwable cause )
    {
        super( msg, cause );
    }
}
