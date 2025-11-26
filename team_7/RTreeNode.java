package org.dfpl.dbp.rtree.team_7;

import java.util.ArrayList;
import java.util.List;

public class RTreeNode {

    // true면 리프 노드, false면 내부 노드
    boolean isLeaf;

    // 리프 노드에서 사용하는 필드
    List<Point> points;

    // 내부 노드에서 사용하는 필드
    List<RTreeNode> children;

    // 이 노드를 대표하는 MBR (최소 경계 사각형)
    Rectangle mbr;

    // 부모 노드를 가리키는 포인터
    RTreeNode parent;

    // 최대 차수 (4-way R-tree → 한 노드에 최대 4개의 엔트리)
    public static final int MAX = 4;

    // 리프 노드 생성
    public static RTreeNode createLeaf() {
        RTreeNode node = new RTreeNode();
        node.isLeaf = true;
        node.points = new ArrayList<>();
        node.children = null;
        node.mbr = null;   // 아직 포인트가 없으므로 MBR은 null
        node.parent = null;
        return node;
    }

    // 내부 노드 생성 메소드
    public static RTreeNode createInternal() {
        RTreeNode node = new RTreeNode();
        node.isLeaf = false;
        node.children = new ArrayList<>();
        node.points = null;
        node.mbr = null;
        node.parent = null;
        return node;
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

        double minX = points.get(0).getX();
        double maxX = points.get(0).getX();
        double minY = points.get(0).getY();
        double maxY = points.get(0).getY();

        for (Point p : points) {
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
        }

        // leftTop = (minX, minY), rightBottom = (minX, minY)
        mbr = new Rectangle(
                new Point(minX, minY),
                new Point(maxX, maxY)
        );
    }

    // 내부 노드 MBR 계산 (자식 MBR을 모두 포함하는 MBR)
    private void updateInternalMBR() {
        if (children == null || children.isEmpty()) {
            mbr = null;
            return;
        }

        // 자식 중 첫 번째의 MBR을 기준으로 시작
        Rectangle first = null;
        for (RTreeNode child : children) {
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
        double minY = first.getLeftTop().getY();
        double maxY = first.getRightBottom().getY();

        for (RTreeNode child : children) {
            if (child.mbr == null) continue;
            Rectangle r = child.mbr;

            minX = Math.min(minX, r.getLeftTop().getX());
            maxX = Math.max(maxX, r.getRightBottom().getX());
            minY = Math.min(minY, r.getLeftTop().getY());
            maxY = Math.max(maxY, r.getRightBottom().getY());
        }

        mbr = new Rectangle(
                new Point(minX, minY),
                new Point(maxX, maxY)
        );
    }
}