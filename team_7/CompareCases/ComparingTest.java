package org.dfpl.dbp.rtree.team_7.CompareCases;

import org.dfpl.dbp.rtree.team_7.Point;
import org.dfpl.dbp.rtree.team_7.RTree;
import org.dfpl.dbp.rtree.team_7.RTreeImpl;
import org.dfpl.dbp.rtree.team_7.Rectangle;

import java.util.*;

//main class for testing performance of structures
public class ComparingTest {

    //test values
    static Rectangle createRange;
    static Rectangle deleteRange;
    static Rectangle searchTestRect = new Rectangle(new Point(0, 0), new Point(5, 5));
    static Point searchTestPoint = new Point(95, 95);
    static int kTest = 5000;

    public static void main(String[] args) {
        //기본 제공된 포인트들
        List<Point> pointList = List.of(new Point(20, 30), new Point(25, 25), new Point(30, 40), new Point(35, 20),
                new Point(40, 35), new Point(15, 45), new Point(45, 15), new Point(28, 32), new Point(30, 150),
                new Point(40, 170), new Point(50, 140), new Point(25, 160), new Point(55, 175), new Point(60, 155),
                new Point(45, 135), new Point(38, 145), new Point(160, 60), new Point(170, 70), new Point(155, 80),
                new Point(180, 55), new Point(175, 90), new Point(165, 95), new Point(150, 75), new Point(185, 85),
                new Point(70, 80), new Point(95, 90), new Point(120, 100), new Point(80, 110), new Point(130, 40),
                new Point(100, 65));

        int createPointCnt = 1000;
        int deletePointCnt = 1000;

        createRange = new Rectangle(new Point(0, 0), new Point(200, 200));    // 범위에 대한 조건이 없어서 과제 코드의 예제 포인트를 참조해서 기본적으로 200*200내의 점만 생성. 변경 가능
        double w = createRange.getRightBottom().getX() - createRange.getLeftTop().getX();
        double h = createRange.getRightBottom().getY() - createRange.getLeftTop().getY();

        deleteRange = createRange;
        deleteRange = new Rectangle(new Point(10, 10), new Point(20, 20));

        List<Point> inputPoints = new ArrayList<>();
        List<Point> deletePoints = new ArrayList<>();

        //랜덤 생성
        Random random = new Random();
        for (int i = 0; i < createPointCnt; i++) {
            Point randomPoint = new Point(createRange.getLeftTop().getX() + random.nextDouble() * w, createRange.getLeftTop().getY() + random.nextDouble() * h);
            inputPoints.add(randomPoint);
            //if(i%(createPointCnt/deletePointCnt)==0) deletePoints.add(randomPoint);   //전체 삭제가 아닌 전역 랜덤 삭제인 경우 테스트
            if (deleteRange.contains(randomPoint)) deletePoints.add(randomPoint);        //전체 삭제가 아닌 지역 삭제인 경우 테스트
        }
        //분포 불균형 추가용
        Rectangle additionalRange = new Rectangle(new Point(0, 20), new Point(20, 50));
        for (int i = 0; i < createPointCnt / 10; i++) {
            Point randomPoint = new Point(additionalRange.getLeftTop().getX() + random.nextDouble() * w, additionalRange.getLeftTop().getY() + random.nextDouble() * h);
            inputPoints.add(randomPoint);
            //if(i%(createPointCnt/deletePointCnt)==0) deletePoints.add(randomPoint);   //전체 삭제가 아닌 전역 랜덤 삭제인 경우 테스트
            if (deleteRange.contains(randomPoint)) deletePoints.add(randomPoint);        //전체 삭제가 아닌 지역 삭제인 경우 테스트
        }
        /*
        //균등 생성
        for(int i=0;i<h;i++) {
            for(int j=0;j<w;j++) {
                Point uniformPoint = new Point(createRange.getLeftTop().getX() + j,  createRange.getLeftTop().getY() + i);
                inputPoints.add(uniformPoint);
            }
        }
        deletePoints = inputPoints;
        */
        //printCompareResults(pointList, pointList);
        printCompareResults(inputPoints, deletePoints);
    }

