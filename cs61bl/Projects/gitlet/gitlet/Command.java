package gitlet;


import java.io.File;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


/**
 * A class that stores all of the commands
 * @author
 */

public class Command {

    static String currdir = System.getProperty("user.dir");


    /**
     * Serializes given objects and saves them to repository (.gitlet)
     */
    public static void sealThem(StagingArea sa, List removedfiles, HashMap branches) {

        File file1 = new File(currdir + "/.gitlet/StagingArea");
        try {
            ObjectOutputStream out =
                new ObjectOutputStream(new FileOutputStream(file1));
            out.writeObject(sa);
            out.close();
        } catch (IOException excp) {
            System.out.println("Internal error serializing commit.");
        }

        File file2 = new File(currdir + "/.gitlet/RemovedFiles");
        try {
            ObjectOutputStream out =
                new ObjectOutputStream(new FileOutputStream(file2));
            out.writeObject(removedfiles);
            out.close();
        } catch (IOException excp) {
            System.out.println("Internal error serializing commit.");
        }

        File file3 = new File(currdir + "/.gitlet/Branches");
        try {
            ObjectOutputStream out =
                new ObjectOutputStream(new FileOutputStream(file3));
            out.writeObject(branches);
            out.close();
        } catch (IOException excp) {
            System.out.println("Internal error serializing commit.");
        }


    }

    /**
     * Creates a new repository in the current directory.
     */
    public static void init() {
        File gitlet = new File(currdir + "/.gitlet");
        if (!gitlet.exists()) {
            if (gitlet.mkdir()) {
                File repository = new File(currdir + "/.gitlet/Repository");
                repository.mkdir();
                Commit commitObj = new Commit("initial commit", null);
                String commitId = Utils.getsha1(commitObj);
                Utils.serialize(commitObj, currdir + "/.gitlet/Repository");
                List removedFiles = new ArrayList();
                StagingArea stagingArea = new StagingArea();
                HashMap branches = new HashMap<String, String>();
                branches.put("master", commitId);
                branches.put("head", "master");
                sealThem(stagingArea, removedFiles, branches);
            } else {
                System.out.println("Internal error creating directory.");
            }
        } else {
            System.out.println("A gitlet version-control system already "
                + "exists in the current directory.");
        }
    }

    /**
     * Adds a copy of the file as it currently
     * exists to the staging area
     */
    public static void add(String filename) {
        StagingArea sa = (StagingArea)
            Utils.deserialize("StagingArea", currdir + "/.gitlet/StagingArea");
        List removedfiles = (List)
            Utils.deserialize("RemovedFiles", currdir + "/.gitlet/RemovedFiles");
        HashMap branches = (HashMap)
            Utils.deserialize("Branches", currdir + "/.gitlet/Branches");
        if (removedfiles.contains(filename)) {
            removedfiles.remove(filename);
            sealThem(sa, removedfiles, branches);
        } else {
            File file = new File(currdir + "/" + filename);
            if (!file.exists()) {
                System.out.println("File does not exist.");
            } else {
                sa.addFile(filename);
                sealThem(sa, removedfiles, branches);
            }
        }
    }

