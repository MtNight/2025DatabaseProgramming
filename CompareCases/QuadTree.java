package org.dfpl.dbp.rtree.CompareCases;

import org.dfpl.dbp.rtree.Point;
import org.dfpl.dbp.rtree.Rectangle;

import java.util.*;

/*
    리프 노드에만 점이 존재하는, 노드 당 하나의 점만을 가지는 Point Region QuadTree
    노드마다 정해진 범위를 가지고, 입력이 들어올 때 마다 범위를 4분할하여 자식으로 가지는 구조.

    문제에서 좌표의 범위를 지정하지 않았기 때문에 삽입 전에 입력 범위를 한 번 체크하는 기능 필요.
    첫 삽입 이후 추가 삽입이 들어올 때 범위가 벗어나면 버그 발생 예정...인데 성능 테스트용이라 그냥 둠.
*/
public class QuadTree {
    qNode root;
    Rectangle WholeRange;
    public QuadTree() { root = null; }

    public void checkRange(List<Point> pointList) {
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        for (Point point : pointList) {
            if (point.getX() < minX) minX = point.getX();
            if (point.getX() > maxX) maxX = point.getX();
            if (point.getY() < minY) minY = point.getY();
            if (point.getY() > maxY) maxY = point.getY();
        }
        WholeRange = new Rectangle(new Point(minX, minY), new Point(maxX, maxY));
    }
    public void add(Point point) {
        if (root == null) root = new qNode(null, WholeRange);

        if (!isPointInRect(point, WholeRange)) {
            System.out.println("Error");
            return;
        }
        rInsertNode(root, point);
    }
    boolean rInsertNode(qNode curNode, Point p) {
        if (!curNode.contains(p)) return false;

        if (curNode.isLeaf()) {
            if (curNode.point == null) {
                curNode.point = p;
                return true;
            }

            curNode.subDivide();
            rInsertNode(curNode.getContainableNode(curNode.point), curNode.point);
            curNode.point = null;
        }

        return rInsertNode(curNode.getContainableNode(p), p);
    }
    public Iterator<Point> search(Rectangle rectangle) {
        ArrayList<Point> results = new ArrayList<>();
        return rRangeSearch(root, rectangle, results).iterator();
    }
    ArrayList<Point> rRangeSearch(qNode curNode, Rectangle rect, ArrayList<Point> results) {
        if (curNode == null) return results;

        if (!curNode.hasIntersection(rect)) return results;

        if (curNode.isLeaf()) {
            if(curNode.point != null  && isPointInRect(curNode.point, rect)) results.add(curNode.point);
            return results;
        }

        results = rRangeSearch(curNode.leftUpNode, rect, results);
        results = rRangeSearch(curNode.leftDownNode, rect, results);
        results = rRangeSearch(curNode.rightUpNode, rect, results);
        results = rRangeSearch(curNode.rightDownNode, rect, results);
        return results;
    }
    boolean isPointInRect(Point p, Rectangle rect) {
        return  p.getX()>=rect.getLeftTop().getX() &&
                p.getX()<=rect.getRightBottom().getX() &&
                p.getY()>=rect.getLeftTop().getY() &&
                p.getY()<=rect.getRightBottom().getY();
    }

    double worstDistInResults;
    public Iterator<Point> nearest(Point source, int maxCount) {
        Map<Point, Double> results = new HashMap<Point, Double>();

        rKNNSearch(root, source, results, maxCount);

        return results.keySet().iterator();
    }
    void rKNNSearch(qNode curNode, Point source, Map<Point, Double> results, int maxCount) {
        if (curNode == null) return;

        if (curNode.isLeaf()) {
            if (curNode.point != null) {
                double dist = source.distance(curNode.point);
                Map.Entry<Point, Double> maxDistAndPoint = getMaxDistancePoint(results);

                if (results.size() < maxCount) {
                    results.put(curNode.point, dist);
                }
                else if (dist < maxDistAndPoint.getValue()) {
                    results.remove(maxDistAndPoint.getKey());
                    results.put(curNode.point, dist);
                }

                maxDistAndPoint = getMaxDistancePoint(results);
                if (results.isEmpty()) worstDistInResults = Double.MAX_VALUE;
                else worstDistInResults = maxDistAndPoint.getValue();
            }
            return;
        }

        boolean[] alreadyCheck  = {false, false, false, false};
        qNode[] childNodes  = {curNode.leftUpNode, curNode.leftDownNode, curNode.rightUpNode, curNode.rightDownNode};
        for (int i=0; i<4; i++) {
            int idx = 0;
            double minDist = Double.MAX_VALUE;
            for (int j=0; j<4; j++) {
                if (alreadyCheck[j]) continue;
                double rectDist = getRectDistance(source, childNodes[j].rect);
                if (minDist > rectDist) {
                    minDist = rectDist;
                    idx = j;
                }
            }

            //탐색하려는 다음 범위와의 거리가 현재 후보 중 최악보다 멈 + 이미 result 꽉 참 = 스킵
            if (results.size() == maxCount && minDist > worstDistInResults) continue;

            rKNNSearch(childNodes[idx], source, results, maxCount);
            alreadyCheck[idx] = true;
        }
    }
    Map.Entry<Point, Double> getMaxDistancePoint(Map<Point, Double> map) {
        double maxDist=-1;
        Point maxPoint=null;
        for (Map.Entry<Point, Double> e : map.entrySet()) {
            if (e.getValue() > maxDist) {
                maxDist = e.getValue();
                maxPoint = e.getKey();
            }
        }
        return new AbstractMap.SimpleEntry<>(maxPoint, maxDist);
    }
    double getRectDistance(Point p, Rectangle rect) {
        double dx = 0;
        double smallCaseX = rect.getLeftTop().getX() - p.getX();
        double bigCaseX = p.getX() - rect.getRightBottom().getX();
        if (smallCaseX > 0) dx = smallCaseX;
        else if (bigCaseX > 0) dx = bigCaseX;

        double dy = 0;
        double smallCaseY = rect.getLeftTop().getY() - p.getY();
        double bigCaseY = p.getY() - rect.getRightBottom().getY();
        if (smallCaseY > 0) dy = smallCaseY;
        else if (bigCaseY > 0) dy = bigCaseY;

        return Math.sqrt(dx*dx + dy*dy);
    }