    /*
    성능 테스트 내용 정리
    Insert
        - 대부분의 경우 RTree의 성능이 좋지 않음. 트리 조정에 의한 오버헤드가 많음
        - ArrayList가 보통 가장 빠른 편. -> 보통 전체 데이터를 다 도는 경우 리스트 구조가 성능이 좋아보임.
    Range Search
        - RTree가 적당히 좋은 성능을 내는 경우가 많음.
        - 특히 포인트 개수가 많지 않거나 탐색범위가 좁을 수록 성능이 좋음.
        - 그렇지 않고 포인트들이 균등 분포할 경우에는 포인트 개수가 늘어나면 성능이 그렇게 좋지는 않음
        - 극단적인 형태의 범위를 줄 경우 RTree와 KDTree는 성능이 괜찮으나 QuadTree는 성능이 많이 나빠짐.
    kNN Search
        - k가 작을 때는 RTree의 성능이 매우 떨어짐.
        - 그러나 k가 매우 큰 경우 다른 트리는 성능이 급격히 떨어지는 데 반해 성능 하락폭이 적음.
        - 참고로 리스트들은 탐색에 대해 k에 영향을 안받고 포인트 개수만 영향 받아서, k가 많이 커지면 오히려 트리보다 리스트들이 성능이 좋음.
    Delete
        - 대부분의 경우 RTree의 성능이 좋지 않음. 역시 트리 조정하는 오버헤드가 큰 것 같음.
        - 단순한 구조라서 트리 조정을 거의 안하는 다른 두 트리가 성능이 보통 좋은 것 같음. 얘들도 트리 조정 기능 넣으면 느려질 듯.
        - 다른 트리들이 랜덤 분포 데이터에 대해서 성능이 좋은 것도 이유 중 하나로 보임.

    과제에 성능 평가는 탐색으로 하라고 했으니 삽입삭제 성능 안좋은 건 간단히 언급만 해도 상관 없을 것 같음
    지금 상태로도 발표자료 제작을 위한 데이터를 뽑을 수는 있을 것 같은데 더 예쁘게 뽑을 수 없나 고민 해봐야됨
    */
    static void printCompareResults(List<Point> testInput, List<Point> testDelete) {
        ArrayList<Point> arrayList = new ArrayList<>();
        LinkedList<Point> linkedList = new LinkedList<>();

        TimeStamp rt = TestRTree(testInput, testDelete);
        TimeStamp arl = TestList(arrayList, testInput, testDelete);
        TimeStamp lkl = TestList(linkedList, testInput, testDelete);
        TimeStamp kdt = TestKDTree(testInput, testDelete);
        TimeStamp qt = TestQuadTree(testInput, testDelete);

        System.out.println("Test Results:\n" +
                "\ttestInput: " + testInput.size() + "\n" +
                "\ttestDelete: " + testDelete.size() + "\n" +
                "\tcreateRange: \t\t\t" + createRange.toString() + "\n" +
                "\trange search range: \t" + searchTestRect.toString() + "\n" +
                "\tnearest search source: \t" + searchTestPoint.toString() + "\n" +
                "\tnearest search count: " + kTest + "\n" +
                "\tdeleteRange: \t\t\t" + deleteRange.toString());

        System.out.println();
        System.out.println("insert time - RTree:\t\t" + rt.insertTime + "ns");
        System.out.println("insert time - ArrayList:\t" + arl.insertTime + "ns");
        System.out.println("insert time - LinkedList:\t" + lkl.insertTime + "ns");
        System.out.println("insert time - KDTree:\t\t" + kdt.insertTime + "ns");
        System.out.println("insert time - QuadTree:\t\t" + qt.insertTime + "ns");
        System.out.println();
        System.out.println("search time - RTree:\t\t" + rt.searchTime + "ns");
        System.out.println("search time - ArrayList:\t" + arl.searchTime + "ns");
        System.out.println("search time - LinkedList:\t" + lkl.searchTime + "ns");
        System.out.println("search time - KDTree:\t\t" + kdt.searchTime + "ns");
        System.out.println("search time - QuadTree:\t\t" + qt.searchTime + "ns");
        System.out.println();
        System.out.println("nearest time - RTree:\t\t" + rt.nearestTime + "ns");
        System.out.println("nearest time - ArrayList:\t" + arl.nearestTime + "ns");
        System.out.println("nearest time - LinkedList:\t" + lkl.nearestTime + "ns");
        System.out.println("nearest time - KDTree:\t\t" + kdt.nearestTime + "ns");
        System.out.println("nearest time - QuadTree:\t" + qt.nearestTime + "ns");
        System.out.println();
        System.out.println("delete time - RTree:\t\t" + rt.deleteTime + "ns");
        System.out.println("delete time - ArrayList:\t" + arl.deleteTime + "ns");
        System.out.println("delete time - LinkedList:\t" + lkl.deleteTime + "ns");
        System.out.println("delete time - KDTree:\t\t" + kdt.deleteTime + "ns");
        System.out.println("delete time - QuadTree:\t\t" + qt.deleteTime + "ns");
        System.out.println();
        System.out.println("whole time - RTree:\t\t\t" + rt.wholeTime + "ns");
        System.out.println("whole time - ArrayList:\t\t" + arl.wholeTime + "ns");
        System.out.println("whole time - LinkedList:\t" + lkl.wholeTime + "ns");
        System.out.println("whole time - KDTree:\t\t" + kdt.wholeTime + "ns");
        System.out.println("whole time - QuadTree:\t\t" + qt.wholeTime + "ns");
        System.out.println("\n--------------------------------------------------------------------------------------------------------------\n");
    }

