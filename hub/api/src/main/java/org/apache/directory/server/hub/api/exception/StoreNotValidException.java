package org.apache.directory.server.hub.api.exception;


public class StoreNotValidException extends Exception
{

    public StoreNotValidException( String msg )
    {
        super( msg );
    }


    public StoreNotValidException( String msg, Throwable cause )
    {
        super( msg, cause );
    }
}
