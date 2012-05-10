package org.apache.directory.server.hub.api.registry;


import java.util.Hashtable;

import org.apache.directory.server.hub.api.meta.DCOperationsManager;



public class PIDHandlerRegistry
{
    private Hashtable<String, DCOperationsManager> handlers = new Hashtable<String, DCOperationsManager>();


    public void setPIDHandler( String pid, DCOperationsManager operationsManager )
    {
        handlers.put( pid, operationsManager );
    }


    public DCOperationsManager getPIDHandler( String pid )
    {
        return handlers.get( pid );
    }
}
