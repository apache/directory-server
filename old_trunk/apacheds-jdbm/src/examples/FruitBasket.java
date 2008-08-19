
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;

import java.io.IOException;
import java.util.Properties;

/**
 * Sample JDBM application to demonstrate the use of basic JDBM operations.
 *
 * @author <a href="mailto:boisvert@intalio.com">Alex Boisvert</a>
 * @version $Id: FruitBasket.java,v 1.3 2003/08/06 20:10:15 boisvert Exp $
 */
public class FruitBasket
{
    RecordManager  recman;
    HTree          hashtable;
    FastIterator   iter;
    String         fruit;
    String         color;


    public FruitBasket()
        throws IOException
    {
        // create or open fruits record manager
        Properties props = new Properties();
        recman = RecordManagerFactory.createRecordManager( "fruits", props );

        // create or load fruit basket (hashtable of fruits)
        long recid = recman.getNamedObject( "basket" );
        if ( recid != 0 ) {
            System.out.println( "Reloading existing fruit basket..." );
            hashtable = HTree.load( recman, recid );
            showBasket();
        } else {
            System.out.println( "Creating new fruit basket..." );
            hashtable = HTree.createInstance( recman );
            recman.setNamedObject( "basket", hashtable.getRecid() );
        }
    }


    public void runDemo()
        throws IOException
    {
        // insert keys and values
        System.out.println();
        System.out.println( "Adding fruits to the basket..." );
        hashtable.put( "bananas", "yellow" );
        hashtable.put( "strawberries", "red" );
        hashtable.put( "kiwis", "green" );

        showBasket();


        // display color of a specific fruit
        System.out.println();
        System.out.println( "Get the color of bananas..." );
        String bananasColor = (String) hashtable.get( "bananas" );
        System.out.println( "bananas are " + bananasColor );
        
        recman.commit();
        
        try {
            // Thread.sleep( 10 * 1000 );
        } catch ( Exception except ) {
            // ignore
        }

        // remove a specific fruit from hashtable
        System.out.println();
        System.out.print( "Removing bananas from the basket..." );
        hashtable.remove( "bananas" );
        recman.commit();
        System.out.println( " done." );

        // iterate over remaining objects
        System.out.println();
        System.out.println( "Remaining fruit colors:" );
        iter = hashtable.keys();
        fruit = (String) iter.next();
        while ( fruit != null ) {
            color = (String) hashtable.get( fruit );
            System.out.println( fruit + " are " + color );
            fruit = (String) iter.next();
        }
        
        // cleanup
        recman.close();
    }


    public void showBasket() 
        throws IOException
    {
        // Display content of fruit basket
        System.out.println();
        System.out.print( "Fruit basket contains: " );
        iter = hashtable.keys();
        fruit = (String) iter.next();
        while ( fruit != null ) {
            System.out.print( " " + fruit );
            fruit = (String) iter.next();
        }
        System.out.println();
    }
    
    
    public static void main( String[] args )
    {
        try {
            FruitBasket basket = new FruitBasket();
            basket.runDemo();
        } catch ( IOException ioe ) {
            ioe.printStackTrace();
        }
    }

}