    // 각 기능들을 테스트하는 코드들.
    // 이거 상속 이용해서 한번에 묶어서 할 수도 있을 것 같은데... RTree코드가 어떻게 만들어질지 모르겠고, 이미 주어져있는 RTree interface를 건드려야 할 수도 있는 게 부담스러워서 그냥 각각 따로 구현함
    static TimeStamp TestRTree(List<Point> inputList, List<Point> deleteList) {
        long startTime, endTime;
        TimeStamp ts = new TimeStamp();
        //Main Case: RTree
        // 시각화/딜레이 없는 RTreeImpl 사용 (성능 측정용)
        RTree rTree = new RTreeImpl(false);

        // Test For Insert Nodes
        startTime = System.nanoTime();
        for (Point point : inputList) {
            rTree.add(point);
        }
        endTime = System.nanoTime();
        ts.insertTime = (endTime - startTime);
        System.out.println("RTree: Time taken For Insert Nodes: " + ts.insertTime + "ns");

        // Test For Range Search
        startTime = System.nanoTime();
        Iterator<Point> iterator = rTree.search(searchTestRect);
        endTime = System.nanoTime();
        ts.searchTime = (endTime - startTime);
        System.out.println("RTree: Time taken For Range Search: " + ts.searchTime + "ns");

        while (iterator != null && iterator.hasNext()) {
            Point next = iterator.next();
            if (inputList.size() < 40) System.out.println("Rtree Search " + next);
        }

        // Test For KNN Search
        startTime = System.nanoTime();
        iterator = rTree.nearest(searchTestPoint, kTest);
        endTime = System.nanoTime();
        ts.nearestTime = (endTime - startTime);
        System.out.println("RTree: Time taken For KNN Search: " + ts.nearestTime + "ns");

        while (iterator.hasNext()) {
            Point next = iterator.next();
            if (inputList.size() < 40) System.out.println("Rtree KNN " + next + ":" + searchTestPoint.distance(next));
        }

        // Test For Delete Nodes
        startTime = System.nanoTime();
        for (Point point : inputList) {   // 기존 로직 그대로 유지
            rTree.delete(point);
        }
        endTime = System.nanoTime();
        ts.deleteTime = (endTime - startTime);
        System.out.println("RTree: Time taken For Delete Nodes: " + ts.deleteTime + "ns");

        System.out.println(rTree.isEmpty());

        ts.calculateWholeTime();
        System.out.println("RTree: Time taken For All Functions: " + ts.wholeTime + "ns");
        System.out.println("\n--------------------------------------------------------------------------------------------------------------\n");

        return ts;
    }

