/*
 * $Id: MasterTable.java,v 1.3 2003/03/13 18:27:33 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.table ;


import java.io.File ;
import java.math.BigInteger ;
import javax.naming.NamingException ;

import org.apache.eve.backend.BackendException ;
import jdbm.recman.RecordManager;
import org.apache.eve.backend.jdbm.index.BigIntegerComparator;
import org.apache.eve.backend.jdbm.index.StringComparator;
import org.apache.eve.backend.jdbm.index.IndexComparator;


/**
 * The master table used to store LDIF based entries.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.3 $
 * @testcase org.apache.eve.backend.berkeley.table.TestMasterTable
 */
public class MasterTable
    extends JdbmTable
{
    /** the name of the dbf file for this table */
	public static final String DBF = "master" ;
    /** the sequence key - stores last sequence value in the admin table */
    public static final String m_sequence = "__sequence__" ;

    private JdbmTable m_adminTbl = null ;


    /**
     * Creates the master entry table using a Berkeley Db for the backing store.
     *
     * @param a_wkdirPath the working directory path where we create the
     * berkeley database files.
     * @throws BackendException if there is an error opening the Db file.
     */
	public MasterTable(RecordManager a_recMan)
        throws BackendException
    {
        super(DBF, a_recMan, new BigIntegerComparator()) ;
        m_adminTbl = new JdbmTable("admin", a_recMan, IndexComparator.strComp) ;
        String l_seqValue = (String) m_adminTbl.get(m_sequence) ;
        if(null == l_seqValue) {
            m_adminTbl.put(m_sequence, BigInteger.ZERO.toString()) ;
        }
    }


    /**
     * Gets an LDIF record out of this MasterTable corresponding to an entry
     * with a id.
     *
     * @param an_id the BigInteger id of the entry to retrieve.
     * @return the LDIF of the entry with operational attributes and all.
     * @throws BackendException if there is a read error on the underlying Db.
     */
    public String get(BigInteger an_id)
        throws BackendException
    {
        return (String) super.get(an_id) ;
    }


    /**
     * Puts a ldif into the master table at an index specified by an_id.  This
     * is used both to create new entries and update existing ones.
     *
     * @param a_ldif the LDIF of the entry with operational attributes and all.
     * @param an_id the BigInteger id of the entry to put.
     * @throws BackendException if there is a write error on the underlying Db.
     */
    public String put(String a_ldif, BigInteger an_id)
        throws BackendException
    {
        return (String) super.put(an_id, a_ldif) ;
    }


    /**
     * Deletes a entry from the master table at an index specified by an_id.
     *
     * @param an_id the BigInteger id of the entry to delete.
     * @throws BackendException if there is a write error on the underlying Db.
     */
    public String del(BigInteger an_id)
        throws BackendException, NamingException
    {
        return (String) super.remove(an_id) ;
    }


    /**
     * Get's the current id value from this master database's sequence without
     * affecting the seq.
     *
     * @return the current value.
     * @throws BackendException if the admin table storing sequences cannot be
     * read.
     */
    public BigInteger getCurrentId()
        throws BackendException
    {
        BigInteger l_id = null ;

        synchronized(m_adminTbl) {
            l_id = new BigInteger((String) m_adminTbl.get(m_sequence)) ;
            if(null == l_id) {
                m_adminTbl.put(m_sequence, BigInteger.ZERO.toString()) ;
                l_id = BigInteger.ZERO ;
            }
        }

        return l_id ;
    }


    /**
     * Get's the next value from this SequenceBDb.  This has the side-effect of
     * changing the current sequence values perminantly in memory and on disk.
     *
     * @return the current value incremented by one.
     * @throws BackendException if the admin table storing sequences cannot be
     * read and writen to.
     */
    public BigInteger getNextId()
        throws BackendException
    {
        BigInteger l_lastVal = null ;
        BigInteger l_nextVal = null ;

        synchronized(m_adminTbl) {
            l_lastVal = new BigInteger((String) m_adminTbl.get(m_sequence)) ;
            if(null == l_lastVal) {
                m_adminTbl.put(m_sequence, BigInteger.ONE.toString()) ;
                return BigInteger.ONE ;
            } else {
                l_nextVal = l_lastVal.add(BigInteger.ONE) ;
                m_adminTbl.put(m_sequence, l_nextVal.toString()) ;
            }
        }

        return (l_nextVal) ;
    }


    /**
     * Gets a persistant property stored in the admin table of this MasterTable.
     *
     * @param a_property the key of the property to get the value of
     * @return the value of the property
     * @throws BackendException when the underlying admin table cannot be read
     */
	public String getProperty(String a_property)
        throws BackendException
    {
        synchronized(m_adminTbl) {
    	    return (String) m_adminTbl.get(a_property) ;
        }
    }


    /**
     * Sets a persistant property stored in the admin table of this MasterTable.
     *
     * @param a_property the key of the property to set the value of
     * @param a_value the value of the property
     * @throws BackendException when the underlying admin table cannot be writen
     */
	public void setProperty(String a_property, String a_value)
        throws BackendException
    {
        synchronized(m_adminTbl) {
            m_adminTbl.put(a_property, a_value) ;
        }
    }
}
