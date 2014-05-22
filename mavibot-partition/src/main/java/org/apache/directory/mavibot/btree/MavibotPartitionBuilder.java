/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.mavibot.btree;


import static org.apache.directory.mavibot.btree.BTreeFactory.createLeaf;
import static org.apache.directory.mavibot.btree.BTreeFactory.createNode;
import static org.apache.directory.mavibot.btree.BTreeFactory.setKey;
import static org.apache.directory.mavibot.btree.BTreeFactory.setValue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.CsnFactory;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schemaloader.JarLdifSchemaLoader;
import org.apache.directory.api.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.DateUtils;
import org.apache.directory.mavibot.btree.serializer.LongSerializer;
import org.apache.directory.mavibot.btree.serializer.StringSerializer;
import org.apache.directory.mavibot.btree.util.Strings;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.partition.impl.btree.mavibot.MavibotIndex;
import org.apache.directory.server.core.partition.impl.btree.mavibot.MavibotPartition;
import org.apache.directory.server.core.partition.impl.btree.mavibot.MavibotRdnIndex;
import org.apache.directory.server.core.shared.DefaultDnFactory;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.apache.directory.server.xdbm.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A BTree builder that builds a tree from the bottom.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@SuppressWarnings("all")
public class MavibotPartitionBuilder
{
    private int numKeysInNode = BTree.DEFAULT_PAGE_SIZE; // default size

    private Dn suffixDn;

    private String outputDir = "/tmp/builder";

    private RecordManager rm;

    private MavibotPartition partition;

    private SchemaManager schemaManager;

    private CsnFactory csnFactory;

    private RandomAccessFile raf;

    private String ldifFile;

    private String masterTableName = "master";
    
    private List<String> indexAttributes = new ArrayList<String>();
    
    private int totalEntries = 0;
    private static final Logger LOG = LoggerFactory.getLogger( MavibotPartitionBuilder.class );


    public MavibotPartitionBuilder( String ldifFile, String outputDir )
    {
        this( ldifFile, outputDir, BTree.DEFAULT_PAGE_SIZE, 1 );
    }


    public MavibotPartitionBuilder( String ldifFile, String outputDir, int numKeysInNode, int rid )
    {
        this.ldifFile = ldifFile;
        this.outputDir = outputDir;
        this.numKeysInNode = numKeysInNode;
        this.csnFactory = new CsnFactory( rid );
    }


