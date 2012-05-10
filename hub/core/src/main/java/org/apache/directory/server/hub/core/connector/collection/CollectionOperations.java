package org.apache.directory.server.hub.core.connector.collection;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.apache.directory.server.hub.api.component.DCProperty;
import org.apache.directory.server.hub.api.component.DCRuntime;
import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.server.hub.api.component.DirectoryComponentConstants;
import org.apache.directory.server.hub.api.exception.ComponentInstantiationException;
import org.apache.directory.server.hub.api.exception.ComponentReconfigurationException;
import org.apache.directory.server.hub.api.meta.DCOperationsManager;
import org.apache.directory.server.hub.core.connector.collection.CollectionConnector.CollectionType;


public class CollectionOperations implements DCOperationsManager
{

    CollectionType type;
    Class collectionClass;


    public CollectionOperations( CollectionType type )
    {
        this.type = type;
        switch ( type )
        {
            case LIST:
            case ARRAY:
                collectionClass = ArrayList.class;
                break;
            case SET:
                collectionClass = HashSet.class;
                break;
        }
    }


    @Override
    public void instantiateComponent( DirectoryComponent component ) throws ComponentInstantiationException
    {
        Collection collection = null;

        try
        {
            collection = ( Collection ) collectionClass.newInstance();
        }
        catch ( Exception e )
        {
            throw new ComponentInstantiationException( "Collection instantiation failed", e );
        }

        Hashtable<DirectoryComponent, Integer> indexMap = generateIndexMap( component );
        List<DirectoryComponent> sortedList = extractSortedList( indexMap );

        collection.addAll( sortedList );
        Object pojo = ( type != CollectionType.ARRAY ) ? collection : collection.toArray();

        component.setRuntimeInfo( new DCRuntime( null, pojo ) );

    }


    @Override
    public void reconfigureComponent( DirectoryComponent component ) throws ComponentReconfigurationException
    {
        Object collectionObj = component.getRuntimeInfo().getPojo();
        Collection collection;

        if ( type == CollectionType.ARRAY )
        {
            collection = new ArrayList();
        }
        else
        {
            collection = ( Collection ) collectionObj;
        }

        collection.clear();

        Hashtable<DirectoryComponent, Integer> indexMap = generateIndexMap( component );
        List<DirectoryComponent> sortedList = extractSortedList( indexMap );

        collection.addAll( sortedList );

        Object pojo = ( type != CollectionType.ARRAY ) ? collection : collection.toArray();

        component.getRuntimeInfo().setPojo( pojo );
    }


    @Override
    public void disposeComponent( DirectoryComponent component )
    {
        component.setRuntimeInfo( null );
    }


    private Hashtable<DirectoryComponent, Integer> generateIndexMap( DirectoryComponent component )
    {
        Hashtable<DirectoryComponent, Integer> collectionMap = new Hashtable<DirectoryComponent, Integer>();

        for ( DCProperty prop : component.getConfiguration() )
        {
            if ( prop.getName().startsWith( DirectoryComponentConstants.DC_PROP_ITEM_PREFIX ) )
            {
                DirectoryComponent reference = ( DirectoryComponent ) prop.getObject();
                Integer index = reference.getConfiguration().getCollectionIndex();

                collectionMap.put( reference, index );
            }
        }

        return collectionMap;
    }


    private List<DirectoryComponent> extractSortedList( Hashtable<DirectoryComponent, Integer> indexMap )
    {
        List<DirectoryComponent> sortedList = new ArrayList<DirectoryComponent>( indexMap.keySet() );

        if ( type == CollectionType.SET )
        {
            return sortedList;
        }

        Collections.sort( sortedList, new IndexMapSorter( indexMap ) );

        return sortedList;
    }

    private class IndexMapSorter implements Comparator<DirectoryComponent>
    {

        private Hashtable<DirectoryComponent, Integer> indexMap;


        public IndexMapSorter( Hashtable<DirectoryComponent, Integer> indexMap )
        {
            this.indexMap = indexMap;
        }


        @Override
        public int compare( DirectoryComponent o1, DirectoryComponent o2 )
        {
            Integer index1 = indexMap.get( o1 );
            Integer index2 = indexMap.get( o2 );

            index1 = ( index1 != null ) ? index1 : new Integer( 0 );
            index2 = ( index2 != null ) ? index2 : new Integer( 0 );

            return index1.compareTo( index2 );
        }

    }
}
