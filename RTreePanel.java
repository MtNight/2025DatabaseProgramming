package org.dfpl.dbp.rtree;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RTreePanel extends JPanel implements RTreeListener {

    private List<Point> allPoints = new ArrayList<>();
    private List<Rectangle> nodeMBRs = new ArrayList<>();

    private Rectangle queryRect = null;
    private List<Rectangle> visitedNodes = new ArrayList<>();
    private List<Rectangle> prunedNodes = new ArrayList<>();
    private List<Point> resultPoints = new ArrayList<>();

    private Point knnSource = null;
    private List<Point> knnCandidates = new ArrayList<>();

    // 좌표 → 화면 좌표 변환용 (간단 스케일)
    private double scale = 3.0; // 값은 나중에 조절
    private int margin = 20;

    public RTreePanel() {
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(1.3f));

        // 1) 전체 노드 MBR
        g2.setColor(Color.LIGHT_GRAY);
        for (Rectangle r : nodeMBRs) {
            drawRect(g2, r);
        }

        // 2) 검색에서 방문한 노드 / 가지친 노드 강조
        g2.setColor(new Color(0, 150, 255, 120)); // 방문 노드
        for (Rectangle r : visitedNodes) {
            drawRect(g2, r);
        }
        g2.setColor(new Color(255, 0, 0, 120));   // 가지친 노드
        for (Rectangle r : prunedNodes) {
            drawRect(g2, r);
        }

        // 3) 범위 검색 박스
        if (queryRect != null) {
            g2.setColor(new Color(0, 255, 0, 80));
            drawRect(g2, queryRect);
        }

        // 4) 전체 포인트
        g2.setColor(Color.BLACK);
        for (Point p : allPoints) {
            drawPoint(g2, p, 4);
        }

        // 5) 검색 결과 포인트
        g2.setColor(Color.BLUE);
        for (Point p : resultPoints) {
            drawPoint(g2, p, 6);
        }

        // 6) KNN 소스 + 후보
        if (knnSource != null) {
            g2.setColor(Color.RED);
            drawPoint(g2, knnSource, 8);
            g2.drawString("source", toScreenX(knnSource.getX()) + 5, toScreenY(knnSource.getY()) - 5);
        }
        g2.setColor(new Color(255, 128, 0));
        for (Point p : knnCandidates) {
            drawPoint(g2, p, 6);
        }
    }

    private int toScreenX(double x) {
        return (int) (margin + x * scale);
    }

    private int toScreenY(double y) {
        // y가 클수록 아래로 내려가게 반전
        return (int) (getHeight() - margin - y * scale);
    }

    private void drawPoint(Graphics2D g2, Point p, int r) {
        int sx = toScreenX(p.getX());
        int sy = toScreenY(p.getY());
        g2.fillOval(sx - r, sy - r, r * 2, r * 2);
    }

    private void drawRect(Graphics2D g2, Rectangle rect) {
        Point lt = rect.getLeftTop();
        Point rb = rect.getRightBottom();

        int x = toScreenX(lt.getX());
        int y = toScreenY(lt.getY());
        int x2 = toScreenX(rb.getX());
        int y2 = toScreenY(rb.getY());

        int w = x2 - x;
        int h = y2 - y;
        if (w < 0) { x += w; w = -w; }
        if (h < 0) { y += h; h = -h; }

        g2.drawRect(x, y, w, h);
    }

    /* -------- RTreeListener 구현 -------- */

    @Override
    public void onTreeChanged(List<Point> allPoints, List<Rectangle> nodeMBRs) {
        this.allPoints = new ArrayList<>(allPoints);
        this.nodeMBRs = new ArrayList<>(nodeMBRs);
        // 검색 관련 정보는 초기화
        this.queryRect = null;
        this.visitedNodes.clear();
        this.prunedNodes.clear();
        this.resultPoints.clear();
        this.knnSource = null;
        this.knnCandidates.clear();
        repaint();
    }

    @Override
    public void onSearchStep(Rectangle query, List<Rectangle> visited, List<Rectangle> pruned, List<Point> results) {
        this.queryRect = query;
        this.visitedNodes = new ArrayList<>(visited);
        this.prunedNodes = new ArrayList<>(pruned);
        this.resultPoints = new ArrayList<>(results);
        repaint();
    }

    @Override
    public void onKnnStep(Point source, List<Rectangle> activeNodes, List<Point> candidates) {
        this.knnSource = source;
        this.nodeMBRs = new ArrayList<>(activeNodes); // 필요에 따라
        this.knnCandidates = new ArrayList<>(candidates);
        repaint();
    }
}