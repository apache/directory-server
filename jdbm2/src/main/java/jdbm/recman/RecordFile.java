/**
 * JDBM LICENSE v1.00
 *
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce the
 *    above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. The name "JDBM" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of Cees de Groot.  For written permission,
 *    please contact cg@cdegroot.com.
 *
 * 4. Products derived from this Software may not be called "JDBM"
 *    nor may "JDBM" appear in their names without prior written
 *    permission of Cees de Groot.
 *
 * 5. Due credit should be given to the JDBM Project
 *    (http://jdbm.sourceforge.net/).
 *
 * THIS SOFTWARE IS PROVIDED BY THE JDBM PROJECT AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * CEES DE GROOT OR ANY CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 2000 (C) Cees de Groot. All Rights Reserved.
 * Contributions are Copyright (C) 2000 by their associated contributors.
 *
 * $Id: RecordFile.java,v 1.6 2005/06/25 23:12:32 doomdark Exp $
 */
package jdbm.recman;


import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.directory.server.i18n.I18n;


/**
 *  This class represents a random access file as a set of fixed size
 *  records. Each record has a physical record number, and records are
 *  cached in order to improve access.
 * <p>
 *  The set of dirty records on the in-use list constitutes a transaction.
 *  Later on, we will send these records to some recovery thingy.
 */
public final class RecordFile 
{
    private TransactionManager transactionManager;

    // state transitions: free -> inUse -> dirty -> inTxn -> free
    // free is a cache, thus a FIFO. The rest are hashes.
    /** The list of free pages */
    private final LinkedList<BlockIo> free = new LinkedList<BlockIo>();
    
    /** The map of pages being currently used */
    private final HashMap<Long,BlockIo> inUse = new HashMap<Long,BlockIo>();
    
    /** The map of dirty pages (page being modified) */
    private final HashMap<Long,BlockIo> dirty = new HashMap<Long,BlockIo>();
    
    /** The map of page in a transaction */
    private final HashMap<Long,BlockIo> inTxn = new HashMap<Long,BlockIo>();

    /** A flag set if transactions is disabled. Default to false */
    private boolean transactionsDisabled = false;

    /** The length of a single block. */
    public final static int BLOCK_SIZE = 4096;

    /** The extension of a record file */
    final static String EXTENSION = ".db";

    /** A block of clean data to wipe clean pages. */
    final static byte[] cleanData = new byte[BLOCK_SIZE];

    /** The underlying file */
    private RandomAccessFile file;
    
    /** The file name */
    private final String fileName;

    
    /**
     * Creates a new object on the indicated filename. The file is
     * opened in read/write mode.
     *
     * @param fileName the name of the file to open or create, without
     *        an extension.
     * @throws IOException whenever the creation of the underlying
     *         RandomAccessFile throws it.
     */
    RecordFile( String fileName ) throws IOException 
    {
        this.fileName = fileName;
        file = new RandomAccessFile( fileName + EXTENSION, "rw" );
    }


    /**
     * @return The TransactionManager if the transaction system is enabled.
     * @throws IOException If we can't create a TransactionManager
     */
    TransactionManager getTxnMgr() throws IOException
    {
        if ( transactionsDisabled )
        {
            throw new IllegalStateException( "Transactions are disabled." );
        }
        
        if ( transactionManager == null )
        {
            transactionManager = new TransactionManager( this );
        }
        
        return transactionManager;
    }


    /**
     * @return the file name.
     */
    String getFileName() 
    {
        return fileName;
    }

    
    /**
     * Disables transactions: doesn't sync and doesn't use the
     * transaction manager.
     */
    void disableTransactions() 
    {
        transactionsDisabled = true;
    }

    
    /**
     * Gets a block from the file. The returned byte array is the in-memory 
     * copy of the record, and thus can be written (and subsequently released 
     * with a dirty flag in order to write the block back).
     *
     * @param blockId The record number to retrieve.
     */
     BlockIo get( long blockId ) throws IOException 
     {
         // try in transaction list, dirty list, free list
         BlockIo blockIo = inTxn.get( blockId );
         
         if ( blockIo != null ) 
         {
             inTxn.remove( blockId );
             inUse.put( blockId, blockIo );
             
             return blockIo;
         }
         
         blockIo = dirty.get( blockId );
         
         if ( blockIo != null ) 
         {
             dirty.remove( blockId );
             inUse.put( blockId, blockIo );
             
             return blockIo;
         }
         
         for ( Iterator<BlockIo> iterator = free.iterator(); iterator.hasNext(); ) 
         {
             BlockIo cur = iterator.next();
             
             if ( cur.getBlockId() == blockId ) 
             {
                 blockIo = cur;
                 iterator.remove();
                 inUse.put( blockId, blockIo );
                 
                 return blockIo;
             }
         }

         // sanity check: can't be on in use list
         if ( inUse.get( blockId ) != null ) 
         {
             throw new Error( I18n.err( I18n.ERR_554, blockId ) );
         }

         // get a new node and read it from the file
         blockIo = getNewBlockIo( blockId );
         long offset = blockId * BLOCK_SIZE;
         long fileLength = file.length();
         
         if ( ( fileLength > 0 ) && ( offset <= fileLength ) ) 
         {
             read( file, offset, blockIo.getData(), BLOCK_SIZE );
         } 
         
         inUse.put( blockId, blockIo );
         blockIo.setClean();
         
         return blockIo;
     }


