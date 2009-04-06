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
package org.apache.directory.server.partition.impl.oracle;

import java.io.ByteArrayInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import oracle.jdbc.OracleTypes;
import oracle.sql.BLOB;
import oracle.sql.CLOB;

import org.apache.directory.shared.ldap.util.Base64;

/**
 * This class is used like a one to one mapping for the 
 * LDAP_ATTRIBUTE pl/sql object. It is encoded and decoded
 * directly from the jdbc driver calling the readSQL and
 * writeSQL methods of the SQLData interface.
 */
public class OracleAttribute implements SQLData
{
    private String sqlType= "LDAP_ATTRIBUTE";
    private OracleConnection connection;
    
    private String name;
    private String svalue;
    private byte[] bvalue;
    private String type;
    private Long   bvalueid;
    private Long   cvalueid;
    
    /**
     * Used from jdbc driver to instanciate the class 
     */
    public OracleAttribute ()
    {}
    
    /**
     * Used from OracleEntry when converting a ServerEntry
     * for a string value
     */
    public OracleAttribute (String name, String value, String type, OracleConnection connection)
    {
        this.name= name;
        this.svalue= value;
        this.type= type;
        this.connection= connection;
    }

    /**
     * Used from OracleEntry when converting a ServerEntry
     * for a binary value
     */
    public OracleAttribute (String name, byte[] value, String type, OracleConnection connection)
    {
        this.name= name;
        this.bvalue= value;
        this.type= type;
        this.connection= connection;
    }
    
    /**
     * Sets a connection to read LOBs values
     * @param connection
     */
    public void setConnection(OracleConnection connection)
    {
        this.connection= connection;
    }

    /**
     * @see SQLData
     */
    public String getSQLTypeName() throws SQLException
    {
        return sqlType;
    }

    /**
     * @see SQLData
     */
    public void readSQL( SQLInput stream, String sqlType ) throws SQLException
    {
        this.sqlType= sqlType;
        
        name= stream.readString();
        type= stream.readString();
        svalue= stream.readString();
        bvalue= stream.readBytes();
        cvalueid= stream.readLong();
        bvalueid= stream.readLong();
    }

    /**
     * @see SQLData
     */
    public void writeSQL( SQLOutput stream ) throws SQLException
    {
        stream.writeString(name);
        stream.writeString(type);
        
        boolean clob= (svalue!=null&&svalue.length()>4000);
        boolean blob= (bvalue!=null&&bvalue.length>2000);
        
        if (clob)
            stream.writeString(null);
        else    
            stream.writeString(svalue);
        
        if (blob)
            stream.writeBytes(null);
        else    
            stream.writeBytes(bvalue);
     
        try
        {
            if (clob)
                stream.writeLong(writeClobValue( connection, svalue ));
            else    
                stream.writeLong(0L);
            
            if (blob)
                stream.writeLong(writeBlobValue( connection, bvalue ));
            else    
                stream.writeLong(0L);
        }
        catch (Exception ex)
        {
            throw new SQLException(ex);
        }
        
    }

    /**
     * 
     * @return the attribute name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get the attribute value
     * @return the attribute value (either a String or a byte[])
     * @throws Exception
     */
    public Object getValue()
    throws Exception
    {
        if (svalue!=null)
            return svalue;
        else
        if (bvalue!=null)
            return bvalue;
        else
        if (cvalueid!=0L)
            return readClobValue( connection, cvalueid );
        else
        if (bvalueid!=0L) 
            return readBlobValue( connection, bvalueid );

        return null;
    }
    
    /**
     * Create an hash for a LOB value to check if we already have one
     * in the database
     * 
     * @param val
     * @return
     * @throws Exception
     */
    public static final String hash(byte[] val)
    throws Exception
    {
        MessageDigest md = MessageDigest.getInstance("SHA");
        DigestInputStream digestIn = new DigestInputStream(new ByteArrayInputStream(val), md);
        while (digestIn.read() != -1);
        byte[] digest = md.digest();
        return "{SHA}"+new String(Base64.encode(digest));
    }
    
    /**
     * Reads a CLOB value (used for >4000 string values)
     * 
     * @param connection
     * @param cvalueid
     * @return
     * @throws Exception
     */
    public static final String readClobValue( OracleConnection connection, long cvalueid )
    throws Exception
    {
        OraclePreparedStatement stmt= ( OraclePreparedStatement ) connection.prepareStatement( "select column_value from table(partition_facade.read_clob(?))" );
        stmt.setLong( 1, cvalueid );
        
        OracleResultSet rs= ( OracleResultSet ) stmt.executeQuery();
        
        rs.next();

        String cvalue= rs.getString( 1 );
        
        rs.close();
        stmt.close();
        
        return cvalue;
    }
    
    /**
     * reads a BLOB value (used for >2000 binary values)
     * 
     * @param connection
     * @param bvalueid
     * @return
     * @throws Exception
     */
    public static final byte[] readBlobValue( OracleConnection connection, long bvalueid )
    throws Exception
    {
        OraclePreparedStatement stmt= ( OraclePreparedStatement ) connection.prepareStatement( "select column_value from table(partition_facade.read_blob(?))" );
        stmt.setLong( 1, bvalueid );
        
        OracleResultSet rs= ( OracleResultSet ) stmt.executeQuery();
        
        rs.next();

        byte[] bvalue= rs.getBytes( 1 );
        
        rs.close();
        stmt.close();
        
        return bvalue;
    }
    
    /**
     * Writes a new CLOB value into the database only if cannot
     * find its hash.
     * 
     * @param connection the connection to use
     * @param value the value to write
     * @return the value id
     * @throws Exception
     */
    public static final Long writeClobValue( OracleConnection connection, String value )
    throws Exception
    {
        String hash= hash(value.getBytes());
        
        long cvalueid= 0L;
        OracleCallableStatement stmt= ( OracleCallableStatement ) connection.prepareCall( "begin partition_facade.write_clob(?,?,?); end;" );
        stmt.setString( 1, hash );
        stmt.registerOutParameter( 2, OracleTypes.NUMBER);
        stmt.registerOutParameter( 3, OracleTypes.CLOB);
        
        stmt.execute();
        
        cvalueid= stmt.getLong( 2 );
        
        CLOB c= stmt.getCLOB( 3 );
        c.open( CLOB.MODE_READWRITE );
        c.truncate( 0 );
        c.setAsciiStream( 1 ).write( value.getBytes() );
        c.close();

        stmt.close();
        
        return cvalueid;
    }
    
    /**
     * Writes a new BLOB value into the database only if cannot
     * find its hash.
     * 
     * @param connection the connection to use
     * @param value the value to write
     * @return the value id
     * @throws Exception
     */
    public static final Long writeBlobValue( OracleConnection connection, byte[] value )
    throws Exception
    {
        String hash= hash(value);
        
        long bvalueid= 0L;
        OracleCallableStatement stmt= ( OracleCallableStatement ) connection.prepareCall( "begin partition_facade.write_blob(?,?,?); end;" );
        stmt.setString( 1, hash );
        stmt.registerOutParameter( 2, OracleTypes.NUMBER);
        stmt.registerOutParameter( 3, OracleTypes.BLOB);
        
        stmt.execute();
        
        bvalueid= stmt.getLong( 2 );
        
        BLOB b= stmt.getBLOB( 3 );
        b.open( BLOB.MODE_READWRITE );
        b.truncate( 0 );
        b.setBinaryStream( 1 ).write( value );
        b.close();

        stmt.close();
        
        return bvalueid;
    }
}
