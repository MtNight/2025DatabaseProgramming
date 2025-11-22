package org.dfpl.dbp.rtree;

public class Rectangle {


    private Point leftTop;
    private Point rightBottom;

    public Rectangle(Point leftTop, Point rightBottom) {
        this.leftTop = leftTop;
        this.rightBottom = rightBottom;
    }

    public Point getLeftTop() {
        return leftTop;
    }

    public void setLeftTop(Point leftTop) {
        this.leftTop = leftTop;
    }

    public Point getRightBottom() {
        return rightBottom;
    }

    public void setRightBottom(Point rightBottom) {
        this.rightBottom = rightBottom;
    }

    // MBR의 면적 계산
    public double area() {
        double width = rightBottom.getX() - leftTop.getX();
        double height = leftTop.getY() - rightBottom.getY();
        return width * height;
    }

    // 점이 MBR 내부에 포함되는지 확인

    public boolean contains(Point p) {
        return (p.getX() >= leftTop.getX() &&
                p.getX() <= rightBottom.getX() &&
                p.getY() <= leftTop.getY() &&   // y ≤ maxY
                p.getY() >= rightBottom.getY()); // y ≥ minY
    }

    // 두 Rectangle이 서로 겹치는지 확인
    public boolean intersects(Rectangle other) {
        return !(other.rightBottom.getX() < this.leftTop.getX() ||   // 오른쪽 < 왼쪽
                other.leftTop.getX() > this.rightBottom.getX() ||   // 왼쪽 > 오른쪽
                other.leftTop.getY() < this.rightBottom.getY() ||   // 위 < 아래
                other.rightBottom.getY() > this.leftTop.getY());    // 아래 > 위
    }

    // 두 MBR을 모두 포함하는 새로운 MBR 생성

    public Rectangle union(Rectangle other) {
        double minX = Math.min(this.leftTop.getX(), other.leftTop.getX());
        double maxX = Math.max(this.rightBottom.getX(), other.rightBottom.getX());

        double maxY = Math.max(this.leftTop.getY(), other.leftTop.getY());
        double minY = Math.min(this.rightBottom.getY(), other.rightBottom.getY());

        return new Rectangle(new Point(minX, maxY), new Point(maxX, minY));
    }

    // 다른 Rectangle을 포함하도록 확장할 때 증가하는 면적 계산
    // R-Tree의 ChooseLeaf 단계에서 사용
    public double enlargement(Rectangle other) {
        Rectangle expanded = this.union(other);
        return expanded.area() - this.area();
    }

    // 점 하나를 포함하도록 MBR 확장
    public void expandToInclude(Point p) {
        double minX = Math.min(leftTop.getX(), p.getX());
        double maxX = Math.max(rightBottom.getX(), p.getX());

        double maxY = Math.max(leftTop.getY(), p.getY());
        double minY = Math.min(rightBottom.getY(), p.getY());

        this.leftTop = new Point(minX, maxY);
        this.rightBottom = new Point(maxX, minY);
    }

    @Override
    public String toString() {
        return "Rectangle [leftTop=(" + leftTop.getX() + "," + leftTop.getY() +
                "), rightBottom=(" + rightBottom.getX() + "," + rightBottom.getY() + ")]";
    }
}