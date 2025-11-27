package org.dfpl.dbp.rtree.team_7;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class RTreeImpl implements RTree {

    // 4-way R-Tree의 최소 엔트리 수 (MIN)는 MAX의 절반인 2
    public static final int MIN = RTreeNode.MAX / 2;

    // R-Tree의 루트 노드
    private RTreeNode root;

    // 재삽입을 위해 수집된 Point 객체를 임시로 담는 리스트
    private List<Point> reinsertPoints = new ArrayList<>();

    // 시각화용 리스너
    private RTreeListener listener;   // RTreePanel 을 붙여야함

    // 시각화 on/off 플래그
    private final boolean visualizationEnabled;

    // RTreeImpl 기본 생성자 → 시각화 켜진 모드 (Assignment45에서 사용)
    public RTreeImpl() {
        this(true);
    }

    // RTreeImpl 생성자 → 시각화 on/off 선택 가능 (ComparingTest 등에서 사용)
    public RTreeImpl(boolean visualizationEnabled) {
        this.visualizationEnabled = visualizationEnabled;
        // R-tree는 최소 1개의 리프 노드로 시작해야 한다.
        this.root = RTreeNode.createLeaf();
        initVisualizer();
    }

    private void initVisualizer() {
        if (!visualizationEnabled) return;   // 시각화 끈 모드에서는 GUI 안 띄움

        RTreePanel panel = new RTreePanel();
        this.listener = panel;

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("4-way R-Tree Visualizer (RTreeImpl)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);

            frame.setSize(800, 900);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private void log(String msg) {
        if (!visualizationEnabled) return;   // 벤치마크 모드에서는 무시
        if (listener instanceof RTreePanel) {
            ((RTreePanel) listener).setLogText(msg);
        }
    }

    @Override
    public void add(Point point) {
        insert(point, false);   // ← 일반 추가: 로그 O
    }

    // 실제 삽입 로직은 여기로 모음
    private void insert(Point point, boolean fromReinsert) {

        // root가 null일 가능성 방어
        if (root == null) {
            root = RTreeNode.createLeaf();
        }

        if (point == null) return;

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

        // 6. 용량 초과 시 분할(split)
        if (leaf.points.size() > RTreeNode.MAX) {
            splitLeaf(leaf);
        }

        // 시각화 갱신
        notifyTreeChanged();

        // 재삽입이 아닐 때만 "ADD" 로그를 찍는다
        if (!fromReinsert) {
            log("ADD: " + point);
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
            if (child.mbr == null) continue;

            double expand = child.mbr.enlargement(pRect);

            if (expand < bestExpand) {
                bestExpand = expand;
                best = child;
            }
        }

        if (best != null) {
            return chooseLeaf(best, p);
        } else if (!node.children.isEmpty() && node.children.get(0).mbr != null) {
            return chooseLeaf(node.children.get(0), p);
        } else {
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

            if (enlargeOld <= enlargeNew) {
                leaf.points.add(p);
            } else {
                newLeaf.points.add(p);
            }
        }

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
                if (children.get(i).mbr == null || children.get(j).mbr == null) continue;

                double diff = Math.abs(children.get(i).mbr.area() - children.get(j).mbr.area());
                if (diff > maxDiff) {
                    maxDiff = diff;
                    seed1 = children.get(i);
                    seed2 = children.get(j);
                }
            }
        }

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

            if (enlarge1 <= enlarge2) {
                group1.children.add(c);
                c.parent = group1;
            } else {
                group2.children.add(c);
                c.parent = group2;
            }
        }

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

    @Override
    public Iterator<Point> search(Rectangle rectangle) {        //반환값이 이터레이터
        // TODO 탐색함수 구현
        //  1. 여기서 포인터 리스트 리턴
        //  2. 만약 루트가 없거나, 루트가 리프노드인데 포인트가 없다면 빈이터레이터 리턴
        //  3. 점 구하는 함수 부르고, 돌아오면 해당하는 리스트 리턴
        if (root == null || (root.isLeaf && root.points.isEmpty())) return Collections.emptyIterator();

        List<Point> result = new ArrayList<>();
        // ★ 시각화를 위한 방문 / 가지치기 리스트
        List<Rectangle> visited = new ArrayList<>();
        List<Rectangle> pruned = new ArrayList<>();

        // 재귀 탐색 + 단계별 시각화
        searchPoints(root, rectangle, result, visited, pruned);

        // ★ 최종 결과 상태를 잠깐 유지
        sleepQuiet(700);   // 0.7~1초 정도 유지 (취향대로 수정 가능)

        // ★ search 관련 오버레이만 초기화 (트리/포인트는 유지)
        notifySearchStep(
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        return result.iterator();
    }

    //TODO 재귀 탐색 함수 구현
// 1. 받아온 사각형 rectangle에 해당 노드의 mbr이 겹치는 지 확인 및 가지치기
// 2. 내부노드면 rectangle에 children의 mbr이 겹치는 지 확인 후 재귀 수행
// 3. 리프노드면 points의 포인트가 사각형에 들어오는지 확인 후 결과값 추가
    private void searchPoints(RTreeNode node,
                              Rectangle rectangle,
                              List<Point> result,
                              List<Rectangle> visited,
                              List<Rectangle> pruned) {

        // node 자체가 가지치기 되는 경우
        if (node.mbr == null || !rectangle.intersects(node.mbr)) {
            if (node.mbr != null) {
                pruned.add(node.mbr);
                visualizeSearchStep(rectangle, visited, pruned, result);
            }
            return;
        }

        // 여기까지 왔으면 이 노드는 "방문"된 것
        visited.add(node.mbr);
        visualizeSearchStep(rectangle, visited, pruned, result);

        // 내부노드라면
        if (!node.isLeaf) {
            for (RTreeNode child : node.children) {
                if (child == null || child.mbr == null) continue;

                if (rectangle.intersects(child.mbr)) {
                    // child.mbr이 검색 범위와 겹치면 재귀 내려감
                    searchPoints(child, rectangle, result, visited, pruned);
                } else {
                    //TODO:(Swing) child.mbr 이 가지치기 되는 경우
                    pruned.add(child.mbr);
                    visualizeSearchStep(rectangle, visited, pruned, result);
                }
            }
            return;
        }

        // 리프노드라면 - 실제 포인트 확인
        for (Point p : node.points) {
            if (rectangle.contains(p)) {   //points의 포인트들이 사각형에 들어오는지 확인
                result.add(p);  //있으면 결과값에 추가
                //TODO:(Swing) 발견한 노드 녹색 표시 point는 p
                visualizeSearchStep(rectangle, visited, pruned, result);
            }
        }
    }

    // ====================== 시각화 공통 Helper ======================
    private void sleepQuiet(int ms) {
        if (!visualizationEnabled) return;   // 벤치마크 모드에서는 sleep 안 함
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }

    private void visualizeSearchStep(Rectangle query,
                                     List<Rectangle> visited,
                                     List<Rectangle> pruned,
                                     List<Point> results) {
        notifySearchStep(
                query,
                new ArrayList<>(visited),
                new ArrayList<>(pruned),
                new ArrayList<>(results)
        );
        sleepQuiet(700);
    }

    //아래는 nearest에 포함되는 함수
    // 후보 객체 (노드 + 그 노드까지의 최소 거리)
    private static class Candidate {
        RTreeNode node;
        double minDist; // find에서 이 MBR까지의 최소 거리

        Candidate(RTreeNode minNode, double minDist) {
            this.node = minNode;
            this.minDist = minDist;
        }
    }

    // 점에서 사각형까지의 최소 거리 (0이면 겹치거나 안에 있음)
    private double minDistToRectangle(Point p, Rectangle rect) {
        double px = p.getX();
        double py = p.getY();

        double rx1 = rect.getLeftTop().getX();
        double ry1 = rect.getLeftTop().getY();
        double rx2 = rect.getRightBottom().getX();
        double ry2 = rect.getRightBottom().getY();

        //점이 사각형 밖에 있다면 가장 가까운 경계까지의 거리를 구한다
        //사각형 안에있으면 0
        double dx = Math.max(0, Math.max(rx1 - px, px - rx2));
        double dy = Math.max(0, Math.max(ry1 - py, py - ry2));

        return Math.sqrt(dx * dx + dy * dy);	//거리 반환
    }

    //최근접 이웃 알고리즘(KNN)으로 탐색
    @Override
    public Iterator<Point> nearest(Point source, int maxCount) {
        // TODO 가장 가까운 점 탐색
        // 1. 루트노드의 모든 엔트리를 후보로 넣은 후
        // 2. 가장 가까운 루트 mbr탐색
        // 3. 점을 발견할 시 result로 넣어주고, result중 가장 먼 점과의 거리가 기준
        // 4. 넣어준 mbr이 기준보다 먼 mbr이면 탐색하지 않음.
        // 5. 모든 루트 mbr탐색
        if (source == null || root == null || (root.isLeaf && root.points.isEmpty()) || maxCount <= 0) {
            return Collections.emptyIterator();
        }

        // 결과 저장용 우선순위 큐
        PriorityQueue<Point> result = new PriorityQueue<>(maxCount,
                (p1, p2) -> Double.compare(p2.distance(source), p1.distance(source))); // 내림차순
        // 탐색 중 후보 관리하는 우선순위 큐
        PriorityQueue<Candidate> candidates = new PriorityQueue<>(Comparator.comparingDouble(c -> c.minDist));

        // KNN 시각화를 위한 리스트
        List<Rectangle> activeNodes = new ArrayList<>();
        List<Point> candidatePointsVis = new ArrayList<>();
        List<Point> removedPointsVis = new ArrayList<>();

        // 루트 노드를 후보로 넣고 시작
        candidates.offer(new Candidate(root, minDistToRectangle(source, root.mbr)));

        //TODO: KNN탐색 시작:(Swing)  기준점 source(이건 표시되니 괜찮.), 찾을 개수 maxCount + 현재 찾은 개수 0(result:size()) 띄워주기
        activeNodes.clear();
        if (root.mbr != null) activeNodes.add(root.mbr);
        candidatePointsVis.clear();
        removedPointsVis.clear();
        notifyKnnStep(source, activeNodes, candidatePointsVis,
                new ArrayList<>(result), removedPointsVis, maxCount);
        sleepQuiet(700);

        while (!candidates.isEmpty()) {
            Candidate cand = candidates.poll();
            double currentCandidateDist = cand.minDist;

            // 현재 후보 노드를 활성노드로 표시
            activeNodes.clear();
            if (cand.node.mbr != null) {
                activeNodes.add(cand.node.mbr);
            }

            // 현재 후보의 minDist가 result의 가장 먼 점(peek)보다 크면 pruning
            if (result.size() == maxCount && currentCandidateDist > result.peek().distance(source)) {
                //TODO:(Swing) 결과값이 5개 다차있고, 최대거리보다 후보 mbr의 거리가 더 멀면 해당 mbr 부분 X표시 (가지치기)
                //cand.node.points를 x표시 하시면 될거에요!
                candidatePointsVis.clear();
                if (cand.node.isLeaf && cand.node.points != null) {
                    candidatePointsVis.addAll(cand.node.points);
                }

                notifyKnnStep(source, activeNodes, candidatePointsVis,
                        new ArrayList<>(result), removedPointsVis, maxCount);
                sleepQuiet(700);
                break;
            }

            if (cand.node.isLeaf) { // 리프: 실제 점 발견
                //TODO:(Swing) 후보 노드들 파란색으로 표시
                // cand.node.points
                candidatePointsVis.clear();
                if (cand.node.points != null) {
                    candidatePointsVis.addAll(cand.node.points);
                }
                notifyKnnStep(source, activeNodes, candidatePointsVis,
                        new ArrayList<>(result), removedPointsVis, maxCount);
                sleepQuiet(700);

                for (Point p : cand.node.points) {
                    result.offer(p);
                    if (result.size() > maxCount) {
                        Point removed = result.poll(); // 가장 먼 것 제거 → 항상 maxCount개 유지
                        //TODO:(Swing) removed(포인트)를 녹색점에서 X표시로 변경
                        removedPointsVis.add(removed);
                    }
                    //TODO:(Swing)발견한 노드 녹색 표시 point는 p
                    // → result PQ에 들어간 것들은 resultSnapshot으로 넘겨서 녹색으로 그림
                    List<Point> resultSnapshot = new ArrayList<>(result);
                    notifyKnnStep(source, activeNodes, candidatePointsVis,
                            resultSnapshot, removedPointsVis, maxCount);
                    sleepQuiet(700);
                }
                //TODO:(Swing) 파란색이었던 후보노드들 녹색으로 변경
                // → candidatePointsVis를 비우고, result만 남게 함
                candidatePointsVis.clear();
                notifyKnnStep(source, activeNodes, candidatePointsVis,
                        new ArrayList<>(result), removedPointsVis, maxCount);
                sleepQuiet(700);

            } else { // 내부 노드: 자식들 추가
                for (RTreeNode child : cand.node.children) {
                    if (child == null || child.mbr == null) continue;
                    double childMinDist = minDistToRectangle(source, child.mbr);
                    candidates.offer(new Candidate(child, childMinDist));
                }
                // 내부 노드 방문 상태도 잠깐 보여줌
                candidatePointsVis.clear();
                notifyKnnStep(source, activeNodes, candidatePointsVis,
                        new ArrayList<>(result), removedPointsVis, maxCount);
                sleepQuiet(700);
            }
        }

        // 결과는 가까운 순으로 정렬된 상태로 반환
        List<Point> sortedResult = new ArrayList<>(result);
        sortedResult.sort(Comparator.comparingDouble(p -> p.distance(source)));

        //TODO:(Swing) 끝나면 지금까지 값들 좌표 반환? 할까요? 모르겠네
        // → 최종 결과를 한 번 더 보여주고 종료
        activeNodes.clear();
        candidatePointsVis.clear();
        notifyKnnStep(source, activeNodes, candidatePointsVis,
                sortedResult, removedPointsVis, maxCount);
        sleepQuiet(2000);

        // 최종 상태 잠깐 보여준 뒤, KNN 오버레이 전부 초기화
        notifyKnnStep(
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                0
        );

        // KNN 탐색 완료 로그 (시각화 모드에서만 label로 보임)
        log("KNN 탐색 완료: 최종 이웃 " + sortedResult.size() + "개");

        return sortedResult.iterator();
    }

    // ======================================================================
    //  Deletion Logic (Condense Tree 및 재삽입 포함)
    @Override
    public void delete(Point point) {
        if (root == null) return;

        RTreeNode leaf = findLeaf(root, point);
        if (leaf == null) return;

        // Point의 equals()를 사용하여 요소 제거
        leaf.points.removeIf(p ->
                p.getX() == point.getX() &&
                        p.getY() == point.getY()
        );

        // 1. 노드 재조정 (Condense Tree) 로직 시작
        condenseTree(leaf);

        // 2. 루트 노드 정리
        if (!root.isLeaf && root.children.size() == 1) {
            RTreeNode newRoot = root.children.get(0);
            newRoot.parent = null;
            root = newRoot;
        } else if (!root.isLeaf && root.children.isEmpty() ||
                root.isLeaf && root.points.isEmpty()) {
            root = RTreeNode.createLeaf();
        }

        log("DELETE: " + point);


        notifyTreeChanged();
    }

    /**
     * @brief 삭제 후 노드 재조정 (Condense Tree) 로직
     */
    private void condenseTree(RTreeNode node) {
        RTreeNode n = node;

        while (n != null && n != root) {
            RTreeNode parent = n.parent;

            if (parent == null) break;

            // 1. 최소 엔트리 조건 검사
            if ((n.isLeaf && n.points.size() < MIN) ||
                    (!n.isLeaf && n.children.size() < MIN)) {

                // 최소 조건을 위반하면 부모로부터 제거
                parent.children.remove(n);

                // === 재삽입을 위한 엔트리 수집 ===
                if (n.isLeaf) {
                    reinsertPoints.addAll(n.points); // 리프 노드의 Point 수집
                    n.points.clear();
                } else {
                    // 내부 노드의 자식들(MBR)도 재삽입해야 하나, 과제 단순화를 위해 무시
                }
            }

            // 2. MBR 갱신
            parent.updateMBR();

            n = parent;
        }

        if (root != null) {
            root.updateMBR();
        }

        // === 재삽입 실행 ===
        performReinsertion();
    }

    /**
     * @brief CondenseTree에서 수집된 Point 엔트리들을 트리에 다시 삽입합니다.
     */
    private void performReinsertion() {
        if (reinsertPoints.isEmpty()) return;

        List<Point> pointsToReinsert = new ArrayList<>(reinsertPoints);
        reinsertPoints.clear();

        for (Point p : pointsToReinsert) {
            add(p); // add 함수를 재사용하여 적절한 위치에 재삽입
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
        // 루트가 null인 경우를 대비하여 방어 코드 추가
        if (root == null) return true;

        return root.isLeaf && root.points.isEmpty();
    }

    // ====================== 시각화 Helper (RTreeNode 기반) ======================

    // 현재 트리의 모든 Point를 수집
    private void collectPoints(RTreeNode node, List<Point> out) {
        if (node == null) return;
        if (node.isLeaf) {
            out.addAll(node.points);
        } else {
            if (node.children == null) return;
            for (RTreeNode child : node.children) {
                collectPoints(child, out);
            }
        }
    }

    // 트리 전체의 mbr들을 모아서 리턴
    private void collectNodeMBRs(RTreeNode node, List<Rectangle> out) {
        if (node == null) return;

        if (node.mbr != null) {
            out.add(node.mbr);
        }

        // ★ leaf면 children이 null이므로 더 내려가지 않는다.
        if (node.isLeaf || node.children == null) {
            return;
        }

        for (RTreeNode child : node.children) {
            collectNodeMBRs(child, out);
        }
    }

    // add/delete 이후 전체 트리 그림 갱신
    private void notifyTreeChanged() {
        if (!visualizationEnabled || listener == null) return;

        List<Point> points = new ArrayList<>();
        collectPoints(root, points);

        if (root != null) {
            root.updateMBR();
        }
        List<Rectangle> mbrs = new ArrayList<>();
        collectNodeMBRs(root, mbrs);

        sleepQuiet(700);

        listener.onTreeChanged(points, mbrs);
    }

    // search 과정 후 한 번에 뷰에 전달
    private void notifySearchStep(Rectangle query, List<Rectangle> visited, List<Rectangle> pruned, List<Point> results) {
        if (!visualizationEnabled || listener == null) return;
        listener.onSearchStep(query, visited, pruned, results);
    }

    // KNN 검색 이후 결과를 뷰에 전달
    private void notifyKnnStep(
            Point source,
            List<Rectangle> activeNodes,
            List<Point> candidatePoints,
            List<Point> resultPoints,      // 확정된 KNN 점들(녹색)
            List<Point> removedPoints,     // 제거된 점들(X)
            int maxCount                   // 목표 개수
    ) {
        if (!visualizationEnabled || listener == null) return;
        listener.onKnnStep(source, activeNodes, candidatePoints, resultPoints, removedPoints, maxCount);
    }
}