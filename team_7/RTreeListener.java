package org.dfpl.dbp.rtree.team_7;

import java.util.List;

public interface RTreeListener {

    // 전체 트리가 바뀔 때 (삽입/삭제 후 등)
    void onTreeChanged(
            List<Point> allPoints,
            List<Rectangle> nodeMBRs
    );

    // 범위 검색 한 단계(또는 완료) 상태
    void onSearchStep(
            Rectangle query,
            List<Rectangle> visitedNodes,
            List<Rectangle> prunedNodes,
            List<Point> resultPoints
    );

    // KNN 탐색 한 단계(또는 완료) 상태
    void onKnnStep(
            Point source,
            List<Rectangle> activeNodes,   // 현재 보고 있는/활성 노드(MBR)
            List<Point> candidatePoints,   // 후보 점(파란색)
            List<Point> resultPoints,      // 확정된 KNN 결과(초록색)
            List<Point> removedPoints,     // 우선순위 큐에서 밀려난 점(X 표시)
            int maxCount                   // 목표 개수
    );
}