    static TimeStamp TestList(List<Point> list, List<Point> inputList, List<Point> deleteList) {
        long startTime, endTime;
        TimeStamp ts = new TimeStamp();
        //Case 0: List

        // Test For Insert Nodes - 그냥 복사해도 되긴 하지만 성능 비교를 위해 그냥 이렇게 넣음
        startTime = System.nanoTime();
        for (Point point : inputList) {
            list.add(point);
        }
        endTime = System.nanoTime();
        ts.insertTime = (endTime - startTime);
        //System.out.println("List: Time taken For Insert Nodes: " + ts.insertTime + "ns");

        // Test For Range Search
        startTime = System.nanoTime();
        Rectangle rect = searchTestRect;
        ArrayList<Point> inRect = new ArrayList<Point>();
        for (Point p : inputList) {
            if (rect.getLeftTop().getX() <= p.getX() && rect.getLeftTop().getY() <= p.getY() &&
                    rect.getRightBottom().getX() >= p.getX() && rect.getRightBottom().getY() >= p.getY()) {
                inRect.add(p);
            }
        }
        Iterator<Point> iterator = inRect.iterator();
        endTime = System.nanoTime();
        ts.searchTime = (endTime - startTime);
        //System.out.println("\nList: Time taken For Range Search: " + ts.searchTime + "ns");

        while (iterator != null && iterator.hasNext()) {
            Point next = iterator.next();
            //if(pointList.size() < 40) System.out.println(next);
        }

        // Test For KNN Search
        startTime = System.nanoTime();
        PriorityQueue<Point> pq = new PriorityQueue<>(kTest, (p1, p2) -> Double.compare(p2.distance(searchTestPoint), p1.distance(searchTestPoint)));
        for (Point p : inputList) {
            pq.offer(p);
            if (pq.size() > kTest) pq.poll();
        }
        iterator = pq.iterator();
        endTime = System.nanoTime();
        ts.nearestTime = (endTime - startTime);
        //System.out.println("\nList: Time taken For KNN Search: " + ts.nearestTime + "ns");

        while (iterator.hasNext()) {
            Point next = iterator.next();
            //if(pointList.size() < 40) System.out.println(next + ":" + searchTestPoint.distance(next));
        }

        // Test For Delete Nodes
        startTime = System.nanoTime();
        for (Point point : deleteList) {
            list.remove(point);
        }
        endTime = System.nanoTime();
        ts.deleteTime = (endTime - startTime);
        //System.out.println("\nList: Time taken For Delete Nodes: " + ts.deleteTime + "ns");

        //System.out.println(list.isEmpty());

        ts.calculateWholeTime();
        //System.out.println("\nList: Time taken For All Functions: " + ts.wholeTime + "ns");
        //System.out.println("\n--------------------------------------------------------------------------------------------------------------\n");

        return ts;
    }