    private BTree build( Iterator<Tuple> sortedTupleItr, String name ) throws Exception
    {
        PersistedBTree btree = ( PersistedBTree ) rm.getManagedTree( name );

        long newRevision = btree.getRevision() + 1;
        btree.setRevision( newRevision );
        
        List<Page> lstLeaves = new ArrayList<Page>();

        int totalTupleCount = 0;

        Page leaf1 = BTreeFactory.createLeaf( btree, newRevision, numKeysInNode );
        lstLeaves.add( leaf1 );

        int leafIndex = 0;

        while ( sortedTupleItr.hasNext() )
        {
            Tuple tuple = sortedTupleItr.next();

            setKey( btree, leaf1, leafIndex, tuple.getKey() );

            Object val = tuple.getValue();
            ValueHolder eh = null;
            
            if( btree.allowDuplicates )
            {
                Set s = ( Set ) val;
                val = s.toArray();
                // to deal with passing an array to varargs param
                eh = new PersistedValueHolder( btree, ( Object[] ) val );
            }
            else
            {
                eh = new PersistedValueHolder( btree, val );
            }

            setValue( btree, leaf1, leafIndex, eh );

            leafIndex++;
            totalTupleCount++;
            if ( ( totalTupleCount % numKeysInNode ) == 0 )
            {
                leafIndex = 0;

                PageHolder pageHolder = ( PageHolder ) rm.writePage( btree, leaf1, newRevision );

                leaf1 = createLeaf( btree, newRevision, numKeysInNode );
                lstLeaves.add( leaf1 );
            }

            //TODO build the whole tree in chunks rather than processing *all* leaves at first
        }

        if ( lstLeaves.isEmpty() )
        {
            return btree;
        }

        // remove null keys and values from the last leaf and resize
        PersistedLeaf lastLeaf = ( PersistedLeaf ) lstLeaves.get( lstLeaves.size() - 1 );
        for ( int i = 0; i < lastLeaf.nbElems; i++ )
        {
            if ( lastLeaf.keys[i] == null )
            {
                int n = i;
                lastLeaf.nbElems = n;
                KeyHolder[] keys = lastLeaf.keys;

                lastLeaf.keys = ( KeyHolder[] ) Array.newInstance( KeyHolder.class, n );
                System.arraycopy( keys, 0, lastLeaf.keys, 0, n );

                ValueHolder[] values = lastLeaf.values;
                lastLeaf.values = ( ValueHolder[] ) Array.newInstance( ValueHolder.class, n );
                System.arraycopy( values, 0, lastLeaf.values, 0, n );

                PageHolder pageHolder = ( PageHolder ) rm.writePage( btree, lastLeaf, newRevision );

                break;
            }
        }

        // make sure either one of the root pages is reclaimed, cause when we call rm.manage()
        // there is already a root page created
        Page rootPage = attachNodes( lstLeaves, btree );

        Page oldRoot = btree.getRootPage();
        
        //System.out.println("built rootpage : " + rootPage);
        btree.setNbElems( totalTupleCount );

        long newRootPageOffset = ( ( AbstractPage ) rootPage ).getOffset();
        System.out.println( "replacing old offset " + btree.getRootPageOffset() + " of the BTree " + name + " with " + newRootPageOffset );
        
        BTreeHeader header = btree.getBtreeHeader();
        
        header.setRootPage( rootPage );
        header.setRevision( btree.getRevision() );
        header.setNbElems( btree.getNbElems() );
        
        long newBtreeHeaderOffset = rm.writeBtreeHeader( btree, header );
        
        // We have a new B-tree header to inject into the B-tree of btrees
        rm.addInBtreeOfBtrees( name, btree.getRevision(), newBtreeHeaderOffset );

        // Store the new revision
        btree.storeRevision( header );

        rm.freePages( ( BTree ) btree, btree.getRevision(), ( List ) Arrays.asList( oldRoot ) );

        return btree;
    }


    @SuppressWarnings("unchecked")
    private Page attachNodes( List<Page> children, BTree btree ) throws IOException
    {
        if ( children.size() == 1 )
        {
            return children.get( 0 );
        }

        List<Page> lstNodes = new ArrayList<Page>();

        int numChildren = numKeysInNode + 1;

        PersistedNode node = ( PersistedNode ) createNode( btree, btree.getRevision(), numKeysInNode );
        lstNodes.add( node );
        int i = 0;
        int totalNodes = 0;

        for ( Page p : children )
        {
            if ( i != 0 )
            {
                setKey( btree, node, i - 1, p.getLeftMostKey() );
            }

            node.children[i] = new PersistedPageHolder( btree, p );

            i++;
            totalNodes++;

            if ( ( totalNodes % numChildren ) == 0 )
            {
                i = 0;

                PageHolder pageHolder = ( PageHolder ) rm.writePage( btree, node, 1 );

                node = ( PersistedNode ) createNode( btree, btree.getRevision(), numKeysInNode );
                lstNodes.add( node );
            }
        }

        // remove null keys and values from the last node and resize
        AbstractPage lastNode = ( AbstractPage ) lstNodes.get( lstNodes.size() - 1 );

        for ( int j = 0; j < lastNode.nbElems; j++ )
        {
            if ( lastNode.keys[j] == null )
            {
                int n = j;
                lastNode.nbElems = n;
                KeyHolder[] keys = lastNode.keys;

                lastNode.keys = ( KeyHolder[] ) Array.newInstance( KeyHolder.class, n );
                System.arraycopy( keys, 0, lastNode.keys, 0, n );

                PageHolder pageHolder = ( PageHolder ) rm.writePage( btree, lastNode, 1 );

                break;
            }
        }

        return attachNodes( lstNodes, btree );
    }


