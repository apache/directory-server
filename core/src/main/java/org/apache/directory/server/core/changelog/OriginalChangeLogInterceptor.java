package org.apache.directory.server.core.changelog;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.*;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.ServerContext;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.ldif.ChangeType;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.util.Base64;
import org.apache.directory.shared.ldap.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


/**
 * An interceptor which maintains a change LOG as it intercepts changes to the
 * directory.  It mainains a changes.LOG file using the LDIF format for changes.
 * It appends changes to this file so the entire LDIF file can be loaded to 
 * replicate the state of the server.
 * 
 */
public class OriginalChangeLogInterceptor extends BaseInterceptor implements Runnable
{
    /** logger used by this class */
    private static final Logger LOG = LoggerFactory.getLogger( OriginalChangeLogInterceptor.class );

    /** time to wait before automatically waking up the writer thread */
    private static final long WAIT_TIMEOUT_MILLIS = 1000;
    
    /** the changes.LOG file's stream which we append change LOG messages to */
    private PrintWriter out;
    
    /** queue of string buffers awaiting serialization to the LOG file */
    private final Queue<StringBuilder> queue = new LinkedList<StringBuilder>();
    
    /** a handle on the attributeType registry to determine the binary nature of attributes */
    private AttributeTypeRegistry registry;
    
    /** determines if this service has been activated */
    private boolean isActive;
    
    /** thread used to asynchronously write change logs to disk */
    private Thread writer;
    
    
    // -----------------------------------------------------------------------
    // Overridden init() and destroy() methods
    // -----------------------------------------------------------------------

    
    public void init( DirectoryService directoryService ) throws NamingException
    {
        super.init( directoryService );

        // Get a handle on the attribute registry to check if attributes are binary
        registry = directoryService.getRegistries().getAttributeTypeRegistry();

        // Open a print stream to use for flushing LDIFs into
        File changes = new File( directoryService.getWorkingDirectory(), "changes.LOG" );
        
        try
        {
            if ( changes.exists() )
            {
                out = new PrintWriter( new FileWriter( changes, true ) );
            }
            else
            {
                out = new PrintWriter( new FileWriter( changes ) );
            }
        }
        catch( Exception e )
        {
            LOG.error( "Failed to open the change LOG file: " + changes, e );
        }
        
        out.println( "# -----------------------------------------------------------------------------" );
        out.println( "# Initializing changelog service: " + DateUtils.getGeneralizedTime() );
        out.println( "# -----------------------------------------------------------------------------" );
        out.flush();
        
        writer = new Thread( this );
        isActive = true;
        writer.start();
    }
    
    
    public void destroy()
    {
        // Gracefully stop writer thread and push remaining enqueued buffers ourselves
        isActive = false;
        
        do
        {
            // Let's notify the writer thread to make it die faster
            synchronized( queue )
            {
                queue.notifyAll();
            }
            
            // Sleep tiny bit waiting for the writer to die
            try
            {
                Thread.sleep( 50 );
            }
            catch ( InterruptedException e )
            {
                LOG.error( "Failed to sleep while waiting for writer to die", e );
            }
        } while ( writer.isAlive() );
        
        // Ok lock down queue and start draining it
        synchronized( queue )
        {
            while ( ! queue.isEmpty() )
            {
                StringBuilder buf = queue.poll();
                
                if ( buf != null )
                {
                    out.println( buf );
                }
            }
        }

        // Print message that we're stopping LOG service, flush and close
        out.println( "# -----------------------------------------------------------------------------" );
        out.println( "# Deactivating changelog service: " + DateUtils.getGeneralizedTime() );
        out.println( "# -----------------------------------------------------------------------------" );
        out.flush();
        out.close();
        
        super.destroy();
    }
    
    
    // -----------------------------------------------------------------------
    // Implementation for Runnable.run() for writer Thread
    // -----------------------------------------------------------------------

    
    public void run()
    {
        while ( isActive )
        {
            StringBuilder buf;

            // Grab semphore to queue and dequeue from it
            synchronized( queue )
            {
                try 
                { 
                    queue.wait( WAIT_TIMEOUT_MILLIS ); 
                } 
                catch ( InterruptedException e ) 
                { 
                    LOG.error( "Failed to to wait() on queue", e );
                }
                
                buf = queue.poll();
                queue.notifyAll();
            }
            
            // Do writing outside of synch block to allow other threads to enqueue
            if ( buf != null )
            {
                out.println( buf );
                out.flush();
            }
        }
    }
    
    
    // -----------------------------------------------------------------------
    // Overridden (only change inducing) intercepted methods
    // -----------------------------------------------------------------------

