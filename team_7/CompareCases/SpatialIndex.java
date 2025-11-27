package org.dfpl.dbp.rtree.team_7.CompareCases;

import org.dfpl.dbp.rtree.team_7.Point;
import org.dfpl.dbp.rtree.team_7.Rectangle;

import java.util.Iterator;

/**
 * RTree, KDTree, QuadTree 등을 공통으로 다루기 위한 공간 인덱스 인터페이스.
 * 각 구조체는 이 인터페이스를 구현해서 add / search / nearest / delete / isEmpty 를 제공한다.
 */
public interface SpatialIndex {

    // 포인트 하나 삽입
    void add(Point point);

    // 범위 검색
    Iterator<Point> search(Rectangle rectangle);

    // KNN 검색
    Iterator<Point> nearest(Point source, int maxCount);

    // 포인트 삭제
    void delete(Point point);

    // 비어있는지 여부
    boolean isEmpty();
}