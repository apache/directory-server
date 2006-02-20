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
package org.apache.directory.server.core.partition.impl.btree.gui;


import java.math.BigInteger;
import java.util.ArrayList;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.swing.table.AbstractTableModel;


/**
 * A general purpose table model for entry attributes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AttributesTableModel extends AbstractTableModel
{
    private static final long serialVersionUID = 3256443603340310841L;
    /** name for the key column */
    public static final String KEY_COL = "Keys";
    /** name for the values column */
    public static final String VAL_COL = "Values";

    /** list of attribute ids */
    private final transient ArrayList keyList;
    /** list of attribute values */
    private final transient ArrayList valList;

    /** the attributes for the entry */
    private final Attributes entry;
    /** the unique id of the entry  */
    private final BigInteger id;
    /** the distinguished name of the entry */
    private final String dn;
    /** whether or not the model is mutable */
    private boolean isMutable = true;


    /**
     * Creates a table model for entry attributes.
     *
     * @param entry the entry to create a model for
     * @param id the id for the entry
     * @param dn the distinguished name of the entry
     * @param isMutable whether or not the model can be changed
     */
    public AttributesTableModel(Attributes entry, BigInteger id, String dn, boolean isMutable)
    {
        this.dn = dn;
        this.id = id;
        this.entry = entry;
        this.isMutable = isMutable;

        NamingEnumeration list = entry.getIDs();
        int rowCount = 0;

        while ( list.hasMoreElements() )
        {
            String attrId = ( String ) list.nextElement();
            rowCount = rowCount + entry.get( attrId ).size();
        }

        keyList = new ArrayList( rowCount );
        valList = new ArrayList( rowCount );

        list = this.entry.getIDs();
        while ( list.hasMoreElements() )
        {
            String l_key = ( String ) list.nextElement();
            Attribute l_attr = this.entry.get( l_key );

            for ( int ii = 0; ii < l_attr.size(); ii++ )
            {
                try
                {
                    keyList.add( l_attr.getID() );
                    valList.add( l_attr.get( ii ) );
                }
                catch ( NamingException e )
                {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * @see AbstractTableModel#getColumnName(int)
     */
    public String getColumnName( int col )
    {
        if ( col == 0 )
        {
            return KEY_COL;
        }
        else if ( col == 1 )
        {
            return VAL_COL;
        }
        else
        {
            throw new RuntimeException( "There can only be 2 columns at index " + "0 and at 1" );
        }
    }


    /**
     * @see javax.swing.table.AbstractTableModel#getRowCount()
     */
    public int getRowCount()
    {
        return keyList.size();
    }


    /**
     * @see javax.swing.table.AbstractTableModel#getColumnCount()
     */
    public int getColumnCount()
    {
        return 2;
    }


    /**
     * @see AbstractTableModel#getColumnClass(int)
     */
    public Class getColumnClass( int c )
    {
        return String.class;
    }


    /**
     * @see AbstractTableModel#isCellEditable(int, int)
     */
    public boolean isCellEditable( int row, int col )
    {
        return isMutable;
    }


    /**
     * @see AbstractTableModel#getValueAt(int, int)
     */
    public Object getValueAt( int row, int col )
    {
        if ( row >= keyList.size() )
        {
            return ( "NULL" );
        }

        if ( getColumnName( col ).equals( KEY_COL ) )
        {
            return keyList.get( row );
        }
        else if ( getColumnName( col ).equals( VAL_COL ) )
        {
            return valList.get( row );
        }
        else
        {
            throw new RuntimeException( "You didn't correctly set col names" );
        }
    }


    /**
     * @see AbstractTableModel#setValueAt(Object, int, int)
     */
    public void setValue( Object val, int row, int col )
    {
        ArrayList list = null;

        if ( col > 1 || col < 0 )
        {
            return;
        }
        else if ( col == 0 )
        {
            list = keyList;
        }
        else
        {
            list = valList;
        }

        if ( row >= keyList.size() )
        {
            return;
        }

        list.set( row, val );
        fireTableCellUpdated( row, col );
    }


    /**
     * Gets the distinguished name of the entry.
     *
     * @return the distinguished name of the entry
     */
    public String getEntryDn()
    {
        return dn;
    }


    /**
     * Gets the unique id for the entry.
     *
     * @return the unique id for the entry
     */
    public BigInteger getEntryId()
    {
        return id;
    }


    /**
     * Deletes a row within the table model.
     *
     * @param row the row index to delete
     */
    public void delete( int row )
    {
        if ( row >= keyList.size() || row < 0 )
        {
            return;
        }

        keyList.remove( row );
        valList.remove( row );
        fireTableRowsDeleted( row, row );
    }


    /**
     * Inserts an attribute key/value into the table model.
     *
     * @param row the row index to insert into
     * @param key the key of the attr to insert
     * @param val the value of the attr to insert
     */
    public void insert( int row, Object key, Object val )
    {
        if ( row >= keyList.size() || row < 0 )
        {
            return;
        }

        keyList.add( row, key );
        valList.add( row, val );
        fireTableRowsInserted( row, row );
    }
}
