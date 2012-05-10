package org.apache.directory.server.hub.api.registry;


import java.util.Hashtable;


public class InjectionRegistry
{
    private Hashtable<String, Object> injections = new Hashtable<String, Object>();

    public void addInjection( String type, Object object )
    {
        injections.put( type, object );
    }


    public void removeInjection( String type )
    {
        injections.remove( type );
    }


    public Object getInjection( String type )
    {
        return injections.get( type );
    }
}