    /**
     * Releases a block.
     *
     * @param blockId The record number to release.
     * @param isDirty If true, the block was modified since the get().
     */
    void release( long blockId, boolean isDirty ) throws IOException 
    {
        BlockIo blockIo = inUse.get( blockId );
        
        if ( blockIo == null )
        {
            throw new IOException( I18n.err( I18n.ERR_555, blockId ) );
        }
        
        if ( ! blockIo.isDirty() && isDirty )
        {
            blockIo.setDirty();
        }
            
        release( blockIo );
    }

    
    /**
     * Releases a block.
     *
     * @param block The block to release.
     */
    void release( BlockIo block ) 
    {
        inUse.remove( block.getBlockId() );
        
        if ( block.isDirty() ) 
        {
            // System.out.println( "Dirty: " + key + block );
            dirty.put( block.getBlockId(), block );
        } 
        else 
        {
            if ( ! transactionsDisabled && block.isInTransaction() ) 
            {
                inTxn.put( block.getBlockId(), block );
            } 
            else 
            {
                free.add( block );
            }
        }
    }
    

    /**
     * Discards a block (will not write the block even if it's dirty)
     *
     * @param block The block to discard.
     */
    void discard( BlockIo block ) 
    {
        inUse.remove( block.getBlockId() );

        // note: block not added to free list on purpose, because
        //       it's considered invalid
    }

    
    /**
     * Commits the current transaction by flushing all dirty buffers to disk.
     */
    void commit() throws IOException 
    {
        // debugging...
        if ( ! inUse.isEmpty() && inUse.size() > 1 ) 
        {
            showList( inUse.values().iterator() );
            throw new Error( I18n.err( I18n.ERR_556, inUse.size() ) );
        }

        //  System.out.println("committing...");

        if ( dirty.size() == 0 ) 
        {
            // if no dirty blocks, skip commit process
            return;
        }

        
        if ( ! transactionsDisabled ) 
        {
            getTxnMgr().start();
        }

        
        for ( BlockIo blockIo : dirty.values() ) 
        {
            // System.out.println("node " + node + " map size now " + dirty.size());
            if ( transactionsDisabled ) 
            {
                sync( blockIo );
                blockIo.setClean();
                free.add( blockIo );
            }
            else 
            {
                getTxnMgr().add( blockIo );
                inTxn.put( blockIo.getBlockId(), blockIo );
            }
        }
        
        dirty.clear();

        if ( ! transactionsDisabled ) 
        {
            getTxnMgr().commit();
        }
    }

    
    /**
     * Rollback the current transaction by discarding all dirty buffers
     */
    void rollback() throws IOException 
    {
        // debugging...
        if ( ! inUse.isEmpty() ) 
        {
            showList( inUse.values().iterator() );
            throw new Error( I18n.err( I18n.ERR_557, inUse.size() ) );
        }
    
        //  System.out.println("rollback...");
        dirty.clear();

        if ( ! transactionsDisabled ) 
        {
            getTxnMgr().synchronizeLogFromDisk();
        }

        if ( ! inTxn.isEmpty() ) 
        {
            showList( inTxn.values().iterator() );
            throw new Error( I18n.err( I18n.ERR_558, inTxn.size() ) );
        }
    }

    
    /**
     * Commits and closes file.
     */
    void close() throws IOException 
    {
        if ( ! dirty.isEmpty() ) 
        {
            commit();
        }
        
        if( ! transactionsDisabled )
        {
            getTxnMgr().shutdown();
        }

        if ( ! inTxn.isEmpty() ) 
        {
            showList( inTxn.values().iterator() );
            throw new Error( I18n.err( I18n.ERR_559 ) );
        }

        // these actually ain't that bad in a production release
        if ( ! dirty.isEmpty() ) 
        {
            System.out.println( "ERROR: dirty blocks at close time" );
            showList( dirty.values().iterator() );
            throw new Error( I18n.err( I18n.ERR_560 ) );
        }
        
        if ( ! inUse.isEmpty() ) 
        {
            System.out.println( "ERROR: inUse blocks at close time" );
            showList( inUse.values().iterator() );
            throw new Error( I18n.err( I18n.ERR_561 ) );
        }

        // debugging stuff to keep an eye on the free list
        // System.out.println("Free list size:" + free.size());
        file.close();
        file = null;
    }


