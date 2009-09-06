
package org.apache.directory.server.core;

import java.util.Set;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.GetMatchedNameOperationContext;
import org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.interceptor.context.GetSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.ListSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.shared.ldap.name.LdapDN;

class MockOperationManager implements OperationManager
{
    int count;
    
    public MockOperationManager( int count )
    {
        this.count = count;
    }
    
    public void add( AddOperationContext opContext ) throws Exception
    {
    }

    
    public void bind( BindOperationContext opContext ) throws Exception
    {
    }

    
    public boolean compare( CompareOperationContext opContext ) throws Exception
    {
        return false;
    }


    public void delete( DeleteOperationContext opContext ) throws Exception
    {
    }

    public LdapDN getMatchedName( GetMatchedNameOperationContext opContext ) throws Exception
    {
        return null;
    }

    public ClonedServerEntry getRootDSE( GetRootDSEOperationContext opContext ) throws Exception
    {
        return null;
    }

    public LdapDN getSuffix( GetSuffixOperationContext opContext ) throws Exception
    {
        return null;
    }

    public boolean hasEntry( EntryOperationContext opContext ) throws Exception
    {
        return false;
    }

    public EntryFilteringCursor list( ListOperationContext opContext ) throws Exception
    {
        return null;
    }

    public Set<String> listSuffixes( ListSuffixOperationContext opContext ) throws Exception
    {
        return null;
    }

    public ClonedServerEntry lookup( LookupOperationContext opContext ) throws Exception
    {
        return null;
    }

    public void modify( ModifyOperationContext opContext ) throws Exception
    {
    }

    public void move( MoveOperationContext opContext ) throws Exception
    {
    }

    public void moveAndRename( MoveAndRenameOperationContext opContext ) throws Exception
    {
    }

    public void rename( RenameOperationContext opContext ) throws Exception
    {
    }

    public EntryFilteringCursor search( SearchOperationContext opContext ) throws Exception
    {
        return new BaseEntryFilteringCursor( new BogusCursor( count ), opContext );
    }


    public void unbind( UnbindOperationContext opContext ) throws Exception
    {
    }
}
