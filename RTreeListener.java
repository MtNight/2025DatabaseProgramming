package org.dfpl.dbp.rtree;

public interface RTreeListener {

    // 전체 트리가 바뀔 때 (삽입/삭제 후 등)
    void onTreeChanged(
            java.util.List<Point> allPoints,
            java.util.List<Rectangle> nodeMBRs
    );

    // 범위 검색 한 단계(또는 완료) 상태
    void onSearchStep(
            Rectangle query,
            java.util.List<Rectangle> visitedNodes,
            java.util.List<Rectangle> prunedNodes,
            java.util.List<Point> resultPoints
    );

    // KNN 탐색 한 단계(또는 완료) 상태
    void onKnnStep(
            Point source,
            java.util.List<Rectangle> activeNodes,
            java.util.List<Point> candidatePoints
    );
}