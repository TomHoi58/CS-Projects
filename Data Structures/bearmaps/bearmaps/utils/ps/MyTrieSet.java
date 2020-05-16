package bearmaps.utils.ps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MyTrieSet implements TrieSet61BL {
    private Node root = new Node(false);

    @Override
    public void clear() {
        root = new Node(false);
    }

    @Override
    public boolean contains(String key) {
        char[] chars = key.toCharArray();
        Node p = root;
        char lastchar = chars[chars.length - 1];
        for (char c : chars) {
            if (!p.next.containsKey(c)) {
                return false;
            } else {
                p = p.next.get(c);
                if (c == lastchar && !p.isKey) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void add(String cleanname,String dirtyname,long id) {
        if (cleanname.equals("")){
            root.isKey = true;
            root.dirtyname.add(dirtyname);
            root.nodeids.add(id);
            return;
        }

        char[] chars = cleanname.toCharArray();
        Node p = root;
        if (chars.length == 0){
            return;
        }

        for (int i = 0; i<chars.length; i++) {
            if (p.next.containsKey(chars[i])) {
                p = p.next.get(chars[i]);
            } else {
                p.next.put(chars[i], new Node(false));
                p = p.next.get(chars[i]);
            }
            if (i == chars.length-1) {
                p.isKey = true;
                p.dirtyname.add(dirtyname);
                p.nodeids.add(id);
            }
        }


    }

    @Override
    public List<String> keysWithPrefix(String prefix) {
        char[] chars = prefix.toCharArray();
        Node p = root;

        List<String> result = new ArrayList<>();
        for (int i = 0; i<chars.length; i++) {
            if (!p.next.containsKey(chars[i])) {
                return result;
            } else {
                p = p.next.get(chars[i]);
                if (i == chars.length-1) {
                    if (p.isKey){
                        result.addAll(p.dirtyname);
                    }
                    for (char a : p.next.keySet()) {
                        colHelp(prefix + a, result, p.next.get(a));
                    }
                    return result;
                }
            }
        }
        return result;
    }

    private void colHelp(String s, List lst, Node n) {
        if (n.isKey) {
            lst.addAll(n.dirtyname);
        }
        for (char c : n.next.keySet()) {
            colHelp(s + c, lst, n.next.get(c));
        }
    }

    public List<Long> getids(String name){
        if (name.equals("")){
            return root.nodeids;
        }
        char[] chars = name.toCharArray();
        Node p = root;
        if (chars.length == 0){
            return new ArrayList<>();
        }
        for (int i =0; i < chars.length; i++) {
            if (p.next.containsKey(chars[i])) {
                p = p.next.get(chars[i]);
            } else {
                System.out.println("givensearchterm is not valid!");
                return new ArrayList<>();
            }
            if (i == chars.length-1) {
                return p.nodeids;
            }
        }
        return new ArrayList<>();

    }

    @Override
    public String longestPrefixOf(String key) {
        throw new UnsupportedOperationException();
    }

    private static class Node {
        private boolean isKey;
        private HashMap<Character, Node> next;
        private List<String> dirtyname;
        private List<Long> nodeids;

        private Node(boolean b) {
            isKey = b;
            next = new HashMap();
            dirtyname = new ArrayList<>();
            nodeids = new ArrayList<>();
        }
    }
}
