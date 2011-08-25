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
 * $Id: PageHeader.java,v 1.2 2003/09/21 15:47:01 boisvert Exp $
 */
package jdbm.recman;


import java.io.IOException;

import org.apache.directory.server.i18n.I18n;


/**
 * This class represents a page header. It is the common superclass for
 * all different page views. It contains the following information:
 * 
 * <ol>
 *   <li>2 bytes: the short block type code</li>
 *   <li>8 bytes: the long block id of the next block in the block list</li>
 *   <li>8 bytes: the long block id of the previous block in the block list</li>
 *   <li>4 bytes: the size of this page
 * </ol>
 * 
 * The page header block view hence sees 18 bytes of page header data.
 */
public class PageHeader implements BlockView 
{
    // offsets into page header's (BlockIo's) buffer
    /** the page (BlockIo's type code) short magic code */
    private static final short O_MAGIC = 0; 
    
    /** the long block id of the next block in the block list */
    private static final short O_NEXT = Magic.SZ_SHORT;  
    
    /** the long block id of the previous block in the block list */
    private static final short O_PREV = O_NEXT + Magic.SZ_LONG; 
    
    /** the size of this page header */
    protected static final short SIZE = O_PREV + Magic.SZ_LONG;

    /** the page header block this view is associated with */
    protected BlockIo block;

    
    /**
     * Constructs a PageHeader object from a block
     *
     * @param block The block that contains the page header
     * @throws IOException if the block is too short to keep the page header.
     */
    protected PageHeader( BlockIo block ) 
    {
        this.block = block;
        block.setView( this );
        
        if ( ! magicOk() )
        {
            throw new Error( I18n.err( I18n.ERR_546, block.getBlockId(), getMagic() ) );
        }
    }
    
    
    /**
     * Constructs a new PageHeader of the indicated type. Used for newly
     * created pages.
     */
    PageHeader( BlockIo block, short type ) 
    {
        this.block = block;
        block.setView( this );
        setType( type );
    }
    
    
    /**
     * Factory method to create or return a page header for the indicated block.
     */
    static PageHeader getView ( BlockIo block ) 
    {
        BlockView view = block.getView();
        
        if ( view != null && view instanceof PageHeader )
        {
            return ( PageHeader ) view;
        }
        else
        {
            return new PageHeader( block );
        }
    }
    
    
    /**
     * Returns true if the magic corresponds with the fileHeader magic.
     */
    private boolean magicOk() 
    {
        int magic = getMagic();
        
        return magic >= Magic.BLOCK
            && magic <= ( Magic.BLOCK + Magic.FREEPHYSIDS_PAGE );
    }
    
    
    /**
     * For paranoia mode
     */
    protected void paranoiaMagicOk() 
    {
        if ( ! magicOk() )
        {
            throw new Error( I18n.err( I18n.ERR_547, getMagic() ) );
        }
    }
    
    
    /** 
     * @return The magic code (ie, the 2 first bytes of the inner BlockIo) 
     */
    short getMagic() 
    {
        return block.readShort( O_MAGIC );
    }

    
    /**
     * @return the next block (ie the long at position 2 in the BlockIo)
     */
    long getNext() 
    {
        paranoiaMagicOk();
        
        return block.readLong( O_NEXT );
    }
    
    
    /** 
     * Sets the next block.
     * 
     * @param The next Block ID
     */
    void setNext( long next ) 
    {
        paranoiaMagicOk();
        block.writeLong( O_NEXT, next );
    }
    
    
    /** 
     * @return the previous block (ie the long at position 10 in the BlockIo)
     */
    long getPrev() 
    {
        paranoiaMagicOk();
        
        return block.readLong( O_PREV );
    }
    
    
    /** 
     * Sets the previous block. 
     */
    void setPrev( long prev ) 
    {
        paranoiaMagicOk();
        block.writeLong( O_PREV, prev );
    }
    
    
    /** 
     * Sets the type of the page header
     * 
     *  @param type The PageHeader type to store at position 0
     */
    void setType( short type ) 
    {
        block.writeShort( O_MAGIC, ( short ) ( Magic.BLOCK + type ) );
    }
}
