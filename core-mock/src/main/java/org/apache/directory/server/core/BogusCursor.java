
package org.apache.directory.server.core;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.naming.NamingException;

import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.cursor.ClosureMonitor;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.cursor.CursorIterator;

class BogusCursor implements Cursor<ServerEntry>
{
    final int count;
    int ii;
    DirectoryService directoryService;


    public BogusCursor(int count)
    {
        this.count = count;
    }


    public boolean available() 
    {
        return ii < count;
    }


    public void close() throws NamingException
    {
        ii = count;
    }


    public boolean hasMoreElements()
    {
        return ii < count;
    }


    public Object nextElement()
    {
        if ( ii >= count )
        {
            throw new NoSuchElementException();
        }

        ii++;
        
        return new Object();
    }


    public void after( ServerEntry element ) throws Exception
    {
    }


    public void afterLast() throws Exception
    {
    }


    public void before( ServerEntry element ) throws Exception
    {
        throw new NotImplementedException();
    }


    public void beforeFirst() throws Exception
    {
        ii = -1;
    }


    public boolean first() throws Exception
    {
        ii = 0;
        return ii < count;
    }


    public ServerEntry get() throws Exception
    {
        return new DefaultServerEntry( directoryService.getRegistries() );
    }


    public boolean isClosed() throws Exception
    {
        return false;
    }


    public boolean isElementReused()
    {
        return false;
    }


    public boolean last() throws Exception
    {
        ii = count;
        return true;
    }


    public boolean next() 
    {
        if ( ii >= count )
        {
            return false;
        }

        ii++;
        
        return true;
    }


    public boolean previous() throws Exception
    {
        if ( ii < 0 )
        {
            return false;
        }
        
        ii--;
        return true;
    }


    public Iterator<ServerEntry> iterator()
    {
        return new CursorIterator<ServerEntry>( this );
    }


    public void close( Exception reason ) throws Exception
    {
    }


    public void setClosureMonitor( ClosureMonitor monitor )
    {
    }


    public DirectoryService getDirectoryService()
    {
        return directoryService;
    }


    public void setDirectoryService( DirectoryService directoryService )
    {
        this.directoryService = directoryService;
    }
}
