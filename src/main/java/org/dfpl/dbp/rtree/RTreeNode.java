package org.dfpl.dbp.rtree;

import java.util.ArrayList;
import java.util.List;

public class RTreeNode {

    // 리프 여부 (true = 리프 노드, false = 내부 노드)
    boolean isLeaf;

    // 리프 노드가 갖는 데이터
    List<Point> points;

    // 내부 노드가 갖는 자식 노드
    List<RTreeNode> children;

    // 이 노드를 대표하는 최소 경계 사각형(MBR: Minimum Bounding Rectangle)
    Rectangle mbr;

    // 부모 노드를 가리키는 포인터
    RTreeNode parent;

    // 최대 차수 (4-way R-tree → 최대 4개 엔트리)
    public static final int MAX = 4;

    // 최소 엔트리 수 (delete 시 필요)
    public static final int MIN = 2;

    // 기본 생성자
    public RTreeNode() {
        this.parent = null;
        this.mbr = null;
    }

    // 리프 노드 생성
    public static RTreeNode createLeaf() {
        RTreeNode node = new RTreeNode();
        node.isLeaf = true;
        node.points = new ArrayList<>();
        node.children = null;
        return node;
    }

    // 내부 노드 생성
    public static RTreeNode createInternal() {
        RTreeNode node = new RTreeNode();
        node.isLeaf = false;
        node.children = new ArrayList<>();
        node.points = null;
        return node;
    }

    // 리프 노드에 포인트 추가
    public void addPoint(Point p) {
        points.add(p);
        updateMBR();
    }

    // 내부 노드에 자식 추가
    public void addChild(RTreeNode child) {
        children.add(child);
        child.parent = this;
        updateMBR();
    }

    // 현재 노드의 MBR을 다시 계산
    public void updateMBR() {
        if (isLeaf) {
            updateLeafMBR();
        } else {
            updateInternalMBR();
        }
    }

    // 리프 노드 MBR 계산
    private void updateLeafMBR() {
        if (points == null || points.isEmpty()) {
            mbr = null;
            return;
        }

        double minX = points.get(0).getX(); //point의 첫 점 기준
        double maxX = points.get(0).getX();
        double minY = points.get(0).getY();
        double maxY = points.get(0).getY();

        for (Point p : points) {     //모든 점 돌며 min찾기
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
        }

        mbr = new Rectangle(
                new Point(minX, maxY),     // leftTop
                new Point(maxX, minY)      // rightBottom
        );
    }

    // 내부 노드 MBR 계산 (자식들의 MBR을 모두 포함하는 최소 경계 사각형)
    private void updateInternalMBR() {
        if (children == null || children.isEmpty()) {
            mbr = null;
            return;
        }

        // 첫 번째 유효한 child MBR 찾기
        Rectangle first = null;
        for (RTreeNode child : children) {   //MBR이 없는 자식
            if (child.mbr != null) {
                first = child.mbr;
                break;
            }
        }

        if (first == null) {
            mbr = null;
            return;
        }

        double minX = first.getLeftTop().getX();
        double maxX = first.getRightBottom().getX();
        double minY = first.getRightBottom().getY();
        double maxY = first.getLeftTop().getY();

        for (RTreeNode child : children) {
            if (child.mbr == null) continue;

            Rectangle r = child.mbr;
            minX = Math.min(minX, r.getLeftTop().getX());
            maxX = Math.max(maxX, r.getRightBottom().getX());
            minY = Math.min(minY, r.getRightBottom().getY());
            maxY = Math.max(maxY, r.getLeftTop().getY());
        }

        mbr = new Rectangle(
                new Point(minX, maxY),
                new Point(maxX, minY)
        );
    }
}
