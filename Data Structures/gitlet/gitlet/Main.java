package gitlet;

import java.io.File;

/* Driver class for Gitlet, the tiny stupid version-control system.
   @author
*/
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */

    public static void main(String[] args) {
        String currdir = System.getProperty("user.dir");
        File gitlet = new File(currdir + "/.gitlet");
        if (args[0].equals("init")) {
            if (args.length == 1) {
                Command.init();
            } else {
                System.out.println("Incorrect operands.");
            }
        } else if (args[0].equals("add")) {
            if (args.length == 2) {
                if (!gitlet.exists()) {
                    System.out.println("Not in an initialized gitlet directory.");
                } else {
                    Command.add(args[1]);
                }
            } else {
                System.out.println("Incorrect operands.");
            }
        } else if (args[0].equals("commit")) {
            if (args.length == 1) {
                System.out.println("Incorrect operands.");
            } else if (args.length == 2) {
                if (args[1].equals("")) {
                    System.out.println("Please enter a commit message.");
                } else if (!gitlet.exists()) {
                    System.out.println("Not in an initialized gitlet directory.");
                } else {
                    Command.commit(args[1]);
                }
            } else {
                System.out.println("Incorrect operands.");
            }
        } else if (args[0].equals("rm")) {
            if (args.length == 2) {
                if (!gitlet.exists()) {
                    System.out.println("Not in an initialized gitlet directory.");
                } else {
                    Command.rm(args[1]);
                }
            } else {
                System.out.println("Incorrect operands.");
            }
        } else if (args[0].equals("log")) {
            if (args.length == 1) {
                if (!gitlet.exists()) {
                    System.out.println("Not in an initialized gitlet directory.");
                } else {
                    Command.log();
                }
            } else {
                System.out.println("Incorrect operands.");
            }
        } else if (args[0].equals("global-log")) {
            if (args.length == 1) {
                if (!gitlet.exists()) {
                    System.out.println("Not in an initialized gitlet directory.");
                } else {
                    Command.globalLog();
                }
            } else {
                System.out.println("Incorrect operands.");
            }
        } else if (args[0].equals("find")) {
            if (args.length == 2) {
                if (!gitlet.exists()) {
                    System.out.println("Not in an initialized gitlet directory.");
                } else {
                    Command.find(args[1]);
                }
            } else {
                System.out.println("Incorrect operands.");
            }
        } else {
            Main2.main2(args);
        }
    }
}







