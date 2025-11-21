package org.dfpl.dbp.rtree;

import java.util.Iterator;

public class RTreeImpl implements RTree {

    // 요건 4-way R-Tree로 구현한다.
    // Maven Project로 만든다.
    // 기존의 R-Tree를 활용하지 않는다.
    // 여러분의 프로젝트에는 최소한의 dependency가 포함되어 있어야 함.
    // 멤버 변수의 활용은 어느정도 자유로움
    // 단, R-Tree 구현이어야 하고, 요행을 바라지 않는다.


    // R-Tree의 루트 노드
    private RTreeNode root;

    public RTreeImpl() {
        // R-tree는 최소 1개의 리프 노드로 시작해야 한다.
        this.root = RTreeNode.createLeaf();
    }


    @Override
    public void add(Point point) {

        // oot가 null일 가능성 방어
        if (root == null) {
            root = RTreeNode.createLeaf();
        }

        // 1. 삽입할 leaf 선택
        RTreeNode leaf = chooseLeaf(root, point);

        // 2. 중복 점이면 무시
        for (Point p : leaf.points) {
            if (p.getX() == point.getX() && p.getY() == point.getY()) {
                return;
            }
        }

        // 3. 삽입
        leaf.points.add(point);

        // 4. 리프노드 MBR 갱신
        leaf.updateMBR();

        // 5. 부모 방향으로 MBR 업데이트
        adjustMBR(leaf);
    }

    // leaf 탐색 함수
    private RTreeNode chooseLeaf(RTreeNode node, Point p) {

        if (node == null) return RTreeNode.createLeaf();

        // 1. 리프노드면 반환
        if (node.isLeaf) return node;

        // 2. 내부 노드 → child 중 확장량 가장 작게 드는 것 선택
        RTreeNode best = null;
        double bestExpand = Double.MAX_VALUE;

        Rectangle pRect = new Rectangle(p, p);

        for (RTreeNode child : node.children) {
            double expand = child.mbr.enlargement(pRect);

            if (expand < bestExpand) {
                bestExpand = expand;
                best = child;
            }
        }

        return chooseLeaf(best, p);
    }

    // 부모 방향 MBR 재계산
    private void adjustMBR(RTreeNode node) {
        while (node != null) {
            node.updateMBR();
            node = node.parent;
        }
    }


    // @Override
    public Iterator<Point> search(Rectangle rect) {
        throw new UnsupportedOperationException("search() not implemented");
    }


    @Override
    public Iterator<Point> nearest(Point source, int maxCount) {
        throw new UnsupportedOperationException("nearest() not implemented");
    }


    @Override
    public void delete(Point point) {
        if (root == null) return;

        RTreeNode leaf = findLeaf(root, point);
        if (leaf == null) return;

        leaf.points.removeIf(p ->
                p.getX() == point.getX() &&
                        p.getY() == point.getY()
        );

        leaf.updateMBR();
        adjustMBR(leaf);

        //root가 리프고 비어 있으면 트리 초기화
        if (root.isLeaf && root.points.isEmpty()) {
            root = RTreeNode.createLeaf();
        }
    }

    // leaf 찾기
    private RTreeNode findLeaf(RTreeNode node, Point target) {
        if (node == null) return null;

        // leaf라면 points에서 검색
        if (node.isLeaf) {
            for (Point p : node.points) {
                if (p.getX() == target.getX() && p.getY() == target.getY()) {
                    return node;
                }
            }
            return null;
        }

        // 내부노드면 child의 MBR이 target을 포함하는 것만 탐색
        for (RTreeNode child : node.children) {
            if (child.mbr.contains(target)) {
                RTreeNode found = findLeaf(child, target);
                if (found != null) return found;
            }
        }

        return null;
    }

    // -------------------------------------------------------------------
    @Override
    public boolean isEmpty() {
        return root.isLeaf && root.points.isEmpty();
    }
}
