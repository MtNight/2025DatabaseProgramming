package org.dfpl.dbp.rtree.CompareCases;

import org.dfpl.dbp.rtree.Point;
import org.dfpl.dbp.rtree.Rectangle;

import java.util.Iterator;
import java.util.List;

//main class for testing performance of structures
public class ComparingTest {
    public static void main(String[] args) {
        long startTime, endTime;

        List<Point> pointList = List.of(new Point(20, 30), new Point(25, 25), new Point(30, 40), new Point(35, 20),
                new Point(40, 35), new Point(15, 45), new Point(45, 15), new Point(28, 32), new Point(30, 150),
                new Point(40, 170), new Point(50, 140), new Point(25, 160), new Point(55, 175), new Point(60, 155),
                new Point(45, 135), new Point(38, 145), new Point(160, 60), new Point(170, 70), new Point(155, 80),
                new Point(180, 55), new Point(175, 90), new Point(165, 95), new Point(150, 75), new Point(185, 85),
                new Point(70, 80), new Point(95, 90), new Point(120, 100), new Point(80, 110), new Point(130, 40),
                new Point(100, 65));
        List<Point> pointToRemove = List.of(new Point(20, 30), new Point(25, 25), new Point(30, 40), new Point(35, 20),
                new Point(40, 35), new Point(15, 45), new Point(45, 15), new Point(28, 32), new Point(30, 150),
                new Point(40, 170), new Point(50, 140), new Point(25, 160), new Point(55, 175), new Point(60, 155),
                new Point(45, 135), new Point(38, 145), new Point(160, 60), new Point(170, 70), new Point(155, 80),
                new Point(180, 55), new Point(175, 90), new Point(165, 95), new Point(150, 75), new Point(185, 85),
                new Point(70, 80), new Point(95, 90), new Point(120, 100), new Point(80, 110), new Point(130, 40),
                new Point(100, 65));

        //Case 1: KDTree
        KDTree kdTree = new KDTree();

        // Test For Insert Nodes
        startTime = System.nanoTime();
        for (Point point : pointList) {
            kdTree.add(point);
        }
        endTime = System.nanoTime();
        System.out.println("Time taken For Insert Nodes: " + (endTime - startTime) +"ns");

        // Test For Range Search
        startTime = System.nanoTime();
        Iterator<Point> iterator = kdTree.search(new Rectangle(new Point(0, 0), new Point(100, 100)));
        endTime = System.nanoTime();
        System.out.println("\nTime taken For Range Search: " + (endTime - startTime) +"ns");

        while (iterator!=null && iterator.hasNext()) {
            Point next = iterator.next();
            System.out.println(next);
        }

        // Test For KNN Search
        startTime = System.nanoTime();
        Point source = new Point(75, 85);
        iterator = kdTree.nearest(source, 5);
        endTime = System.nanoTime();
        System.out.println("\nTime taken For KNN Search: " + (endTime - startTime) +"ns");
        while (iterator.hasNext()) {
            Point next = iterator.next();
            System.out.println(next + ":" + source.distance(next));
        }

        // Test For Delete Nodes
        startTime = System.nanoTime();
        for (Point point : pointToRemove) {
            kdTree.delete(point);
        }
        endTime = System.nanoTime();
        System.out.println("\nTime taken For Delete Nodes: " + (endTime - startTime) +"ns");

        System.out.println(kdTree.isEmpty());
    }
}
