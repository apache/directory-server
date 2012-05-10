package org.apache.directory.server.hub.api.exception;


public class HubStoreException extends Exception
{
    public HubStoreException( String msg )
    {
        this( msg, null );
    }


    public HubStoreException( String msg, Throwable cause )
    {
        super( msg, cause );
    }
}
