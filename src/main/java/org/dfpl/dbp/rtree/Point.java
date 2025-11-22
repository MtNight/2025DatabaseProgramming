package org.dfpl.dbp.rtree;

import java.util.Objects;

public class Point {
    private double x;
    private double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double distance(Point other) {
        double dx = this.x - other.getX();
        double dy = this.y - other.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() {
        return "Point [x=" + x + ", y=" + y + "]";
    }

    // --- R-Tree 로직에 필수적인 추가 부분 ---

    /**
     * 두 Point 객체가 x, y 좌표가 같으면 동일한 것으로 간주하도록 재정의합니다.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        // Double.compare를 사용하여 double 타입 비교의 정확성을 높입니다.
        return Double.compare(point.x, x) == 0 &&
                Double.compare(point.y, y) == 0;
    }

    /**
     * x와 y 좌표를 기반으로 해시 코드를 생성하도록 재정의합니다.
     * (equals가 true인 두 객체는 반드시 같은 hashCode를 반환해야 합니다.)
     */
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}