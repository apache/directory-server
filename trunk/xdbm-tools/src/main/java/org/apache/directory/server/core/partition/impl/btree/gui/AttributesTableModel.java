/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.partition.impl.btree.gui;


import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Value;


/**
 * A general purpose table model for entry attributes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AttributesTableModel extends AbstractTableModel
{
    private static final long serialVersionUID = 3256443603340310841L;

    /** name for the key column */
    public static final String KEY_COL = "Keys";

    /** name for the values column */
    public static final String VAL_COL = "Values";

    /** list of attribute ids */
    private final ArrayList<Object> keyList;

    /** list of attribute values */
    private final ArrayList<Object> valList;

    /** the unique id of the entry  */
    private final Long id;

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
    public AttributesTableModel( Entry entry, Long id, String dn, boolean isMutable )
    {
        this.dn = dn;
        this.id = id;
        this.isMutable = isMutable;

        int rowCount = 0;

        for ( Attribute attribute : entry )
        {
            String attrId = attribute.getId();
            rowCount = rowCount + entry.get( attrId ).size();
        }

        keyList = new ArrayList<Object>( rowCount );
        valList = new ArrayList<Object>( rowCount );

        for ( Attribute attribute : entry )
        {
            for ( Value<?> value : attribute )
            {
                keyList.add( attribute.getId() );
                valList.add( value.getValue() );
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
            throw new RuntimeException( I18n.err( I18n.ERR_728 ) );
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
    public Class<String> getColumnClass( int c )
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
            throw new RuntimeException( I18n.err( I18n.ERR_729 ) );
        }
    }


    /**
     * @see AbstractTableModel#setValueAt(Object, int, int)
     */
    public void setValue( Object val, int row, int col )
    {
        ArrayList<Object> list = null;

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
    public Long getEntryId()
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
