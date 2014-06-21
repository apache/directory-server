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
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
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

    //private MavibotPartition partition;

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
        List<Page> lstNodes = new ArrayList<Page>();

        int totalLeaves = 1;
        int totalTuples = 0;
        
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
            totalTuples++;
            
            if ( leafIndex == numKeysInNode )
            {
                leafIndex = 0;
                
                PageHolder pageHolder = ( PageHolder ) rm.writePage( btree, leaf1, newRevision );

                if( ( totalLeaves % ( numKeysInNode + 1 ) ) == 0 )
                {
                    //System.out.println( "Processed tuples " + totalTuples );
                    cleanLastLeaf( lstLeaves, btree, newRevision );
                    if( !lstLeaves.isEmpty() )
                    {
                        Page node = attachNodes( lstLeaves, btree );
                        lstNodes.add( node );
                        lstLeaves.clear();
                    }
                }

                ( ( PersistedLeaf ) leaf1 )._clearValues_();
                
                leaf1 = createLeaf( btree, newRevision, numKeysInNode );
                totalLeaves++;
                lstLeaves.add( leaf1 );
            }
        }

        if( !lstLeaves.isEmpty() )
        {
            cleanLastLeaf( lstLeaves, btree, newRevision );
            if( !lstLeaves.isEmpty() )
            {
                Page node = attachNodes( lstLeaves, btree );
                lstNodes.add( node );
                lstLeaves.clear();
            }
        }
        
        if ( lstNodes.isEmpty() )
        {
            return btree;
        }
        
        // make sure either one of the root pages is reclaimed, cause when we call rm.manage()
        // there is already a root page created
        Page rootPage = attachNodes( lstNodes, btree );
        lstNodes.clear();
        
        Page oldRoot = btree.getRootPage();
        
        //System.out.println("built rootpage : " + rootPage);
        btree.setNbElems( totalTuples );

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
        btree.storeRevision( header, rm.isKeepRevisions() );

        rm.freePages( ( BTree ) btree, btree.getRevision(), ( List ) Arrays.asList( oldRoot ) );

        return btree;
    }

    
    private void cleanLastLeaf( List<Page> lstLeaves, BTree btree, long newRevision ) throws IOException
    {
        if( lstLeaves.isEmpty() )
        {
            return;
        }
        
        // remove null keys and values from the last leaf and resize
        PersistedLeaf lastLeaf = ( PersistedLeaf ) lstLeaves.get( lstLeaves.size() - 1 );
        
        if ( lastLeaf.keys[0] == null )
        {
            lstLeaves.remove( lastLeaf );
            //System.out.println( "removed last leaf" );
            return;
        }
        
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
        int attachedChildren = 0;

        for ( Page p : children )
        {
            if ( i != 0 )
            {
                setKey( btree, node, i - 1, p.getLeftMostKey() );
            }

            node.children[i] = new PersistedPageHolder( btree, p );

            i++;
            attachedChildren++;

            if ( ( attachedChildren % numChildren ) == 0 )
            {
                PageHolder pageHolder = ( PageHolder ) rm.writePage( btree, node, 1 );

                if( children.size() == attachedChildren )
                {
                    break;
                }

                i = 0;
                
                node = ( PersistedNode ) createNode( btree, btree.getRevision(), numKeysInNode );
                lstNodes.add( node );
            }
        }

        // remove null keys and values from the last node and resize
        AbstractPage lastNode = ( AbstractPage ) lstNodes.get( lstNodes.size() - 1 );

        if ( lastNode.keys[0] == null )
        {
            lstNodes.remove( lastNode );
            //System.out.println( "removed last node" );
            return attachNodes( lstNodes, btree );
        }

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


    private static void calcLevels( int totalKeys, int numKeysPerPage )
    {
        int numLevels = 0;

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

            if ( numLevels == 0 )
            {
                System.out.println( "Total Leaves " + totalKeys );
            }
            else
            {
                System.out.println( "Total Nodes " + totalKeys );
            }

            numLevels++;
        }

        System.out.println( numLevels );
    }


    private Set<DnTuple> getDnTuples() throws Exception
    {
        File file = new File( ldifFile );

        raf = new RandomAccessFile( file, "r" );

        FastLdifReader reader = new FastLdifReader( file );

        Set<DnTuple> sortedDnSet = new TreeSet<DnTuple>();

        while ( reader.hasNext() )
        {
            // FastLdifReader will always return NULL LdifEntry
            // call getDnTuple() after next() to get a DnTuple
            LdifEntry entry = reader.next();
            
            DnTuple dt = reader.getDnTuple();
            
            dt.getDn().apply( schemaManager );
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

            final SchemaAwareLdifReader lar = new SchemaAwareLdifReader( schemaManager );

            final AttributeType atEntryUUID = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ENTRY_UUID_AT );
            final AttributeType atEntryParentID = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ENTRY_PARENT_ID_AT );
            final AttributeType atCsn = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ENTRY_CSN_AT );
            final AttributeType atCreator = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.CREATORS_NAME_AT );
            final AttributeType atCreatedTime = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.CREATE_TIMESTAMP_AT );
            
            final Attribute creatorsName = new DefaultAttribute( atCreator, ServerDNConstants.ADMIN_SYSTEM_DN );
            final Attribute createdTime = new DefaultAttribute( atCreatedTime, DateUtils.getGeneralizedTime() );
            final Attribute entryCsn = new DefaultAttribute( atCsn, csnFactory.newInstance().toString() );
            
            final Tuple t = new Tuple();
            
            @Override
            public boolean hasNext()
            {
                return itr.hasNext();
            }


            @Override
            public Tuple<String, Entry> next()
            {

                DnTuple dt = itr.next();
                t.setKey( dt.getId() );

                try
                {
                    
                    byte[] data = new byte[dt.getLen()];
                    raf.seek( dt.getOffset() );
                    raf.readFully( data, 0, data.length );

                    Entry entry = lar.parseLdifEntry( Strings.utf8ToString( data ) ).getEntry();

                    entry.add( atEntryUUID, dt.getId() );
                    entry.add( atEntryParentID, dt.getParentId() );
                    entry.add( entryCsn );
                    entry.add( creatorsName );
                    entry.add( createdTime );

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
            return;
        }
        
        if ( ( sortedDnSet == null ) || ( sortedDnSet.isEmpty() ) )
        {
            String message = "No entries found in the given LDIF file, aborting bulk load";
            System.out.println( message );
            LOG.info( message );
        }
        
        MavibotPartition partition = null;
        try
        {
            long partT0 = System.currentTimeMillis();
            System.out.print( "Creating partition..." );
            
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
            e.printStackTrace();
            return;
        }
        
        Iterator<String> userIndexItr = partition.getUserIndices();
        
        try
        {
            // the RecordManager must be re-initialized cause we are
            // setting the "values" of leaves to null while building
            // the tree to avoid OOM errors
            partition.destroy();
            
            rm = new RecordManager( new File( partition.getPartitionPath() ).getAbsolutePath() );
            
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
                e.printStackTrace();
                LOG.warn( "Failed to build the index " + id.getAttribute().getName() );
                LOG.warn( "", e );
                return;
            }
        }
        
        try
        {
            System.out.print( "Building presence index..." );
            long presenceT0 = System.currentTimeMillis();
            buildPresenceIndex( userIndexItr );
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

    
    private void buildPresenceIndex( Iterator<String> itr ) throws Exception
    {
        Set<String> idxOids = new HashSet<String>();
        
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
            BTree tree = rm.getManagedTree( name );
            TupleCursor cursor = tree.browse();
            long fetched = 0;
            while ( cursor.hasNext() )
            {
                fetched++;
                Tuple t = cursor.next();
                //System.out.println( t );
            }
            cursor.close();
            
            if( fetched != tree.getNbElems() )
            {
                System.err.println( "The number of elements fetched from the btree did not match with the stored count " + name + " ( fetched = " + fetched + ", stored count = " + tree.getNbElems() + " )" );
            }
            else
            {
                System.out.println( "The number of elements in the btree " + name + " " + fetched );
            }
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


    /** no qualifier */ SchemaManager getSchemaManager()
    {
        return schemaManager;
    }


    /** no qualifier */ String getMasterTableName()
    {
        return masterTableName;
    }


    public static void help()
    {
        System.out.println( "Usage" );
        System.out.println( "java -jar bulkloader.jar <options>" );
        System.out.println( "Available options are:" );
        
        Option[] options = Option.values();
        
        for( Option o : options )
        {
            if( o == Option.UNKNOWN )
            {
                continue;
            }
            
            System.out.println( o.getText() + "    " + o.getDesc() );
        }
    }
    
    private static String getArgAt( int position, Option opt, String[] args )
    {
        if( position >= args.length )
        {
            System.out.println( "No value was provided for the option " + opt.getText() );
            System.exit( 1 );
        }
        
        return args[position];
    }
    
    public static void main( String[] args ) throws Exception
    {
        String inFile = null;
        String outDirPath = null;
        int numKeysInNode = 16;
        int rid = 1;
        boolean cleanOutDir = false;
        boolean verifyMasterTable = false;

        if ( args.length < 2 )
        {
           help();
           System.exit( 0 );
        }
        
        for( int i =0; i < args.length; i++ )
        {
            Option opt = Option.getOpt( args[i] );
            
            switch( opt )
            {
                case HELP:
                    help();
                    System.exit( 0 );
                    break;
                    
                case INPUT_FILE:
                    inFile = getArgAt( ++i, opt, args );
                    break;

                case OUT_DIR:
                    outDirPath = getArgAt( ++i, opt, args );
                    break;

                case CLEAN_OUT_DIR:
                    cleanOutDir = true;
                    break;

                case VERIFY_MASTER_TABLE:
                    verifyMasterTable = true;
                    break;

                case NUM_KEYS_PER_NODE:
                    numKeysInNode = Integer.parseInt( getArgAt( ++i, opt, args ) );
                    break;

                case DS_RID:
                    rid = Integer.parseInt( getArgAt( ++i, opt, args ) );
                    break;

                case UNKNOWN:
                    System.out.println( "Unknown option " + args[i] );
                    continue;
            }
        }
        
        if( ( inFile == null ) || ( inFile.trim().length() == 0 ) )
        {
            System.out.println( "Invalid input file" );
            return;
        }
        
        if( !new File( inFile ).exists() )
        {
            System.out.println( "The input file " + inFile + " doesn't exist" );
            return;
        }
        
        //calcLevels( 502, 16 );
        
        File outDir = new File( outDirPath );
        
        if( outDir.exists() )
        {
            if( !cleanOutDir )
            {
                System.out.println( "The output directory is not empty, pass " + Option.CLEAN_OUT_DIR.getText() + " to force delete the contents or specify a different directory"  );
                return;
            }
            
            FileUtils.deleteDirectory( outDir );
        }
        
        MavibotPartitionBuilder builder = new MavibotPartitionBuilder( inFile, outDirPath, numKeysInNode, rid );
        
        long start = System.currentTimeMillis();
        
        builder.buildPartition();
        
        long end = System.currentTimeMillis();
        
        System.out.println( "Total time taken " + ( end - start ) + "msec" );
        
        if ( verifyMasterTable )
        {
            System.out.println( "Verifying the contents of master table" );
            builder.testBTree( "master" );
        }
    }
}
