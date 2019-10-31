package gitlet;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/* The suite of all JUnit tests for the gitlet package.
   @author
 */
public class UnitTest {

    @Test
    public void mainTest() {

        Command.add("tom.txt");

        String currdir = System.getProperty("user.dir");
        Command.add("tom.txt");
        StagingArea sa = (StagingArea)
            Utils.deserialize("StagingArea", currdir + "/.gitlet/StagingArea");
        sa.clear();
        List removedfiles = (List) Utils.deserialize("RemovedFiles",
            currdir + "/.gitlet/RemovedFiles");
        HashMap branches = (HashMap)
            Utils.deserialize("Branches", currdir + "/.gitlet/branches");
        Command.sealThem(sa, removedfiles, branches);


        System.out.println(sa.engname);


        try {
            File file = new File(currdir + "/testing.txt");
            file.createNewFile();

        } catch (IOException e) {
            System.out.println("nada");
        }
        try {
            File file1 = new File(currdir + "/testing.txt");
            File file2 = new File(currdir + "/nah.txt");


            byte[] a = Utils.readContents(file1);
            byte[] c = Utils.readContents(file2);
            byte[] b = "<<<<<<< HEAD\n".getBytes();


            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(a);
            outputStream.write(c);
            outputStream.write(b);

            byte[] d = outputStream.toByteArray();


            Utils.writeContents(file1, d);
            file1.createNewFile();
        } catch (IOException e) {
            System.out.println("Internal error creating a new file");
        }


        System.out.println(branches);

    }

    @Test
    public void commandTest() {


    }

}


