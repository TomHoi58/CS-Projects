package gitlet;

import org.junit.Test;


public class CommitTest {

    @Test
    public void mainTest() {
        Commit test1 = new Commit("i love gitlet", "1231231sdfsdfsdf");
        test1.addBlobs("tom", "sdfsdfsagkjsdfsdfk");
        test1.addBlobs("jerry", "12sdjfkhsdjfkbnskf");
        String currdir = System.getProperty("user.dir");

        Utils.serialize(test1, currdir);
        System.out.println(Utils.getsha1(test1));
        test1 = (Commit)
            Utils.deserialize("ac8ccd924d604c31ec6a5734d9459abdace13294",
                currdir + "/ac8ccd924d604c31ec6a5734d9459abdace13294");
        System.out.println(test1.message);


        Commit test2 = new Commit("i love gitlet", "1231231sdfsdfsdf");
        test2.addBlobs("tom", "sdfsdfsagkjsdfsdfk");
        test2.addBlobs("jerry", "12sdjfkhsdjfkbnskf");

    }
}
