package gitlet;

import java.io.File;

/**
 * Extension of Main class.
 */

public class Main2 {

    /**
     * Continuation of main method.
     */

    public static void main2(String[] args) {
        String currdir = System.getProperty("user.dir");
        File gitlet = new File(currdir + "/.gitlet");
        if (args[0].equals("status")) {
            if (args.length == 1) {
                if (!gitlet.exists()) {
                    System.out.println("Not in an initialized gitlet directory.");
                } else {
                    Command.status();
                }
            } else {
                System.out.println("Incorrect operands.");
            }
        } else if (args[0].equals("checkout")) {
            if (args.length == 1) {
                System.out.println("Incorrect operands.");
            } else if (args.length == 2) {
                if (!gitlet.exists()) {
                    System.out.println("Not in an initialized gitlet directory.");
                } else {
                    Command.checkoutBranch(args[1]);
                }
            } else if (args[1].equals("--")) {
                if (args.length == 3) {
                    if (!gitlet.exists()) {
                        System.out.println("Not in an initialized gitlet directory.");
                    } else {
                        Command.checkoutFile(args[2]);
                    }
                } else {
                    System.out.println("Incorrect operands.");
                }
            } else if (args[2].equals("--")) {
                if (args.length == 4) {
                    if (!gitlet.exists()) {
                        System.out.println("Not in an initialized gitlet directory.");
                    } else {
                        Command.checkoutCommit(args[1], args[3]);
                    }
                } else {
                    System.out.println("Incorrect operands.");
                }
            } else {
                System.out.println("Incorrect operands.");
            }
        } else if (args[0].equals("branch")) {
            if (args.length == 2) {
                if (!gitlet.exists()) {
                    System.out.println("Not in an initialized gitlet directory.");
                } else {
                    Command.branch(args[1]);
                }
            } else {
                System.out.println("Incorrect operands.");
            }
        } else if (args[0].equals("rm-branch")) {
            if (args.length == 2) {
                if (!gitlet.exists()) {
                    System.out.println("Not in an initialized gitlet directory.");
                } else {
                    Command.rmBranch(args[1]);
                }
            } else {
                System.out.println("Incorrect operands.");
            }
        } else if (args[0].equals("reset")) {
            if (args.length == 2) {
                if (!gitlet.exists()) {
                    System.out.println("Not in an initialized gitlet directory.");
                } else {
                    Command.reset(args[1]);
                }
            } else {
                System.out.println("Incorrect operands.");
            }
        } else {
            Main3.main3(args);
        }
    }
}
