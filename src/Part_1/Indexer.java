package Part_1;

import java.util.Queue;

public class Indexer {

    static protected Queue<String> docQueue;
    static protected boolean stop;

    public static void stop() {
        stop = true;
    }

    public void parseAll(){
        while (true) {
            if (!docQueue.isEmpty()) {

            }
            if (stop) {
                break;
            }
        }
    }
}
