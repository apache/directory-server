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
 * $Id: PageManager.java,v 1.3 2005/06/25 23:12:32 doomdark Exp $
 */
package jdbm.recman;


import java.io.IOException;

import org.apache.directory.server.i18n.I18n;


/**
 * This class manages the linked lists of pages that make up a recordFile. It contains
 * a FileHeadrer and a reference to the recordFile it manages.<br/>
 */
final class PageManager 
{
    /** our record recordFile */
    private RecordFile recordFile;
    
    /** header data */
    private FileHeader header;
    
    /** recordFile header containing block */
    private BlockIo headerBuf;

    
    /**
     * Creates a new page manager using the indicated record recordFile.
     * We will load in memory the associated FileHeader, which is
     * read from the RecordFile if it exists, or is created if it doesn't.
     * 
     * @param The associated RecordFile
     * @throws IOException If there is an issue storing data into the recordFile
     */
    PageManager( RecordFile recordFile ) throws IOException 
    {
        this.recordFile = recordFile;
        
        // Note that we hold on to the recordFile header node.
        headerBuf = recordFile.get( 0 );
        
        // Assume recordFile is new if the recordFile header's magic number is 0. 
        boolean isNew = headerBuf.readShort( 0 ) == 0;

        header = new FileHeader( headerBuf, isNew );
    }
    
    
    /**
     * Allocates a page of the indicated type. 
     * 
     * @param The page type we want to allocate
     * @return The record ID of the page.
     */
    long allocate( short type ) throws IOException 
    {
        if ( type == Magic.FREE_PAGE )
        {
            // We can't allocate FREE page. A page becomes FREE after it ha sbeen used.
            throw new Error( I18n.err( I18n.ERR_548 ) );
        }
        
        boolean isNew = false;
        
        // Do we have something on the free list?
        long freeBlock = header.getFirstOf( Magic.FREE_PAGE );
        
        if ( freeBlock != 0 ) 
        {
            // yes. Point to it and make the next of that page the
            // new first free page.
            header.setFirstOf( Magic.FREE_PAGE, getNext( freeBlock ) );
        }
        else 
        {
            // nope. make a new record
            freeBlock = header.getLastOf( Magic.FREE_PAGE );

            if ( freeBlock == 0 )
            {
                // very new recordFile - allocate record #1
                freeBlock = 1;
            }
            
            header.setLastOf( Magic.FREE_PAGE, freeBlock + 1 );
            isNew = true;
        }
        
        // Cool. We have a record, add it to the correct list
        BlockIo buf = recordFile.get( freeBlock );
        PageHeader pageHdr = null;
        
        if ( isNew )
        {
            pageHdr = new PageHeader( buf, type );
        }
        else
        {
            pageHdr = PageHeader.getView( buf );
        }
        
        long oldLast = header.getLastOf( type );
        
        // Clean data.
        System.arraycopy( RecordFile.cleanData, 0, buf.getData(), 0, RecordFile.BLOCK_SIZE );
        pageHdr.setType( type );
        pageHdr.setPrev( oldLast );
        pageHdr.setNext( 0 );
        
        if ( oldLast == 0 )
        {
            // This was the first one of this type
            header.setFirstOf( type, freeBlock );
        }
        
        header.setLastOf( type, freeBlock );
        recordFile.release( freeBlock, true );
        
        // If there's a previous, fix up its pointer
        if ( oldLast != 0 ) 
        {
            buf = recordFile.get( oldLast );
            pageHdr = PageHeader.getView( buf );
            pageHdr.setNext( freeBlock );
            recordFile.release( oldLast, true );
        }
        
        // remove the view, we have modified the type.
        buf.setView( null );
        
        return freeBlock;
    }
    
    
    /**
     * Frees a page of the indicated type.
     */
    void free( short type, long recid ) throws IOException 
    {
        if ( type == Magic.FREE_PAGE )
        {
            throw new Error( I18n.err( I18n.ERR_549 ) );
        }
        
        if ( recid == 0 )
        {
            throw new Error( I18n.err( I18n.ERR_550 ) );
        }
        
        // get the page and read next and previous pointers
        BlockIo buf = recordFile.get( recid );
        PageHeader pageHdr = PageHeader.getView( buf );
        long prev = pageHdr.getPrev();
        long next = pageHdr.getNext();
        
        // put the page at the front of the free list.
        pageHdr.setType( Magic.FREE_PAGE );
        pageHdr.setNext( header.getFirstOf( Magic.FREE_PAGE ) );
        pageHdr.setPrev( 0 );
        
        header.setFirstOf( Magic.FREE_PAGE, recid );
        recordFile.release( recid, true );
        
        // remove the page from its old list
        if ( prev != 0 ) 
        {
            buf = recordFile.get( prev );
            pageHdr = PageHeader.getView( buf );
            pageHdr.setNext( next );
            recordFile.release( prev, true );
        }
        else 
        {
            header.setFirstOf( type, next );
        }
        
        if ( next != 0 ) 
        {
            buf = recordFile.get( next );
            pageHdr = PageHeader.getView( buf );
            pageHdr.setPrev( prev );
            recordFile.release( next, true );
        }
        else 
        {
            header.setLastOf( type, prev );
        }
    }
    
    
    /**
     * Returns the page following the indicated block
     */
    long getNext( long block ) throws IOException 
    {
        try 
        {
            return PageHeader.getView( recordFile.get( block ) ).getNext();
        } 
        finally 
        {
            recordFile.release( block, false );
        }
    }
    
    
    /**
     * Returns the page before the indicated block
     */
    long getPrev( long block ) throws IOException 
    {
        try 
        {
            return PageHeader.getView( recordFile.get( block ) ).getPrev();
        } 
        finally 
        {
            recordFile.release( block, false );
        }
    }
    
    
    /**
     * Returns the first page on the indicated list.
     */
    long getFirst( short type ) throws IOException 
    {
        return header.getFirstOf( type );
    }

    
    /**
     * Returns the last page on the indicated list.
     */
    long getLast( short type ) throws IOException 
    {
        return header.getLastOf( type );
    }
    
    
    /**
     * Commit all pending (in-memory) data by flushing the page manager.
     * This forces a flush of all outstanding blocks (this is an implicit
     * {@link RecordFile#commit} as well).
     */
    void commit() throws IOException 
    {
        // write the header out
        recordFile.release( headerBuf );
        recordFile.commit();

        // and obtain it again
        headerBuf = recordFile.get( 0 );
        header = new FileHeader( headerBuf, false );
    }

    
    /**
     * Flushes the page manager. This forces a flush of all outstanding
     * blocks (this is an implicit {@link RecordFile#commit} as well).
     * 
     * @TODO completely wrong description of method
     */
    void rollback() throws IOException 
    {
        // release header
        recordFile.discard( headerBuf );
        recordFile.rollback();
        // and obtain it again
        headerBuf = recordFile.get( 0 );
        
        if ( headerBuf.readShort( 0 ) == 0 )
        {
            header = new FileHeader( headerBuf, true );
        }
        else
        {
            header = new FileHeader( headerBuf, false );
        }
    }
    
    
    /**
     * Closes the page manager. This flushes the page manager and releases
     * the lock on the header.
     */
    void close() throws IOException 
    {   
        recordFile.release( headerBuf );
        recordFile.commit();
        headerBuf = null;
        header = null;
        recordFile = null;
    }
    
    
    /**
     *  Returns the recordFile header.
     */
    FileHeader getFileHeader() 
    {
        return header;
    }    
    
    RecordFile getRecordFile()
    {
        return recordFile;
    }
}