    public void calcLevels()
    {
        int numLevels = 0;

        int numKeysPerPage = 16;

        int totalKeys = 10000000;

        while ( totalKeys > 1 )
        {
            if ( numLevels > 0 )
            {
                // for nodes
                numKeysPerPage += 1;
            }

            int rem = ( totalKeys % numKeysPerPage );

            totalKeys = totalKeys / numKeysPerPage;

            if ( rem != 0 )
            {
                totalKeys += 1;
            }

            System.out.println( "Total keys " + totalKeys );

            numLevels++;
        }

        System.out.println( numLevels );
    }


    private void createPartition() throws Exception
    {
        DnFactory dnFactory = new DefaultDnFactory( schemaManager, null );

        partition = new MavibotPartition( schemaManager, dnFactory );
        partition.setId( "builder" );
        partition.setSuffixDn( suffixDn );

        File dir = new File( outputDir );
        partition.setPartitionPath( dir.toURI() );

        for( String atName : indexAttributes )
        {
            schemaManager.lookupAttributeTypeRegistry( atName );
            partition.addIndex( new MavibotIndex( atName, false ) );
        }
        
        partition.initialize();

        masterTableName = partition.getMasterTable().getName();
        
        rm = partition.getRecordMan();
    }


    private Set<DnTuple> getDnTuples() throws Exception
    {
        File file = new File( ldifFile );

        raf = new RandomAccessFile( file, "r" );

        LdifReader reader = new LdifReader( file );

        Set<DnTuple> sortedDnSet = new TreeSet<DnTuple>();

        while ( reader.hasNext() )
        {
            LdifEntry entry = reader.next();
            entry.getDn().apply( schemaManager );
            DnTuple dt = new DnTuple( entry.getDn(), entry.getOffset(), entry.getLengthBeforeParsing() );
            sortedDnSet.add( dt );
        }

        reader.close();

        if ( sortedDnSet.isEmpty() )
        {
            return Collections.EMPTY_SET;
        }

        Iterator<DnTuple> itr = sortedDnSet.iterator();
        /*
        FileWriter fw = new FileWriter( "/tmp/dntuples.txt" );
        while( itr.hasNext() )
        {
            fw.write( itr.next().getDn().getName() + "\n" );
        }
        
        fw.close();
        itr = sortedDnSet.iterator();
        */
        DnTuple root = itr.next();
        root.setParent( null );

        suffixDn = root.getDn();
        
        System.out.println( "Using " + suffixDn.getName() + " as the partition's root DN" );
        
        Map<String, DnTuple> parentDnIdMap = new HashMap<String, DnTuple>();
        parentDnIdMap.put( root.getDn().getNormName(), root );

        DnTuple prevTuple = root;
        
        while ( itr.hasNext() )
        {
            DnTuple dt = itr.next();

            String parentDn = dt.getDn().getParent().getNormName();
            DnTuple parent = parentDnIdMap.get( parentDn );

            if ( parent == null )
            {
                if ( parentDn.equals( prevTuple.getDn().getNormName() ) )
                {
                    parentDnIdMap.put( prevTuple.getDn().getNormName(), prevTuple );
                    parent = prevTuple;
                }
                else
                {
                    throw new IllegalStateException( "Parent entry's ID of the entry " + dt.getDn().getName()
                        + " not found." );
                }
            }
            else
            {
                // load certain siblings
                if( !dt.getDn().isDescendantOf( prevTuple.getDn() ) )
                {
                    //System.out.println( "adding dn " + prevTuple.getDn().getName() + " to the map");
                    parentDnIdMap.put( prevTuple.getDn().getNormName(), root );
                }
            }
            
            dt.setParent( parent );
            parent.addChild();
            parent.addDecendent();
            
            prevTuple = dt;
        }

//        for ( DnTuple dt : sortedDnSet )
//        {
//            System.out.println( dt );
//        }

        return sortedDnSet;
    }


