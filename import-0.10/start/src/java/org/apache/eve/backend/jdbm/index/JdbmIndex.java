/*
 * Id: JdbmIndex.java,v 1.10 2003/01/31 04:26:27 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.index ;


import java.io.IOException ;
import java.io.File ;
import java.math.BigInteger ;
import javax.naming.NameParser ;
import javax.naming.NamingException ;

import org.apache.eve.schema.Schema ;
import org.apache.eve.backend.Cursor ;
import org.apache.ldap.common.util.StringTools ;
import org.apache.eve.schema.Normalizer ;
import org.apache.ldap.common.NotImplementedException ;
import org.apache.eve.backend.BackendException ;
import org.apache.eve.backend.jdbm.table.JdbmTable ;
import org.apache.eve.backend.jdbm.table.TableComparator ;

import jdbm.recman.RecordManager ;

import org.apache.regexp.RE ;
import org.apache.commons.collections.LRUMap ;
import org.apache.avalon.framework.logger.Logger ;
import org.apache.avalon.framework.ExceptionUtil ;
import org.apache.avalon.framework.logger.AbstractLogEnabled ;


/**
 * Doc me!
 * @todo Doc me!
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.12 $
 */
public class JdbmIndex
    extends AbstractLogEnabled
    implements Index
{
    public static final String FORWARD_BTREE = "_forward" ;
    public static final String REVERSE_BTREE = "_reverse" ;

    private final boolean isString ;
    private final boolean isDecimal ;
    private final boolean isInteger ;
    private final String m_filePath ;
    private final String m_attribute ;
    private final Normalizer m_normalizer ;

    private JdbmTable m_forward = null ;
    private JdbmTable m_reverse = null ;
    private RecordManager m_recMan = null ;
    private LRUMap m_keyCache = null ;


    public JdbmIndex(String a_attribute, Schema a_schema, String a_wkDirPath)
        throws BackendException, NamingException
    {
        File l_file = new File(a_wkDirPath + File.separator + a_attribute) ;
        m_filePath = l_file.getAbsolutePath() ;
        m_attribute = a_attribute ;

        TableComparator l_tableComp = null ;

        if(a_attribute == Schema.DN_ATTR ||
            a_attribute.equals(Schema.DN_ATTR)) {
            final NameParser l_parser = a_schema.getNormalizingParser() ;
            m_normalizer = new Normalizer() {
                public String getEqualityMatch() {
                    return "distinguishedNameMatch" ;
                }
                public String normalize(String a_str)
                    throws NamingException
                {
                    return l_parser.parse(a_str).toString() ;
                }
            } ;
            this.isDecimal = false ;
            this.isString = true ;
            this.isInteger = false ;
            m_keyCache = new LRUMap(1000) ;
            l_tableComp = IndexComparator.strComp ;
        } else if(a_attribute == Schema.EXISTANCE_ATTR ||
            a_attribute.equals(Schema.EXISTANCE_ATTR)) {
            m_normalizer = new Normalizer() {
                public String getEqualityMatch() {
                    return "caseIgnoreMatch" ;
                }
                public String normalize(String a_str) {
                    return StringTools.deepTrimToLower(a_str) ;
                }
            } ;
            this.isDecimal = false ;
            this.isString = true ;
            this.isInteger = false ;
            m_keyCache = new LRUMap(1000) ;
            l_tableComp = IndexComparator.strComp ;
        } else if(a_attribute == Schema.HIERARCHY_ATTR ||
            a_attribute.equals(Schema.HIERARCHY_ATTR)) {
            m_normalizer = new Normalizer() {
                public String getEqualityMatch() {
                    return "integerMatch" ;
                }
                public String normalize(String a_str) {
                    return a_str ;
                }
            } ;
            this.isDecimal = false ;
            this.isString = false ;
            this.isInteger = true ;
            l_tableComp = IndexComparator.intComp ;
        } else {
            m_normalizer = a_schema.getNormalizer(m_attribute, true) ;
            if(a_schema.isDecimal(m_attribute)) {
                this.isDecimal = true ;
                this.isInteger = false ;
                this.isString = false ;
    
                // We do not have a plan for handling decimal values now!
                throw new NotImplementedException() ;
            } else if(a_schema.isNumeric(m_attribute)) {
                this.isInteger = true ;
                this.isDecimal = false ;
                this.isString = false ;
                l_tableComp = IndexComparator.intComp ;
            } else if(a_schema.isBinary(m_attribute)) {
                throw new IllegalArgumentException(
                    "Cannot create an index on binary attribute: '"
                    + m_attribute + "'") ;
            } else {
                this.isInteger = false ;
                this.isDecimal = false ;
                this.isString = true ;
                l_tableComp = IndexComparator.strComp ;
                m_keyCache = new LRUMap(1000) ;
            }
        }

        try {
            m_recMan = new RecordManager(m_filePath) ;
            m_recMan.disableTransactions() ;
        } catch(IOException e) {
            throw new BackendException("Could not initialize the record "
                + "manager:\n" + ExceptionUtil.printStackTrace(e), e) ;
        }

        m_forward = new JdbmTable(m_attribute + FORWARD_BTREE,
            true, m_recMan, new IndexComparator(l_tableComp, true)) ;
        m_reverse = new JdbmTable(m_attribute + REVERSE_BTREE,
            true, m_recMan, new IndexComparator(l_tableComp, false)) ;
    }


    public void enableLogging(Logger a_logger)
    {
        super.enableLogging(a_logger) ;
        m_forward.enableLogging(a_logger) ;
        m_reverse.enableLogging(a_logger) ;
    }


    public String getAttribute()
    {
        return m_attribute ;
    }


    public String getFilePath()
    {
        return m_filePath ;
    }


    ////////////////////////
    // Scan Count Methods //
    ////////////////////////


    public int count()
        throws BackendException, NamingException
    {
        return m_forward.count() ;
    }


    public Object getCanonical(Object an_attrVal)
        throws NamingException
    {
        if(this.isString) {
            String l_canonical = (String) m_keyCache.get(an_attrVal) ;

            if(null == l_canonical) {
                l_canonical = m_normalizer.normalize((String) an_attrVal) ;

                // Double map it so if we use an already normalized
                // value we can get back the same normalized value.
                // and not have to regenerate a second time.
                m_keyCache.put(an_attrVal, l_canonical) ;
                m_keyCache.put(l_canonical, l_canonical) ;
            }

            return l_canonical ;
        }

        return an_attrVal ;
    }


    public int count(Object an_attrVal)
        throws BackendException, NamingException
    {
        return m_forward.count(getCanonical(an_attrVal)) ;
    }


    public int count(Object an_attrVal, boolean isGreaterThan)
        throws BackendException, NamingException
    {
        return m_forward.count(getCanonical(an_attrVal), isGreaterThan) ;
    }


    /////////////////////////////////
    // Forward and Reverse Lookups //
    /////////////////////////////////


    public BigInteger getForward(Object an_attrVal)
        throws BackendException, NamingException
    {
        return (BigInteger) m_forward.get(getCanonical(an_attrVal)) ;
    }


    public Object getReverse(BigInteger a_id)
        throws BackendException
    {
        return m_reverse.get(a_id) ;
    }


    //////////////////////
    // Add/Drop Methods //
    //////////////////////


    public synchronized void add(Object an_attrVal, BigInteger a_id)
        throws BackendException, NamingException
    {
        m_forward.put(getCanonical(an_attrVal), a_id) ;
        m_reverse.put(a_id, getCanonical(an_attrVal)) ;
    }


    public synchronized void drop(Object an_attrVal, BigInteger a_id)
        throws BackendException, NamingException
    {
        m_forward.remove(getCanonical(an_attrVal), a_id) ;
        m_reverse.remove(a_id, getCanonical(an_attrVal)) ;
    }


    ///////////////////////
    // Cursor Operations //
    ///////////////////////


    public IndexCursor getReverseCursor(BigInteger a_id)
        throws BackendException, NamingException
    {
        IndexCursor l_cursor =
            new IndexCursor(m_reverse.getCursor(a_id), true) ;
        l_cursor.enableLogging(getLogger()) ;
        return l_cursor ;
    }


    public IndexCursor getCursor()
        throws BackendException, NamingException
    {
        IndexCursor l_cursor = new IndexCursor(m_forward.getCursor()) ;
        l_cursor.enableLogging(getLogger()) ;
        return l_cursor ;
    }


    public IndexCursor getCursor(Object an_attrVal)
        throws BackendException, NamingException
    {
        IndexCursor l_cursor =
            new IndexCursor(m_forward.getCursor(getCanonical(an_attrVal))) ;
        l_cursor.enableLogging(getLogger()) ;
        return l_cursor ;
    }


    public IndexCursor getCursor(Object an_attrVal, boolean isGreaterThan)
        throws BackendException, NamingException
    {
        IndexCursor l_cursor = new IndexCursor(
            m_forward.getCursor(getCanonical(an_attrVal), isGreaterThan)) ;
        l_cursor.enableLogging(getLogger()) ;
        return l_cursor ;
    }


    public IndexCursor getCursor(RE a_regex)
        throws BackendException, NamingException
    {
        IndexCursor l_cursor =
            new IndexCursor(m_forward.getCursor(), false, a_regex) ;
        l_cursor.enableLogging(getLogger()) ;
        return l_cursor ;
    }


    public IndexCursor getCursor(RE a_regex, String a_prefix)
        throws BackendException, NamingException
    {
        IndexCursor l_cursor = new IndexCursor(m_forward.
            getCursor(getCanonical(a_prefix), true), false, a_regex) ;
        l_cursor.enableLogging(getLogger()) ;
        return l_cursor ;
    }


    //////////////////////////////////////////////////
    // Value Assertion (a.k.a Index Lookup) Methods //
    //////////////////////////////////////////////////


    public boolean hasValue(Object an_attrVal, BigInteger a_id)
        throws BackendException, NamingException
    {
        return m_forward.has(getCanonical(an_attrVal), a_id) ;
    }


    public boolean hasValue(Object an_attrVal, BigInteger a_id,
        boolean isGreaterThan)
        throws BackendException, NamingException
    {
        return m_forward.has(getCanonical(an_attrVal), a_id, isGreaterThan) ;
    }


    public boolean hasValue(RE a_regex, BigInteger a_id)
        throws BackendException, NamingException
    {
        IndexCursor l_cursor =
            new IndexCursor(m_reverse.getCursor(a_id), true, a_regex) ;
        l_cursor.enableLogging(getLogger()) ;

        boolean hasValue = l_cursor.hasMore() ;
        l_cursor.close() ;
        return hasValue ;
    }


    /////////////////////////
    // Maintenance Methods //
    /////////////////////////


    public synchronized void close()
        throws BackendException
    {
        try {
            m_forward.close() ;
			m_reverse.close() ;
            m_recMan.commit() ;
            m_recMan.close() ;
        } catch(IOException e) {
            throw new BackendException("Encountered error while closing "
                + "backend index file for attribute " + this.m_attribute) ;
        }
    }


    public synchronized void sync()
        throws BackendException
    {
        try {
            m_recMan.commit() ;
        } catch(IOException e) {
            throw new BackendException("Encountered error while closing "
                + "backend index file for attribute " + this.m_attribute) ;
        }
    }
}

