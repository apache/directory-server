package org.apache.directory.server.hub.api.registry;


import java.util.Hashtable;
import java.util.List;

import org.apache.directory.server.hub.api.meta.DCMetadataDescriptor;



public class DCMetadataRegistry
{
    private Hashtable<String, DCMetadataDescriptor> metadatas = new Hashtable<String, DCMetadataDescriptor>();


    public void addMetadataDescriptor( DCMetadataDescriptor meta )
    {
        metadatas.put( meta.getMetadataPID(), meta );
    }


    public void addMetadataDescriptor( List<DCMetadataDescriptor> metas )
    {
        for ( DCMetadataDescriptor meta : metas )
        {
            addMetadataDescriptor( meta );
        }
    }


    public void removeMetadataDescriptor( String metaPID )
    {
        metadatas.remove( metaPID );
    }


    public DCMetadataDescriptor getMetadataDescriptor( String metaPID )
    {
        return metadatas.get( metaPID );
    }
}
