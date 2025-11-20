package org.dfpl.dbp.rtree.CompareCases;

import org.dfpl.dbp.rtree.Point;
import org.dfpl.dbp.rtree.Rectangle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

//main class for testing performance of structures
public class ComparingTest {

    public static void main(String[] args) {
        List<Point> pointList = List.of(new Point(20, 30), new Point(25, 25), new Point(30, 40), new Point(35, 20),
                new Point(40, 35), new Point(15, 45), new Point(45, 15), new Point(28, 32), new Point(30, 150),
                new Point(40, 170), new Point(50, 140), new Point(25, 160), new Point(55, 175), new Point(60, 155),
                new Point(45, 135), new Point(38, 145), new Point(160, 60), new Point(170, 70), new Point(155, 80),
                new Point(180, 55), new Point(175, 90), new Point(165, 95), new Point(150, 75), new Point(185, 85),
                new Point(70, 80), new Point(95, 90), new Point(120, 100), new Point(80, 110), new Point(130, 40),
                new Point(100, 65));

        int testPointCnt = 100000;
        List<Point> testPoints = new ArrayList<Point>();
        Random random = new Random();
        int warmup=0;
        for(int i=0;i<testPointCnt;i++) {
            testPoints.add(new Point(random.nextDouble(),  random.nextDouble()));
            warmup++;
        }

        TimeStamp kdt = TestKDTree(testPoints);
        TimeStamp qt = TestQuadTree(testPoints);

        System.out.println("insert time - KDTree:\t"+kdt.insertTime+"ns");
        System.out.println("insert time - QuadTree:\t"+qt.insertTime+"ns");
        System.out.println();
        System.out.println("search time - KDTree:\t"+kdt.searchTime+"ns");
        System.out.println("search time - QuadTree:\t"+qt.searchTime+"ns");
        System.out.println();
        System.out.println("nearest time - KDTree:\t"+kdt.nearestTime+"ns");
        System.out.println("nearest time - QuadTree:"+qt.nearestTime+"ns");
        System.out.println();
        System.out.println("delete time - KDTree:\t"+kdt.deleteTime+"ns");
        System.out.println("delete time - QuadTree:\t"+qt.deleteTime+"ns");
        System.out.println();
        System.out.println("whole time - KDTree:\t"+kdt.wholeTime+"ns");
        System.out.println("whole time - QuadTree:\t"+qt.wholeTime+"ns");
    }
    /*
    두 테스트 케이스 테스트 결과
    포인트가 십만개정도 넘어가면 KDTree가 빠름. 그 이하에선 QuadTree가 좋음.

    이론적인 평균 시간복잡도 (최악 시간복잡도는 O(N)으로 동일)
    KDTree
        삽입 - Θ(log N)
        범위 탐색 - Θ(√N + k)
        kNN 탐색 - Θ(log N + k)
        삭제 - Θ(log N)

    QuadTree
        삽입 - Θ(1)
        범위 탐색 - Θ(√N + k)
        kNN 탐색 - Θ(log N + k)
        삭제 - Θ(1)

    KDTree는 밸런싱을 안하다보니 삽입 순서에 영향을 크게 받긴 하나, N이 커질수록 치우침이 줄어들어 실행 시간이 점진적으로만 증가하는 것으로 보임
    QuadTree는 KDTree보다 분할도 더 하는 데다가, 삽입삭제 시 속도가 빠름. 그러나 정확하게 반반으로 나누기 때문에 N이 커질수록 점점 분할이 어려워져서 느려지는 듯?

    */