    static TimeStamp TestKDTree(List<Point> inputList, List<Point> deleteList) {
        long startTime, endTime;
        TimeStamp ts = new TimeStamp();
        //Case 1: KDTree
        KDTree kdTree = new KDTree();

        // Test For Insert Nodes
        startTime = System.nanoTime();
        for (Point point : inputList) {
            kdTree.add(point);
        }
        endTime = System.nanoTime();
        ts.insertTime = (endTime - startTime);
        System.out.println("KDTree: Time taken For Insert Nodes: " + ts.insertTime + "ns");

        // Test For Range Search
        startTime = System.nanoTime();
        Iterator<Point> iterator = kdTree.search(searchTestRect);
        endTime = System.nanoTime();
        ts.searchTime = (endTime - startTime);
        System.out.println("KDTree: Time taken For Range Search: " + ts.searchTime + "ns");

        while (iterator != null && iterator.hasNext()) {
            Point next = iterator.next();
            if (inputList.size() < 40) System.out.println("KDtree Search " + next);
        }

        // Test For KNN Search
        startTime = System.nanoTime();
        iterator = kdTree.nearest(searchTestPoint, kTest);
        endTime = System.nanoTime();
        ts.nearestTime = (endTime - startTime);
        System.out.println("KDTree: Time taken For KNN Search: " + ts.nearestTime + "ns");

        while (iterator.hasNext()) {
            Point next = iterator.next();
            if (inputList.size() < 40) System.out.println("KDtree KNN " + next + ":" + searchTestPoint.distance(next));
        }

        // Test For Delete Nodes
        startTime = System.nanoTime();
        for (Point point : deleteList) {
            kdTree.delete(point);
        }
        endTime = System.nanoTime();
        ts.deleteTime = (endTime - startTime);
        System.out.println("KDTree: Time taken For Delete Nodes: " + ts.deleteTime + "ns");

        System.out.println(kdTree.isEmpty());

        ts.calculateWholeTime();
        System.out.println("KDTree: Time taken For All Functions: " + ts.wholeTime + "ns");
        System.out.println("\n--------------------------------------------------------------------------------------------------------------\n");

        return ts;
    }

    static TimeStamp TestQuadTree(List<Point> inputList, List<Point> deleteList) {
        long startTime, endTime;
        TimeStamp ts = new TimeStamp();
        //Case 2: QuadTree
        QuadTree quadTree = new QuadTree();

        // Test For Insert Nodes
        startTime = System.nanoTime();
        quadTree.checkRange(inputList);
        for (Point point : inputList) {
            quadTree.add(point);
        }
        endTime = System.nanoTime();
        ts.insertTime = (endTime - startTime);
        System.out.println("QuadTree: Time taken For Insert Nodes: " + ts.insertTime + "ns");

        // Test For Range Search
        startTime = System.nanoTime();
        Iterator<Point> iterator = quadTree.search(searchTestRect);
        endTime = System.nanoTime();
        ts.searchTime = (endTime - startTime);
        System.out.println("QuadTree: Time taken For Range Search: " + ts.searchTime + "ns");

        while (iterator != null && iterator.hasNext()) {
            Point next = iterator.next();
            if (inputList.size() < 40) System.out.println("Quadtree Search " + next);
        }

        // Test For KNN Search
        startTime = System.nanoTime();
        iterator = quadTree.nearest(searchTestPoint, kTest);
        endTime = System.nanoTime();
        ts.nearestTime = (endTime - startTime);
        System.out.println("QuadTree: Time taken For KNN Search: " + ts.nearestTime + "ns");
        while (iterator.hasNext()) {
            Point next = iterator.next();
            if (inputList.size() < 40)
                System.out.println("Quadtree KNN " + next + ":" + searchTestPoint.distance(next));
        }

        // Test For Delete Nodes
        startTime = System.nanoTime();
        for (Point point : deleteList) {
            quadTree.delete(point);
        }
        endTime = System.nanoTime();
        ts.deleteTime = (endTime - startTime);
        System.out.println("QuadTree: Time taken For Delete Nodes: " + ts.deleteTime + "ns");

        System.out.println(quadTree.isEmpty());

        ts.calculateWholeTime();
        System.out.println("QuadTree: Time taken For All Functions: " + ts.wholeTime + "ns");
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