/*
 * $Id: Index.java,v 1.6 2003/03/13 18:27:25 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.index ;

import java.math.BigInteger ;
import javax.naming.NamingException ;

import org.apache.regexp.RE ;

import org.apache.eve.backend.Cursor ;
import org.apache.eve.backend.BackendException ;


/**
 * Doc me!
 * @todo Doc me!
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.6 $
 */
public interface Index
{
    public String getAttribute() ;

    public String getFilePath() ;

    public int count()
        throws BackendException, NamingException ;

    public int count(Object an_attrVal)
        throws BackendException, NamingException ;

    public int count(Object an_attrVal, boolean isGreaterThan)
        throws BackendException, NamingException ;

    public BigInteger getForward(Object an_attrVal)
        throws BackendException, NamingException ;

    public Object getReverse(BigInteger a_id)
        throws BackendException ;

	public void add(Object an_attrVal, BigInteger a_id)
        throws BackendException, NamingException ;

	public void drop(Object an_attrVal, BigInteger a_id)
        throws BackendException, NamingException ;


    ///////////////////////
    // Cursor Operations //
    ///////////////////////

    /*
    public IndexCursor getReverseCursor()
        throws BackendException, NamingException ;
        */

    public IndexCursor getReverseCursor(BigInteger a_id)
        throws BackendException, NamingException ;

    public IndexCursor getCursor()
        throws BackendException, NamingException ;

    public IndexCursor getCursor(Object an_attrVal)
        throws BackendException, NamingException ;

    public IndexCursor getCursor(Object an_attrVal, boolean isGreaterThan)
        throws BackendException, NamingException ;

    public IndexCursor getCursor(RE a_regex)
        throws BackendException, NamingException ;

    public IndexCursor getCursor(RE a_regex, String a_prefix)
        throws BackendException, NamingException ;

    public boolean hasValue(Object an_attrVal, BigInteger a_id)
        throws BackendException, NamingException ;

    public boolean hasValue(Object an_attrVal, BigInteger a_id,
        boolean isGreaterThan)
        throws BackendException, NamingException ;

    public boolean hasValue(RE a_regex, BigInteger a_id)
        throws BackendException, NamingException ;

    public void close()
        throws BackendException ;

    public void sync()
        throws BackendException ;
}
