package org.dfpl.dbp.rtree;

public class Rectangle {

	// 좌상단 포인트와 우하단 포인트로 표현
    
    // 수정: 좌상단을 (minX, maxY), 우하단을 (maxX, minY) 형태(일반적인 수학 좌표계)를 가정하고 써있던 코드인데
    // 다른 코드와 통일성 등을 위해 왼쪽위를 최소점으로 생각하는 스크린 좌표로 변경했습니다. 그에 맞춰서 RTreeImpl도 살짝 수정

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
        double height = rightBottom.getY() - leftTop.getY();
        return width * height;
    }

    // 점이 MBR 내부에 포함되는지 확인
    public boolean contains(Point p) {
        return (p.getX() >= leftTop.getX() &&
                p.getX() <= rightBottom.getX() &&
                p.getY() >= leftTop.getY() &&   // y ≤ minY
                p.getY() <= rightBottom.getY()); // y ≥ maxY
    }

    // 두 Rectangle이 서로 겹치는지 확인
    public boolean intersects(Rectangle other) {
        return !(other.rightBottom.getX() < this.leftTop.getX() ||   // 오른쪽 < 왼쪽
                other.leftTop.getX() > this.rightBottom.getX() ||   // 왼쪽 > 오른쪽
                other.rightBottom.getY() < this.leftTop.getY() ||   // 위 < 아래
                other.leftTop.getY() > this.rightBottom.getY());    // 아래 > 위
    }

    // 두 MBR을 모두 포함하는 새로운 MBR 생성
    public Rectangle union(Rectangle other) {
        double minX = Math.min(this.leftTop.getX(), other.leftTop.getX());
        double maxX = Math.max(this.rightBottom.getX(), other.rightBottom.getX());

        double minY = Math.min(this.leftTop.getY(), other.leftTop.getY());
        double maxY = Math.max(this.rightBottom.getY(), other.rightBottom.getY());

        return new Rectangle(new Point(minX, minY), new Point(maxX, maxY));
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

        double minY = Math.min(leftTop.getY(), p.getY());
        double maxY = Math.max(rightBottom.getY(), p.getY());

        this.leftTop = new Point(minX, minY);
        this.rightBottom = new Point(maxX, maxY);
    }

    @Override
    public String toString() {
        return "Rectangle [leftTop=(" + leftTop.getX() + "," + leftTop.getY() +
                "), rightBottom=(" + rightBottom.getX() + "," + rightBottom.getY() + ")]";
    }
}
