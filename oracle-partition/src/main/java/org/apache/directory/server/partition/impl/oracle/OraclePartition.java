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


import java.sql.ResultSet;
import java.util.Map;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleResultSet;
import oracle.jdbc.driver.OraclePreparedStatement;
import oracle.jdbc.pool.OracleDataSource;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.partition.AbstractPartition;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;

/**
 * A {@link Partition} that stores entries in
 * <a href="http://www.oracle.com/">Oracle</a> database.
 *
 */
public final class OraclePartition extends AbstractPartition
{

    private static final Logger log = LoggerFactory.getLogger( OraclePartition.class ); 
    private static final ThreadLocal<OracleConnectionWrapper> connectionWrapper= new ThreadLocal<OracleConnectionWrapper>();
    
    private int FETCH_SIZE= 3;
    private XStream xstream= new XStream();
    private OracleDataSource dataSource;

    private String id;
    private String suffix;
    private LdapDN normSuffixDN;
    private DirectoryService directoryService;
    private ServerEntry contextEntry;
    
    
    public OraclePartition ()
    {
    }
    
    public ServerEntry getContextEntry()
    {
        return contextEntry;
    }
    
    /*
     * NOTE: do not store registries on startup because they will change 
     * on inizialization
     */
    public Registries getRegistries()
    {
        return directoryService.getRegistries();
    }

    public void setContextEntry( ServerEntry contextEntry )
    {
        this.contextEntry = contextEntry;
    }

    public void setDirectoryService(DirectoryService ds)
    {
        directoryService= ds;
        configureXStream( xstream );        
    }

    /**
     * Configures the <a href="http://xstream.codehaus.org/">XStream</a> instance
     * used to pass filter infos from java to pl/sql
     * 
     * @param xstream: the stream to configure
     */
    public void configureXStream(XStream xstream)
    {
        xstream.registerConverter(new FilterConverter(directoryService));
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.alias("OrNode", org.apache.directory.shared.ldap.filter.OrNode.class);
        xstream.alias("AndNode", org.apache.directory.shared.ldap.filter.AndNode.class);
        xstream.alias("NotNode", org.apache.directory.shared.ldap.filter.NotNode.class);
        xstream.alias("AbstractExprNode", org.apache.directory.shared.ldap.filter.AbstractExprNode.class);
        xstream.alias("ApproximateNode", org.apache.directory.shared.ldap.filter.ApproximateNode.class);
        xstream.alias("AssertionNode", org.apache.directory.shared.ldap.filter.AssertionNode.class);
        xstream.alias("EqualityNode", org.apache.directory.shared.ldap.filter.EqualityNode.class);
        xstream.alias("ExtensibleNode", org.apache.directory.shared.ldap.filter.ExtensibleNode.class);
        xstream.alias("GreaterEqNode", org.apache.directory.shared.ldap.filter.GreaterEqNode.class);
        xstream.alias("LessEqNode", org.apache.directory.shared.ldap.filter.LessEqNode.class);
        xstream.alias("SubstringNode", org.apache.directory.shared.ldap.filter.SubstringNode.class);
        xstream.alias("PresenceNode", org.apache.directory.shared.ldap.filter.PresenceNode.class);
        xstream.alias("ScopeNode", org.apache.directory.shared.ldap.filter.ScopeNode.class);
    }

    /**
     * 
     * @return the configured datasource used to connect to the oracle database
     */
    public OracleDataSource getDataSource()
    {
        return dataSource;
    }

    /**
     * Used by spring configuration to set the OracleDataSource instance 
     * that this partition has to use to connect to the database
     * 
     * @param dataSource
     */
    public void setDataSource( OracleDataSource dataSource )
    {
        this.dataSource = dataSource;
    }

