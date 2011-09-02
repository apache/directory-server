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
 * $Id: BlockIo.java,v 1.2 2002/08/06 05:18:36 boisvert Exp $
 */
package jdbm.recman;


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.atomic.AtomicInteger;


import org.apache.directory.server.i18n.I18n;


/**
 * This class wraps a page-sized byte array and provides methods to read and 
 * write data to and from it. The readers and writers are just the ones that 
 * the rest of the toolkit needs, nothing else. Values written are compatible 
 * with java.io routines.
 * 
 * This block is never accessed directly, so it does not have to be thread-safe.
 *
 * @see java.io.DataInput
 * @see java.io.DataOutput
 */
public final class BlockIo implements java.io.Externalizable 
{
    public final static long serialVersionUID = 2L;

    /** The block Identifier */
    private long blockId;

    /** The row data contained in this block */
    private byte[] data;
    
    /** A view on the BlockIo */
    private BlockView view = null;
    
    /** A flag set when this block has been modified */
    private boolean dirty = false;
    
    /** The number of pending transaction on this block */
    private AtomicInteger transactionCount = new AtomicInteger( 0 );

    
    /**
     * Default constructor for serialization
     */
    public BlockIo() 
    {
        // empty
    }

    
    /**
     * Constructs a new BlockIo instance.
     * 
     * @param blockId The identifier for this block
     * @param data The data to store
     */
    /*No qualifier*/ BlockIo( long blockId, byte[] data ) 
    {
        // remove me for production version
        if ( blockId < 0 )
        {
            throw new Error( I18n.err( I18n.ERR_539_BAD_BLOCK_ID, blockId ) );
        }
        
        this.blockId = blockId;
        this.data = data;
    }

    
    /**
     * @return the underlying array
     */
    /*No qualifier*/ byte[] getData() 
    {
        return data;
    }

    
    /**
     * Sets the block number. Should only be called by RecordFile.
     * 
     * @param The block identifier
     */
    /*No qualifier*/ void setBlockId( long blockId ) 
    {
        if ( isInTransaction() )
        {
            throw new Error( I18n.err( I18n.ERR_540 ) );
        }
        
        if ( blockId < 0 )
        {
            throw new Error( I18n.err( I18n.ERR_539_BAD_BLOCK_ID, blockId ) );
        }
            
        this.blockId = blockId;
    }

    
    /**
     * @return the block number.
     */
    /*No qualifier*/ long getBlockId() 
    {
        return blockId;
    }

    
    /**
     * @return the current view of the block.
     */
    public BlockView getView() 
    {
        return view;
    }

    
    /**
     * Sets the current view of the block.
     * 
     * @param view the current view
     */
    public void setView( BlockView view ) 
    {
        this.view = view;
    }

    
    /**
     * Sets the dirty flag
     */
    /*No qualifier*/ void setDirty() 
    {
        dirty = true;
    }

    
    /**
     * Clears the dirty flag
     */
    /*No qualifier*/ void setClean() 
    {
        dirty = false;
    }

    
    /**
     * Returns true if the dirty flag is set.
     */
    /*No qualifier*/ boolean isDirty() 
    {
        return dirty;
    }

    
    /**
     * Returns true if the block is still dirty with respect to the 
     * transaction log.
     */
    /*No qualifier*/ boolean isInTransaction() 
    {
        return transactionCount.get() != 0;
    }


    /**
     * Increments transaction count for this block, to signal that this
     * block is in the log but not yet in the data recordFile. The method also
     * takes a snapshot so that the data may be modified in new transactions.
     */
    /*No qualifier*/ void incrementTransactionCount() 
    {
        transactionCount.getAndIncrement();
    }

    
    /**
     * Decrements transaction count for this block, to signal that this
     * block has been written from the log to the data recordFile.
     */
    /*No qualifier*/ void decrementTransactionCount() 
    {
        if ( transactionCount.decrementAndGet() < 0 )
        {
            throw new Error( I18n.err( I18n.ERR_541, getBlockId() ) );
        }
    }
    

    /**
     * Reads a byte from the indicated position
     * 
     * @param pos the position at which we will read the byte
     * @return the read byte
     */
    public byte readByte( int pos ) 
    {
        return data[pos];
    }
    

