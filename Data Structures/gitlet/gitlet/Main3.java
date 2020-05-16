package gitlet;

import java.io.File;

/** Extension of Main2 class.*/

public class Main3 {

    /**
     * Continuation of main3 method.
     */

    public static void main3(String[] args) {
        String currdir = System.getProperty("user.dir");
        File gitlet = new File(currdir + "/.gitlet");
        if (args[0].equals("merge")) {
            if (args.length == 2) {
                if (!gitlet.exists()) {
                    System.out.println("Not in an initialized gitlet directory.");
                } else {
                    Command.merge(args[1]);
                }
            } else {
                System.out.println("Incorrect operands.");
            }
        } else {
            System.out.println("No command with that name exists.");
        }
    }
}