    /**
     * This method returns the connection associated with this thread:
     * using MINA the partition code is executed inside a thread pool,
     * each thread should have its database connection. Opening and closing
     * connections on each partition method results in a serious server 
     * slowdown. 
     * 
     * @return a connection wrapper that closes the wrapped connection 
     * once this thread ends.
     * 
     * @throws Exception
     */
    public OracleConnectionWrapper getConnectionWrapper()
    throws Exception
    {
        OracleConnectionWrapper w= connectionWrapper.get();
        
        if (w==null||w.getConnection().isClosed())
        {
            w= new OracleConnectionWrapper((OracleConnection)dataSource.getConnection());
            w.getConnection().setAutoCommit(false);
            Map typeMap = w.getConnection().getTypeMap();
            typeMap.put( "LDAP_ENTRY", OracleEntry.class );
            typeMap.put( "LDAP_ATTRIBUTE", OracleAttribute.class );
            typeMap.put( "LDAP_ATTRIBUTE_TABLE", OracleAttribute[].class );
            typeMap.put( "VC_ARR", String[].class );

            connectionWrapper.set(w);
        }
            
        return w;
    }

    /**
     * 
     * @return the <a href="http://xstream.codehaus.org/">XStream</a> instance
     * used to pass filter infos from java to pl/sql.
     */
    public XStream getXStream()
    {
        return xstream;
    }
    
    /**
     * Normalize and reverse the dn to use it in a database index:
     * using a dn "as is" will not use the database index in a good
     * way and will create <a href="http://en.wikipedia.org/wiki/Block_contention">block contention</a>, 
     * because a lot of DNs (uid=1222,ou=People,dc=example,dc=com) are likely to 
     * end up in the same block. Using a reverse index will create somenthing
     * like this (moc=cd,elpmaxe=cd,elpoeP=uo,2221=diu) that is better but
     * still a little ugly because values come first. Reversing the dn by rdn
     * will generate a pretty indexable value instead (dc=com,dc=example,ou=People,uid=1222).   
     * 
     * 
     * @param dn an {@link LdapDN}
     * @return the dn normalized and reversed
     */
    public static final String toReversedDn(LdapDN dn)
    {
        StringBuffer sb= new StringBuffer();
        String[] pieces= dn.getNormName().split(",");
        
        for (int i = pieces.length-1; i > -1 ; i--)
            sb.append(","+pieces[i]);
        
        
        return sb.toString().substring(1);
    }

    /**
     * @see Partition
     */
    public void add( final AddOperationContext ctx ) throws Exception
    {
        // FIXME: bypass error on 2nd startup: DefaultPatitionNexus tries to add an existing entry (check issue DIRSERVER-1344)
        if (ServerDNConstants.SYSTEM_DN.equals( ctx.getDn().getUpName() )&&lookup( ctx.getDn() ) != null) return;
        
        executeDml("begin partition_facade.add(?); end;",OracleEntry.fromServerEntry((ServerEntry)((ClonedServerEntry)ctx.getEntry()).getClonedEntry(),this));
    }


    /**
     * @see Partition
     */
    public void bind( BindOperationContext ctx ) throws Exception
    {
        
        if (lookup(ctx.getDn())==null)
            throw new NamingException( "Unknown dn: "+ctx.getDn().getUpName() );
          
    }


    /**
     * @see Partition
     */
    public void delete(final DeleteOperationContext ctx ) throws Exception
    {
        executeDml("begin partition_facade.delete(?); end;",toReversedDn( ctx.getDn() ));
    }


    /**
     * @see Partition
     */
    public int getCacheSize()
    {
        return 0;
    }


    /**
     * @see Partition
     */
    public String getId()
    {
        return id;
    }


    /**
     * @see Partition
     */
    public String getSuffix()
    {
        return suffix;
    }

    
    /**
     * @see Partition
     */
    public LdapDN getSuffixDn() throws Exception
    {
        return normSuffixDN;
    }


    /**
     * @see Partition
     */
    public LdapDN getUpSuffixDn() throws Exception
    {
        return new LdapDN(suffix);
    }