    /**
     * Writes a byte to the indicated position
     * 
     * @param pos The position where we want to write the value to
     * @param value the byte value we want to write into the BlockIo
     */
    public void writeByte( int pos, byte value ) 
    {
        data[pos] = value;
        dirty = true;
    }

    
    /**
     * Reads a short from the indicated position
     * 
     * @param pos the position at which we will read the short
     * @return the read short
     */
    public short readShort( int pos ) 
    {
        return ( short )
            ( ( ( data[pos+0] & 0xff ) << 8 ) |
             ( ( data[pos+1] & 0xff ) << 0 ) );
    }

    
    /**
     * Writes a short to the indicated position
     * 
     * @param pos The position where we want to write the value to
     * @param value the short value we want to write into the BlockIo
     */
    public void writeShort( int pos, short value ) 
    {
        data[pos+0] = ( byte ) ( 0xff & ( value >> 8 ) );
        data[pos+1] = ( byte ) ( 0xff & ( value >> 0 ) );
        dirty = true;
    }

    
    /**
     * Reads an int from the indicated position
     * 
     * @param pos the position at which we will read the int
     * @return the read int
     */
    public int readInt( int pos ) 
    {
        return
            ( data[pos+0] << 24) |
            ( ( data[pos+1] & 0xff ) << 16) |
            ( ( data[pos+2] & 0xff ) <<  8) |
            ( ( data[pos+3] & 0xff ) <<  0 );
    }

    
    /**
     * Writes an int to the indicated position
     * 
     * @param pos The position where we want to write the value to
     * @param value the int value we want to write into the BlockIo
     */
    public void writeInt( int pos, int value ) 
    {
        data[pos+0] = ( byte ) ( 0xff & ( value >> 24 ) );
        data[pos+1] = ( byte ) ( 0xff & ( value >> 16 ) );
        data[pos+2] = ( byte ) ( 0xff & ( value >>  8 ) );
        data[pos+3] = ( byte ) ( 0xff & ( value >>  0 ) );
        dirty = true;
    }

    
    /**
     * Reads a long from the indicated position
     * 
     * @param pos the position at which we will read the long
     * @return the read long
     */
    public long readLong( int pos )
    {
        return
            ( ( long )( (long)data[pos+0] << 56 ) |
                        ( (long)( data[pos+1] & 0xff ) << 48 ) |
                        ( (long)( data[pos+2] & 0xff ) << 40 ) |
                        ( (long)( data[pos+3] & 0xff ) << 32 ) |
                        ( (long)( data[pos+4] & 0xff ) << 24 ) |
                        ( (long)( data[pos+5] & 0xff ) << 16 ) |
                        ( (long)( data[pos+6] & 0xff ) <<  8 ) |
                        ( (long)( data[pos+7] & 0xff ) ) );
    }

    
    /**
     * Writes a long to the indicated position
     * 
     * @param pos The position where we want to write the value to
     * @param value the long value we want to write into the BlockIo
     */
    public void writeLong(int pos, long value) {
        data[pos+0] = (byte)(0xff & (value >> 56));
        data[pos+1] = (byte)(0xff & (value >> 48));
        data[pos+2] = (byte)(0xff & (value >> 40));
        data[pos+3] = (byte)(0xff & (value >> 32));
        data[pos+4] = (byte)(0xff & (value >> 24));
        data[pos+5] = (byte)(0xff & (value >> 16));
        data[pos+6] = (byte)(0xff & (value >>  8));
        data[pos+7] = (byte)(0xff & (value >>  0));
        dirty = true;
    }

    
    /**
     * {@inheritDoc}
     */
    public String toString() 
    {
        if ( view != null )
        {
            return view.toString();
        }
        
        StringBuilder sb = new StringBuilder();
        
        sb.append( "BlockIO ( " );
        
        // The blockID
        sb.append( blockId ).append( ", " );
        
        // Is it dirty ?
        if ( dirty )
        {
            sb.append( "dirty, " );
        }
        else
        {
            sb.append( "clean, " );
        }
        
        // The view
        if ( view != null )
        {
            sb.append( view.getClass().getSimpleName() ).append( ", " );
        }
        else
        {
            sb.append( "no view, " );
        }
        
        // The transaction count
        sb.append( "tx: " ).append( transactionCount.get() );

        sb.append( " )" );
        
        return sb.toString();
    }

    
    /**
     * implement externalizable interface
     */
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException 
    {
        blockId = in.readLong();
        int length = in.readInt();
        data = new byte[length];
        in.readFully(data);
    }

    
    /**
     * implement externalizable interface
     */
    public void writeExternal( ObjectOutput out ) throws IOException 
    {
        out.writeLong( blockId );
        out.writeInt( data.length );
        out.write( data );
    }
}