    public void add( NextInterceptor next, AddOperationContext opContext ) throws NamingException
    {
        StringBuilder buf;
        next.add( opContext );
        
        if ( ! isActive )
        {
            return;
        }
        
        // Append comments that can be used to track the user and time this operation occurred
        buf = new StringBuilder();
        buf.append( "\n#! creatorsName: " );
        buf.append( getPrincipalName() );
        buf.append( "\n#! createTimestamp: " );
        buf.append( DateUtils.getGeneralizedTime() );
        
        // Append the LDIF entry now
        buf.append( LdifUtils.convertToLdif( opContext.getEntry() ) );

        // Enqueue the buffer onto a queue that is emptied by another thread asynchronously. 
        synchronized ( queue )
        {
            queue.offer( buf );
            queue.notifyAll();
        }
    }

    /**
     * The delete operation has to be stored with a way to restore the deleted element.
     * There is no way to do that but reading the entry and dump it into the LOG.
     */
    public void delete( NextInterceptor next, DeleteOperationContext opContext ) throws NamingException
    {
        next.delete( opContext );

        if ( ! isActive )
        {
            return;
        }
        
        // Append comments that can be used to track the user and time this operation occurred
        StringBuilder buf = new StringBuilder();
        buf.append( "\n#! deletorsName: " );
        buf.append( getPrincipalName() );
        buf.append( "\n#! deleteTimestamp: " );
        buf.append( DateUtils.getGeneralizedTime() );
        
        Entry entry = new Entry();
        entry.setDn( opContext.getDn().getUpName() );
        entry.setChangeType( ChangeType.Delete );
        buf.append( LdifUtils.convertToLdif( entry ) );
        

        // Enqueue the buffer onto a queue that is emptied by another thread asynchronously. 
        synchronized ( queue )
        {
            queue.offer( buf );
            queue.notifyAll();
        }
    }

    
    public void modify( NextInterceptor next, ModifyOperationContext opContext ) throws NamingException
    {
        StringBuilder buf;
        next.modify( opContext );

        if ( ! isActive )
        {
            return;
        }
        
        // Append comments that can be used to track the user and time this operation occurred
        buf = new StringBuilder();
        buf.append( "\n#! modifiersName: " );
        buf.append( getPrincipalName() );
        buf.append( "\n#! modifyTimestamp: " );
        buf.append( DateUtils.getGeneralizedTime() );
        
        // Append the LDIF record now
        buf.append( "\ndn: " );
        buf.append( opContext.getDn() );
        buf.append( "\nchangetype: modify" );

        List<ModificationItemImpl> mods = opContext.getModItems();
        
        for ( ModificationItemImpl mod :mods )
        {
            append( buf, mod.getAttribute(), getModOpStr( mod.getModificationOp() ) );
        }
        
        buf.append( "\n" );

        // Enqueue the buffer onto a queue that is emptied by another thread asynchronously. 
        synchronized ( queue )
        {
            queue.offer( buf );
            queue.notifyAll();
        }
    }


