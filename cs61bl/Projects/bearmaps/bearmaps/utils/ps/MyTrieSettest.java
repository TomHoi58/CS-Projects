package bearmaps.utils.ps;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MyTrieSettest {

    private static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }
    @Test
    public void ramdom(){
        MyTrieSet test = new MyTrieSet();

//        test.add("the phoenix pastificio","The Phoenix Pastificio", 4143614889);
        System.out.println("Tom" +cleanString("76")+"Hey");
    }
}
