package org.dfpl.dbp.rtree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class RTreeImpl implements RTree {

    // 4-way R-Tree의 최소 엔트리 수 (MIN)를 RTreeNode 클래스 대신 RTreeImpl에 정의 (MAX의 절반으로 가정)
    public static final int MIN = RTreeNode.MAX / 2;

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
            // Point의 equals()가 오버라이드되었다고 가정하고, 기존 로직 유지
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

        // 6. 용량 초과 시 분할(split)
        if (leaf.points.size() > RTreeNode.MAX) {
            splitLeaf(leaf);
        }
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
            // child.mbr이 null인 경우 방어 로직 추가
            if (child.mbr == null) continue;

            double expand = child.mbr.enlargement(pRect);

            if (expand < bestExpand) {
                bestExpand = expand;
                best = child;
            }
        }

        // best가 null이 아닐 때만 재귀 호출
        if (best != null) {
            return chooseLeaf(best, p);
        } else {
            // 예외 상황 처리: 자식은 있는데 MBR이 모두 null인 경우 (발생해서는 안됨)
            // 임시로 첫 번째 자식 선택
            if (!node.children.isEmpty() && node.children.get(0).mbr != null) {
                return chooseLeaf(node.children.get(0), p);
            }
            // 최종적으로 리프 노드를 찾지 못한 경우, 임시로 리프 생성 (매우 위험한 동작)
            return RTreeNode.createLeaf();
        }
    }

    // 부모 방향 MBR 재계산
    private void adjustMBR(RTreeNode node) {
        while (node != null) {
            node.updateMBR();
            node = node.parent;
        }
    }

    // ======================================================================
    //  Leaf Node Split (4-way, Linear split)
    private void splitLeaf(RTreeNode leaf) {

        // 기존 포인트들 복사
        List<Point> pts = new ArrayList<>(leaf.points);
        leaf.points.clear();

        // 1. 가장 멀리 떨어진 두 점을 seed로 선택
        Point seed1 = null;
        Point seed2 = null;
        double maxDist = -1;

        for (int i = 0; i < pts.size(); i++) {
            for (int j = i + 1; j < pts.size(); j++) {
                double d = pts.get(i).distance(pts.get(j));
                if (d > maxDist) {
                    maxDist = d;
                    seed1 = pts.get(i);
                    seed2 = pts.get(j);
                }
            }
        }

        // seed1, seed2가 null일 가능성 방지 (points.size() < 2인 경우는 split이 호출되면 안됨)
        if (seed1 == null || seed2 == null) return;

        // 새 리프 노드 생성
        RTreeNode newLeaf = RTreeNode.createLeaf();
        newLeaf.parent = leaf.parent;

        // seed 배치
        leaf.points.add(seed1);
        newLeaf.points.add(seed2);

        // seed만 있을 때의 MBR 계산
        leaf.updateMBR();
        newLeaf.updateMBR();

        // 2. 나머지 포인트 배치
        for (Point p : pts) {
            if (p == seed1 || p == seed2) continue;

            double enlargeOld = enlargementAfterInsert(leaf, p);
            double enlargeNew = enlargementAfterInsert(newLeaf, p);

            // 동률 시 기존 노드(leaf)에 삽입 (기존 로직 유지)
            if (enlargeOld <= enlargeNew) {
                leaf.points.add(p);
                // leaf.updateMBR(); // MBR 업데이트는 모든 할당 후 일괄적으로 하는 것이 좋습니다.
            } else {
                newLeaf.points.add(p);
                // newLeaf.updateMBR();
            }
        }

        // 최종 MBR 업데이트
        leaf.updateMBR();
        newLeaf.updateMBR();

        // 3. 부모 갱신
        adjustParentAfterSplit(leaf, newLeaf);
    }

    // leaf에 점 하나 더 넣었을 때 면적 증가량 계산
    private double enlargementAfterInsert(RTreeNode leaf, Point p) {
        Rectangle pRect = new Rectangle(p, p);
        if (leaf.mbr == null) {
            return pRect.area();
        }
        Rectangle newMBR = leaf.mbr.union(pRect);
        return newMBR.area() - leaf.mbr.area();
    }

    // ======================================================================
    //  Internal Node Split
    private void splitInternal(RTreeNode node) {

        List<RTreeNode> children = new ArrayList<>(node.children);
        node.children.clear();

        // 1. seed 두 개 선택 (MBR 면적 차이가 가장 큰 두 자식)
        RTreeNode seed1 = null;
        RTreeNode seed2 = null;
        double maxDiff = -1;

        for (int i = 0; i < children.size(); i++) {
            for (int j = i + 1; j < children.size(); j++) {
                // MBR이 null이면 계산 불가
                if (children.get(i).mbr == null || children.get(j).mbr == null) continue;

                double diff = Math.abs(children.get(i).mbr.area() - children.get(j).mbr.area());
                if (diff > maxDiff) {
                    maxDiff = diff;
                    seed1 = children.get(i);
                    seed2 = children.get(j);
                }
            }
        }

        // seed1, seed2가 null일 가능성 방지 (children.size() < 2인 경우는 split이 호출되면 안됨)
        if (seed1 == null || seed2 == null) return;

        RTreeNode group1 = node;                 // 기존 노드가 그룹1
        RTreeNode group2 = RTreeNode.createInternal();
        group2.parent = node.parent;

        group1.children.add(seed1);
        group2.children.add(seed2);
        seed1.parent = group1;
        seed2.parent = group2;

        group1.updateMBR();
        group2.updateMBR();

        // 2. 나머지 자식들 배치
        for (RTreeNode c : children) {
            if (c == seed1 || c == seed2) continue;

            double enlarge1 = enlargementAfterInsert(group1, c.mbr);
            double enlarge2 = enlargementAfterInsert(group2, c.mbr);

            // 동률 시 기존 노드(group1)에 삽입 (기존 로직 유지)
            if (enlarge1 <= enlarge2) {
                group1.children.add(c);
                c.parent = group1;
                // group1.updateMBR();
            } else {
                group2.children.add(c);
                c.parent = group2;
                // group2.updateMBR();
            }
        }

        // 최종 MBR 업데이트
        group1.updateMBR();
        group2.updateMBR();

        // 3. 부모 갱신
        adjustParentAfterSplit(group1, group2);
    }

    // 내부 노드에 Rectangle 하나 더 포함시킬 때 증가 면적
    private double enlargementAfterInsert(RTreeNode node, Rectangle r) {
        if (node.mbr == null) {
            return r.area();
        }
        Rectangle newMBR = node.mbr.union(r);
        return newMBR.area() - node.mbr.area();
    }

    // leaf/internal split 후 부모 처리 (root split 포함)
    private void adjustParentAfterSplit(RTreeNode n1, RTreeNode n2) {

        RTreeNode parent = n1.parent;

        // root 분할인 경우
        if (parent == null) {
            RTreeNode newRoot = RTreeNode.createInternal();
            newRoot.children.add(n1);
            newRoot.children.add(n2);
            n1.parent = newRoot;
            n2.parent = newRoot;
            newRoot.updateMBR();
            root = newRoot;
            return;
        }

        // 기존 parent에는 n1이 이미 있으므로, n2만 추가
        parent.children.add(n2);
        n2.parent = parent;

        // parent도 overflow면 다시 split
        if (parent.children.size() > RTreeNode.MAX) {
            splitInternal(parent);
        } else {
            adjustMBR(parent);
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

    // ======================================================================
    //  Deletion Logic (수정됨)
    @Override
    public void delete(Point point) {
        if (root == null) return;

        RTreeNode leaf = findLeaf(root, point);
        if (leaf == null) return;

        // Point의 equals()를 사용하도록 List.remove()를 사용하는 것이 더 정확하나,
        // 기존 로직을 유지하면서 Point.equals()가 동일한 좌표에 대해 true를 반환한다고 가정합니다.
        // leaf.points.remove(point); // <== 권장되는 방식
        leaf.points.removeIf(p ->
                p.getX() == point.getX() &&
                        p.getY() == point.getY()
        );

        // 1. 노드 재조정 (Condense Tree) 로직 시작
        condenseTree(leaf);

        // 2. 루트 노드 정리 (Condense Tree 로직의 일부)
        if (!root.isLeaf && root.children.size() == 1) {
            // 루트가 내부 노드인데 자식이 하나만 남은 경우, 그 자식을 새 루트로 승격
            RTreeNode newRoot = root.children.get(0);
            newRoot.parent = null;
            root = newRoot;
        } else if (root.isLeaf && root.points.isEmpty()) {
            // 루트가 리프이고 비어 있으면 트리 초기화
            root = RTreeNode.createLeaf();
        }
    }

    /**
     * @brief 삭제 후 노드 재조정 (Condense Tree) 로직 구현
     * MIN 엔트리 조건을 위반한 노드를 제거하고 MBR을 연쇄적으로 갱신합니다.
     * (재삽입 로직은 생략하고 단순 제거/정리만 구현했습니다.)
     */
    private void condenseTree(RTreeNode node) {
        RTreeNode n = node;

        // Stack<RTreeNode> reinsertList = new Stack<>(); // 재삽입 리스트 (선택적)

        while (n != null && n != root) {
            RTreeNode parent = n.parent;

            if (parent == null) break; // 혹시 모를 방어

            // 1. 최소 엔트리 조건 검사
            if ((n.isLeaf && n.points.size() < MIN) ||
                    (!n.isLeaf && n.children.size() < MIN)) {

                // 최소 조건을 위반하면 부모로부터 제거
                parent.children.remove(n);
                // TODO: 제거된 노드의 엔트리/자식들을 reinsertList에 추가하는 로직 필요

            }

            // 2. MBR 갱신
            // (노드가 제거되었거나, 엔트리가 줄었거나)
            parent.updateMBR();

            n = parent;
        }

        // 루트의 MBR 최종 갱신
        if (root != null) {
            root.updateMBR();
        }

        // TODO: reinsertList의 엔트리/자식들을 R-Tree에 다시 삽입하는 로직 추가 필요
    }

    // leaf 찾기
    private RTreeNode findLeaf(RTreeNode node, Point target) {
        if (node == null) return null;

        // leaf라면 points에서 검색
        if (node.isLeaf) {
            for (Point p : node.points) {
                // Point의 equals()가 오버라이드되었다고 가정하고, 기존 로직 유지
                if (p.getX() == target.getX() && p.getY() == target.getY()) {
                    return node;
                }
            }
            return null;
        }

        // 내부노드면 child의 MBR이 target을 포함하는 것만 탐색
        for (RTreeNode child : node.children) {
            // MBR이 null인 경우 방지
            if (child.mbr != null && child.mbr.contains(target)) {
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