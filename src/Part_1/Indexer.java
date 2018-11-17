/**
 * This class creates the posting files for the corpus
 */

package Part_1;

import java.util.LinkedList;
import java.util.Queue;

public class Indexer {

    static protected Queue<String> docQueue = new LinkedList<>();
    static protected boolean stop;

    public void indexAll(){
        while (true) {
            if (!docQueue.isEmpty()) {

            }
            else {
                if (stop) {
                    break;
                }
            }
        }
    }

    /**
     * stops creating the indexes
     */
    public static void stop() {
        stop = true;
    }
}
