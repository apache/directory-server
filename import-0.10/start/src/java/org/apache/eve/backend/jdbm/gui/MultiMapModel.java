/*
 * $Id: MultiMapModel.java,v 1.2 2003/03/13 18:27:24 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.gui ;


import java.util.ArrayList ;
import javax.swing.table.AbstractTableModel ;
import org.apache.commons.collections.MultiMap;
import java.util.Iterator;
import java.util.Collection;


public class MultiMapModel
    extends AbstractTableModel
{
    public static final String KEY_COL = "Keys" ;
    public static final String VAL_COL = "Values" ;

    private final MultiMap m_map ;
    private final ArrayList m_keys ;
    private final int m_rowCount ;
    private final ArrayList m_vals ;


    public MultiMapModel(MultiMap a_map)
    {
        this.m_map = a_map ;
        m_rowCount = m_map.values().size() ;
        this.m_keys = new ArrayList(m_rowCount) ;
        this.m_vals = new ArrayList(m_rowCount) ;

        Iterator l_keyList = m_map.keySet().iterator() ;
        while(l_keyList.hasNext()) {
            String l_key = (String) l_keyList.next() ;
            Iterator l_valList = ((Collection) m_map.get(l_key)).iterator() ;
            while(l_valList.hasNext()) {
                String l_val = l_valList.next().toString() ;
                m_keys.add(l_key) ;
                m_vals.add(l_val) ;
            }
        }
    }


    public String getColumnName(int a_col)
    {
        if(a_col == 0) {
            return KEY_COL ;
        } else if(a_col == 1) {
            return VAL_COL ;
        } else {
            throw new RuntimeException("There can only be 2 columns at index "
                + "0 and at 1") ;
        }
    }


    public int getRowCount()
    {
        return m_rowCount ;
    }


    public int getColumnCount()
    {
        return 2 ;
    }


    public Object getValueAt(int a_row, int a_col)
    {
		if(a_row >= m_rowCount) {
			return("NULL") ;
		}

        if(this.getColumnName(a_col).equals(KEY_COL)) {
            return this.m_keys.get(a_row) ;
        } else if(this.getColumnName(a_col).equals(VAL_COL)) {
            return this.m_vals.get(a_row) ;
        } else {
            throw new RuntimeException("You did not correctly set col names") ;
        }
    }
}
