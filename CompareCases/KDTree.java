package org.dfpl.dbp.rtree.CompareCases;

import org.dfpl.dbp.rtree.Point;
import org.dfpl.dbp.rtree.Rectangle;

import java.util.*;

public class KDTree {
    kdNode root;
    public KDTree() {
        root = null;
    }

    public void add(Point point) {
        kdNode n =rInsertNode(root, point, 0);
        if(root == null) root = n;
        //System.out.println(n.point.toString());
    }
    kdNode rInsertNode(kdNode curNode, Point p, int depth) {
        if (curNode == null) return new kdNode(p);

        boolean isBasedOnX = depth % 2 == 0;

        if((isBasedOnX && (p.getX() < curNode.point.getX()) ||
          (!isBasedOnX && (p.getY() < curNode.point.getY())))
        )
        {
            curNode.leftNode = rInsertNode(curNode.leftNode, p, depth + 1);
        }
        else {
            curNode.rightNode = rInsertNode(curNode.rightNode, p, depth + 1);
        }
        return curNode;
    }

    public Iterator<Point> search(Rectangle rectangle) {
        ArrayList<Point> results = new ArrayList<>();
        return rRangeSearch(root, rectangle, results, 0).iterator();
    }
    ArrayList<Point> rRangeSearch(kdNode curNode, Rectangle rec, ArrayList<Point> results, int depth) {
        if (curNode == null) return null;

        if(IsPointInRect(curNode.point, rec)) {
            results.add(curNode.point);
        }

        boolean isBasedOnX = depth % 2 == 0;

        ArrayList<Point> r;
        if ((isBasedOnX && rec.getLeftTop().getX() <= curNode.point.getX()) ||
           (!isBasedOnX && rec.getLeftTop().getY() <= curNode.point.getY())) {
            r = rRangeSearch(curNode.leftNode, rec, results, depth + 1);
            if (r != null) results = r;
        }

        if ((isBasedOnX && rec.getRightBottom().getX() >= curNode.point.getX()) ||
           (!isBasedOnX && rec.getRightBottom().getY() >= curNode.point.getY())) {
            r = rRangeSearch(curNode.rightNode, rec, results, depth + 1);
            if (r != null) results = r;
        }

        return results;
    }
    boolean IsPointInRect(Point p, Rectangle rec) {
        if (p.getX()>=rec.getLeftTop().getX() &&
            p.getX()<=rec.getRightBottom().getX() &&
            p.getY()>=rec.getLeftTop().getY() &&
            p.getY()<=rec.getRightBottom().getY()) {
            return true;
        }
        return false;
    }


    public Iterator<Point> nearest(Point source, int maxCount) {
        Map<Point, Double> results = new HashMap<Point, Double>();
        results = rKNNSearch(root, source, results, maxCount, 0);

        return results.keySet().iterator();
    }
    Map<Point, Double> rKNNSearch(kdNode curNode, Point source, Map<Point, Double> results, int maxCount, int depth) {
        if (curNode == null) return null;

        double dist = source.distance(curNode.point);
        Map.Entry<Point, Double> maxPointDist = GetMaxDistancePoint(results);

        if (results.size() < maxCount) {
            results.put(curNode.point, dist);
        }
        else if (dist < maxPointDist.getValue()) {
            results.remove(maxPointDist.getKey());
            results.put(curNode.point, dist);
        }

        boolean isBasedOnX = depth % 2 == 0;
        double srcValue, curValue;
        kdNode nearNodes, farNodes;
        if (isBasedOnX) {
            srcValue = source.getX();
            curValue = curNode.point.getX();
        }
        else {
            srcValue = source.getY();
            curValue = curNode.point.getY();
        }
        if (srcValue < curValue) {
            nearNodes = curNode.leftNode;
            farNodes =  curNode.rightNode;
        }
        else {
            nearNodes = curNode.rightNode;
            farNodes =  curNode.leftNode;
        }

        Map<Point, Double> r = rKNNSearch(nearNodes, source, results, maxCount, depth + 1);
        if (r != null) results = r;

        boolean shouldVisit;
        if (results.size() < maxCount) shouldVisit = true;
        else {
            double splitDist = Math.abs(srcValue - curValue);
            maxPointDist = GetMaxDistancePoint(results);
            if(splitDist*splitDist < maxPointDist.getValue()) {
                shouldVisit = true;
            }
            else shouldVisit = false;
        }

        if(shouldVisit)  {
            r = rKNNSearch(farNodes, source, results, maxCount, depth + 1);
            if (r != null) results = r;
        }
        return results;
    }
    Map.Entry<Point, Double> GetMaxDistancePoint(Map<Point, Double> map) {
        double maxDist=0;
        Point maxPoint=null;
        for (Map.Entry<Point, Double> e : map.entrySet()) {
            if (e.getValue() > maxDist) {
                maxDist = e.getValue();
                maxPoint = e.getKey();
            }
        }
        return new AbstractMap.SimpleEntry<>(maxPoint, maxDist);
    }

