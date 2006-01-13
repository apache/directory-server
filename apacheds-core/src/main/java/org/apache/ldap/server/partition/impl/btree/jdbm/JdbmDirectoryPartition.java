/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.partition.impl.btree.jdbm;


import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import jdbm.RecordManager;
import jdbm.helper.MRU;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;

import org.apache.ldap.common.exception.LdapNameNotFoundException;
import org.apache.ldap.common.exception.LdapSchemaViolationException;
import org.apache.ldap.common.message.LockableAttributeImpl;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.common.schema.Normalizer;
import org.apache.ldap.common.util.NamespaceTools;
import org.apache.ldap.server.DirectoryServiceConfiguration;
import org.apache.ldap.server.configuration.DirectoryPartitionConfiguration;
import org.apache.ldap.server.partition.DirectoryPartition;
import org.apache.ldap.server.partition.impl.btree.BTreeDirectoryPartition;
import org.apache.ldap.server.partition.impl.btree.Index;
import org.apache.ldap.server.partition.impl.btree.IndexAssertion;
import org.apache.ldap.server.partition.impl.btree.IndexAssertionEnumeration;
import org.apache.ldap.server.partition.impl.btree.IndexNotFoundException;
import org.apache.ldap.server.partition.impl.btree.IndexRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A {@link DirectoryPartition} that stores entries in
 * <a href="http://jdbm.sourceforge.net/">JDBM</a> database.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JdbmDirectoryPartition extends BTreeDirectoryPartition
{
    private static final Logger log = LoggerFactory.getLogger( JdbmDirectoryPartition.class );

    /** the JDBM record manager used by this database */
    private RecordManager recMan;
    /** the user provided suffix of this backend database */
    private Name upSuffix;
    /** the normalized suffix of this backend database */
    private Name normSuffix;
    /** the working directory to use for files */
    private File workingDirectory;
    /** the master table storing entries by primary key */
    private JdbmMasterTable master;
    /** a map of attribute names to user indices */
    private Map indices;
    /** a map of index names to system indices */
    private Map sysIndices;

    /** true if open */
    private boolean initialized;

    /** the normalized distinguished name index */
    private Index ndnIdx;
    /** the user provided distinguished name index */
    private Index updnIdx;
    /** the attribute existance index */
    private Index existanceIdx;
    /** the parent child relationship index */
    private Index hierarchyIdx;
    /** the one level scope alias index */
    private Index oneAliasIdx;
    /** the subtree scope alias index */
    private Index subAliasIdx;
    /** a system index on aliasedObjectName attribute */
    private Index aliasIdx;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a store based on JDBM B+Trees.
     */
    public JdbmDirectoryPartition()
    {
    }

    public synchronized void init( DirectoryServiceConfiguration factoryCfg, DirectoryPartitionConfiguration cfg ) throws NamingException
    {
        this.upSuffix = new LdapName( cfg.getSuffix() );
        this.normSuffix = cfg.getNormalizedSuffix( factoryCfg.getGlobalRegistries().getMatchingRuleRegistry() );

        File workingDirectory = new File(
                factoryCfg.getStartupConfiguration().getWorkingDirectory().getPath() +
                File.separator + cfg.getName() );
        
        workingDirectory.mkdirs();
        
        this.workingDirectory = workingDirectory;
            
        try 
        {
            String path = workingDirectory.getPath() + File.separator + "master";
            BaseRecordManager base = new BaseRecordManager( path );
            base.disableTransactions();
            recMan = new CacheRecordManager( base, new MRU( 1000 ) );
        } 
        catch ( IOException e )
        {
            NamingException ne = new NamingException( 
                "Could not initialize RecordManager" );
            ne.setRootCause( e );
            throw ne;
        }

        master = new JdbmMasterTable( recMan );
        indices = new HashMap();
        sysIndices = new HashMap();
        
        super.init( factoryCfg, cfg );
        initialized = true;
    }
    
    
    public synchronized void destroy()
    {
        if ( !initialized )
        {
            return;
        }

        ArrayList array = new ArrayList();
        array.addAll( indices.values() );
        
        if ( null != ndnIdx )
        {
            array.add( ndnIdx );
        }
        
        if ( null != updnIdx )
        {
            array.add( updnIdx );
        }

        if ( null != aliasIdx )
        {
            array.add( aliasIdx );
        }

        if ( null != oneAliasIdx )
        {
            array.add( oneAliasIdx );
        }

        if ( null != subAliasIdx )
        {
            array.add( subAliasIdx );
        }

        if ( null != hierarchyIdx )
        {
            array.add( hierarchyIdx );
        }

        if ( null != existanceIdx )
        {
            array.add( existanceIdx );
        }
        
        Iterator list = array.iterator();
        
        while ( list.hasNext() ) 
        {
            Index index = ( Index ) list.next();

            try 
            {
               index.close();
            } 
            catch ( Throwable t ) 
            {
                log.error( "Failed to close an index.", t );
            }
        }

        try 
        {
            master.close();
        } 
        catch ( Throwable t ) 
        {
            log.error( "Failed to close the master.", t );
        }

        try 
        {
            recMan.close();
        } 
        catch ( Throwable t ) 
        {
            log.error( "Failed to close the record manager", t );
        }

        initialized = false;
    }


    public boolean isInitialized()
    {
        return initialized;
    }


    public synchronized void sync() throws NamingException
    {
        if( !initialized )
        {
            return;
        }

        ArrayList array = new ArrayList();
        array.addAll( indices.values() );
        array.add( ndnIdx );
        array.add( updnIdx );
        array.add( aliasIdx );
        array.add( oneAliasIdx );
        array.add( subAliasIdx );
        array.add( hierarchyIdx );
        array.add( existanceIdx );
        
        Iterator list = array.iterator();

        // Sync all user defined indices
        while ( list.hasNext() ) 
        {
            Index idx = ( Index ) list.next();

            idx.sync();
        }
        
        master.sync();

        try 
        {
            recMan.commit();
        }
        catch ( Throwable t ) 
        {
            throw ( NamingException ) new NamingException(
                    "Failed to commit changes to the record manager." ).initCause( t );
        }        
    }


    // ------------------------------------------------------------------------
    // I N D E X   M E T H O D S
    // ------------------------------------------------------------------------


    public void addIndexOn( AttributeType spec ) throws NamingException
    {
        Index idx = new JdbmIndex( spec, workingDirectory );
        indices.put( spec.getName().toLowerCase(), idx );
    }

    
    public Index getExistanceIndex() 
    {
        return existanceIdx;
    }
    

    public void setExistanceIndexOn( AttributeType attrType ) throws NamingException
    {
        if ( existanceIdx != null )
        {
            NamingException e = new NamingException( "Index already set!" );
            throw e;
        }

        existanceIdx = new JdbmIndex( attrType, workingDirectory );
        sysIndices.put( attrType.getName().toLowerCase(), existanceIdx );
    }

    
    public Index getHierarchyIndex() 
    {
        return hierarchyIdx;
    }
    

    public void setHierarchyIndexOn( AttributeType attrType ) throws NamingException
    {
        if ( hierarchyIdx != null )
        {
            NamingException e = new NamingException( "Index already set!" );
            throw e;
        }

        hierarchyIdx = new JdbmIndex( attrType, workingDirectory );
        sysIndices.put( attrType.getName().toLowerCase(), hierarchyIdx );
    }

    
    public Index getAliasIndex()
    {
        return aliasIdx;    
    }


    public void setAliasIndexOn( AttributeType attrType ) throws NamingException
    {
        if ( aliasIdx != null )
        {
            NamingException e = new NamingException( "Index already set!" );
            throw e;
        }

        aliasIdx = new JdbmIndex( attrType, workingDirectory );
        sysIndices.put( attrType.getName().toLowerCase(), aliasIdx );
    }    
    

    public Index getOneAliasIndex()
    {
        return oneAliasIdx;
    }


    public void setOneAliasIndexOn( AttributeType attrType ) throws NamingException
    {
        if ( oneAliasIdx != null )
        {
            NamingException e = new NamingException( "Index already set!" );
            throw e;
        }

        oneAliasIdx = new JdbmIndex( attrType, workingDirectory );
        sysIndices.put( attrType.getName().toLowerCase(), oneAliasIdx );
    }


    public Index getSubAliasIndex()
    {
        return subAliasIdx;
    }


    public void setSubAliasIndexOn( AttributeType attrType ) throws NamingException
    {
        if ( subAliasIdx != null )
        {
            NamingException e = new NamingException( "Index already set!" );
            throw e;
        }

        subAliasIdx = new JdbmIndex( attrType, workingDirectory );
        sysIndices.put( attrType.getName().toLowerCase(), subAliasIdx );
    }


    public Index getUpdnIndex()
    {
        return updnIdx;
    }


    public void setUpdnIndexOn( AttributeType attrType ) throws NamingException
    {
        if ( updnIdx != null )
        {
            NamingException e = new NamingException( "Index already set!" );
            throw e;
        }

        updnIdx = new JdbmIndex( attrType, workingDirectory );
        sysIndices.put( attrType.getName().toLowerCase(), updnIdx );
    }

    
    public Index getNdnIndex() 
    {
        return ndnIdx;
    }
    

    public void setNdnIndexOn( AttributeType attrType ) throws NamingException
    {
        if ( ndnIdx != null )
        {
            NamingException e = new NamingException( "Index already set!" );
            throw e;
        }

        ndnIdx = new JdbmIndex( attrType, workingDirectory );
        sysIndices.put( attrType.getName().toLowerCase(), ndnIdx );
    }

    
    public Iterator getUserIndices()
    {
        return indices.keySet().iterator();
    }


    public Iterator getSystemIndices()
    {
        return sysIndices.keySet().iterator();
    }


    public boolean hasUserIndexOn( String attribute )
    {
        return indices.containsKey( attribute ) ||
            indices.containsKey( attribute.toLowerCase() );
    }


    public boolean hasSystemIndexOn( String attribute )
    {
        return sysIndices.containsKey( attribute ) ||
            sysIndices.containsKey( attribute.toLowerCase() );
    }


    /**
     * @todo replace lookups to use the OID instead of the name.  Also note
     * that the OID registry can be used to go between names and oids.
     * 
     * @see org.apache.ldap.server.partition.impl.btree.BTreeDirectoryPartition#getUserIndex(String)
     */
    public Index getUserIndex( String attribute ) throws IndexNotFoundException
    {
        String lowerCased = attribute.toLowerCase();

        if ( indices.containsKey( attribute ) ) 
        {
            return ( Index ) indices.get( attribute );
        } 
        else if ( indices.containsKey( lowerCased ) ) 
        {
            return ( Index ) indices.get( lowerCased );
        } 
        else 
        {
            throw new IndexNotFoundException( "An index on attribute " +
                attribute + " does not exist!" );
        }
    }
    
    
    /**
     * @todo replace lookups to use the OID instead of the name.  Also note
     * that the OID registry can be used to go between names and oids.
     * 
     * @see BTreeDirectoryPartition#getEntryId(String)
     */
    public Index getSystemIndex( String indexName ) throws IndexNotFoundException
    {
        String lowerCased = indexName.toLowerCase();

        if ( sysIndices.containsKey( indexName ) ) 
        {
            return ( Index ) sysIndices.get( indexName );
        } 
        else if ( sysIndices.containsKey( lowerCased ) ) 
        {
            return ( Index ) sysIndices.get( lowerCased );
        } 
        else 
        {
            throw new IndexNotFoundException( "A system index by the name of " +
                indexName + " does not exist!" );
        }
    }


    public BigInteger getEntryId( String dn ) throws NamingException
    {
        return ndnIdx.forwardLookup( dn );
    }


    public String getEntryDn( BigInteger id ) throws NamingException
    {
        return ( String ) ndnIdx.reverseLookup( id );
    }


    public BigInteger getParentId( String dn ) throws NamingException
    {
        BigInteger childId = ndnIdx.forwardLookup( dn );
        return ( BigInteger ) hierarchyIdx.reverseLookup( childId );
    }


    public BigInteger getParentId( BigInteger childId ) throws NamingException
    {
        return ( BigInteger ) hierarchyIdx.reverseLookup( childId );
    }
    
    
    public String getEntryUpdn( BigInteger id ) throws NamingException
    {
        return ( String ) updnIdx.reverseLookup( id );
    }


    public String getEntryUpdn( String dn ) throws NamingException
    {
        BigInteger id = ndnIdx.forwardLookup( dn );
        return ( String ) updnIdx.reverseLookup( id );
    }


    public int count() throws NamingException
    {
        return master.count();
    }
    
    
    /**
     * Removes the index entries for an alias before the entry is deleted from
     * the master table.
     * 
     * @todo Optimize this by walking the hierarchy index instead of the name 
     * @param aliasId the id of the alias entry in the master table
     * @throws NamingException if we cannot delete the indices
     */
    private void dropAliasIndices( BigInteger aliasId ) throws NamingException
    {
        String targetDn = ( String ) aliasIdx.reverseLookup( aliasId );
        BigInteger targetId = getEntryId( targetDn );
        String aliasDn = getEntryDn( aliasId );
        Name ancestorDn = new LdapName( aliasDn ).getPrefix( 1 );
        BigInteger ancestorId = getEntryId( ancestorDn.toString() );
        
        /*
         * We cannot just drop all tuples in the one level and subtree indices
         * linking baseIds to the targetId.  If more than one alias refers to
         * the target then droping all tuples with a value of targetId would
         * make all other aliases to the target inconsistent.
         * 
         * We need to walk up the path of alias ancestors until we reach the 
         * upSuffix, deleting each ( ancestorId, targetId ) tuple in the
         * subtree scope alias.  We only need to do this for the direct parent
         * of the alias on the one level subtree.
         */
        oneAliasIdx.drop( ancestorId, targetId );
        subAliasIdx.drop( ancestorId, targetId );
        
        while ( ! ancestorDn.equals( upSuffix ) )
        {
            ancestorDn = ancestorDn.getPrefix( 1 );
            ancestorId = getEntryId( ancestorDn.toString() );
            
            subAliasIdx.drop( ancestorId, targetId );
        }    

        // Drops all alias tuples pointing to the id of the alias to be deleted
        aliasIdx.drop( aliasId );
    }
    
    
    /**
     * Adds indices for an aliasEntry to be added to the database while checking
     * for constrained alias constructs like alias cycles and chaining.
     * 
     * @param aliasDn normalized distinguished name for the alias entry
     * @param aliasTarget the user provided aliased entry dn as a string
     * @param aliasId the id of alias entry to add
     * @throws NamingException if index addition fails, of the alias is not 
     * allowed due to chaining or cycle formation.
     */
    private void addAliasIndices( BigInteger aliasId, Name aliasDn, 
        String aliasTarget ) throws NamingException
    {
        Name targetDn = null;            // Name value of aliasedObjectName
        BigInteger targetId = null;      // Id of the aliasedObjectName
        Normalizer normalizer = null;    // Temporary handle for Dn's
        Name ancestorDn = null;          // Name of an alias entry relative
        BigInteger ancestorId = null;    // Id of an alias entry relative

        // Access aliasedObjectName, normalize it and generate the Name 
        normalizer = oneAliasIdx.getAttribute().getEquality().getNormalizer();
        targetDn = new LdapName( ( String ) normalizer.normalize( aliasTarget ) );
           
        /*
         * Check For Cycles
         * 
         * Before wasting time to lookup more values we check using the target
         * dn to see if we have the possible formation of an alias cycle.  This
         * happens when the alias refers back to a target that is also a 
         * relative of the alias entry.  For detection we test if the aliased
         * entry Dn starts with the target Dn.  If it does then we know the 
         * aliased target is a relative and we have a perspecitive cycle.
         */ 
        if ( aliasDn.startsWith( targetDn ) )
        {
            if ( aliasDn.equals( targetDn ) )
            {
                throw new NamingException( "[36] aliasDereferencingProblem - " 
                    + "attempt to create alias to itself." );
            }
            
            throw new NamingException( "[36] aliasDereferencingProblem - " 
                + "attempt to create alias with cycle to relative " 
                + aliasTarget + " not allowed from descendent alias " 
                + aliasDn );
        }
            
        /*
         * Check For Aliases External To Naming Context
         * 
         * id may be null but the alias may be to a valid entry in 
         * another namingContext.  Such aliases are not allowed and we
         * need to point it out to the user instead of saying the target
         * does not exist when it potentially could outside of this upSuffix.
         */
        if ( ! targetDn.startsWith( upSuffix ) )
        {
            // Complain specifically about aliases to outside naming contexts
            throw new NamingException( "[36] aliasDereferencingProblem -"
                + " the alias points to an entry outside of the " + upSuffix
                + " namingContext to an object whose existance cannot be"
                + " determined." );
        }

        // L O O K U P   T A R G E T   I D
        targetId = ndnIdx.forwardLookup( targetDn.toString() );

        /*
         * Check For Target Existance
         * 
         * We do not allow the creation of inconsistant aliases.  Aliases should
         * not be broken links.  If the target does not exist we start screaming
         */
        if ( null == targetId )
        {
            // Complain about target not existing
            throw new NamingException( "[33] aliasProblem - " 
                + "the alias when dereferenced would not name a known object "
                + "the aliasedObjectName must be set to a valid existing "
                + "entry." );
        }
        
        /*
         * Detect Direct Alias Chain Creation
         * 
         * Rather than resusitate the target to test if it is an alias and fail
         * due to chaing creation we use the alias index to determine if the
         * target is an alias.  Hence if the alias we are about to create points
         * to another alias as its target in the aliasedObjectName attribute, 
         * then we have a situation where an alias chain is being created.  
         * Alias chaining is not allowed so we throw and exception. 
         */
        if ( null != aliasIdx.reverseLookup( targetId ) )
        {
            // Complain about illegal alias chain
            throw new NamingException( "[36] aliasDereferencingProblem -" 
                + " the alias points to another alias.  Alias chaining is" 
                + " not supported by this backend." );
        }
        
        // Add the alias to the simple alias index
        aliasIdx.add( aliasTarget, aliasId );
        
        /*
         * Handle One Level Scope Alias Index
         * 
         * The first relative is special with respect to the one level alias
         * index.  If the target is not a sibling of the alias then we add the
         * index entry maping the parent's id to the aliased target id.
         */
        ancestorDn = aliasDn.getPrefix( 1 );
        ancestorId = getEntryId( ancestorDn.toString() );
        
        if ( ! NamespaceTools.isSibling( targetDn, aliasDn ) )
        {
            oneAliasIdx.add( ancestorId, targetId );
        }

        /*
         * Handle Sub Level Scope Alias Index
         * 
         * Walk the list of relatives from the parents up to the upSuffix, testing
         * to see if the alias' target is a descendant of the relative.  If the
         * alias target is not a descentant of the relative it extends the scope
         * and is added to the sub tree scope alias index.  The upSuffix node is
         * ignored since everything is under its scope.  The first loop 
         * iteration shall handle the parents.
         */
        while ( ! ancestorDn.equals( upSuffix ) && null != ancestorId )
        {
            if ( ! NamespaceTools.isDescendant( ancestorDn, targetDn ) )
            {
                subAliasIdx.add( ancestorId, targetId );
            }
            
            ancestorDn = ancestorDn.getPrefix( 1 );
            ancestorId = getEntryId( ancestorDn.toString() );
        }        
    }


    public void add( String updn, Name dn, Attributes entry ) throws NamingException
    {
        BigInteger id;
        BigInteger parentId = null;

        id = master.getNextId();

        //
        // Suffix entry cannot have a parent since it is the root so it is 
        // capped off using the zero value which no entry can have since 
        // entry sequences start at 1.
        //
        
        if ( dn.equals( normSuffix ) )
        {
            parentId = BigInteger.ZERO;
        }
        else 
        {
            parentId = getEntryId( dn.getPrefix( 1 ).toString() );
        }

        // don't keep going if we cannot find the parent Id
        if ( parentId == null )
        {
            throw new LdapNameNotFoundException( "Id for parent '" + dn.getPrefix( 1 ).toString() + "' not found!" );
        }

        Attribute objectClass = entry.get( "objectClass" );

        if ( objectClass == null )
        {
            String msg = "Entry " + updn + " contains no objectClass attribute: " + entry;

            throw new LdapSchemaViolationException( msg, ResultCodeEnum.OBJECTCLASSVIOLATION );
        }

        // Start adding the system indices
        // Why bother doing a lookup if this is not an alias.

        if ( entry.get( "objectClass" ).contains( DirectoryPartition.ALIAS_OBJECT ) )
        {
            addAliasIndices( id, dn, ( String ) entry.get( DirectoryPartition.ALIAS_ATTRIBUTE ).get() );
        }
        
        ndnIdx.add( dn.toString(), id );
        updnIdx.add( updn, id );
        hierarchyIdx.add( parentId, id );
        
        // Now work on the user defined indices
        NamingEnumeration list = entry.getIDs();
        while ( list.hasMore() ) 
        {
            String attribute = ( String ) list.next();
            
            if ( hasUserIndexOn( attribute ) ) 
            {
                Index idx = getUserIndex( attribute );
                NamingEnumeration values = entry.get( attribute ).getAll();
                
                while ( values.hasMore() ) 
                {
                    idx.add( values.next(), id );
                }

                // Adds only those attributes that are indexed
                existanceIdx.add( attribute.toLowerCase(), id );
            }
        }

        master.put( entry, id );
    }


    public Attributes lookup( BigInteger id ) throws NamingException
    {
        return master.get( id );
    }


    public void delete( BigInteger id ) throws  NamingException
    {
        Attributes entry = lookup( id );
        BigInteger parentId = getParentId( id );
        NamingEnumeration attrs = entry.getIDs();
        
        if ( entry.get( "objectClass" ).contains( DirectoryPartition.ALIAS_OBJECT ) )
        {
            dropAliasIndices( id );
        }

        ndnIdx.drop( id );
        updnIdx.drop( id );
        hierarchyIdx.drop( id );
        
        // Remove parent's reference to entry only if entry is not the upSuffix
        if ( ! parentId.equals( BigInteger.ZERO ) )
        {
            hierarchyIdx.drop( parentId, id );
        }
        
        while ( attrs.hasMore() ) 
        {
            String attr = ( ( String ) attrs.next() );

            if ( hasUserIndexOn( attr ) )
            {
                Index index = getUserIndex( attr );
                NamingEnumeration values = entry.get( attr ).getAll();
                
                while ( values.hasMore() )
                {
                    index.drop( values.next(), id );
                }

                existanceIdx.drop( attr.toLowerCase(), id );
            }
        }

        master.delete( id );
    }


    public NamingEnumeration list( BigInteger id ) throws  NamingException
    {
        return hierarchyIdx.listIndices( id );
    }


    public int getChildCount( BigInteger id ) throws NamingException
    {
        return hierarchyIdx.count( id );
    }


    public Name getSuffix( boolean normalized )
    {
        if ( normalized )
        {
            return normSuffix;
        }
        
        return upSuffix;
    }


    public Attributes getSuffixEntry() throws NamingException
    {
        BigInteger id = getEntryId( upSuffix.toString() );

        if ( null == id )
        {
            return null;
        }

        return lookup( id );
    }


    public void setProperty( String propertyName, String propertyValue )
        throws NamingException
    {
        master.setProperty( propertyName, propertyValue );
    }


    public String getProperty( String propertyName ) throws NamingException
    {
        return master.getProperty( propertyName );
    }


    public Attributes getIndices( BigInteger id ) throws  NamingException
    {
        Attributes attributes = new LockableAttributesImpl();

        // Get the distinguishedName to id mapping
        attributes.put( "_nDn", getEntryDn( id ) );
        attributes.put( "_upDn", getEntryUpdn( id ) );
        attributes.put( "_parent", getParentId( id ) );

        // Get all standard index attribute to value mappings
        Iterator idxList = this.indices.values().iterator();
        while ( idxList.hasNext() )
        {
            Index index = ( Index ) idxList.next();
            NamingEnumeration list = index.listReverseIndices( id );
            while ( list.hasMore() ) 
            {
                IndexRecord rec = ( IndexRecord ) list.next();
                Object val = rec.getIndexKey();
                String attrId = index.getAttribute().getName();
                Attribute attr = attributes.get( attrId );
                if ( attr == null)
                {
                    attr = new LockableAttributeImpl( attrId );
                }
                attr.add( val );
                attributes.put( attr );
            }
        }

        // Get all existance mappings for this id creating a special key
        // that looks like so 'existance[attribute]' and the value is set to id
        NamingEnumeration list = existanceIdx.listReverseIndices( id );
        StringBuffer val = new StringBuffer();
        while ( list.hasMore() ) 
        {
            IndexRecord rec = ( IndexRecord ) list.next();
            val.append( "_existance[" ); 
            val.append( rec.getIndexKey() );
            val.append( "]" );

            String valStr = val.toString();
            Attribute attr = attributes.get( valStr );
            if ( attr == null )
            {
                attr = new LockableAttributeImpl( valStr );
            }
            attr.add( rec.getEntryId() );
            attributes.put( attr );
            val.setLength( 0 );
        }

        // Get all parent child mappings for this entry as the parent using the
        // key 'child' with many entries following it.
        list = hierarchyIdx.listIndices( id );
        Attribute childAttr = new LockableAttributeImpl( "_child" );
        attributes.put( childAttr );
        while ( list.hasMore() )
        {
            IndexRecord rec = ( IndexRecord ) list.next();
            childAttr.add( rec.getEntryId() );
        }

        return attributes;
    }


    /**
     * Adds a set of attribute values while affecting the appropriate indices. 
     * The entry is not persisted: it is only changed in anticipation for a put 
     * into the master table.
     *
     * @param id the primary key of the entry
     * @param entry the entry to alter
     * @param mods the attribute and values to add 
     * @throws NamingException if index alteration or attribute addition
     * fails.
     */
    private void add( BigInteger id, Attributes entry, Attribute mods )
        throws NamingException 
    {
        if ( hasUserIndexOn( mods.getID() ) )
        {
            Index idx = getUserIndex( mods.getID() );
            idx.add( mods, id );

            // If the attr didn't exist for this id add it to existance index
            if ( ! existanceIdx.hasValue( mods.getID().toLowerCase(), id ) )
            {
                existanceIdx.add( mods.getID().toLowerCase(), id );
            }
        }

        // add all the values in mods to the same attribute in the entry
        Attribute entryAttrToAddTo = entry.get( mods.getID() );

        if ( entryAttrToAddTo == null )
        {
            entryAttrToAddTo = new LockableAttributeImpl( mods.getID() );
            entry.put( entryAttrToAddTo );
        }

        for ( int ii = 0; ii < mods.size(); ii++ )
        {
            entryAttrToAddTo.add( mods.get( ii ) );
        }

        if ( mods.getID().equals( DirectoryPartition.ALIAS_ATTRIBUTE ) )
        {
            String ndnStr = ( String ) ndnIdx.reverseLookup( id );
            addAliasIndices( id, new LdapName( ndnStr ), 
                ( String ) mods.get() );
        }
    }
    
    
    /**
     * Completely removes the set of values for an attribute having the values 
     * supplied while affecting the appropriate indices.  The entry is not 
     * persisted: it is only changed in anticipation for a put into the master 
     * table.  Note that an empty attribute w/o values will remove all the 
     * values within the entry where as an attribute w/ values will remove those
     * attribute values it contains.
     *
     * @param id the primary key of the entry
     * @param entry the entry to alter
     * @param mods the attribute and its values to delete
     * @throws NamingException if index alteration or attribute modification 
     * fails.
     */
    private void remove( BigInteger id, Attributes entry, Attribute mods )
        throws NamingException
    {
        if ( hasUserIndexOn( mods.getID() ) )
        {
            Index idx = getUserIndex( mods.getID() );
            idx.drop( mods, id );
    
            /* 
             * If no attribute values exist for this entryId in the index then
             * we remove the existance index entry for the removed attribute.
             */
            if ( null == idx.reverseLookup( id ) )
            {
                existanceIdx.drop( mods.getID(), id );
            }
        }

        /*
         * If there are no attribute values in the modifications then this 
         * implies the compelete removal of the attribute from the entry. Else
         * we remove individual attribute values from the entry in mods one 
         * at a time.
         */
        if ( mods.size() == 0 )
        {
            entry.remove( mods.getID() );
        }
        else
        {
            Attribute entryAttr = entry.get( mods.getID() );
            NamingEnumeration values = mods.getAll();
            while ( values.hasMore() ) 
            {
                entryAttr.remove( values.next() );
            }
            
            // if nothing is left just remove empty attribute
            if ( entryAttr.size() == 0 )
            {
                entry.remove( entryAttr.getID() );
            }
        }

        // Aliases->single valued comp/partial attr removal is not relevant here
        if ( mods.getID().equals( DirectoryPartition.ALIAS_ATTRIBUTE ) )
        {
            dropAliasIndices( id );
        }
    }


    /**
     * Completely replaces the existing set of values for an attribute with the
     * modified values supplied affecting the appropriate indices.  The entry
     * is not persisted: it is only changed in anticipation for a put into the
     * master table.
     *
     * @param id the primary key of the entry
     * @param entry the entry to alter
     * @param mods the replacement attribute and values
     * @throws NamingException if index alteration or attribute modification 
     * fails.
     */
    private void replace( BigInteger id, Attributes entry, Attribute mods )
        throws NamingException
    {
        if ( hasUserIndexOn( mods.getID() ) )
        {
            Index idx = getUserIndex( mods.getID() );
            
            // Drop all existing attribute value index entries and add new ones
            idx.drop( id );
            idx.add( mods, id );
    
            /* 
             * If no attribute values exist for this entryId in the index then
             * we remove the existance index entry for the removed attribute.
             */
            if ( null == idx.reverseLookup( id ) )
            {
                existanceIdx.drop( mods.getID(), id );
            }
        }

        if ( mods.getID().equals( DirectoryPartition.ALIAS_ATTRIBUTE ) )
        {
            dropAliasIndices( id );
        }
        
        // Automatically replaces old attributes with new modified ones
        entry.put( mods );
        
        if ( mods.getID().equals( DirectoryPartition.ALIAS_ATTRIBUTE ) )
        {
            String ndnStr = ( String ) ndnIdx.reverseLookup( id );
            addAliasIndices( id, new LdapName( ndnStr ), 
                ( String ) mods.get() );
        }
    }


    public void modify( Name dn, int modOp, Attributes mods ) throws NamingException
    {
        NamingEnumeration attrs = null;
        BigInteger id = getEntryId( dn.toString() );
        Attributes entry = master.get( id );
        
        switch ( modOp )
        {
            case( DirContext.ADD_ATTRIBUTE ):
                attrs = mods.getIDs();
                
                while ( attrs.hasMore() )
                {
                    String attrId = ( String ) attrs.next();
                    Attribute attr = mods.get( attrId );
                    add( id, entry, attr );
                }
                
                break;
            case( DirContext.REMOVE_ATTRIBUTE ):
                attrs = mods.getIDs();
                
                while ( attrs.hasMore() )
                {
                    String attrId = ( String ) attrs.next();
                    Attribute attr = mods.get( attrId );
                    remove( id, entry, attr );
                }
                
                break;
            case( DirContext.REPLACE_ATTRIBUTE ):
                attrs = mods.getIDs();
                
                while ( attrs.hasMore() )
                {
                    String attrId = ( String ) attrs.next();
                    Attribute attr = mods.get( attrId );
                    replace( id, entry, attr );
                }
                
                break;
            default:
                throw new NamingException( 
                    "Unidentified modification operation" );
        }
        
        master.put( entry, id );
    }
    

    public void modify( Name dn, ModificationItem [] mods ) throws NamingException
    {
        BigInteger id = getEntryId( dn.toString() );
        Attributes entry = master.get( id );
        
        for ( int ii = 0; ii < mods.length; ii++ )
        {
            Attribute attrMods = mods[ii].getAttribute();

            switch ( mods[ ii ].getModificationOp() )
            {
                case( DirContext.ADD_ATTRIBUTE ):
                    add( id, entry, attrMods );
                    break;
                case( DirContext.REMOVE_ATTRIBUTE ):
                    remove( id, entry, attrMods );
                    break;
                case( DirContext.REPLACE_ATTRIBUTE ):
                    replace( id, entry, attrMods );
                    break;
                default:
                    throw new NamingException( 
                        "Unidentified modification operation" );
            }
        }
        
        master.put( entry, id );
    }


    /**
     * Changes the relative distinuished name of an entry specified by a 
     * distinguished name with the optional removal of the old Rdn attribute
     * value from the entry.  Name changes propagate down as dn changes to the 
     * descendants of the entry where the Rdn changed. 
     * 
     * An Rdn change operation does not change parent child relationships.  It 
     * merely propagates a name change at a point in the DIT where the Rdn is 
     * changed. The change propagates down the subtree rooted at the 
     * distinguished name specified.
     *
     * @param dn the normalized distinguished name of the entry to alter
     * @param newRdn the new Rdn to set
     * @param deleteOldRdn whether or not to remove the old Rdn attr/val
     * @throws NamingException if there are any errors propagating the name 
     *        changes.
     */
    public void modifyRn( Name dn, String newRdn, boolean deleteOldRdn )
        throws NamingException
    {
        String newRdnAttr = NamespaceTools.getRdnAttribute( newRdn );
        String newRdnValue = NamespaceTools.getRdnValue( newRdn );
        BigInteger id = getEntryId( dn.toString() );
        Attributes entry = lookup( id );
        Name updn = new LdapName( getEntryUpdn( id ) );

        /* 
         * H A N D L E   N E W   R D N
         * ====================================================================
         * Add the new Rdn attribute to the entry.  If an index exists on the 
         * new Rdn attribute we add the index for this attribute value pair.
         * Also we make sure that the existance index shows the existance of the
         * new Rdn attribute within this entry.
         */

        Attribute rdnAttr = entry.get( newRdnAttr );
        if ( rdnAttr == null )
        {
            rdnAttr = new LockableAttributeImpl( newRdnAttr );
        }

        // add the new Rdn value only if it is not already present in the entry
        if ( ! rdnAttr.contains( newRdnValue ) )
        {
            rdnAttr.add( newRdnValue );
        }

        entry.put( rdnAttr );
        
        if ( hasUserIndexOn( newRdnAttr ) )
        {
            Index idx = getUserIndex( newRdnAttr );
            idx.add( newRdnValue, id );
            
            // Make sure the altered entry shows the existance of the new attrib
            if ( ! existanceIdx.hasValue( newRdnAttr.toLowerCase(), id ) )
            {
                existanceIdx.add( newRdnAttr.toLowerCase(), id );
            }
        }

        /*
         * H A N D L E   O L D   R D N
         * ====================================================================
         * If the old Rdn is to be removed we need to get the attribute and 
         * value for it.  Keep in mind the old Rdn need not be based on the 
         * same Rdn as the new one.  We remove the Rdn value from the entry
         * and remove the value/id tuple from the index on the old Rdn attr
         * if any.  We also test if the delete of the old Rdn index tuple 
         * removed all the attribute values of the old Rdn using a reverse
         * lookup.  If so that means we blew away the last value of the old 
         * Rdn attribute.  In this case we need to remove the attrName/id 
         * tuple from the existance index.
         */

        if ( deleteOldRdn )
        {
            String oldRdn = updn.get( updn.size() - 1 );
            String oldRdnAttr = NamespaceTools.getRdnAttribute( oldRdn );
            String oldRdnValue = NamespaceTools.getRdnValue( oldRdn );
            
            entry.get( oldRdnAttr ).remove( oldRdnValue );

            if ( hasUserIndexOn( oldRdnAttr ) )
            {
                Index idx = getUserIndex( oldRdnAttr );
                idx.drop( oldRdnValue, id );
                
                /*
                 * If there is no value for id in this index due to our
                 * drop above we remove the oldRdnAttr from the existance idx
                 */
                if ( null == idx.reverseLookup( id ) )
                {
                    existanceIdx.drop( oldRdnAttr, id );
                }
            }
        }
        
        /*
         * H A N D L E   D N   C H A N G E
         * ====================================================================
         * 1) Build the new user defined distinguished name
         *      - clone / copy old updn
         *      - remove old upRdn from copy
         *      - add the new upRdn to the copy
         * 2) Make call to recursive modifyDn method to change the names of the
         *    entry and its descendants
         */

        Name newUpdn = ( Name ) updn.clone();      // copy da old updn
        newUpdn.remove( newUpdn.size() - 1 );      // remove old upRdn
        newUpdn.add( newUpdn.size(), newRdn );   // add da new upRdn
        modifyDn( id, newUpdn, false );            // propagate dn changes
    }
    
    
    /*
     * The move operation severs a child from a parent creating a new parent
     * child relationship.  As a consequence the relationships between the 
     * old ancestors of the child and its descendants change.  A descendant is
     *   
     */

    /**
     * Recursively modifies the distinguished name of an entry and the names of
     * its descendants calling itself in the recursion.
     *
     * @param id the primary key of the entry
     * @param updn User provided distinguished name to set as the new DN
     * @param isMove whether or not the name change is due to a move operation
     * which affects alias indices.
     * @throws NamingException if something goes wrong
     */
    private void modifyDn( BigInteger id, Name updn, boolean isMove )
        throws  NamingException
    {
        String aliasTarget = null;

        // Now we can handle the appropriate name indices for all cases
        ndnIdx.drop( id );
        ndnIdx.add( ndnIdx.getNormalized( updn.toString() ), id );
        
        updnIdx.drop( id );
        updnIdx.add( updn.toString(), id );
        
        /* 
         * Read Alias Index Tuples
         * 
         * If this is a name change due to a move operation then the one and
         * subtree indices for aliases were purged before the aliases were 
         * moved.  Now we must add them for each alias entry we have moved.  
         * 
         * aliasTarget is used as a marker to tell us if we're moving an 
         * alias.  If it is null then the moved entry is not an alias.
         */
        if ( isMove ) 
        {
            aliasTarget = ( String ) aliasIdx.reverseLookup( id );
    
            if ( null != aliasTarget )
            {
                addAliasIndices( id, new LdapName( getEntryDn( id ) ), 
                    aliasTarget );
            }
        }
        
        NamingEnumeration children = list( id );
        while ( children.hasMore() ) 
        {
            // Get the child and its id
            IndexRecord rec = ( IndexRecord ) children.next();
            BigInteger childId = rec.getEntryId();
            
            /* 
             * Calculate the Dn for the child's new name by copying the parents
             * new name and adding the child's old upRdn to new name as its Rdn
             */
            Name childUpdn = ( Name ) updn.clone();
            Name oldUpdn = new LdapName( getEntryUpdn( childId ) );
            String rdn = LdapName.getRdn( oldUpdn ); 
            childUpdn.add( childUpdn.size(),  rdn );

            // Recursively change the names of the children below
            modifyDn( childId, childUpdn, isMove );
        }
    }


    public void move( Name oldChildDn, Name newParentDn, String newRdn,
        boolean deleteOldRdn ) throws NamingException
    {
        BigInteger childId = getEntryId( oldChildDn.toString() );
        modifyRn( oldChildDn, newRdn, deleteOldRdn );
        move( oldChildDn, childId, newParentDn );
    }


    public void move( Name oldChildDn, Name newParentDn ) throws NamingException
    {
        BigInteger childId = getEntryId( oldChildDn.toString() );
        move( oldChildDn, childId, newParentDn );
    }


    /**
     * Moves an entry under a new parent.  The operation causes a shift in the
     * parent child relationships between the old parent, new parent and the 
     * child moved.  All other descendant entries under the child never change
     * their direct parent child relationships.  Hence after the parent child
     * relationship changes are broken at the old parent and set at the new
     * parent a modifyDn operation is conducted to handle name changes 
     * propagating down through the moved child and its descendants.
     * 
     * @param oldChildDn the normalized dn of the child to be moved
     * @param newParentDn the normalized dn of the new parent for the child
     * @throws NamingException if something goes wrong
     */
    private void move( Name oldChildDn, BigInteger childId, Name newParentDn ) throws NamingException
    {
        // Get the child and the new parent to be entries and Ids
        BigInteger newParentId = getEntryId( newParentDn.toString() );
        BigInteger oldParentId = getParentId( childId );
        
        /*
         * All aliases including and below oldChildDn, will be affected by
         * the move operation with respect to one and subtree indices since 
         * their relationship to ancestors above oldChildDn will be 
         * destroyed.  For each alias below and including oldChildDn we will
         * drop the index tuples mapping ancestor ids above oldChildDn to the
         * respective target ids of the aliases.
         */
        dropMovedAliasIndices( oldChildDn );
        
        /*
         * Drop the old parent child relationship and add the new one
         * Set the new parent id for the child replacing the old parent id
         */
        hierarchyIdx.drop( oldParentId, childId );
        hierarchyIdx.add( newParentId, childId );

        /*
         * Build the new user provided DN (updn) for the child using the child's
         * user provided RDN & the new parent's UPDN.  Basically add the child's
         * UpRdn String to the tail of the new parent's Updn Name.
         */
        Name childUpdn = new LdapName( getEntryUpdn( childId ) );
        String childRdn = childUpdn.get( childUpdn.size() - 1 ); 
        Name newUpdn = new LdapName( getEntryUpdn( newParentId ) );
        newUpdn.add( newUpdn.size(), childRdn );

        // Call the modifyDn operation with the new updn
        modifyDn( childId, newUpdn, true );
    }


    /**
     * For all aliases including and under the moved base, this method removes
     * one and subtree alias index tuples for old ancestors above the moved base
     * that will no longer be ancestors after the move.
     * 
     * @param movedBase the base at which the move occured - the moved node
     * @throws NamingException if system indices fail
     */
    private void dropMovedAliasIndices( final Name movedBase ) throws NamingException
    {
        // Find all the aliases from movedBase down
        IndexAssertion isBaseDescendant = new IndexAssertion()
        {
            public boolean assertCandidate( IndexRecord rec )
                throws NamingException
            {
                String dn = getEntryDn( rec.getEntryId() );
                if ( dn.endsWith( movedBase.toString() ) )
                {
                    return true;
                }
                
                return false;
            }
        };
        
        BigInteger movedBaseId = getEntryId( movedBase.toString() );
        if ( aliasIdx.reverseLookup( movedBaseId ) != null ) 
        {
            dropAliasIndices( movedBaseId, movedBase );
        }
        
        NamingEnumeration aliases = new IndexAssertionEnumeration(
            aliasIdx.listIndices( movedBase.toString(), true ), 
            isBaseDescendant );
        while ( aliases.hasMore() )
        {
            IndexRecord entry = ( IndexRecord ) aliases.next();
            dropAliasIndices( entry.getEntryId(), movedBase );
        }
    }
    
    
    /**
     * For the alias id all ancestor one and subtree alias tuples are moved 
     * above the moved base.
     * 
     * @param aliasId the id of the alias 
     * @param movedBase the base where the move occured
     * @throws NamingException if indices fail
     */
    private void dropAliasIndices( BigInteger aliasId, Name movedBase )
        throws NamingException
    {
        String targetDn = ( String ) aliasIdx.reverseLookup( aliasId );
        BigInteger targetId = getEntryId( targetDn );
        String aliasDn = getEntryDn( aliasId );
        
        /*
         * Start droping index tuples with the first ancestor right above the 
         * moved base.  This is the first ancestor effected by the move.
         */
        Name ancestorDn = movedBase.getPrefix( 1 );
        BigInteger ancestorId = getEntryId( ancestorDn.toString() );
        
        /*
         * We cannot just drop all tuples in the one level and subtree indices
         * linking baseIds to the targetId.  If more than one alias refers to
         * the target then droping all tuples with a value of targetId would
         * make all other aliases to the target inconsistent.
         * 
         * We need to walk up the path of alias ancestors right above the moved 
         * base until we reach the upSuffix, deleting each ( ancestorId,
         * targetId ) tuple in the subtree scope alias.  We only need to do 
         * this for the direct parent of the alias on the one level subtree if
         * the moved base is the alias.
         */
        if ( aliasDn.equals( movedBase.toString() ) )
        {
            oneAliasIdx.drop( ancestorId, targetId );
        }
        
        subAliasIdx.drop( ancestorId, targetId );
        
        while ( ! ancestorDn.equals( upSuffix ) )
        {
            ancestorDn = ancestorDn.getPrefix( 1 );
            ancestorId = getEntryId( ancestorDn.toString() );
            
            subAliasIdx.drop( ancestorId, targetId );
        }    
    }
}