    /**
     * Creates a new commit in repository
     */
    public static void commit(String message) {
        StagingArea sa = (StagingArea)
            Utils.deserialize("StagingArea", currdir + "/.gitlet/StagingArea");
        List removedfiles = (List)
            Utils.deserialize("RemovedFiles", currdir + "/.gitlet/RemovedFiles");
        HashMap branches = (HashMap)
            Utils.deserialize("Branches", currdir + "/.gitlet/Branches");
        if (sa.engname.isEmpty() && removedfiles.isEmpty()) {
            System.out.println("No changes added to the commit.");

        } else {
            Commit newcommit = new Commit(message, (String) branches.get(branches.get("head")));
            Commit parentcommit = (Commit) Utils.deserialize(newcommit.parent,
                currdir + "/.gitlet/Repository/" + newcommit.parent);
            newcommit.blobs.putAll(parentcommit.blobs);
            for (Object name : removedfiles) {
                newcommit.blobs.remove(name);
            }
            for (int i = 0; i < sa.engname.size(); i++) {
                newcommit.blobs.put(sa.engname.get(i), sa.shaid.get(i));
                try {
                    File file1 = new File(currdir + "/.gitlet/Repository/" + sa.shaid.get(i));
                    Utils.writeContents(file1, sa.contents.get(i));
                    file1.createNewFile();
                } catch (IOException e) {
                    System.out.println("Internal error creating a new file");
                }
            }
            sa.clear();
            removedfiles.clear();
            String commitId = Utils.getsha1(newcommit);
            Utils.serialize(newcommit, currdir + "/.gitlet/Repository");
            branches.put(branches.get("head"), commitId);
            sealThem(sa, removedfiles, branches);
        }

    }

    /**
     * Untracks the file in repository
     */
    public static void rm(String filename) {
        StagingArea sa = (StagingArea)
            Utils.deserialize("StagingArea", currdir + "/.gitlet/StagingArea");
        List removedfiles = (List)
            Utils.deserialize("RemovedFiles", currdir + "/.gitlet/RemovedFiles");
        HashMap branches = (HashMap)
            Utils.deserialize("Branches", currdir + "/.gitlet/Branches");
        String commitid = (String)
            branches.get(branches.get("head"));
        Commit curcommit = (Commit)
            Utils.deserialize(commitid, currdir + "/.gitlet/Repository/" + commitid);
        if (curcommit.blobs.containsKey(filename)) {
            WorkingDirectory.removeFiles(filename);
            if (sa.engname.contains(filename)) {
                sa.removeFile(filename);
            }
            removedfiles.add(filename);
            sealThem(sa, removedfiles, branches);
        } else if (sa.engname.contains(filename)) {
            sa.removeFile(filename);
            sealThem(sa, removedfiles, branches);
        } else {
            System.out.println("No reason to remove the file.");
        }

    }

    /**
     * Displays info about each commit in the branch
     */
    public static void log() {   // Could be changed to return string
        HashMap branches = (HashMap)
            Utils.deserialize("Branches", currdir + "/.gitlet/Branches");
        String commitid = (String)
            branches.get(branches.get("head"));
        while (commitid != null) {
            System.out.println("===");
            System.out.print("Commit ");
            System.out.println(commitid);
            Commit curcommit = (Commit)
                Utils.deserialize(commitid, currdir + "/.gitlet/Repository/" + commitid);
            System.out.println(curcommit.timestamp);
            System.out.println(curcommit.message);
            System.out.println();
            commitid = curcommit.parent;
        }
    }

    /**
     * Displays info about all the commits made in repository
     */
    public static void globalLog() {  // Could be changed to return string
        List allFiles = Utils.plainFilenamesIn(currdir + "/.gitlet/Repository");
        for (Object name : allFiles) {
            Commit currcommit = (Commit)
                Utils.deserialize((String) name, currdir + "/.gitlet/Repository/" + name);
            if (currcommit == null) {
                continue;
            }
            System.out.println("===");
            System.out.print("Commit ");
            System.out.println(name);
            System.out.println(currcommit.timestamp);
            System.out.println(currcommit.message);
            System.out.println();
        }
    }

    /**
     * Displays all the commits with the given message
     */
    public static void find(String message) {
        // TODO
        List allFiles = Utils.plainFilenamesIn(currdir + "/.gitlet/Repository");
        List box = new ArrayList();
        for (Object name : allFiles) {
            Commit currcommit = (Commit)
                Utils.deserialize((String) name, currdir + "/.gitlet/Repository/" + name);
            if (currcommit == null) {
                continue;
            }
            if (currcommit.message.equals(message)) {
                System.out.println(name);
                box.add(name);
            }
        }
        if (box.isEmpty()) {
            System.out.println("Found no commit with that message.");
        }
    }

