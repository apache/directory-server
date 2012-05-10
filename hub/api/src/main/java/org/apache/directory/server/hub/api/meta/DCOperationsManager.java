package org.apache.directory.server.hub.api.meta;

import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.server.hub.api.exception.ComponentInstantiationException;
import org.apache.directory.server.hub.api.exception.ComponentReconfigurationException;




public interface DCOperationsManager
{
    void instantiateComponent( DirectoryComponent component ) throws ComponentInstantiationException;


    void reconfigureComponent( DirectoryComponent component ) throws ComponentReconfigurationException;


    void disposeComponent( DirectoryComponent component );
}