    public void delete(Point point) {
        root = rDeleteNode(root, point);
    }
    qNode rDeleteNode(qNode curNode, Point p) {
        if (curNode == null) return null;

        if (!curNode.contains(p)) return curNode;

        if(curNode.isLeaf()) {
            if (curNode.point != null
             && curNode.point.getX() == p.getX()
             && curNode.point.getY() == p.getY()) {
                curNode.point = null;
            }
            return curNode;
        }

        curNode.leftUpNode = rDeleteNode(curNode.leftUpNode, p);
        curNode.leftDownNode = rDeleteNode(curNode.leftDownNode, p);
        curNode.rightUpNode = rDeleteNode(curNode.rightUpNode, p);
        curNode.rightDownNode = rDeleteNode(curNode.rightDownNode, p);

        return tryMerge(curNode);
    }
    qNode tryMerge(qNode node) {
        if (!node.leftUpNode.isLeaf() ||
            !node.leftDownNode.isLeaf() ||
            !node.rightUpNode.isLeaf() ||
            !node.rightDownNode.isLeaf()) return node;

        int cnt = 0;
        Point tmp = null;
        if (node.leftUpNode.point != null) {
            tmp=node.leftUpNode.point;
            cnt++;
        }
        if (node.leftDownNode.point != null) {
            tmp=node.leftDownNode.point;
            cnt++;
        }
        if (node.rightUpNode.point != null) {
            tmp=node.rightUpNode.point;
            cnt++;
        }
        if (node.rightDownNode.point != null) {
            tmp=node.rightDownNode.point;
            cnt++;
        }

        if (cnt > 1) return node;

        node.leftUpNode = null;
        node.leftDownNode = null;
        node.rightUpNode = null;
        node.rightDownNode = null;
        node.point = tmp;

        return node;
    }
    public boolean isEmpty() {
        if (root == null) return true;
        if (root.isLeaf() && root.point == null) return true;
        return false;
    }
}
class qNode {
    Point point;
    Rectangle rect;
    Point middlePoint;
    qNode leftUpNode;
    qNode leftDownNode;
    qNode rightUpNode;
    qNode rightDownNode;

    public qNode(Point p, Rectangle r) {
        point = p;
        rect = r;
        middlePoint = new Point((rect.getRightBottom().getX() + rect.getLeftTop().getX())/2.0f, (rect.getRightBottom().getY() + rect.getLeftTop().getY())/2.0f);
        leftUpNode = null;
        leftDownNode = null;
        rightUpNode = null;
        rightDownNode = null;
    }
    public boolean isLeaf() {
        return leftUpNode == null;// && rightDownNode == null && leftDownNode == null && rightUpNode == null;
    }
    public boolean contains(Point p) {
        return  p.getX() >= rect.getLeftTop().getX() &&
                p.getX() <= rect.getRightBottom().getX() &&
                p.getY() >= rect.getLeftTop().getY() &&
                p.getY() <= rect.getRightBottom().getY();
    }
    public void subDivide() {
        Rectangle luRect = new Rectangle(rect.getLeftTop(), middlePoint);
        Rectangle ldRect = new Rectangle(new Point(rect.getLeftTop().getX(), middlePoint.getY()), new Point(middlePoint.getX(), rect.getRightBottom().getY()));
        Rectangle ruRect = new Rectangle(new Point(middlePoint.getX(), rect.getLeftTop().getY()), new Point(rect.getRightBottom().getX(), middlePoint.getY()));
        Rectangle rdRect = new Rectangle(middlePoint, rect.getRightBottom());

        leftUpNode = new qNode(null, luRect);
        leftDownNode = new qNode(null, ldRect);
        rightUpNode = new qNode(null, ruRect);
        rightDownNode = new qNode(null, rdRect);
    }
    public qNode getContainableNode(Point p) {
        if (isLeaf() || p == null) return null;

        if (p.getX() < middlePoint.getX()) {
            if (p.getY() < middlePoint.getY())  return leftUpNode;
            else                                return leftDownNode;
        }
        else {
            if (p.getY() < middlePoint.getY())  return rightUpNode;
            else                                return rightDownNode;
        }
    }
    public boolean hasIntersection(Rectangle r) {
        return !(rect.getLeftTop().getX()       > r.getRightBottom().getX() ||
                 rect.getRightBottom().getX()   < r.getLeftTop().getX() ||
                 rect.getLeftTop().getY()       > r.getRightBottom().getY() ||
                 rect.getRightBottom().getY()   < r.getLeftTop().getY());
    }
}
