package org.apache.eve.db.jdbm;


import java.math.BigInteger;
import javax.naming.NamingException;
import javax.naming.directory.Attributes; 

import jdbm.RecordManager;
import jdbm.helper.StringComparator;

import org.apache.ldap.common.util.BigIntegerComparator;
import org.apache.eve.db.MasterTable;


/**
 * The master table used to store the Attributes of entries.
 *
 * @todo rename to JdbmMasterTable
 */
public class DefaultMasterTable extends DefaultTable implements MasterTable
{
    /**  */
    private DefaultTable adminTbl = null;


    /**
     * Creates the master entry table using a Berkeley Db for the backing store.
     *
     * @param recMan the jdbm record manager
     * @throws NamingException if there is an error opening the Db file.
     */
    public DefaultMasterTable( RecordManager recMan )
        throws NamingException
    {
        super( DBF, recMan, new BigIntegerComparator() );
        adminTbl = new DefaultTable( "admin", recMan, new StringComparator() );
        String seqValue = ( String ) adminTbl.get( SEQPROP_KEY );
        
        if ( null == seqValue ) 
        {
            adminTbl.put( SEQPROP_KEY, BigInteger.ZERO.toString() );
        }
    }


    /**
     * Gets the Attributes of an entry from this MasterTable.
     *
     * @param id the BigInteger id of the entry to retrieve.
     * @return the Attributes of the entry with operational attributes and all.
     * @throws NamingException if there is a read error on the underlying Db.
     */
    public Attributes get( BigInteger id ) throws NamingException
    {
        return ( Attributes ) super.get( id );
    }


    /**
     * Puts the Attributes of an entry into this master table at an index 
     * specified by id.  Used both to create new entries and update existing 
     * ones.
     *
     * @param entry the Attributes of entry w/ operational attributes
     * @param id the BigInteger id of the entry to put
     * @return the Attributes of the entry put
     * @throws NamingException if there is a write error on the underlying Db.
     */
    public Attributes put( Attributes entry, BigInteger id ) throws NamingException
    {
        return ( Attributes ) super.put( id, entry );
    }


    /**
     * Deletes a entry from the master table at an index specified by id.
     *
     * @param id the BigInteger id of the entry to delete
     * @return the Attributes of the deleted entry
     * @throws NamingException if there is a write error on the underlying Db
     */
    public Attributes delete( BigInteger id ) throws NamingException
    {
        return ( Attributes ) super.remove( id );
    }


    /**
     * Get's the current id value from this master database's sequence without
     * affecting the seq.
     *
     * @return the current value.
     * @throws NamingException if the admin table storing sequences cannot be
     * read.
     */
    public BigInteger getCurrentId() throws NamingException
    {
        BigInteger id = null;

        synchronized ( adminTbl ) 
        {
            id = new BigInteger( ( String ) adminTbl.get( SEQPROP_KEY ) );
            
            if ( null == id ) 
            {
                adminTbl.put( SEQPROP_KEY, BigInteger.ZERO.toString() );
                id = BigInteger.ZERO;
            }
        }

        return id;
    }


    /**
     * Get's the next value from this SequenceBDb.  This has the side-effect of
     * changing the current sequence values perminantly in memory and on disk.
     * Master table sequence begins at BigInteger.ONE.  The BigInteger.ZERO is
     * used for the fictitious parent of the suffix root entry.
     *
     * @return the current value incremented by one.
     * @throws NamingException if the admin table storing sequences cannot be
     * read and writen to.
     */
    public BigInteger getNextId() throws NamingException
    {
        BigInteger lastVal = null;
        BigInteger nextVal = null;

        synchronized ( adminTbl ) 
        {
            lastVal = new BigInteger( ( String ) 
                adminTbl.get( SEQPROP_KEY ) );
            
            if ( null == lastVal ) 
            {
                adminTbl.put( SEQPROP_KEY, BigInteger.ONE.toString() );
                return BigInteger.ONE;
            } 
            else 
            {
                nextVal = lastVal.add( BigInteger.ONE );
                adminTbl.put( SEQPROP_KEY, nextVal.toString() );
            }
        }

        return nextVal;
    }


    /**
     * Gets a persistant property stored in the admin table of this MasterTable.
     *
     * @param property the key of the property to get the value of
     * @return the value of the property
     * @throws NamingException when the underlying admin table cannot be read
     */
    public String getProperty( String property ) throws NamingException
    {
        synchronized ( adminTbl ) 
        {
            return ( String ) adminTbl.get( property );
        }
    }


    /**
     * Sets a persistant property stored in the admin table of this MasterTable.
     *
     * @param property the key of the property to set the value of
     * @param value the value of the property
     * @throws NamingException when the underlying admin table cannot be writen
     */
    public void setProperty( String property, String value ) throws NamingException
    {
        synchronized ( adminTbl ) 
        {
            adminTbl.put( property, value );
        }
    }
}
