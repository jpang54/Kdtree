import edu.princeton.cs.algs4.Point2D;
import edu.princeton.cs.algs4.Queue;
import edu.princeton.cs.algs4.RectHV;

public class KdTreeST<Value> {
    private static final int VERTICAL = 0; // denotes vertical orientation
    private static final int HORIZONTAL = 1; // denotes horizontal orientation

    private Node root; // root node of k-d tree
    private int totalNodes; // number of nodes in the k-d tree

    // nodes that make up the k-d tree
    private class Node {
        /* @citation Adapted from: https://www.cs.princeton.edu/courses/archive/
        fall21/cos226/assignments/kdtree/checklist.php. Accessed 10/7/21. */
        private Point2D p;   // the point
        private Value val;   // the symbol table maps the point to this value
        private RectHV rect; // the axis-aligned rectangle corresponding to this node
        private Node left;   // the left/bottom subtree
        private Node right;  // the right/top subtree
        private int orientation; // whether the point is vertical or horizontal

        // constructs a new node with the given parameters
        public Node(Point2D p, Value val, int orientation, double xmin,
                    double ymin, double xmax, double ymax) {
            this.p = p;
            this.val = val;
            this.orientation = orientation;
            this.rect = new RectHV(xmin, ymin, xmax, ymax);
        }

    }

    // construct an empty k-d tree
    public KdTreeST() {
        root = null;
        totalNodes = 0;
    }

    // is the k-d tree empty?
    public boolean isEmpty() {
        return root == null;
    }

    // number of nodes
    public int size() {
        return totalNodes;
    }

    // associate the value val with point p
    public void put(Point2D p, Value val) {
        if (p == null || val == null) {
            throw new IllegalArgumentException();
        }

        if (!contains(p)) {
            totalNodes++;
        }
        // always start from root, which has an infinite bounding box
        root = put(root, p, val, VERTICAL, Double.NEGATIVE_INFINITY,
                   Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                   Double.POSITIVE_INFINITY);

    }

    // helper method for put()
    private Node put(Node x, Point2D p, Value value, int orientation,
                     double xmin, double ymin, double xmax, double ymax) {
        /* @citation Adapted from: https://algs4.cs.princeton.edu/32bst/BST.
        java.html. Accessed 10/7/21. */

        // base case for adding a new node if the point doesn't exist in tree
        if (x == null) return new Node(p, value, orientation, xmin, ymin, xmax,
                                       ymax);

        // update value for repeat points
        if (p.equals(x.p)) {
            x.val = value;
            return x;
        }

        // vertical orientation compares x-coordinates; also reduce bounding box
        if (orientation == VERTICAL) {
            if (p.x() < (x.p).x()) {
                x.left = put(x.left, p, value, HORIZONTAL, xmin, ymin,
                             (x.p).x(), ymax);
            }
            else {
                x.right = put(x.right, p, value, HORIZONTAL, (x.p).x(), ymin,
                              xmax, ymax);
            }
        }
        // horizontal orientation compares y-coordinates
        else {
            if (p.y() < (x.p).y()) {
                x.left = put(x.left, p, value, VERTICAL, xmin, ymin, xmax,
                             (x.p).y());
            }
            else {
                x.right = put(x.right, p, value, VERTICAL, xmin, (x.p).y(),
                              xmax, ymax);
            }
        }

        return x;
    }

    // value associated with point p
    public Value get(Point2D p) {
        return get(root, p, VERTICAL);
    }

    // helper class for get()
    private Value get(Node x, Point2D p, int orientation) {
        /* @citation Adapted from: https://algs4.cs.princeton.edu/32bst/BST.
        java.html. Accessed 10/8/21. */
        if (p == null) {
            throw new IllegalArgumentException();
        }

        // base case for nonexistent matching node
        if (x == null) return null;

        if (p.equals(x.p)) {
            return x.val;
        }

        // vertical orientation compares x-coordinates
        if (orientation == VERTICAL) {
            if (p.x() < (x.p).x()) {
                return get(x.left, p, HORIZONTAL);
            }
            else {
                return get(x.right, p, HORIZONTAL);
            }
        }
        // horizontal orientation compares y-coordinates
        else {
            if (p.y() < (x.p).y()) {
                return get(x.left, p, VERTICAL);
            }
            else {
                return get(x.right, p, VERTICAL);
            }
        }
    }

    // does the symbol table contain point p?
    public boolean contains(Point2D p) {
        /* @citation Adapted from: https://algs4.cs.princeton.edu/32bst/BST.
        java.html. Accessed 10/8/21. */
        if (p == null) {
            throw new IllegalArgumentException();
        }
        return get(p) != null;
    }

