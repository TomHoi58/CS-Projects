package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * A class that stores staged files
 */

public class StagingArea implements Serializable {

    /** Instance variables to help StagingArea class */

    List<String> engname; //short for english name
    List<String> shaid;   // short for sha1 ID
    List<byte[]> contents;
    String curdir;

    public StagingArea() {
        curdir = System.getProperty("user.dir");
        this.engname = new ArrayList<>();
        this.shaid = new ArrayList<>();
        this.contents = new ArrayList<>();
    }

    /**
     * read the file according to the name in the current path and make a SHA1
     * and see if it is in SHA1list or in the current commit's blobs Hashmap,
     * if not, do the following:
     * add the name to englishNamelist
     * read the file in the current path and write it to the contents list
     * Hash the content and add it to the SHA1list*
     */
    public void addFile(String filename) { //only one file added
        File file = new File(this.curdir + "/" + filename);
        byte[] bytes = Utils.readContents(file);
        String sha1 = Utils.sha1(bytes);
        //Deserialize Hashmap branches
        HashMap branches = (HashMap)
            Utils.deserialize("Branches", this.curdir + "/.gitlet/Branches");

        String curcommitid = (String) branches.get(branches.get("head"));
        Commit curcommit = (Commit)
            Utils.deserialize(curcommitid, this.curdir + "/.gitlet/Repository/" + curcommitid);

        if (curcommit.blobs.containsKey(filename)) {
            if (sha1.equals(curcommit.blobs.get(filename))) {
                return;
            }
        }
        engname.add(filename);
        contents.add(bytes);
        shaid.add(Utils.sha1(bytes));
    }

    /**
     * Removes file from the staging area, if not file doesn't exist ,error
     */
    public void removeFile(String name) { //only one file added
        if (!engname.contains(name)) {
            System.out.println("can't remove file that doesn't exist in SA!");
        } else {
            int index = engname.indexOf(name);
            engname.remove(index);
            contents.remove(index);
            shaid.remove(index);
        }
    }

    /**
     * Removes all of the files from the staging area
     */
    public void clear() {
        engname = new ArrayList<>();
        shaid = new ArrayList<>();
        contents = new ArrayList<>();
    }


}
