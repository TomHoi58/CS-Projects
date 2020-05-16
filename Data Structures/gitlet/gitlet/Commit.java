package gitlet;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;

/**
 * A class that generates commits objects with information about each commit
 */

public class Commit implements Serializable {
    String message;
    String timestamp;
    String parent; // Commit object only knows its own parent
    HashMap blobs;


    public Commit(String message, String parent) {
        Timestamp time = new Timestamp(System.currentTimeMillis());
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        this.message = message;
        this.timestamp = sdf.format(time);
        this.parent = parent;
        this.blobs = new HashMap<String, String>();
    }


    /**
     * Adds blobs from the staging area to the commit object
     */
    public void addBlobs(String engname, String shaid) {
        blobs.put(engname, shaid);
    }
}