    // -----------------------------------------------------------------------
    // Though part left as an exercise (Not Any More!)
    // -----------------------------------------------------------------------

    
    public void rename ( NextInterceptor next, RenameOperationContext renameContext ) throws NamingException
    {
        next.rename( renameContext );
        
        if ( ! isActive )
        {
            return;
        }
        
        StringBuilder buf;
        
        // Append comments that can be used to track the user and time this operation occurred
        buf = new StringBuilder();
        buf.append( "\n#! principleName: " );
        buf.append( getPrincipalName() );
        buf.append( "\n#! operationTimestamp: " );
        buf.append( DateUtils.getGeneralizedTime() );
        
        // Append the LDIF record now
        buf.append( "\ndn: " );
        buf.append( renameContext.getDn() );
        buf.append( "\nchangetype: modrdn" );
        buf.append( "\nnewrdn: " ).append( renameContext.getNewRdn() );
        buf.append( "\ndeleteoldrdn: " ).append( renameContext.getDelOldDn() ? "1" : "0" );
        
        buf.append( "\n" );

        // Enqueue the buffer onto a queue that is emptied by another thread asynchronously. 
        synchronized ( queue )
        {
            queue.offer( buf );
            queue.notifyAll();
        }
    }

    
    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext moveAndRenameOperationContext )
        throws NamingException
    {
        next.moveAndRename( moveAndRenameOperationContext );
        
        if ( ! isActive )
        {
            return;
        }
        
        StringBuilder buf;
        
        // Append comments that can be used to track the user and time this operation occurred
        buf = new StringBuilder();
        buf.append( "\n#! principleName: " );
        buf.append( getPrincipalName() );
        buf.append( "\n#! operationTimestamp: " );
        buf.append( DateUtils.getGeneralizedTime() );
        
        // Append the LDIF record now
        buf.append( "\ndn: " );
        buf.append( moveAndRenameOperationContext.getDn() );
        buf.append( "\nchangetype: modrdn" ); // FIXME: modrdn --> moddn ?
        buf.append( "\nnewrdn: " ).append( moveAndRenameOperationContext.getNewRdn() );
        buf.append( "\ndeleteoldrdn: " ).append( moveAndRenameOperationContext.getDelOldDn() ? "1" : "0" );
        buf.append( "\nnewsperior: " ).append( moveAndRenameOperationContext.getParent() );
        
        buf.append( "\n" );

        // Enqueue the buffer onto a queue that is emptied by another thread asynchronously. 
        synchronized ( queue )
        {
            queue.offer( buf );
            queue.notifyAll();
        }
    }

    
    public void move ( NextInterceptor next, MoveOperationContext moveOperationContext ) throws NamingException
    {
        next.move( moveOperationContext );
        
        if ( ! isActive )
        {
            return;
        }
        
        StringBuilder buf;
        
        // Append comments that can be used to track the user and time this operation occurred
        buf = new StringBuilder();
        buf.append( "\n#! principleName: " );
        buf.append( getPrincipalName() );
        buf.append( "\n#! operationTimestamp: " );
        buf.append( DateUtils.getGeneralizedTime() );
        
        // Append the LDIF record now
        buf.append( "\ndn: " );
        buf.append( moveOperationContext.getDn() );
        buf.append( "\nchangetype: moddn" );
        buf.append( "\nnewsperior: " ).append( moveOperationContext.getParent() );
        
        buf.append( "\n" );

        // Enqueue the buffer onto a queue that is emptied by another thread asynchronously. 
        synchronized ( queue )
        {
            queue.offer( buf );
            queue.notifyAll();
        }
    }

    
    // -----------------------------------------------------------------------
    // Private utility methods used by interceptor methods
    // -----------------------------------------------------------------------

    
    /**
     * Appends an Attribute and its values to a buffer containing an LDIF entry taking
     * into account whether or not the attribute's syntax is binary or not.
     * 
     * @param buf the buffer written to and returned (for chaining)
     * @param attr the attribute written to the buffer
     * @return the buffer argument to allow for call chaining.
     * @throws NamingException if the attribute is not identified by the registry
     */
    private StringBuilder append( StringBuilder buf, Attribute attr ) throws NamingException
    {
        String id = attr.getID();
        int sz = attr.size();
        boolean isBinary = ! registry.lookup( id ).getSyntax().isHumanReadable();
        
        if ( isBinary )
        {
            for ( int ii = 0; ii < sz; ii++  )
            {
                buf.append( "\n" );
                buf.append( id );
                buf.append( ":: " );
                Object value = attr.get( ii );
                String encoded;
                
                if ( value instanceof String )
                {
                    encoded = ( String ) value;
                    
                    try
                    {
                        encoded = new String( Base64.encode( encoded.getBytes( "UTF-8" ) ) );
                    }
                    catch ( UnsupportedEncodingException e )
                    {
                        LOG.error( "can't convert to UTF-8: " + encoded, e );
                    }
                }
                else
                {
                    encoded = new String( Base64.encode( ( byte[] ) attr.get( ii ) ) );
                }
                buf.append( encoded );
            }
        }
        else
        {
            for ( int ii = 0; ii < sz; ii++  )
            {
                buf.append( "\n" );
                buf.append( id );
                buf.append( ": " );
                buf.append( attr.get( ii ) );
            }
        }
        
        return buf;
    }
    

    /**
     * Gets the DN of the user currently bound to the server executing this operation.  If 
     * the user is anonymous "" is returned.
     * 
     * @return the DN of the user executing the current intercepted operation
     * @throws NamingException if we cannot access the interceptor stack
     */
    private String getPrincipalName() throws NamingException
    {
        ServerContext ctx = ( ServerContext ) InvocationStack.getInstance().peek().getCaller();
        return ctx.getPrincipal().getName();
    }


    /**
     * Gets a String representation of the JNDI attribute modificaion flag.  Here are the mappings:
     * <table>
     *   <tr><th>JNDI Constant</th><th>Returned String</th></tr>
     *   <tr><td>DirContext.ADD_ATTRIBUTE</td><td>'add: '</td></tr>
     *   <tr><td>DirContext.REMOVE_ATTRIBUTE</td><td>'delete: '</td></tr>
     *   <tr><td>DirContext.REPLACE_ATTRIBUTE</td><td>'replace: '</td></tr>
     * </table>
     * <ul><li>
     * Note that the String in right hand column is quoted to show trailing space.
     * </li></ul>
     * 
     * @param modOp the int value of the JNDI modification operation
     * @return the string representation of the JNDI Modification operation
     */
    private String getModOpStr( int modOp ) 
    {
        String opStr;
        
        switch( modOp )
        {
            case( DirContext.ADD_ATTRIBUTE ):
                opStr = "add: ";
                break;
                
            case( DirContext.REMOVE_ATTRIBUTE ):
                opStr = "delete: ";
                break;
                
            case( DirContext.REPLACE_ATTRIBUTE ):
                opStr = "replace: ";
                break;
                
            default:
                throw new IllegalArgumentException( "Undefined attribute modify operation: " + modOp );
        }
        return opStr;
    }
    

    /**
     * Appends a modification delta instruction to an LDIF: i.e. 
     * <pre>
     * add: telephoneNumber
     * telephoneNumber: +1 408 555 1234
     * telephoneNumber: +1 408 444 9999
     * -
     * </pre>
     * 
     * @param buf the buffer to append the attribute delta to
     * @param mod the modified values if any for that attribute
     * @param modOp the modification operation as a string followd by ": "
     * @return the buffer argument provided for chaining
     * @throws NamingException if the modification attribute id is undefined
     */
    private StringBuilder append( StringBuilder buf, Attribute mod, String modOp ) throws NamingException
    {
        buf.append( "\n" );
        buf.append( modOp );
        buf.append( mod.getID() );
        append( buf, mod );
        buf.append( "\n-" );
        return buf;
    }
}
