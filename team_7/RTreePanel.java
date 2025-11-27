package org.dfpl.dbp.rtree.team_7;

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

    private List<Rectangle> knnActiveNodes = new ArrayList<>();
    private List<Point> knnResultPoints = new ArrayList<>();
    private List<Point> knnRemovedPoints = new ArrayList<>();
    private int knnMaxCount = 0;

    private Point knnSource = null;
    private List<Point> knnCandidates = new ArrayList<>();


    // 좌표 → 화면 좌표 변환용 (간단 스케일)
    private double scale = 3.0; // 값은 나중에 조절
    private int margin = 20;

    private JLabel logLabel;

    public RTreePanel() {
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        logLabel = new JLabel("Ready", SwingConstants.CENTER);
        logLabel.setPreferredSize(new Dimension(800, 50));
        logLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logLabel.setVerticalAlignment(SwingConstants.CENTER);
        logLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));

        add(logLabel, BorderLayout.NORTH);
    }

    public void setLogText(String text) {
        logLabel.setText(text);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(800, 800);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);


        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(3f));

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
            // X 표시
            Point lt = r.getLeftTop();
            Point rb = r.getRightBottom();
            int x1 = toScreenX(lt.getX());
            int y1 = toScreenY(lt.getY());
            int x2 = toScreenX(rb.getX());
            int y2 = toScreenY(rb.getY());
            g2.drawLine(x1, y1, x2, y2);
            g2.drawLine(x1, y2, x2, y1);
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

        // 6) KNN 결과 포인트 (녹색)
        g2.setColor(Color.GREEN.darker());
        for (Point p : knnResultPoints) {
            drawPoint(g2, p, 7);
        }

        // 7) KNN 후보 포인트 (주황색)
        g2.setColor(new Color(255, 128, 0));
        for (Point p : knnCandidates) {
            drawPoint(g2, p, 6);
        }

        // 8) 제거된 포인트(X 표시, 빨간색)
        g2.setColor(Color.RED);
        for (Point p : knnRemovedPoints) {
            drawX(g2, p, 8);
        }


        // 6) KNN 소스 + 후보
        if (knnSource != null) {
            // 기준점(빨간색)
            g2.setColor(Color.RED);
            drawPoint(g2, knnSource, 8);
            g2.setFont(g2.getFont().deriveFont(14f));
            g2.drawString("source",
                    toScreenX(knnSource.getX()) + 5,
                    toScreenY(knnSource.getY()) - 5);
        }

        // 활성 노드(MBR) – 파란 사각형
        g2.setColor(new Color(0, 0, 255, 80));
        for (Rectangle r : knnActiveNodes) {
            drawRect(g2, r);
        }

        // 후보 점 – 파란 점
        g2.setColor(Color.BLUE);
        for (Point p : knnCandidates) {
            drawPoint(g2, p, 6);
        }

        // 확정된 KNN 결과 – 초록 점
        g2.setColor(Color.GREEN.darker());
        for (Point p : knnResultPoints) {
            drawPoint(g2, p, 7);
        }

        // 제거된 점 – 빨간 X 표시
        g2.setColor(Color.RED);
        for (Point p : knnRemovedPoints) {
            int sx = toScreenX(p.getX());
            int sy = toScreenY(p.getY());
            int r = 6;
            g2.drawLine(sx - r, sy - r, sx + r, sy + r);
            g2.drawLine(sx - r, sy + r, sx + r, sy - r);
        }

        if (knnMaxCount > 0 && knnSource != null) {
            // KNN 상태 텍스트 (위쪽에 표시) - "found" 대신 "현재 후보" 느낌으로
            String text = "KNN 탐색 중: 현재 후보 "
                    + knnResultPoints.size() + " / " + knnMaxCount
                    + "개 (MBR 거리 기반 가지치기)";
            setLogText(text);
        }
    }



    private void drawX(Graphics2D g2, Point p, int r) {
        int sx = toScreenX(p.getX());
        int sy = toScreenY(p.getY());
        g2.drawLine(sx - r, sy - r, sx + r, sy + r);
        g2.drawLine(sx - r, sy + r, sx + r, sy - r);
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

        // KNN 관련 정보도 초기화
        this.knnSource = null;
        this.knnCandidates.clear();
        this.knnActiveNodes.clear();
        this.knnResultPoints.clear();
        this.knnRemovedPoints.clear();
        this.knnMaxCount = 0;

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
    public void onKnnStep(Point source,
                          List<Rectangle> activeNodes,
                          List<Point> candidates,
                          List<Point> resultPoints,
                          List<Point> removedPoints,
                          int maxCount) {
        this.knnSource = source;
        this.knnActiveNodes = new ArrayList<>(activeNodes);
        this.knnCandidates = new ArrayList<>(candidates);
        this.knnResultPoints = new ArrayList<>(resultPoints);
        this.knnRemovedPoints = new ArrayList<>(removedPoints);
        this.knnMaxCount = maxCount;
        repaint();
    }
}