    // all points in the symbol table
    public Iterable<Point2D> points() {
        /* @citation Adapted from: https://algs4.cs.princeton.edu/32bst/BST.
        java.html. Accessed 10/8/21. */
        Queue<Point2D> points = new Queue<Point2D>();
        Queue<Node> queue = new Queue<Node>();

        queue.enqueue(root);

        while (!queue.isEmpty()) {
            Node x = queue.dequeue();
            if (x == null) continue; // skip over null nodes
            points.enqueue(x.p);
            queue.enqueue(x.left); // check the children
            queue.enqueue(x.right);
        }

        return points;
    }

    // all points that are inside the rectangle (or on the boundary)
    public Iterable<Point2D> range(RectHV rect) {
        /* @citation Adapted from: https://algs4.cs.princeton.edu/32bst/BST.
        java.html. Accessed 10/8/21. */
        if (rect == null) {
            throw new IllegalArgumentException();
        }

        Queue<Point2D> points = new Queue<Point2D>();
        Queue<Node> queue = new Queue<Node>();

        queue.enqueue(root);

        while (!queue.isEmpty()) {
            Node x = queue.dequeue();
            if (x == null) continue; // skip over null nodes
            if (x.rect.intersects(rect)) {
                if (rect.contains(x.p)) points.enqueue(x.p);
                queue.enqueue(x.left); // check rectangles of children
                queue.enqueue(x.right);
            }
        }
        return points;
    }

    // a nearest neighbor of point p; null if the symbol table is empty
    public Point2D nearest(Point2D p) {
        if (p == null) {
            throw new IllegalArgumentException();
        }
        return findNearestNeighbor(root, p, null, Double.POSITIVE_INFINITY);
    }

    // helper method for nearest()
    private Point2D findNearestNeighbor(Node x, Point2D p, Point2D champ,
                                        double minDist) {
        /* @citation Adapted from: https://edstem.org/us/courses/7744/lessons/
        21592/slides/125265. Accessed 10/8/21. */

        // keep track of champions in each subtree
        Point2D left;
        Point2D right;

        if (x == null) return champ; // base case for null nodes

        // return if the bounding box is not closer than best so far
        if (minDist < x.rect.distanceSquaredTo(p)) {
            return champ;
        }

        // check if point is closer than best so far
        if (x.p.distanceSquaredTo(p) < minDist) {
            champ = x.p;
            minDist = champ.distanceSquaredTo(p);

        }

        // vertical orientation compares x-coordinates
        if (x.orientation == VERTICAL) {
            if (p.x() < x.p.x()) {
                left = findNearestNeighbor(x.left, p, champ, minDist);
                minDist = left.distanceSquaredTo(p); // update smallest distance

                right = findNearestNeighbor(x.right, p, left, minDist);
            }
            else {
                right = findNearestNeighbor(x.right, p, champ, minDist);
                minDist = right.distanceSquaredTo(p); // update smallest distance

                left = findNearestNeighbor(x.left, p, right, minDist);
            }
        }
        // horizontal orientation compares y-coordinates
        else {
            if (p.y() < x.p.y()) {
                left = findNearestNeighbor(x.left, p, champ, minDist);
                minDist = left.distanceSquaredTo(p);

                right = findNearestNeighbor(x.right, p, left, minDist);
            }
            else {
                right = findNearestNeighbor(x.right, p, champ, minDist);
                minDist = right.distanceSquaredTo(p);

                left = findNearestNeighbor(x.left, p, right, minDist);
            }
        }

        // compare the two champions from the two subtrees
        if (left.distanceSquaredTo(p) < right.distanceSquaredTo(p)) return left;
        else return right;
    }

    // unit testing (required)
    public static void main(String[] args) {
        KdTreeST<Integer> test = new KdTreeST<Integer>();

        Point2D a = new Point2D(0, 0);
        Point2D b = new Point2D(1, 1);
        Point2D c = new Point2D(2, 1);

        test.put(a, 1);
        test.put(b, 2);
        test.put(c, 3);

        System.out.println(test.isEmpty()); // false
        System.out.println(test.size()); // 3
        System.out.println(test.get(a)); // 1
        System.out.println(test.get(b)); // 2

        test.put(b, 4);
        System.out.println(test.size()); // 3
        System.out.println(test.get(b)); // 4

        System.out.println(test.contains(new Point2D(2, 2))); // false

        System.out.println(test.points()); // a b and c

        RectHV rect = new RectHV(-1, -1, 1, 1);
        System.out.println(test.range(rect)); // a and b

        System.out.println(test.nearest(a)); // a
        System.out.println(test.nearest(new Point2D(-1, 1))); // a
    }
}