    /**
     * Displays branches, staged files, and removed files
     */
    public static void status() { // Could be changed to return string
        StagingArea sa = (StagingArea)
            Utils.deserialize("StagingArea", currdir + "/.gitlet/StagingArea");
        List removedfiles = (List)
            Utils.deserialize("RemovedFiles", currdir + "/.gitlet/RemovedFiles");
        HashMap branches = (HashMap)
            Utils.deserialize("Branches", currdir + "/.gitlet/Branches");
        System.out.println("=== Branches ===");
        System.out.println("*" + branches.get("head"));
        List<String> brancheskeys = new ArrayList<String>(branches.keySet());
        Collections.sort(brancheskeys);
        for (String key : brancheskeys) {
            if (!key.equals("head") && !key.equals(branches.get("head"))) {
                System.out.println(key);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        List engname = sa.engname;
        Collections.sort(engname);
        for (Object name : engname) {
            System.out.println(name.toString());
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        Collections.sort(removedfiles);
        for (Object file : removedfiles) {
            System.out.println(file.toString());
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();

    }

    /**
     * Takes file from head commit of current branch
     * and puts it in the working directory
     */
    public static void checkoutFile(String filename) {
        // TODO
        HashMap branches = (HashMap)
            Utils.deserialize("Branches", currdir + "/.gitlet/Branches");
        String commitid = (String) branches.get(branches.get("head"));
        Commit currcommit = (Commit)
            Utils.deserialize(commitid, currdir + "/.gitlet/Repository/" + commitid);
        if (currcommit.blobs.containsKey(filename)) {
            String blobid = (String) currcommit.blobs.get(filename);
            File blob = new File(currdir + "/.gitlet/Repository/" + blobid);
            byte[] bytes = Utils.readContents(blob);
            WorkingDirectory.addFiles(filename, bytes);
        } else {
            System.out.println("File does not exist in that commit.");
        }


    }

    /**
     * Takes file from commit with given commit ID
     * and puts it in the working directory
     */
    public static void checkoutCommit(String commitid, String filename) {
        List allFiles = Utils.plainFilenamesIn(currdir + "/.gitlet/Repository");
        if (commitid.length() < 40) {
            for (Object name : allFiles) {
                if (name.toString().startsWith(commitid)) {
                    commitid = (String) name;
                    break;
                }
            }
        }
        if (allFiles.contains(commitid)) {
            Commit givcommit = (Commit)
                Utils.deserialize(commitid, currdir + "/.gitlet/Repository/" + commitid);
            if (givcommit.blobs.containsKey(filename)) {
                String blobid = (String) givcommit.blobs.get(filename);
                File blob = new File(currdir + "/.gitlet/Repository/" + blobid);
                byte[] bytes = Utils.readContents(blob);
                WorkingDirectory.addFiles(filename, bytes);
            } else {
                System.out.println("File does not exist in that commit.");
            }
        } else {
            System.out.println("No commit with that id exists.");
        }
    }


    /**
     * Takes all files from head commit of given branch
     * and puts them in the working directory
     */
    public static void checkoutBranch(String branchname) {
        StagingArea sa = (StagingArea)
            Utils.deserialize("StagingArea", currdir + "/.gitlet/StagingArea");
        List removedfiles = (List)
            Utils.deserialize("RemovedFiles", currdir + "/.gitlet/RemovedFiles");
        HashMap branches = (HashMap)
            Utils.deserialize("Branches", currdir + "/.gitlet/Branches");
        if (!branches.containsKey(branchname)) {
            System.out.println("No such branch exists.");
        } else if (branchname.equals("head")) {
            System.out.println("Can't check out to head branch!");
        } else if (branches.get("head").equals(branchname)) {
            System.out.println("No need to checkout the current branch.");
        } else {
            String curcommitid = (String) branches.get(branches.get("head"));
            Commit curcommit = (Commit)
                Utils.deserialize(curcommitid, currdir + "/.gitlet/Repository/" + curcommitid);
            String givcommitid = (String) branches.get(branchname);
            Commit givcommit = (Commit)
                Utils.deserialize(givcommitid, currdir + "/.gitlet/Repository/" + givcommitid);
            for (Object filename : givcommit.blobs.keySet()) {
                List allFilesinworkingdir = Utils.plainFilenamesIn(currdir);
                if (allFilesinworkingdir.contains(filename)
                    && !curcommit.blobs.containsKey(filename)) {
                    File fileinworkdir = new File(currdir + "/" + filename);
                    byte[] bytes = Utils.readContents(fileinworkdir);
                    String shaidforbytes = Utils.sha1(bytes);
                    if (!shaidforbytes.equals(givcommit.blobs.get(filename))) {
                        System.out.println("There is an untracked file in the way; "
                            + "delete it or add it first.");
                        return;
                    }
                }
            }
            for (Object filename : givcommit.blobs.keySet()) {
                String blobid = (String) givcommit.blobs.get(filename);
                File blob = new File(currdir + "/.gitlet/Repository/" + blobid);
                byte[] bytes = Utils.readContents(blob);
                WorkingDirectory.addFiles((String) filename, bytes);
            }
            for (Object filename : curcommit.blobs.keySet()) {
                if (!givcommit.blobs.containsKey(filename)) {
                    WorkingDirectory.removeFiles((String) filename);
                }
            }
            branches.put("head", branchname);
            sa.clear();
            sealThem(sa, removedfiles, branches);
        }
    }

    /**
     * Creates branch with given name and points
     * branch to the current head commit
     */
    public static void branch(String branchname) {
        HashMap branches = (HashMap)
            Utils.deserialize("Branches", currdir + "/.gitlet/Branches");
        if (!branches.containsKey(branchname)) {
            String curcommitid = (String) branches.get(branches.get("head"));
            branches.put(branchname, curcommitid);
            File file3 = new File(currdir + "/.gitlet/Branches");
            try {
                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file3));
                out.writeObject(branches);
                out.close();
            } catch (IOException excp) {
                System.out.println("Internal error serializing commit.");
            }
        } else {
            System.out.println("A branch with that name already exists.");
        }
    }

    /**
     * Deletes the pointer associated with the branch
     */
    public static void rmBranch(String branchname) {
        HashMap branches = (HashMap)
            Utils.deserialize("Branches", currdir + "/.gitlet/Branches");
        if (!branches.containsKey(branchname)) {
            System.out.println("A branch with that name does not exist.");
        } else if (branches.get("head").equals(branchname)) {
            System.out.println("Cannot remove the current branch.");
        } else {
            branches.remove(branchname);
            File file3 = new File(currdir + "/.gitlet/Branches");
            try {
                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file3));
                out.writeObject(branches);
                out.close();
            } catch (IOException excp) {
                System.out.println("Internal error serializing commit.");
            }
        }
    }

