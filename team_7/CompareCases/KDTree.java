package org.dfpl.dbp.rtree.team_7.CompareCases;

import org.dfpl.dbp.rtree.team_7.Point;
import org.dfpl.dbp.rtree.team_7.Rectangle;

import java.util.*;

/*
    트리 밸런싱을 고려하지 않는 2D KDTree
    점 자체를 기준으로, X축과 Y축을 번갈아가며 공간을 2분할해가는 구조.

    밸런싱을 안하기 때문에 삽입 포인트가 적으면 성능이 운에 많이 좌우됨
*/
public class KDTree implements SpatialIndex {
    kdNode root;

    public KDTree() {
        root = null;
    }

    @Override
    public void add(Point point) {
        kdNode n = rInsertNode(root, point, 0);
        if (root == null) root = n;
        //System.out.println(n.point.toString());
    }

    kdNode rInsertNode(kdNode curNode, Point p, int depth) {
        if (curNode == null) return new kdNode(p);

        boolean isBasedOnX = depth % 2 == 0;

        if ((isBasedOnX && (p.getX() < curNode.point.getX()) ||
                (!isBasedOnX && (p.getY() < curNode.point.getY())))
        ) {
            curNode.leftNode = rInsertNode(curNode.leftNode, p, depth + 1);
        } else {
            curNode.rightNode = rInsertNode(curNode.rightNode, p, depth + 1);
        }
        return curNode;
    }

    @Override
    public Iterator<Point> search(Rectangle rectangle) {
        ArrayList<Point> results = new ArrayList<>();
        return rRangeSearch(root, rectangle, results, 0).iterator();
    }

    ArrayList<Point> rRangeSearch(kdNode curNode, Rectangle rect, ArrayList<Point> results, int depth) {
        if (curNode == null) return null;

        if (isPointInRect(curNode.point, rect)) {
            results.add(curNode.point);
        }

        boolean isBasedOnX = depth % 2 == 0;

        ArrayList<Point> r;
        if ((isBasedOnX && rect.getLeftTop().getX() <= curNode.point.getX()) ||
                (!isBasedOnX && rect.getLeftTop().getY() <= curNode.point.getY())) {
            r = rRangeSearch(curNode.leftNode, rect, results, depth + 1);
            if (r != null) results = r;
        }

        if ((isBasedOnX && rect.getRightBottom().getX() >= curNode.point.getX()) ||
                (!isBasedOnX && rect.getRightBottom().getY() >= curNode.point.getY())) {
            r = rRangeSearch(curNode.rightNode, rect, results, depth + 1);
            if (r != null) results = r;
        }

        return results;
    }

    boolean isPointInRect(Point p, Rectangle rect) {
        return p.getX() >= rect.getLeftTop().getX() &&
                p.getX() <= rect.getRightBottom().getX() &&
                p.getY() >= rect.getLeftTop().getY() &&
                p.getY() <= rect.getRightBottom().getY();
    }

    @Override
    public Iterator<Point> nearest(Point source, int maxCount) {
        Map<Point, Double> results = new HashMap<Point, Double>();
        results = rKNNSearch(root, source, results, maxCount, 0);

        return results.keySet().iterator();
    }

    Map<Point, Double> rKNNSearch(kdNode curNode, Point source, Map<Point, Double> results, int maxCount, int depth) {
        if (curNode == null) return null;

        double dist = source.distance(curNode.point);
        Map.Entry<Point, Double> maxPointDist = getMaxDistancePoint(results);

        if (results.size() < maxCount) {
            results.put(curNode.point, dist);
        } else if (dist < maxPointDist.getValue()) {
            results.remove(maxPointDist.getKey());
            results.put(curNode.point, dist);
        }

        boolean isBasedOnX = depth % 2 == 0;
        double srcValue, curValue;
        kdNode nearNodes, farNodes;
        if (isBasedOnX) {
            srcValue = source.getX();
            curValue = curNode.point.getX();
        } else {
            srcValue = source.getY();
            curValue = curNode.point.getY();
        }
        if (srcValue < curValue) {
            nearNodes = curNode.leftNode;
            farNodes = curNode.rightNode;
        } else {
            nearNodes = curNode.rightNode;
            farNodes = curNode.leftNode;
        }

        Map<Point, Double> r = rKNNSearch(nearNodes, source, results, maxCount, depth + 1);
        if (r != null) results = r;

        boolean shouldVisit;
        if (results.size() < maxCount) shouldVisit = true;
        else {
            double splitDist = Math.abs(srcValue - curValue);
            maxPointDist = getMaxDistancePoint(results);
            if (splitDist * splitDist < maxPointDist.getValue()) {
                shouldVisit = true;
            } else shouldVisit = false;
        }

        if (shouldVisit) {
            r = rKNNSearch(farNodes, source, results, maxCount, depth + 1);
            if (r != null) results = r;
        }
        return results;
    }

    Map.Entry<Point, Double> getMaxDistancePoint(Map<Point, Double> map) {
        double maxDist = 0;
        Point maxPoint = null;
        for (Map.Entry<Point, Double> e : map.entrySet()) {
            if (e.getValue() > maxDist) {
                maxDist = e.getValue();
                maxPoint = e.getKey();
            }
        }
        return new AbstractMap.SimpleEntry<>(maxPoint, maxDist);
    }

    @Override
    public void delete(Point point) {
        rDeleteNode(root, point, 0);
    }

    kdNode rDeleteNode(kdNode curNode, Point p, int depth) {
        if (curNode == null) return null;

        boolean isBasedOnX = depth % 2 == 0;

        if (curNode.point.getX() == p.getX() && curNode.point.getY() == p.getY()) {
            if (curNode.rightNode != null) {
                kdNode minNode = rFindMinNode(curNode.rightNode, isBasedOnX, depth + 1);
                curNode.point = minNode.point;
                curNode.rightNode = rDeleteNode(curNode.rightNode, minNode.point, depth + 1);
                return curNode;
            }
            if (curNode.leftNode != null) {
                kdNode minNode = rFindMinNode(curNode.leftNode, isBasedOnX, depth + 1);
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
        ) {
            curNode.leftNode = rDeleteNode(curNode.leftNode, p, depth + 1);
        } else {
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
            return rFindMinNode(curNode.leftNode, isXAxis, depth + 1);
        }
        kdNode leftMinNode = rFindMinNode(curNode.leftNode, isXAxis, depth + 1);
        kdNode rightMinNode = rFindMinNode(curNode.rightNode, isXAxis, depth + 1);

        kdNode minNode = curNode;

        if (leftMinNode != null) {
            if (comparePointByAxis(leftMinNode.point, minNode.point, isXAxis) < 0) {
                minNode = leftMinNode;
            }
        }
        if (rightMinNode != null) {
            if (comparePointByAxis(rightMinNode.point, minNode.point, isXAxis) < 0) {
                minNode = rightMinNode;
            }
            ;
        }

        return minNode;
    }

    int comparePointByAxis(Point a, Point b, boolean isXAxis) {
        if (isXAxis == true) return Double.compare(a.getX(), b.getX());
        return Double.compare(a.getY(), b.getY());
    }

    @Override
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