    static TimeStamp TestKDTree(List<Point> pointList) {
        long startTime, endTime;
        TimeStamp ts = new TimeStamp();
        //Case 1: KDTree
        KDTree kdTree = new KDTree();

        // Test For Insert Nodes
        startTime = System.nanoTime();
        for (Point point : pointList) {
            kdTree.add(point);
        }
        endTime = System.nanoTime();
        ts.insertTime = (endTime - startTime);
        System.out.println("KDTree: Time taken For Insert Nodes: " + ts.insertTime + "ns");

        // Test For Range Search
        startTime = System.nanoTime();
        Iterator<Point> iterator = kdTree.search(new Rectangle(new Point(0, 0), new Point(100, 100)));
        endTime = System.nanoTime();
        ts.searchTime = (endTime - startTime);
        System.out.println("\nKDTree: Time taken For Range Search: " + ts.searchTime + "ns");

        while (iterator != null && iterator.hasNext()) {
            Point next = iterator.next();
            if(pointList.size() < 40) System.out.println(next);
        }

        // Test For KNN Search
        startTime = System.nanoTime();
        Point source = new Point(75, 85);
        iterator = kdTree.nearest(source, 5);
        endTime = System.nanoTime();
        ts.nearestTime = (endTime - startTime);
        System.out.println("\nKDTree: Time taken For KNN Search: " + ts.nearestTime + "ns");

        while (iterator.hasNext()) {
            Point next = iterator.next();
            if(pointList.size() < 40) System.out.println(next + ":" + source.distance(next));
        }

        // Test For Delete Nodes
        startTime = System.nanoTime();
        for (Point point : pointList) {
            kdTree.delete(point);
        }
        endTime = System.nanoTime();
        ts.deleteTime = (endTime - startTime);
        System.out.println("\nKDTree: Time taken For Delete Nodes: " + ts.deleteTime + "ns");

        System.out.println(kdTree.isEmpty());

        ts.calculateWholeTime();
        System.out.println("\nKDTree: Time taken For All Functions: " + ts.wholeTime + "ns");
        System.out.println("\n--------------------------------------------------------------------------------------------------------------\n");

        return ts;
    }
    static TimeStamp TestQuadTree(List<Point> pointList) {
        long startTime, endTime;
        TimeStamp ts = new TimeStamp();
        //Case 2: QuadTree
        QuadTree quadTree = new QuadTree();

        // Test For Insert Nodes
        startTime = System.nanoTime();
        quadTree.checkRange(pointList);
        for (Point point : pointList) {
            quadTree.add(point);
        }
        endTime = System.nanoTime();
        ts.insertTime = (endTime - startTime);
        System.out.println("QuadTree: Time taken For Insert Nodes: " + ts.insertTime +"ns");

        // Test For Range Search
        startTime = System.nanoTime();
        Iterator<Point> iterator = quadTree.search(new Rectangle(new Point(0, 0), new Point(100, 100)));
        endTime = System.nanoTime();
        ts.searchTime = (endTime - startTime);
        System.out.println("\nQuadTree: Time taken For Range Search: " + ts.searchTime +"ns");

        while (iterator!=null && iterator.hasNext()) {
            Point next = iterator.next();
            if(pointList.size() < 40) System.out.println(next);
        }

        // Test For KNN Search
        startTime = System.nanoTime();
        Point source = new Point(75, 85);
        iterator = quadTree.nearest(source, 5);
        endTime = System.nanoTime();
        ts.nearestTime = (endTime - startTime);
        System.out.println("\nQuadTree: Time taken For KNN Search: " + ts.nearestTime +"ns");
        while (iterator.hasNext()) {
            Point next = iterator.next();
            if(pointList.size() < 40) System.out.println(next + ":" + source.distance(next));
        }

        // Test For Delete Nodes
        startTime = System.nanoTime();
        for (Point point : pointList) {
            quadTree.delete(point);
        }
        endTime = System.nanoTime();
        ts.deleteTime = (endTime - startTime);
        System.out.println("\nQuadTree: Time taken For Delete Nodes: " + ts.deleteTime +"ns");

        System.out.println(quadTree.isEmpty());

        ts.calculateWholeTime();
        System.out.println("\nKDTree: Time taken For All Functions: " + ts.wholeTime +"ns");
        System.out.println("\n--------------------------------------------------------------------------------------------------------------\n");

        return ts;
    }
}

class TimeStamp {
    public long insertTime;
    public long searchTime;
    public long nearestTime;
    public long deleteTime;
    public long wholeTime;

    public void calculateWholeTime() {
        wholeTime = insertTime + searchTime + nearestTime + deleteTime;
    }
}
