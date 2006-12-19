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
package org.apache.directory.mitosis.store.derby;


import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.naming.Name;
import javax.naming.ldap.LdapName;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.common.CSNVector;
import org.apache.directory.mitosis.common.ReplicaId;
import org.apache.directory.mitosis.common.DefaultCSN;
import org.apache.directory.mitosis.common.UUID;
import org.apache.directory.mitosis.configuration.ReplicationConfiguration;
import org.apache.directory.mitosis.operation.Operation;
import org.apache.directory.mitosis.operation.OperationCodec;
import org.apache.directory.mitosis.store.ReplicationLogIterator;
import org.apache.directory.mitosis.store.ReplicationStore;
import org.apache.directory.mitosis.store.ReplicationStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DerbyReplicationStore implements ReplicationStore
{
    private static final Logger log = LoggerFactory.getLogger( DerbyReplicationStore.class );

    private static final String DEFAULT_TABLE_PREFIX = "REPLICATION_";
    private static final String KEY_REPLICA_ID = "replicaId";

    private static final String DRIVER_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
    private static final String DB_URI_PREFIX = "jdbc:derby:";

    private String dbURI;
    private BasicDataSource dataSource;
    private ReplicaId replicaId;
    private String tablePrefix = DEFAULT_TABLE_PREFIX;
    private String metadataTableName;
    private String uuidTableName;
    private String logTableName;
    private Set<ReplicaId> knownReplicaIds;
    private final Object knownReplicaIdsLock = new Object();
    private final OperationCodec operationCodec = new OperationCodec();


    public String getTablePrefix()
    {
        return tablePrefix;
    }


    public void setTablePrefix( String tablePrefix )
    {
        if ( tablePrefix == null )
        {
            tablePrefix = DEFAULT_TABLE_PREFIX;
        }

        tablePrefix = tablePrefix.trim();
        if ( tablePrefix.length() == 0 )
        {
            tablePrefix = DEFAULT_TABLE_PREFIX;
        }

        this.tablePrefix = tablePrefix;
    }


    public void open( DirectoryServiceConfiguration serviceCfg, ReplicationConfiguration cfg )
    {
        replicaId = cfg.getReplicaId();

        // Calculate DB URI
        dbURI = DB_URI_PREFIX + serviceCfg.getStartupConfiguration().getWorkingDirectory().getPath() + File.separator
            + "replication";

        // Create database if not exists.
        try
        {
            Class.forName( DRIVER_NAME );
            Connection con = DriverManager.getConnection( dbURI + ";create=true" );
            con.close();
        }
        catch ( Exception e )
        {
            throw new ReplicationStoreException( "Failed to initialize Derby database.", e );
        }

        // Initialize DataSource
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName( DRIVER_NAME );
        dataSource.setUrl( dbURI );
        dataSource.setUsername( "sa" );
        dataSource.setPassword( "" );
        this.dataSource = dataSource;

        // Pre-calculate table names
        metadataTableName = tablePrefix + "METADATA";
        uuidTableName = tablePrefix + "UUID";
        logTableName = tablePrefix + "LOG";

        initSchema();
        loadMetadata();
    }


    private void initSchema()
    {
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;

        try
        {
            con = dataSource.getConnection();
            con.setAutoCommit( true );

            stmt = con.createStatement();

            try
            {
                rs = stmt.executeQuery( "SELECT M_KEY FROM " + metadataTableName + " WHERE M_KEY IS NULL" );
                rs.close();
                rs = null;
            }
            catch ( SQLException e )
            {
                stmt.executeUpdate( "CREATE TABLE " + metadataTableName + " ("
                    + "    M_KEY VARCHAR(30) NOT NULL PRIMARY KEY," + "    M_VALUE VARCHAR(100) NOT NULL )" );
            }

            try
            {
                rs = stmt.executeQuery( "SELECT UUID FROM " + uuidTableName + " WHERE UUID IS NULL" );
                rs.close();
                rs = null;
            }
            catch ( SQLException e )
            {
                stmt.executeUpdate( "CREATE TABLE " + uuidTableName + " (" + "    UUID CHAR(32) NOT NULL PRIMARY KEY,"
                    + "    DN CLOB NOT NULL" + ")" );
            }

            try
            {
                rs = stmt.executeQuery( "SELECT CSN_REPLICA_ID FROM " + logTableName + " WHERE CSN_REPLICA_ID IS NULL" );
                rs.close();
                rs = null;
            }
            catch ( SQLException e )
            {
                stmt.executeUpdate( "CREATE TABLE " + logTableName + " (" + "    CSN_REPLICA_ID VARCHAR(16) NOT NULL,"
                    + "    CSN_TIMESTAMP BIGINT NOT NULL," + "    CSN_OP_SEQ INTEGER NOT NULL,"
                    + "    OPERATION BLOB NOT NULL," + "CONSTRAINT " + logTableName + "_PK PRIMARY KEY ("
                    + "    CSN_REPLICA_ID," + "    CSN_TIMESTAMP," + "    CSN_OP_SEQ)" + ")" );
            }
        }
        catch ( SQLException e )
        {
            throw new ReplicationStoreException( "Failed to initialize DB schema.", e );
        }
        finally
        {
            SQLUtil.cleanup( con, stmt, rs );
        }
    }


    private void loadMetadata()
    {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            con = dataSource.getConnection();
            con.setAutoCommit( true );
            con.setTransactionIsolation( Connection.TRANSACTION_REPEATABLE_READ );
            con.setReadOnly( true );

            // Check if replicaId is already registered
            ps = con.prepareStatement( "SELECT M_VALUE FROM " + metadataTableName + " WHERE M_KEY=?" );
            ps.setString( 1, KEY_REPLICA_ID );
            rs = ps.executeQuery();
            if ( rs.next() )
            {
                // If already registered, match it with what user specified.
                String actualReplicaId = rs.getString( 1 );
                if ( !replicaId.getId().equalsIgnoreCase( actualReplicaId ) )
                {
                    throw new ReplicationStoreException( "Replica ID mismatches: " + actualReplicaId + " (expected: "
                        + replicaId + ")" );
                }
            }
            else
            {
                rs.close();
                rs = null;
                ps.close();
                ps = null;

                con.setReadOnly( false );
                // If not registered yet, register with what user specified.
                ps = con.prepareStatement( "INSERT INTO " + metadataTableName + " (M_KEY, M_VALUE) VALUES (?,?)" );
                ps.setString( 1, KEY_REPLICA_ID );
                ps.setString( 2, replicaId.getId() );
                ps.executeUpdate();
            }

            if ( rs != null )
            {
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

            // Get known replica IDs.
            ps = con.prepareStatement( "SELECT DISTINCT CSN_REPLICA_ID FROM " + logTableName );
            rs = ps.executeQuery();
            knownReplicaIds = new HashSet<ReplicaId>();
            while ( rs.next() )
            {
                knownReplicaIds.add( new ReplicaId( rs.getString( 1 ) ) );
            }
        }
        catch ( Exception e )
        {
            if ( e instanceof ReplicationStoreException )
            {
                throw ( ReplicationStoreException ) e;
            }
            throw new ReplicationStoreException( e );
        }
        finally
        {
            SQLUtil.cleanup( con, ps, rs );
        }
    }


    public void close()
    {
        try
        {
            dataSource.close();
        }
        catch ( SQLException e )
        {
            log.warn( "Failed to close the dataSource.", e );
        }
        dataSource = null;
        replicaId = null;

        try
        {
            DriverManager.getConnection( dbURI + ";shutdown=true" );
        }
        catch ( Exception e )
        {
            // An exception is thrown always.
        }
    }


    public ReplicaId getReplicaId()
    {
        return replicaId;
    }


    public Set<ReplicaId> getKnownReplicaIds()
    {
        return new HashSet<ReplicaId>( knownReplicaIds );
    }


    public Name getDN( UUID uuid )
    {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            con = dataSource.getConnection();
            con.setTransactionIsolation( Connection.TRANSACTION_READ_COMMITTED );
            con.setReadOnly( true );
            ps = con.prepareStatement( "SELECT DN FROM " + uuidTableName + " WHERE UUID=?" );
            ps.setString( 1, uuid.toOctetString() );
            rs = ps.executeQuery();
            if ( rs.next() )
            {
                return new LdapName( rs.getString( 1 ) );
            }
            else
            {
                return null;
            }
        }
        catch ( Exception e )
        {
            throw new ReplicationStoreException( e );
        }
        finally
        {
            SQLUtil.cleanup( con, ps, rs );
        }
    }


    public boolean putUUID( UUID uuid, Name dn )
    {
        String uuidString = uuid.toOctetString();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            con = dataSource.getConnection();
            con.setAutoCommit( false );
            con.setTransactionIsolation( Connection.TRANSACTION_REPEATABLE_READ );
            con.setReadOnly( true );

            // Check if the specified uuid already exists
            ps = con.prepareStatement( "SELECT UUID FROM " + uuidTableName + " WHERE UUID=?" );
            ps.setString( 1, uuidString );
            rs = ps.executeQuery();
            if ( rs.next() )
            {
                return false;
            }

            rs.close();
            rs = null;

            // insert
            con.setReadOnly( false );
            ps = con.prepareStatement( "INSERT INTO " + uuidTableName + " (UUID, DN) VALUES(?,?)" );
            ps.setString( 1, uuidString );
            ps.setString( 2, dn.toString() );

            int updateCnt = ps.executeUpdate();
            con.commit();
            return updateCnt == 1;
        }
        catch ( Exception e )
        {
            try
            {
                con.rollback();
            }
            catch ( SQLException e1 )
            {
            }

            throw new ReplicationStoreException( e );
        }
        finally
        {
            SQLUtil.cleanup( con, ps, rs );
        }
    }


    public boolean removeUUID( UUID uuid )
    {
        String uuidString = uuid.toOctetString();
        Connection con = null;
        PreparedStatement ps = null;

        try
        {
            con = dataSource.getConnection();
            con.setAutoCommit( true );
            con.setTransactionIsolation( Connection.TRANSACTION_READ_UNCOMMITTED );
            con.setReadOnly( false );

            // Check if the specified uuid already exists
            ps = con.prepareStatement( "DELETE FROM " + uuidTableName + " WHERE UUID=?" );
            ps.setString( 1, uuidString );
            return ps.executeUpdate() == 1;
        }
        catch ( Exception e )
        {
            throw new ReplicationStoreException( e );
        }
        finally
        {
            SQLUtil.cleanup( con, ps, null );
        }
    }


    public void putLog( Operation op )
    {
        CSN csn = op.getCSN();
        byte[] encodedOp = operationCodec.encode( op );
        Connection con = null;
        PreparedStatement ps = null;

        try
        {
            con = dataSource.getConnection();
            con.setAutoCommit( true );
            con.setTransactionIsolation( Connection.TRANSACTION_READ_UNCOMMITTED );
            con.setReadOnly( false );

            // Check if the specified uuid already exists
            ps = con.prepareStatement( "INSERT INTO " + logTableName
                + " (CSN_REPLICA_ID, CSN_TIMESTAMP, CSN_OP_SEQ, OPERATION) VALUES(?,?,?,?)" );
            ps.setString( 1, csn.getReplicaId().getId() );
            ps.setLong( 2, csn.getTimestamp() );
            ps.setInt( 3, csn.getOperationSequence() );
            ps.setBytes( 4, encodedOp );
            if ( ps.executeUpdate() != 1 )
            {
                throw new ReplicationStoreException( "Failed to insert a row." );
            }
        }
        catch ( Exception e )
        {
            if ( e instanceof ReplicationStoreException )
            {
                throw ( ReplicationStoreException ) e;
            }

            throw new ReplicationStoreException( e );
        }
        finally
        {
            SQLUtil.cleanup( con, ps, null );
        }

        if ( !knownReplicaIds.contains( csn.getReplicaId() ) )
        {
            synchronized ( knownReplicaIdsLock )
            {
                Set<ReplicaId> newKnownReplicaIds = new HashSet<ReplicaId>( knownReplicaIds );
                newKnownReplicaIds.add( csn.getReplicaId() );
                knownReplicaIds = newKnownReplicaIds;
            }
        }
    }


    public ReplicationLogIterator getLogs( CSNVector updateVector, boolean inclusive )
    {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        updateVector = getNormalizedUpdateVector( updateVector );

        StringBuffer buf = new StringBuffer( "SELECT CSN_REPLICA_ID, CSN_TIMESTAMP, CSN_OP_SEQ, OPERATION FROM "
            + logTableName + " " );

        if ( updateVector.size() > 0 )
        {
            buf.append( "WHERE " );
            for ( int i = updateVector.size();; )
            {
                buf.append( "( CSN_REPLICA_ID = ? AND (CSN_TIMESTAMP = ? AND CSN_OP_SEQ >" + ( inclusive ? "=" : "" )
                    + " ? OR CSN_TIMESTAMP > ?) ) " );
                i--;
                if ( i == 0 )
                {
                    break;
                }
                else
                {
                    buf.append( "OR " );
                }

            }
        }
        buf.append( "ORDER BY CSN_TIMESTAMP ASC, CSN_OP_SEQ ASC" );

        String query = buf.toString();

        try
        {
            con = dataSource.getConnection();
            con.setAutoCommit( true );
            con.setTransactionIsolation( Connection.TRANSACTION_READ_UNCOMMITTED );
            con.setReadOnly( true );

            // Check if the specified uuid already exists
            ps = con.prepareStatement( query );

            Iterator i = updateVector.getReplicaIds().iterator();
            int paramIdx = 1;
            while ( i.hasNext() )
            {
                ReplicaId replicaId = ( ReplicaId ) i.next();
                CSN csn = updateVector.getCSN( replicaId );
                ps.setString( paramIdx++, replicaId.getId() );
                ps.setLong( paramIdx++, csn.getTimestamp() );
                ps.setInt( paramIdx++, csn.getOperationSequence() );
                ps.setLong( paramIdx++, csn.getTimestamp() );
            }
            rs = ps.executeQuery();

            return new DerbyReplicationLogIterator( operationCodec, con, ps, rs );
        }
        catch ( Exception e )
        {
            throw new ReplicationStoreException( e );
        }
    }


    private CSNVector getNormalizedUpdateVector( CSNVector updateVector )
    {
        CSNVector newUV = new CSNVector();
        synchronized ( knownReplicaIds )
        {
            Iterator<ReplicaId> i = knownReplicaIds.iterator();
            while ( i.hasNext() )
            {
                newUV.setCSN( new DefaultCSN( 0, i.next(), 0 ) );
            }
        }

        newUV.setAllCSN( updateVector );
        return newUV;
    }


    public ReplicationLogIterator getLogs( CSN fromCSN, boolean inclusive )
    {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            con = dataSource.getConnection();
            con.setAutoCommit( true );
            con.setTransactionIsolation( Connection.TRANSACTION_READ_UNCOMMITTED );
            con.setReadOnly( true );

            // Check if the specified uuid already exists
            ps = con
                .prepareStatement( "SELECT CSN_REPLICA_ID, CSN_TIMESTAMP, CSN_OP_SEQ, OPERATION FROM " + logTableName
                    + " " + "WHERE CSN_REPLICA_ID = ? AND (CSN_TIMESTAMP = ? AND CSN_OP_SEQ >"
                    + ( inclusive ? "=" : "" ) + " ? OR CSN_TIMESTAMP > ?) "
                    + "ORDER BY CSN_TIMESTAMP ASC, CSN_OP_SEQ ASC" );
            ps.setString( 1, fromCSN.getReplicaId().getId() );
            ps.setLong( 2, fromCSN.getTimestamp() );
            ps.setInt( 3, fromCSN.getOperationSequence() );
            ps.setLong( 4, fromCSN.getTimestamp() );
            rs = ps.executeQuery();

            return new DerbyReplicationLogIterator( operationCodec, con, ps, rs );
        }
        catch ( Exception e )
        {
            throw new ReplicationStoreException( e );
        }
    }


    public int removeLogs( CSN toCSN, boolean inclusive )
    {
        Connection con = null;
        PreparedStatement ps = null;

        try
        {
            con = dataSource.getConnection();
            con.setAutoCommit( true );
            con.setTransactionIsolation( Connection.TRANSACTION_READ_UNCOMMITTED );
            con.setReadOnly( false );

            // Check if the specified uuid already exists
            ps = con.prepareStatement( "DELETE FROM " + logTableName + " WHERE "
                + "CSN_REPLICA_ID = ? AND (CSN_TIMESTAMP = ? AND CSN_OP_SEQ <" + ( inclusive ? "=" : "" )
                + " ? OR CSN_TIMESTAMP < ?)" );
            ps.setString( 1, toCSN.getReplicaId().getId() );
            ps.setLong( 2, toCSN.getTimestamp() );
            ps.setInt( 3, toCSN.getOperationSequence() );
            ps.setLong( 4, toCSN.getTimestamp() );
            return ps.executeUpdate();
        }
        catch ( Exception e )
        {
            throw new ReplicationStoreException( e );
        }
        finally
        {
            SQLUtil.cleanup( con, ps, null );
        }
    }


    public int getLogSize()
    {
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;

        try
        {
            con = dataSource.getConnection();
            con.setTransactionIsolation( Connection.TRANSACTION_READ_COMMITTED );
            con.setReadOnly( true );
            stmt = con.createStatement();
            rs = stmt.executeQuery( "SELECT COUNT(*) FROM " + logTableName );
            rs.next();
            return rs.getInt( 1 );
        }
        catch ( Exception e )
        {
            throw new ReplicationStoreException( e );
        }
        finally
        {
            SQLUtil.cleanup( con, stmt, rs );
        }
    }


    public int getLogSize( ReplicaId replicaId )
    {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            con = dataSource.getConnection();
            con.setTransactionIsolation( Connection.TRANSACTION_READ_COMMITTED );
            con.setReadOnly( true );
            ps = con.prepareStatement( "SELECT COUNT(*) FROM " + logTableName + " WHERE CSN_REPLICA_ID=?" );
            ps.setString( 1, replicaId.getId() );
            rs = ps.executeQuery();
            rs.next();
            return rs.getInt( 1 );
        }
        catch ( Exception e )
        {
            throw new ReplicationStoreException( e );
        }
        finally
        {
            SQLUtil.cleanup( con, ps, rs );
        }
    }


    public CSNVector getUpdateVector()
    {
        return getVector( false );
    }


    public CSNVector getPurgeVector()
    {
        return getVector( true );
    }


    private CSNVector getVector( boolean min )
    {
        final String ORDER = min ? "ASC" : "DESC";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        CSNVector result = new CSNVector();

        try
        {
            con = dataSource.getConnection();
            con.setTransactionIsolation( Connection.TRANSACTION_READ_COMMITTED );
            con.setReadOnly( true );
            ps = con.prepareStatement( "SELECT CSN_TIMESTAMP, CSN_OP_SEQ FROM " + logTableName
                + " WHERE CSN_REPLICA_ID=? ORDER BY CSN_TIMESTAMP " + ORDER + ", CSN_OP_SEQ " + ORDER );

            Iterator<ReplicaId> it = knownReplicaIds.iterator();
            while ( it.hasNext() )
            {
                ReplicaId replicaId = it.next();
                ps.setString( 1, replicaId.getId() );
                rs = ps.executeQuery();
                if ( rs.next() )
                {
                    result.setCSN( new DefaultCSN( rs.getLong( 1 ), replicaId, rs.getInt( 2 ) ) );
                }
                rs.close();
                rs = null;
                ps.clearParameters();
            }

            return result;
        }
        catch ( Exception e )
        {
            throw new ReplicationStoreException( e );
        }
        finally
        {
            SQLUtil.cleanup( con, ps, rs );
        }
    }
}
