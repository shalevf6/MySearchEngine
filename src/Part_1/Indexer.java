package Part_1;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * This class creates the posting files for the corpus
 */
public class Indexer {

    static protected BlockingQueue<String> docQueue = new ArrayBlockingQueue<>(100);
    static protected boolean stop = false;

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