    /**
     * @see Partition
     */
    public EntryFilteringCursor list(final ListOperationContext ctx ) throws Exception
    {
        return new BaseEntryFilteringCursor( new OracleEntryCursorAdaptor(this, ctx), ctx );
    }

    /**
     * 
     * @param dn
     * @return the looked up {@link OracleEntry}
     * @throws Exception
     */
    private OracleEntry lookup(LdapDN dn)
    throws Exception
    {
        OracleCursorWrapper cw= prepareCursor( "select partition_facade.lookup_dn(?) from dual", toReversedDn( dn ) );
        OracleResultSet rs= cw.getResultSet();
        
        while (rs.next())
          return (OracleEntry)rs.getObject( 1 );
        
        return null;
    }

    /**
     * @see Partition
     */
    public ClonedServerEntry lookup( LookupOperationContext ctx ) throws Exception
    {
        return new ClonedServerEntry(lookup(ctx.getDn()).toServerEntry( this ));
    }
    
    /**
     * @see Partition
     */
    public ClonedServerEntry lookup( Long id ) throws Exception
    {
        return null;
    }
    
    /** 
     * A partition implementation that aims to be used for ou=system must implement this method because 
     * it is called before the DirectoryServer start and the default AbstractPartition implementation
     * goes through some logic that checks the server to be started
     * 
     * @see org.apache.directory.server.core.partition.AbstractPartition#hasEntry(org.apache.directory.server.core.interceptor.context.EntryOperationContext)
     */
    public boolean hasEntry( EntryOperationContext ctx ) throws Exception
    {
        try
        {
            return lookup( ctx.getDn() ) != null; 
        }
        catch ( NameNotFoundException e )
        {
            return false;
        }
    }    

    /**
     * @see Partition
     */
    public void modify( ModifyOperationContext ctx ) throws Exception
    {
        executeDml("begin partition_facade.modify(?); end;",OracleEntry.fromServerEntry( ((ServerEntry)ctx.getEntry().getClonedEntry()), this));
    }


    /**
     * @see Partition
     */
    public void move( MoveOperationContext ctx ) throws Exception
    {
        executeDml("begin partition_facade.move(?,?,?); end;", toReversedDn( ctx.getParent() ),
                                                               ctx.getDn().getRdn().getUpName()+","+ctx.getParent().getUpName(),
                                                               toReversedDn( ctx.getDn() ));
    }


    /**
     * @see Partition
     */
    public void moveAndRename( MoveAndRenameOperationContext ctx ) throws Exception
    {
        executeDml("begin partition_facade.move_and_rename(?,?,?,?,?); end;", toReversedDn( ctx.getParent() ),
                                                                              ctx.getNewRdn().getNormName(),
                                                                              ctx.getParent().getUpName(),
                                                                              ctx.getNewRdn().getUpName(),
                                                                              toReversedDn( ctx.getDn() ));
    }


    /**
     * @see Partition
     */
    public void rename( RenameOperationContext ctx ) throws Exception
    {
        executeDml("begin partition_facade.rename(?,?,?); end;", ctx.getNewRdn().getNormName(),
                                                                 ctx.getNewRdn().getUpName(),
                                                                 toReversedDn( ctx.getDn() ));
    }


    /**
     * @see Partition
     */
    public EntryFilteringCursor search( SearchOperationContext ctx ) throws Exception
    {
        return new BaseEntryFilteringCursor( new OracleEntryCursorAdaptor(this, ctx), ctx );
    }


    /**
     * @see Partition
     */
    public void setCacheSize( int cacheSize )
    {}


    /**
     * @see Partition
     */
    public void setId( String id )
    {
        this.id= id;
    }


    /**
     * @see Partition
     */
    public void setSuffix( String suffix )
    {
        this.suffix= suffix;
        
        // NOTE: needed from partition nexus bootstrap to find system partition for uid=admin
        try
        {
            this.normSuffixDN= LdapDN.normalize( new LdapDN(suffix), getRegistries().getAttributeTypeRegistry().getNormalizerMapping() );
        }
        catch ( Exception e )
        {
            throw new RuntimeException();
        }
    }