    public void delete(Point point) {
        rDeleteNode(root, point, 0);
    }
    kdNode rDeleteNode(kdNode curNode, Point p, int depth) {
        if (curNode == null) return null;

        boolean isBasedOnX = depth % 2 == 0;

        if (curNode.point.getX() == p.getX() && curNode.point.getY() == p.getY()) {
            if (curNode.rightNode != null) {
                kdNode minNode = rFindMinNode(curNode.rightNode, isBasedOnX, depth+1);
                curNode.point = minNode.point;
                curNode.rightNode = rDeleteNode(curNode.rightNode, minNode.point, depth+1);
                return curNode;
            }
            if (curNode.leftNode != null) {
                kdNode minNode = rFindMinNode(curNode.leftNode, isBasedOnX, depth+1);
                curNode.point = minNode.point;
                curNode.rightNode = rDeleteNode(curNode.leftNode, minNode.point, depth + 1);
                curNode.leftNode = null;
                return curNode;
            }

            root = null;
            return null;
        }

        if (((isBasedOnX && p.getX() < curNode.point.getX()) ||
           ((!isBasedOnX && p.getY() < curNode.point.getY())))
        )
        {
            curNode.leftNode = rDeleteNode(curNode.leftNode, p, depth + 1);
        }
        else {
            curNode.rightNode = rDeleteNode(curNode.rightNode, p, depth + 1);
        }
        return curNode;
    }
    kdNode rFindMinNode(kdNode curNode, boolean isXAxis, int depth) {
        if (curNode == null) return null;

        boolean isBasedOnX = depth % 2 == 0;

        if (isBasedOnX == isXAxis) {
            if (curNode.leftNode == null) {
                return curNode;
            }
            return rFindMinNode(curNode.leftNode, isXAxis, depth+1);
        }
        kdNode leftMinNode = rFindMinNode(curNode.leftNode, isXAxis, depth+1);
        kdNode rightMinNode = rFindMinNode(curNode.rightNode, isXAxis, depth+1);

        kdNode minNode = curNode;

        if (leftMinNode != null) {
            if (compareCoord(leftMinNode.point, minNode.point, isXAxis) < 0) {
                minNode = leftMinNode;
            }
        }
        if (rightMinNode != null) {
            if (compareCoord(rightMinNode.point, minNode.point, isXAxis) < 0) {
                minNode = rightMinNode;
            };
        }

        return minNode;
    }
    int compareCoord(Point a, Point b, boolean isXAxis) {
        if (isXAxis == true) return Double.compare(a.getX(), b.getX());
        return Double.compare(a.getY(), b.getY());
    }

    public boolean isEmpty() {
        return (root == null);
    }
}

class kdNode {
    Point point;
    kdNode leftNode;
    kdNode rightNode;

    public kdNode(Point p) {
        point = p;
        leftNode = null;
        rightNode = null;
    }
}
