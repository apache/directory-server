/*
 * $Id: JdbmTableModel.java,v 1.2 2003/03/13 18:27:24 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.gui ;


import java.util.ArrayList ;
import javax.swing.table.AbstractTableModel ;


public class JdbmTableModel
    extends AbstractTableModel
{
    public static final String KEY_COL = "Keys" ;
    public static final String VAL_COL = "Values" ;

    private ArrayList m_keyList ;
    private ArrayList m_valList ;


    public JdbmTableModel(ArrayList a_keyList, ArrayList a_valList)
    {
        this.m_keyList = a_keyList ;
        this.m_valList = a_valList ;
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
        return m_keyList.size() ;
    }


    public int getColumnCount()
    {
        return 2 ;
    }


    public Object getValueAt(int a_row, int a_col)
    {
		if(a_row >= m_keyList.size()) {
			return("NULL") ;
		}

        if(this.getColumnName(a_col).equals(KEY_COL)) {
            return this.m_keyList.get(a_row) ;
        } else if(this.getColumnName(a_col).equals(VAL_COL)) {
            return this.m_valList.get(a_row) ;
        } else {
            throw new RuntimeException("You did not correctly set col names") ;
        }
    }
}