    /**
     * Checks out all the files tracked by the given commit
     */
    public static void reset(String commitid) {
        List allFiles = Utils.plainFilenamesIn(currdir + "/.gitlet/Repository");
        if (commitid.length() < 40) {
            for (Object name : allFiles) {
                if (name.toString().startsWith(commitid)) {
                    commitid = (String) name;
                    break;
                }
            }
        }
        StagingArea sa = (StagingArea)
            Utils.deserialize("StagingArea", currdir + "/.gitlet/StagingArea");
        List removedfiles = (List)
            Utils.deserialize("RemovedFiles", currdir + "/.gitlet/RemovedFiles");
        HashMap branches = (HashMap)
            Utils.deserialize("Branches", currdir + "/.gitlet/Branches");
        if (!allFiles.contains(commitid)) {
            System.out.println("No commit with that id exists.");
        } else {
            String curcommitid = (String) branches.get(branches.get("head"));
            Commit curcommit = (Commit)
                Utils.deserialize(curcommitid, currdir + "/.gitlet/Repository/" + curcommitid);
            Commit givcommit = (Commit)
                Utils.deserialize(commitid, currdir + "/.gitlet/Repository/" + commitid);
            for (Object filename : givcommit.blobs.keySet()) {
                List allFilesinworkingdir = Utils.plainFilenamesIn(currdir);
                if (allFilesinworkingdir.contains(filename)
                    && !curcommit.blobs.containsKey(filename)) {
                    File fileinworkdir = new File(currdir + "/" + filename);
                    byte[] bytes = Utils.readContents(fileinworkdir);
                    String shaidforbytes = Utils.sha1(bytes);
                    if (!shaidforbytes.equals(givcommit.blobs.get(filename))) {
                        System.out.println("There is an untracked file in the way; "
                            + "delete it or add it first.");
                        return;
                    }
                }
            }
            for (Object filename : givcommit.blobs.keySet()) {
                String blobid = (String) givcommit.blobs.get(filename);
                File blob = new File(currdir + "/.gitlet/Repository/" + blobid);
                byte[] bytes = Utils.readContents(blob);
                WorkingDirectory.addFiles((String) filename, bytes);
            }
            for (Object filename : curcommit.blobs.keySet()) {
                if (!givcommit.blobs.containsKey(filename)) {
                    WorkingDirectory.removeFiles((String) filename);
                }
            }
            branches.put(branches.get("head"), commitid);
            sa.clear();
            sealThem(sa, removedfiles, branches);
        }
    }