    private void buildMasterTable( Set<DnTuple> sortedDnSet ) throws Exception
    {
        final Set<DnTuple> idSortedSet = new TreeSet<DnTuple>( new Comparator<DnTuple>()
        {

            @Override
            public int compare( DnTuple dt0, DnTuple dt1 )
            {
                return dt0.getId().compareTo( dt1.getId() );
            }
        } );

        idSortedSet.addAll( sortedDnSet );

//        System.out.println( "Sorted on ID" );
//        for ( DnTuple dt : idSortedSet )
//        {
//            System.out.println( dt );
//        }

        Iterator<Tuple> entryItr = new Iterator<Tuple>()
        {

            private Iterator<DnTuple> itr = idSortedSet.iterator();


            @Override
            public boolean hasNext()
            {
                return itr.hasNext();
            }


            @Override
            public Tuple<String, Entry> next()
            {
                DnTuple dt = itr.next();
                Tuple t = new Tuple();
                t.setKey( dt.getId() );

                try
                {
                    byte[] data = new byte[dt.getLen()];
                    raf.seek( dt.getOffset() );
                    raf.readFully( data, 0, data.length );

                    LdifReader lar = new LdifReader();

                    Entry entry = lar.parseLdif( Strings.utf8ToString( data ) ).get( 0 ).getEntry();

                    // make it schema aware
                    entry = new DefaultEntry( schemaManager, entry );
                    entry.add( SchemaConstants.ENTRY_UUID_AT, dt.getId() );
                    entry.add( SchemaConstants.ENTRY_PARENT_ID_AT, dt.getParentId() );
                    entry.add( SchemaConstants.ENTRY_CSN_AT, csnFactory.newInstance().toString() );
                    entry.add( SchemaConstants.CREATORS_NAME_AT, ServerDNConstants.ADMIN_SYSTEM_DN );
                    entry.add( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

                    t.setValue( entry );
                }
                catch ( Exception e )
                {
                    LOG.warn( "Failed to parse the entry for the DnTuple " + dt );
                    throw new RuntimeException( e );
                }

                return t;
            }


            @Override
            public void remove()
            {
                throw new UnsupportedOperationException( "Not supported" );
            }

        };

        build( entryItr, masterTableName );
    }


    private void buildRdnIndex( Set<DnTuple> sortedDnSet ) throws Exception
    {
        final Set<DnTuple> parentIdRdnSortedSet = new TreeSet<DnTuple>( new Comparator<DnTuple>()
        {

            @Override
            public int compare( DnTuple dt0, DnTuple dt1 )
            {
                int val = dt0.getParentId().compareTo( dt1.getParentId() );
                if ( val != 0 )
                {
                    return val;
                }

                Rdn[] dt0Rdns = dt0.getDn().getRdns().toArray( new Rdn[0] );

                Rdn[] dt1Rdns = dt1.getDn().getRdns().toArray( new Rdn[0] );

                if ( dt0Rdns.length == 1 )
                {
                    // Special case : we only have one rdn.
                    val = dt0Rdns[0].getNormName().compareTo( dt1Rdns[0].getNormName() );

                    return val;
                }
                else
                {
                    for ( int i = 0; i < dt0Rdns.length; i++ )
                    {
                        val = dt0Rdns[i].getNormName().compareTo( dt1Rdns[i].getNormName() );

                        if ( val != 0 )
                        {
                            return val;
                        }
                    }

                    return 0;
                }
            }
        } );

        parentIdRdnSortedSet.addAll( sortedDnSet );

//        System.out.println( "Sorted on ParentID and RDNs" );
//        for ( DnTuple dt : parentIdRdnSortedSet )
//        {
//            System.out.println( dt );
//        }

        Iterator<Tuple> parentIdAndRdnFwdItr = new Iterator<Tuple>()
        {
            Iterator<DnTuple> itr = parentIdRdnSortedSet.iterator();


            @Override
            public void remove()
            {
            }


            @Override
            public Tuple next()
            {
                DnTuple dt = itr.next();
                Tuple t = new Tuple();

                ParentIdAndRdn rdn = new ParentIdAndRdn( dt.getParentId(), dt.getDn().getRdns() );
                rdn.setNbChildren( dt.getNbChildren() );
                rdn.setNbDescendants( dt.getNbDecendents() );
                
                t.setKey( rdn );
                t.setValue( dt.getId() );

                return t;
            }


            @Override
            public boolean hasNext()
            {
                return itr.hasNext();
            }
        };

        String forwardRdnTree = ApacheSchemaConstants.APACHE_RDN_AT_OID + MavibotRdnIndex.FORWARD_BTREE;

        build( parentIdAndRdnFwdItr, forwardRdnTree );

        Iterator<Tuple> parentIdAndRdnRevItr = new Iterator<Tuple>()
        {
            Iterator<DnTuple> itr = parentIdRdnSortedSet.iterator();


            @Override
            public void remove()
            {
            }


            @Override
            public Tuple next()
            {
                DnTuple dt = itr.next();
                Tuple t = new Tuple();

                ParentIdAndRdn rdn = new ParentIdAndRdn( dt.getParentId(), dt.getDn().getRdns() );
                rdn.setNbChildren( dt.getNbChildren() );
                rdn.setNbDescendants( dt.getNbDecendents() );
                
                t.setKey( dt.getId() );
                t.setValue( rdn );

                return t;
            }


            @Override
            public boolean hasNext()
            {
                return itr.hasNext();
            }
        };

        String revRdnTree = ApacheSchemaConstants.APACHE_RDN_AT_OID + MavibotRdnIndex.REVERSE_BTREE;

        build( parentIdAndRdnRevItr, revRdnTree );
    }


    public void buildPartition()
    {
        try
        {
            System.out.println( "Loading schema using JarLdifSchemaLoader" );
            JarLdifSchemaLoader loader = new JarLdifSchemaLoader();
            schemaManager = new DefaultSchemaManager( loader );
            schemaManager.loadAllEnabled();
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to initialize the schema manager", e );
            return;
        }

        Set<DnTuple> sortedDnSet = null;
        try
        {
            long sortT0 = System.currentTimeMillis();
            System.out.println( "Sorting the LDIF data..." );
            
            sortedDnSet = getDnTuples();
            long sortT1 = System.currentTimeMillis();

            totalEntries = sortedDnSet.size();
            
            System.out.println( "Completed sorting, total number of entries " + totalEntries + 
                ", time taken : " + ( sortT1 - sortT0 ) + "ms" );
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to parse the given LDIF file ", e );
            e.printStackTrace();
        }
        
        if ( ( sortedDnSet == null ) || ( sortedDnSet.isEmpty() ) )
        {
            String message = "No entries found in the given LDIF file, aborting bulk load";
            System.out.println( message );
            LOG.info( message );
        }
        
        try
        {
            long partT0 = System.currentTimeMillis();
            System.out.print( "Creating partition..." );
            createPartition();
            long partT1 = System.currentTimeMillis();
            System.out.println( ", time taken : " + ( partT1 - partT0 ) + "ms" );
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to initialize the partition", e );
            return;
        }

        try
        {
            long masterT0 = System.currentTimeMillis();
            System.out.print( "Building master table..." );
            buildMasterTable( sortedDnSet );
            long masterT1 = System.currentTimeMillis();
            System.out.println( ", time taken : " + ( masterT1 - masterT0 ) + "ms" );
        }
        catch( Exception e )
        {
            LOG.warn( "Failed to build master table", e );
            return;
        }
        
        try
        {
            long rdnT0 = System.currentTimeMillis();
            System.out.print( "Building RDN index." );
            buildRdnIndex( sortedDnSet );
            long rdnT1 = System.currentTimeMillis();
            System.out.println( ", time taken : " + ( rdnT1 - rdnT0 ) + "ms" );
        }
        catch( Exception e )
        {
            LOG.warn( "Failed to build the RDN index", e );
            return;
        }
        
        // not needed anymore
        System.out.println( "Clearing the sorted DN set." );
        sortedDnSet.clear();
        
        for( Index<?, String> id : partition.getAllIndices() )
        {
            // RDN and presence indices are built separately
            String oid = id.getAttribute().getOid();
            
            if( ApacheSchemaConstants.APACHE_RDN_AT_OID.equals( oid ) 
                || ApacheSchemaConstants.APACHE_PRESENCE_AT_OID.equals( oid ) )
            {
                continue;
            }
            
            String ignoreVal = null;
            
            if( SchemaConstants.OBJECT_CLASS_AT_OID.equals( oid ) )
            {
                // should be a normalized val
                ignoreVal = "top";
            }
            
            try
            {
                long indexT0 = System.currentTimeMillis();
                System.out.print("Building index " + id.getAttribute().getName() );
                buildIndex( id, ignoreVal );
                long indexT1 = System.currentTimeMillis();
                System.out.println( ", time taken : " + ( indexT1 - indexT0 ) + "ms" );
            }
            catch( Exception e )
            {
                LOG.warn( "Failed to build the index " + id.getAttribute().getName() );
                LOG.warn( "", e );
                return;
            }
        }
        
        try
        {
            System.out.print( "Building presence index..." );
            long presenceT0 = System.currentTimeMillis();
            buildPresenceIndex();
            long presenceT1 = System.currentTimeMillis();
            System.out.println( ", time taken : " + ( presenceT1 - presenceT0 ) + "ms" );
        }
        catch( Exception e )
        {
            LOG.warn( "Failed to build the presence index." );
            LOG.warn( "", e );
            return;
        }
        
        System.out.println( "Patition building complete." );
    }

    
    private void buildPresenceIndex() throws Exception
    {
        Set<String> idxOids = new HashSet<String>();
        Iterator<String> itr = partition.getUserIndices();
        
        while( itr.hasNext() )
        {
            idxOids.add( itr.next() );
        }

        BTree masterTree = rm.getManagedTree( masterTableName );

        BTree fwdTree = rm.getManagedTree( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID + MavibotIndex.FORWARD_BTREE );
        boolean fwdDupsAllowed = fwdTree.isAllowDuplicates();
        
        Comparator fwdKeyComparator = fwdTree.getKeySerializer().getComparator();
        
        final Map<String, Set> fwdMap = new TreeMap<String, Set>();

        TupleCursor<String, Entry> cursor = masterTree.browse();
        
        while ( cursor.hasNext() )
        {
            Tuple<String, Entry> t = cursor.next();
            
            Entry e = t.getValue();
            
            for( String oid : idxOids )
            {
                Attribute at = e.get( oid );
                if( at == null )
                {
                    continue;
                }
                
                Set<String> idSet = fwdMap.get( oid );
                
                if( idSet == null )
                {
                    idSet = new TreeSet<String>();
                    idSet.add( t.getKey() );

                    fwdMap.put( oid, idSet );
                }
            }
        }
        
        cursor.close();
        
        Iterator<Tuple> tupleItr = new Iterator<Tuple>()
        {
            Iterator<java.util.Map.Entry<String, Set>> itr = fwdMap.entrySet().iterator();
            
            @Override
            public Tuple next()
            {
                java.util.Map.Entry<String, Set> e = itr.next();
                
                Tuple t = new Tuple();
                t.setKey( e.getKey() );
                t.setValue( e.getValue() );
                
                return t;
            }
            
            
            @Override
            public boolean hasNext()
            {
                return itr.hasNext();
            }
            
            @Override
            public void remove()
            {
            }
        };
        
        build( tupleItr, fwdTree.getName() );
    }
    
    
    private void buildIndex( Index<?, String> idx, String ignoreVal ) throws Exception
    {
        BTree masterTree = rm.getManagedTree( masterTableName );
        
        AttributeType type = idx.getAttribute();
        boolean isBinary = type.getSyntax().isHumanReadable();
        boolean singleValued = type.isSingleValued();
        
        BTree fwdTree = rm.getManagedTree( type.getOid() + MavibotIndex.FORWARD_BTREE );
        boolean fwdDupsAllowed = fwdTree.isAllowDuplicates();
        
        Comparator fwdKeyComparator = fwdTree.getKeySerializer().getComparator();
        
        Set<Tuple> fwdSet = new TreeSet<Tuple>( new IndexTupleComparator( fwdKeyComparator ) );
        
        Map fwdMap = new TreeMap( fwdKeyComparator );
        
        BTree revTree = null;
        boolean revDupsAllowed;
        Set<Tuple> revSet = null;
        Map<String,Tuple> revMap = null;
        Comparator revValComparator = null;
        
        if( idx.hasReverse() )
        {
            revTree = rm.getManagedTree( type.getOid() + MavibotIndex.REVERSE_BTREE );
            revDupsAllowed = revTree.isAllowDuplicates();
            Comparator revKeyComparator = revTree.getKeySerializer().getComparator();
            revValComparator = revTree.getValueSerializer().getComparator();
            revSet = new TreeSet<Tuple>( new IndexTupleComparator( revKeyComparator ) );
            revMap = new TreeMap( revKeyComparator );
        }
        
        
        TupleCursor<String, Entry> cursor = masterTree.browse();
        
        while ( cursor.hasNext() )
        {
            Tuple<String, Entry> t = cursor.next();
            
            Entry e = t.getValue();
            Attribute at = e.get( type );
            if( at == null )
            {
                continue;
            }
            
            if( singleValued )
            {
                Value v = at.get();
                Object normVal = v.getNormValue();
                
                if( ignoreVal != null )
                {
                    if( normVal.equals( ignoreVal ) )
                    {
                        continue;
                    }
                }
                
                Tuple fwdTuple = new Tuple( normVal, t.getKey() );
                fwdSet.add( fwdTuple );
                
                if( revTree != null )
                {
                    Tuple revTuple = new Tuple( t.getKey(), v.getNormValue() );
                    revSet.add( revTuple );
                }
            }
            else
            {
                for( Value v : at )
                {
                    Object val = v.getNormValue();
                    
                    if( ignoreVal != null )
                    {
                        if( val.equals( ignoreVal ) )
                        {
                            continue;
                        }
                    }
                    
                    Tuple fwdTuple = ( Tuple ) fwdMap.get( val );
                    
                    if( fwdTuple == null )
                    {
                        Set<String> idSet = new TreeSet<String>();
                        idSet.add( t.getKey() );
                        
                        fwdTuple = new Tuple( val, idSet );
                        fwdMap.put( val, fwdTuple );
                    }
                    else
                    {
                        Set<String> idSet = ( Set<String> ) fwdTuple.getValue();
                        idSet.add( t.getKey() );
                    }
                    
                    if( revTree != null )
                    {
                        Tuple revTuple = revMap.get( t.getKey() );
                        
                        if( revTuple == null )
                        {
                            Set valSet = new TreeSet( revValComparator );
                            valSet.add( val );
                            revTuple = new Tuple( t.getKey(), valSet );
                        }
                        else
                        {
                            Set valSet = ( Set ) revTuple.getValue();
                            valSet.add( val );
                        }
                    }
                }
            }
        }
        
        cursor.close();
        
        if( singleValued )
        {
            if( fwdSet.isEmpty() )
            {
                return;
            }
            
            build( fwdSet.iterator(), fwdTree.getName() );
            
            if( revTree != null )
            {
                build( revSet.iterator(), revTree.getName() );
            }
        }
        else
        {
            if( fwdMap.isEmpty() )
            {
                return;
            }
            
            build( fwdMap.values().iterator(), fwdTree.getName() );

            if( revTree != null )
            {
                build( revMap.values().iterator(), revTree.getName() );
            }
        }
    }
    
    
    public void testBTree( String name )
    {
        try
        {
            BTree tree = partition.getRecordMan().getManagedTree( name );
            TupleCursor cursor = tree.browse();
            while ( cursor.hasNext() )
            {
                System.out.println( cursor.next() );
            }
            cursor.close();
            
            System.out.println( "The number of elements in the btree " + name + " " + tree.getNbElems() );
//            Index idx = partition.getRdnIndex();
//            org.apache.directory.api.ldap.model.cursor.Cursor idxCur = idx.forwardCursor();
//            while( idxCur.next() )
//            {
//                System.out.println( idxCur.get() );
//            }
//            
//            idxCur.close();
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
    
    public void testPartition() throws Exception
    {
        partition.sync();
        partition.destroy();
        
        createPartition();
        
        testBTree( masterTableName );
        
        SearchRequest req = new SearchRequestImpl();
        req.setBase( suffixDn );
        
        ExprNode filter = new PresenceNode( schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT ) );
        
        req.setFilter( filter );
        req.setScope( SearchScope.SUBTREE );
        
        SearchOperationContext searchCtx = new SearchOperationContext( null, req );
        
        EntryFilteringCursor cur = partition.search( searchCtx );
        
        int count = 0;
        while( cur.next() )
        {
            count++;
            System.out.println( cur.get() );
        }
        
        cur.close();
        
        System.out.println( "Total search results " + count );
    }

    
    /** no qualifier */ int getTotalEntries()
    {
        return totalEntries;
    }


    /** no qualifier */ int getNumKeysInNode()
    {
        return numKeysInNode;
    }


    /** no qualifier */ RecordManager getRm()
    {
        return rm;
    }


    /** no qualifier */ MavibotPartition getPartition()
    {
        return partition;
    }


    /** no qualifier */ SchemaManager getSchemaManager()
    {
        return schemaManager;
    }


    /** no qualifier */ String getMasterTableName()
    {
        return masterTableName;
    }


    public static void main( String[] args ) throws Exception
    {
        File outDir = new File( "/tmp/builder" );
        
        if( outDir.exists() )
        {
            FileUtils.deleteDirectory( outDir );
        }
        
        File file = new File( "/Users/dbugger/other-projects/slamd~svn/trunk/slamd/package/slamd/tools/MakeLDIF/30k-users.ldif" );

        //file = new File( "/tmp/builder-test.ldif" );
//        InputStream in = MavibotPartitionBuilder.class.getClassLoader().getResourceAsStream( "builder-test.ldif" );
//        FileUtils.copyInputStreamToFile( in, file );
//        in.close();
        
        MavibotPartitionBuilder builder = new MavibotPartitionBuilder( file.getAbsolutePath(), outDir.getAbsolutePath() );
        
        long start = System.currentTimeMillis();
        
        builder.buildPartition();
        
        long end = System.currentTimeMillis();
        
        System.out.println( "Total time taken " + ( end - start ) + "msec" );
        
        //String fwdRdnTree = ApacheSchemaConstants.APACHE_RDN_AT_OID + MavibotRdnIndex.FORWARD_BTREE;
        //builder.testBTree( fwdRdnTree );
        
        //String revRdnTree = ApacheSchemaConstants.APACHE_RDN_AT_OID + MavibotRdnIndex.REVERSE_BTREE;
        //builder.testBTree( revRdnTree );
        //builder.testPartition();
        
    }
    
    /*
    public static void main( String[] args ) throws Exception
    {
        File outDir = new File( "/tmp/builder" );
        
        if( outDir.exists() )
        {
            FileUtils.deleteDirectory( outDir );
        }
        
        
        File file = new File( "/tmp/30k.ldif" );

        MavibotPartitionBuilder builder = new MavibotPartitionBuilder( file.getAbsolutePath(), outDir.getAbsolutePath() );
        
        builder.buildPartition();
    }
    */
}