    /**
     * @see Partition
     */
    public void unbind( UnbindOperationContext ctx ) throws Exception
    {}

    
    /**
     * Used to query database data
     *
     * NOTE: Each database access pass through the partition_facade pl/sql package
     * to map Partition functions one by one on the database and make
     * the tuning easier for Oracle DBAs 
     * 
     * @param statement the sql or plsql statement to prepare and execute
     * @param objects bind variables to bind on 
     * @return a resultset wrapper that closes it on instance finalization
     * @throws Exception
     */
    public OracleCursorWrapper prepareCursor(String statement, Object... objects)
    throws Exception
    {
        OracleConnection connection= getConnectionWrapper().getConnection();
        OraclePreparedStatement  stmt=  ( OraclePreparedStatement ) connection.prepareStatement(statement,OracleResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
        
        stmt.setFetchSize( FETCH_SIZE );
        stmt.setFetchDirection( OracleResultSet.FETCH_UNKNOWN );
        
        int idx= 1;
        
        for (Object obj: objects)
           if (obj instanceof String[])
           {
               ArrayDescriptor desc = ArrayDescriptor.createDescriptor("VC_ARR",connection);
               ARRAY array = new ARRAY(desc, connection, obj);
               stmt.setARRAY(idx++,array);
           }
           else
               stmt.setObject(idx++,obj);
        
        return new OracleCursorWrapper(stmt,( OracleResultSet ) stmt.executeQuery());
    }
    
    /**
     * 
     * Used to make changes to database data
     *
     * NOTE: Each database access pass through the partition_facade pl/sql package
     * to map Partition functions one by one on the database and make
     * the tuning easier for Oracle DBAs 
     * 
     * @param statement the DML statement 
     * @param objects
     * @throws Exception
     */
    public void executeDml(String statement, Object... objects)
    throws Exception
    {
        OraclePreparedStatement  stmt=  ( OraclePreparedStatement ) getConnectionWrapper().getConnection().prepareStatement(statement);

        int idx= 1;
        
        for (Object obj: objects)
            stmt.setObject(idx++,obj);

        stmt.execute();
        stmt.close();
    }

    /**
     * A commodity method to lookup attribute OID
     * 
     * @param directoryService
     * @param att the attribute to lookup
     * @return the attribute OID
     * @throws Exception
     */
    public static final String normAtt(DirectoryService directoryService, String att)
    throws Exception
    {
        return directoryService.getRegistries().getAttributeTypeRegistry().lookup(att).getOid();
    }   
    
    /**
     * A commodity class to wrap oracle connections
     * and close them only when needed 
     */
    public static class OracleConnectionWrapper
    {
        private OracleConnection connection;
        
        public OracleConnectionWrapper(OracleConnection connection)
        {
            this.connection= connection;
        }
        
        public OracleConnection getConnection()
        {
            return connection;
        }
        
        @Override
        protected void finalize() throws Throwable
        {
            try { connection.close(); } catch (Exception e) {}
            super.finalize();
        }
        
    }
    
    /**
     * A commodity class to wrap oracle resultsets
     * and close them only when needed 
     */
    public static class OracleCursorWrapper
    {
        private OraclePreparedStatement statement;
        private OracleResultSet resultSet;
        
        public OracleCursorWrapper(OraclePreparedStatement statement, OracleResultSet resultSet)
        {
            this.statement= statement;
            this.resultSet= resultSet;
        }
        
        public OraclePreparedStatement getStatement()
        {
            return statement;
        }
        
        public OracleResultSet getResultSet()
        {
            return resultSet;
        }
        
        @Override
        protected void finalize() throws Throwable
        {
            try { resultSet.close(); } catch (Exception e) {}
            try { statement.close(); } catch (Exception e) {}
            super.finalize();
        }
    }

}