    /**
     * Merges files from the given branch into the current branch
     */
    public static void merge(String branchname) {

        /**
         * 1.desialize stagingarea object, list removedfiles and
         */
        StagingArea sa = (StagingArea)
            Utils.deserialize("StagingArea", currdir + "/.gitlet/StagingArea");
        List removedfiles = (List)
            Utils.deserialize("RemovedFiles", currdir + "/.gitlet/RemovedFiles");
        HashMap branches = (HashMap)
            Utils.deserialize("Branches", currdir + "/.gitlet/Branches");

        String curcommitid = (String) branches.get(branches.get("head"));
        String givcommitid = (String) branches.get(branchname);
        String splitcommitid = null;
        /**
         * 2. find split commit
         */
        HashMap givenbranchmap = new HashMap();
        String commitid = givcommitid;
        while (commitid != null) {
            givenbranchmap.put(commitid, 1);
            Commit commit = (Commit)
                Utils.deserialize(commitid, currdir + "/.gitlet/Repository/" + commitid);
            commitid = commit.parent;
        }
        commitid = curcommitid;
        while (commitid != null) {
            if (givenbranchmap.containsKey(commitid)) {
                splitcommitid = commitid;
                break;
            }
            Commit commit = (Commit)
                Utils.deserialize(commitid, currdir + "/.gitlet/Repository/" + commitid);
            commitid = commit.parent;
        }
        Commit curcommit = (Commit)
            Utils.deserialize(curcommitid, currdir + "/.gitlet/Repository/" + curcommitid);
        Commit givcommit = (Commit)
            Utils.deserialize(givcommitid, currdir + "/.gitlet/Repository/" + givcommitid);
        Commit splitcommit = (Commit)
            Utils.deserialize(splitcommitid, currdir + "/.gitlet/Repository/" + splitcommitid);


        /**
         * 3. uncommitted changes
         */
        if (!sa.engname.isEmpty() || !removedfiles.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }

        /**
         * 4. uncommited changes for modified but not staged
         */
        for (Object filename : curcommit.blobs.keySet()) {
            List allFilesinworkingdir = Utils.plainFilenamesIn(currdir);
            if (allFilesinworkingdir.contains(filename)) {
                File fileinworkdir = new File(currdir + "/" + filename);
                byte[] bytes = Utils.readContents(fileinworkdir);
                String shaidforbytes = Utils.sha1(bytes);
                if (!shaidforbytes.equals(curcommit.blobs.get(filename))) {
                    System.out.println("You have uncommitted changes.");
                    return;
                }
            }
        }
        /**
         * 5. A branch with that name does not exist
         */
        if (!branches.containsKey(branchname)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }

        /**
         * 6. Cannot merge a branch with itself
         */
        if (branches.get("head").equals(branchname)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }

        /**
         * 7. Given branch is an ancestor of the current branch
         */

        if (splitcommitid.equals(givcommitid)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }

        /**
         * 7. Current branch fast-forwarded.
         */

        if (splitcommitid.equals(curcommitid)) {
            branches.put(branches.get("head"), givcommitid);
            sealThem(sa, removedfiles, branches);
            System.out.println("Current branch fast-forwarded.");
            return;
        } else {

            /**
             * 8. untracked file in the way
             */
            for (Object filename : givcommit.blobs.keySet()) {
                List allFilesinworkingdir = Utils.plainFilenamesIn(currdir);
                if (allFilesinworkingdir.contains(filename)
                    && !curcommit.blobs.containsKey(filename)
                    && !splitcommit.blobs.containsKey(filename)) {
                    File fileinworkdir = new File(currdir + "/" + filename);
                    byte[] bytes = Utils.readContents(fileinworkdir);
                    String shaidforbytes = Utils.sha1(bytes);
                    if (!shaidforbytes.equals(givcommit.blobs.get(filename))) {
                        System.out.println("There is an untracked file in the way; "
                            + "delete it or add it first.");
                        return;
                    }
                }
            }

            /**
             * 9. loop over the splitcommit keys
             */
            for (Object filename : splitcommit.blobs.keySet()) {
                if (curcommit.blobs.containsKey(filename)
                    && givcommit.blobs.containsKey(filename)) {
                    if (!splitcommit.blobs.get(filename).equals(curcommit.blobs.get(filename))
                        && !splitcommit.blobs.get(filename).equals(givcommit.blobs.get(filename))) {

//                        try {
//                            String curblobid = (String) curcommit.blobs.get(filename);
//                            String givblobid = (String) givcommit.blobs.get(filename);
//                            File file0 = new File(currdir + "/" + filename);
//                            File file1 = new File(currdir + "/.gitlet/Repository/" + curblobid);
//                            File file2 = new File(currdir + "/.gitlet/Repository/" + givblobid);
//
//
//                            byte[] a = "<<<<<<< HEAD\n".getBytes();
//                            byte[] b = Utils.readContents(file1);
//                            byte[] c = "=======\n".getBytes();
//                            byte[] d = Utils.readContents(file2);
//                            byte[] e = ">>>>>>>".getBytes();
//
//                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                            outputStream.write(a);
//                            outputStream.write(b);
//                            outputStream.write(c);
//                            outputStream.write(d);
//                            outputStream.write(e);
//
//                            byte[] f = outputStream.toByteArray();
//
//
//                            Utils.writeContents(file0, f);
//                            file0.createNewFile();
//                        } catch (IOException e) {
//                            System.out.println("Internal error creating a new file");
//                        }

                        try {
                            String curblobid = (String) curcommit.blobs.get(filename);
                            String givblobid = (String) givcommit.blobs.get(filename);
                            File file0 = new File(currdir + "/" + filename);
                            File file1 = new File(currdir + "/.gitlet/Repository/" + curblobid);
                            File file2 = new File(currdir + "/.gitlet/Repository/" + givblobid);


                            String a = "<<<<<<< HEAD\n";
                            String b =new String (Utils.readContents(file1));
                            String c = "=======\n";
                            String d = new String (Utils.readContents(file2));
                            String e = ">>>>>>>";

                            String f = a+b+c+d+e;

                            byte[] g = f.getBytes();
                            Utils.writeContents(file0, g);
                            file0.createNewFile();
                        } catch (IOException e) {
                            System.out.println("Internal error creating a new file");
                        }

                        System.out.println("Encountered a merge conflict.");
                        return;
                    }
                    if (!splitcommit.blobs.get(filename).equals(curcommit.blobs.get(filename))
                        && splitcommit.blobs.get(filename).equals(givcommit.blobs.get(filename))) {
                        continue;
                    }
                    if (splitcommit.blobs.get(filename).equals(curcommit.blobs.get(filename))
                        && !splitcommit.blobs.get(filename).equals(givcommit.blobs.get(filename))) {
                        Command.checkoutCommit(givcommitid, (String) filename);
                        sa.addFile((String) filename);
                        continue;
                    }
                    if (splitcommit.blobs.get(filename).equals(curcommit.blobs.get(filename))
                        && splitcommit.blobs.get(filename).equals(givcommit.blobs.get(filename))) {
                        continue;
                    }
                } else {
                    if (curcommit.blobs.containsKey(filename)
                        && (!splitcommit.blobs.get(filename)
                        .equals(curcommit.blobs.get(filename)))) {

//                        try {
//                            String curblobid = (String) curcommit.blobs.get(filename);
//                            File file0 = new File(currdir + "/" + filename);
//                            File file1 = new File(currdir + "/.gitlet/Repository/" + curblobid);
//
//
//                            byte[] a = "<<<<<<< HEAD\n".getBytes();
//                            byte[] b = Utils.readContents(file1);
//                            byte[] c = "=======\n".getBytes();
//                            byte[] e = ">>>>>>>".getBytes();
//
//                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                            outputStream.write(a);
//                            outputStream.write(b);
//                            outputStream.write(c);
//                            outputStream.write(e);
//
//                            byte[] f = outputStream.toByteArray();
//
//
//                            Utils.writeContents(file0, f);
//                            file0.createNewFile();
//                        } catch (IOException e) {
//                            System.out.println("Internal error creating a new file");
//                        }

                        try {
                            String curblobid = (String) curcommit.blobs.get(filename);
                            String givblobid = (String) givcommit.blobs.get(filename);
                            File file0 = new File(currdir + "/" + filename);
                            File file1 = new File(currdir + "/.gitlet/Repository/" + curblobid);


                            String a = "<<<<<<< HEAD\n";
                            String b =new String (Utils.readContents(file1));
                            String c = "=======\n";

                            String e = ">>>>>>>";

                            String f = a+b+c+e;

                            byte[] g = f.getBytes();
                            Utils.writeContents(file0, g);
                            file0.createNewFile();
                        } catch (IOException e) {
                            System.out.println("Internal error creating a new file");
                        }


                        System.out.println("Encountered a merge conflict.");
                        return;
                    }
                    if (curcommit.blobs.containsKey(filename)
                        && (splitcommit.blobs.get(filename)
                        .equals(curcommit.blobs.get(filename)))) {
                        Command.rm((String) filename);
                        continue;
                    }
                    if (!curcommit.blobs.containsKey(filename)
                        && (!splitcommit.blobs.get(filename)
                        .equals(givcommit.blobs.get(filename)))) {

//                        try {
//                            String givblobid = (String) givcommit.blobs.get(filename);
//                            File file0 = new File(currdir + "/" + filename);
//                            File file2 = new File(currdir + "/.gitlet/Repository/" + givblobid);
//
//
//                            byte[] a = "<<<<<<< HEAD\n".getBytes();
//                            byte[] c = "=======\n".getBytes();
//                            byte[] d = Utils.readContents(file2);
//                            byte[] e = ">>>>>>>".getBytes();
//
//                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                            outputStream.write(a);
//                            outputStream.write(c);
//                            outputStream.write(d);
//                            outputStream.write(e);
//
//                            byte[] f = outputStream.toByteArray();
//
//
//                            Utils.writeContents(file0, f);
//                            file0.createNewFile();
//                        } catch (IOException e) {
//                            System.out.println("Internal error creating a new file");
//                        }

                        try {
                            String curblobid = (String) curcommit.blobs.get(filename);
                            String givblobid = (String) givcommit.blobs.get(filename);
                            File file0 = new File(currdir + "/" + filename);
                            File file2 = new File(currdir + "/.gitlet/Repository/" + givblobid);


                            String a = "<<<<<<< HEAD\n";
                            String c = "=======\n";
                            String d = new String (Utils.readContents(file2));
                            String e = ">>>>>>>";

                            String f = a+c+d+e;

                            byte[] g = f.getBytes();
                            Utils.writeContents(file0, g);
                            file0.createNewFile();
                        } catch (IOException e) {
                            System.out.println("Internal error creating a new file");
                        }

                        System.out.println("Encountered a merge conflict.");
                        return;
                    }
                    if (!curcommit.blobs.containsKey(filename)
                        && (splitcommit.blobs.get(filename)
                        .equals(givcommit.blobs.get(filename)))) {
                        continue;
                    }
                    if (!curcommit.blobs.containsKey(filename)
                        && !givcommit.blobs.containsKey(filename)) {
                        continue;
                    }
                }
            }

            /**
             * 10. loop over the givcommit keys
             */
            for (Object filename : givcommit.blobs.keySet()) {
                if (!splitcommit.blobs.containsKey(filename)) {
                    if (!curcommit.blobs.containsKey(filename)) {
                        Command.checkoutCommit(givcommitid, (String) filename);
                        sa.addFile((String) filename);
                        continue;
                    } else {
                        if (givcommit.blobs.get(filename)
                            .equals(curcommit.blobs.get(filename))) {
                            continue;
                        } else {

//                            try {
//                                String curblobid = (String) curcommit.blobs.get(filename);
//                                String givblobid = (String) givcommit.blobs.get(filename);
//                                File file0 = new File(currdir + "/" + filename);
//                                File file1 = new File(currdir + "/.gitlet/Repository/" + curblobid);
//                                File file2 = new File(currdir + "/.gitlet/Repository/" + givblobid);
//
//
//                                byte[] a = "<<<<<<< HEAD\n".getBytes();
//                                byte[] b = Utils.readContents(file1);
//                                byte[] c = "=======\n".getBytes();
//                                byte[] d = Utils.readContents(file2);
//                                byte[] e = ">>>>>>>".getBytes();
//
//                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                                outputStream.write(a);
//                                outputStream.write(b);
//                                outputStream.write(c);
//                                outputStream.write(d);
//                                outputStream.write(e);
//
//                                byte[] f = outputStream.toByteArray();
//
//
//                                Utils.writeContents(file0, f);
//                                file0.createNewFile();
//                            } catch (IOException e) {
//                                System.out.println("Internal error creating a new file");
//                            }

                            try {
                                String curblobid = (String) curcommit.blobs.get(filename);
                                String givblobid = (String) givcommit.blobs.get(filename);
                                File file0 = new File(currdir + "/" + filename);
                                File file1 = new File(currdir + "/.gitlet/Repository/" + curblobid);
                                File file2 = new File(currdir + "/.gitlet/Repository/" + givblobid);


                                String a = "<<<<<<< HEAD\n";
                                String b =new String (Utils.readContents(file1));
                                String c = "=======\n";
                                String d = new String (Utils.readContents(file2));
                                String e = ">>>>>>>";

                                String f = a+b+c+d+e;

                                byte[] g = f.getBytes();
                                Utils.writeContents(file0, g);
                                file0.createNewFile();
                            } catch (IOException e) {
                                System.out.println("Internal error creating a new file");
                            }

                            System.out.println("Encountered a merge conflict.");
                            return;
                        }

                    }
                }

            }

            /**
             * 11. merge completed
             */
            System.out.println("Merged " + branches.get("head") + " with " + branchname + ".");
            sealThem(sa, removedfiles, branches);
        }


    }

}
