package bearmaps.utils.ps;

import java.util.List;

public class KDTree implements PointSet {
    protected KDTreeNode root;

    /* Constructs a KDTree using POINTS. You can assume POINTS contains at least one
       Point object. */
    public KDTree(List<Point> points) {
        for (Point p : points) {
            insert(p);
        }
    }

    /*

    You might find this insert helper method useful when constructing your KDTree!
    Think of what arguments you might want insert to take in. If you need
    inspiration, take a look at how we do BST insertion!

    */

    private void insert(Point p) {
        root = insertHelper(root, p, true);
    }

    private KDTreeNode insertHelper(KDTreeNode node, Point p, Boolean orientation) {

        if (node == null) {
            return new KDTreeNode(p, orientation);
        } else if (node.point().equals(p)) {
            return new KDTreeNode(p, orientation, node.left, node.right);
        } else if (pointComparator(node.point(), p, orientation) > 0) {
            node.left = insertHelper(node.left, p, !orientation);
            return node;
        } else {
            node.right = insertHelper(node.right, p, !orientation);
            return node;
        }
    }

    private int pointComparator(Point p1, Point p2, boolean orientation) {
        if (orientation) {
            return Double.compare(p1.getX(), p2.getX());
        }
        return Double.compare(p1.getY(), p2.getY());
    }


    /* Returns the closest Point to the inputted X and Y coordinates. This method
       should run in O(log N) time on average, where N is the number of POINTS. */
    public Point nearest(double x, double y) {
        return nearestHelper(root, new Point(x, y), root, true).point();
    }

    private KDTreeNode nearestHelper(KDTreeNode node, Point goal,
                                     KDTreeNode best, boolean orientation) {
        KDTreeNode goodside;
        KDTreeNode badside;
        Point bestBadSidePoint;

        if (node == null) {
            return best;
        }
        if (Point.distance(node.point(), goal) < Point.distance(best.point(), goal)) {
            best = node;
        }
        if (pointComparator(node.point(), goal, orientation) > 0) {
            goodside = node.left;
            badside = node.right;
        } else {
            badside = node.left;
            goodside = node.right;
        }
        best = nearestHelper(goodside, goal, best, !orientation);
        if (orientation) {
            bestBadSidePoint = new Point(node.point.getX(), goal.getY());
        } else {
            bestBadSidePoint = new Point(goal.getX(), node.point.getY());
        }
        if (Point.distance(best.point(), goal) > Point.distance(bestBadSidePoint, goal)) {
            best = nearestHelper(badside, goal, best, !orientation);
        }
        return best;
    }

    private class KDTreeNode {

        private static final boolean HORIZONTAL = false;
        private static final boolean VERTICAL = true;
        private Point point;
        private KDTreeNode left;
        private KDTreeNode right;
        private boolean orientation;

        // If you want to add any more instance variables, put them here!

        KDTreeNode(Point p, boolean orientation) {
            this.point = p;
            this.orientation = orientation;
        }

        KDTreeNode(Point p, boolean orientation, KDTreeNode left, KDTreeNode right) {
            this.point = p;
            this.orientation = orientation;
            this.left = left;
            this.right = right;
        }

        Point point() {
            return point;
        }

        KDTreeNode left() {
            return left;
        }

        KDTreeNode right() {
            return right;
        }

        // If you want to add any more methods, put them here!

    }
}
