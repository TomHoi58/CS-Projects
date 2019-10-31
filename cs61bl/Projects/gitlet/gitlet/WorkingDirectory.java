package gitlet;

import java.io.File;
import java.io.IOException;

/**
 * A class with access to methods in regards to the working directory
 */

public class WorkingDirectory {

    private static String currdir = System.getProperty("user.dir"); // short for current directory

    /**
     * Adds files to the working directory, overwrite if necessary
     */
    public static void addFiles(String filename, byte[] bytes) {
        try {
            File file = new File(currdir + "/" + filename);
            Utils.writeContents(file, bytes);
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * Removes file from the working directory, if the file doesn't exist, no error
     */
    public static void removeFiles(String filename) {
        File file = new File(currdir + "/" + filename);
        file.delete();
    }


}
