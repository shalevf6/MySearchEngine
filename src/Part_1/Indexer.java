package Part_1;

import GeneralClasses.Document;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * This class creates the posting files for the corpus
 */
public class Indexer implements Runnable {

    static BlockingQueue<Document> docQueue = new ArrayBlockingQueue<>(1000);
    static private boolean stop = false;

    private void indexAll(){
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

    @Override
    public void run() {
        indexAll();
    }
}