    /**
     * Force closing the file and underlying transaction manager.
     * Used for testing purposed only.
     */
    void forceClose() throws IOException 
    {
        if ( ! transactionsDisabled ) 
        {
            getTxnMgr().forceClose();
        }
        file.close();
    }

    
    /**
     * Prints contents of a list
     */
    private void showList( Iterator<BlockIo> i ) 
    {
        int cnt = 0;
        while ( i.hasNext() ) 
        {
            System.out.println( "elem " + cnt + ": " + i.next() );
            cnt++;
        }
    }


    /**
     * Returns a new BlockIo. The BlockIo is retrieved (and removed) from the 
     * released list or created new.
     */
    private BlockIo getNewBlockIo( long blockId ) throws IOException 
    {
        BlockIo blockIo = null;

        if ( ! free.isEmpty() ) 
        {
            blockIo = ( BlockIo ) free.removeFirst();
            blockIo.setBlockId( blockId );
        }
        
        if ( blockIo == null )
        {
            blockIo = new BlockIo( blockId, new byte[BLOCK_SIZE] );
        }
        
        blockIo.setView( null );
        
        return blockIo;
    }
    

    /**
     * Synchronizes a BlockIo to disk. This is called by the transaction manager's
     * synchronization code.
     * 
     * @param blockIo The blocIo to write on disk
     * @exception IOException If we have a problem while trying to write the blockIo to disk
     */
    void sync( BlockIo blockIo ) throws IOException 
    {
        byte[] data = blockIo.getData();
        
        if ( data != null ) 
        {
            // Write the data to disk now.
            long offset = blockIo.getBlockId() * BLOCK_SIZE;
            file.seek( offset );
            file.write( data );
        }
    }

    
    /**
     * Releases a node from the transaction list, if it was sitting there.
     *
     * @param recycle true if block data can be reused
     */
    void releaseFromTransaction( BlockIo node, boolean recycle ) throws IOException 
    {
        if ( ( inTxn.remove( node.getBlockId() ) != null ) && recycle ) 
        {
            free.add( node );
        }
    }
    

    /**
     * Synchronizes the file.
     */
    void sync() throws IOException 
    {
        file.getFD().sync();
    }


    /**
     * Utility method: Read a block from a RandomAccessFile
     */
    private static void read( RandomAccessFile file, long offset, byte[] buffer, int nBytes ) throws IOException 
    {
        file.seek( offset );
        int remaining = nBytes;
        int pos = 0;
        while ( remaining > 0 ) 
        {
            int read = file.read( buffer, pos, remaining );
            if ( read == -1 ) 
            {
                System.arraycopy( cleanData, 0, buffer, pos, remaining );
                break;
            }
            remaining -= read;
            pos += read;
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "RecordFile<" ).append( fileName ).append( ", " );
        
        // The file size
        sb.append( "size : " );
        
        try
        {
            sb.append( file.length() ).append( "bytes" );
        }
        catch ( IOException ioe )
        {
            sb.append( "unknown" );
        }
        
        // Transactions
        if ( transactionsDisabled )
        {
            sb.append( "(noTx)" );
        }
        else
        {
            sb.append( "(Tx)" );
        }
        
        // Dump the free blocks
        sb.append( "\n    Free blockIo : " ).append( free.size() );
                
        for ( BlockIo blockIo : free )
        {
            sb.append( "\n         " );
            sb.append( blockIo );
        }
        
        // Dump the inUse blocks
        sb.append( "\n    InUse blockIo : " ).append( inUse.size() );
        
        for ( BlockIo blockIo : inUse.values() )
        {
            sb.append( "\n         " );
            sb.append( blockIo );
        }
        
        // Dump the dirty blocks
        sb.append( "\n    Dirty blockIo : " ).append( dirty.size() );
        
        for ( BlockIo blockIo : dirty.values() )
        {
            sb.append( "\n         " );
            sb.append( blockIo );
        }
        
        // Dump the inTxn blocks
        sb.append( "\n    InTxn blockIo : " ).append( inTxn.size() );
        
        for ( BlockIo blockIo : inTxn.values() )
        {
            sb.append( "\n         " );
            sb.append( blockIo );
        }

        
        return sb.toString();
    }
}
