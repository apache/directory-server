/*
 * $Id: IndexRecord.java,v 1.3 2003/03/13 18:27:26 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.index;


import jdbm.helper.Tuple ;
import java.math.BigInteger ;


public class IndexRecord
{
    private final Tuple m_tuple = new Tuple() ;


    public void setTuple(Tuple a_tuple)
    {
        m_tuple.setKey(a_tuple.getKey()) ;
        m_tuple.setValue(a_tuple.getValue()) ;
    }


    public void setSwapped(Tuple a_tuple)
    {
        m_tuple.setKey(a_tuple.getValue()) ;
        m_tuple.setValue(a_tuple.getKey()) ;
    }


    public BigInteger getEntryId()
    {
        return (BigInteger) m_tuple.getValue() ;
    }


    public Object getIndexKey()
    {
        return m_tuple.getKey() ;
    }


    public void setEntryId(BigInteger a_id)
    {
        m_tuple.setValue(a_id) ;
    }


    public void setIndexKey(Object a_key)
    {
        m_tuple.setKey(a_key) ;
    }
}

