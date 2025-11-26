package org.dfpl.dbp.rtree.team_7;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RTreePanel extends JPanel implements RTreeListener {

    private List<org.dfpl.dbp.rtree.team_7.Point> allPoints = new ArrayList<>();
    private List<org.dfpl.dbp.rtree.team_7.Rectangle> nodeMBRs = new ArrayList<>();

    private org.dfpl.dbp.rtree.team_7.Rectangle queryRect = null;
    private List<org.dfpl.dbp.rtree.team_7.Rectangle> visitedNodes = new ArrayList<>();
    private List<org.dfpl.dbp.rtree.team_7.Rectangle> prunedNodes = new ArrayList<>();
    private List<org.dfpl.dbp.rtree.team_7.Point> resultPoints = new ArrayList<>();

    private List<org.dfpl.dbp.rtree.team_7.Rectangle> knnActiveNodes = new ArrayList<>();
    private List<org.dfpl.dbp.rtree.team_7.Point> knnResultPoints = new ArrayList<>();
    private List<org.dfpl.dbp.rtree.team_7.Point> knnRemovedPoints = new ArrayList<>();
    private int knnMaxCount = 0;

    private org.dfpl.dbp.rtree.team_7.Point knnSource = null;
    private List<org.dfpl.dbp.rtree.team_7.Point> knnCandidates = new ArrayList<>();


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
        for (org.dfpl.dbp.rtree.team_7.Rectangle r : nodeMBRs) {
            drawRect(g2, r);
        }

        // 2) 검색에서 방문한 노드 / 가지친 노드 강조
        g2.setColor(new Color(0, 150, 255, 120)); // 방문 노드
        for (org.dfpl.dbp.rtree.team_7.Rectangle r : visitedNodes) {
            drawRect(g2, r);
        }

        g2.setColor(new Color(255, 0, 0, 120));   // 가지친 노드
        for (org.dfpl.dbp.rtree.team_7.Rectangle r : prunedNodes) {
            drawRect(g2, r);
            // X 표시
            org.dfpl.dbp.rtree.team_7.Point lt = r.getLeftTop();
            org.dfpl.dbp.rtree.team_7.Point rb = r.getRightBottom();
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
        for (org.dfpl.dbp.rtree.team_7.Point p : allPoints) {
            drawPoint(g2, p, 4);
        }

        // 5) 검색 결과 포인트
        g2.setColor(Color.BLUE);
        for (org.dfpl.dbp.rtree.team_7.Point p : resultPoints) {
            drawPoint(g2, p, 6);
        }

        // 6) KNN 결과 포인트 (녹색)
        g2.setColor(Color.GREEN.darker());
        for (org.dfpl.dbp.rtree.team_7.Point p : knnResultPoints) {
            drawPoint(g2, p, 7);
        }

        // 7) KNN 후보 포인트 (주황색)
        g2.setColor(new Color(255, 128, 0));
        for (org.dfpl.dbp.rtree.team_7.Point p : knnCandidates) {
            drawPoint(g2, p, 6);
        }

        // 8) 제거된 포인트(X 표시, 빨간색)
        g2.setColor(Color.RED);
        for (org.dfpl.dbp.rtree.team_7.Point p : knnRemovedPoints) {
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
        for (org.dfpl.dbp.rtree.team_7.Rectangle r : knnActiveNodes) {
            drawRect(g2, r);
        }

        // 후보 점 – 파란 점
        g2.setColor(Color.BLUE);
        for (org.dfpl.dbp.rtree.team_7.Point p : knnCandidates) {
            drawPoint(g2, p, 6);
        }

        // 확정된 KNN 결과 – 초록 점
        g2.setColor(Color.GREEN.darker());
        for (org.dfpl.dbp.rtree.team_7.Point p : knnResultPoints) {
            drawPoint(g2, p, 7);
        }

        // 제거된 점 – 빨간 X 표시
        g2.setColor(Color.RED);
        for (org.dfpl.dbp.rtree.team_7.Point p : knnRemovedPoints) {
            int sx = toScreenX(p.getX());
            int sy = toScreenY(p.getY());
            int r = 6;
            g2.drawLine(sx - r, sy - r, sx + r, sy + r);
            g2.drawLine(sx - r, sy + r, sx + r, sy - r);
        }
        if (knnMaxCount > 0) {

            // KNN 상태 텍스트 (위쪽에 표시)
            String text = "KNN: found " + knnResultPoints.size() + " / " + knnMaxCount;
            setLogText(text);
        }
    }



    private void drawX(Graphics2D g2, org.dfpl.dbp.rtree.team_7.Point p, int r) {
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

    private void drawPoint(Graphics2D g2, org.dfpl.dbp.rtree.team_7.Point p, int r) {
        int sx = toScreenX(p.getX());
        int sy = toScreenY(p.getY());
        g2.fillOval(sx - r, sy - r, r * 2, r * 2);
    }

    private void drawRect(Graphics2D g2, org.dfpl.dbp.rtree.team_7.Rectangle rect) {
        org.dfpl.dbp.rtree.team_7.Point lt = rect.getLeftTop();
        org.dfpl.dbp.rtree.team_7.Point rb = rect.getRightBottom();

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
    public void onTreeChanged(List<org.dfpl.dbp.rtree.team_7.Point> allPoints, List<org.dfpl.dbp.rtree.team_7.Rectangle> nodeMBRs) {
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
    public void onSearchStep(org.dfpl.dbp.rtree.team_7.Rectangle query, List<org.dfpl.dbp.rtree.team_7.Rectangle> visited, List<org.dfpl.dbp.rtree.team_7.Rectangle> pruned, List<org.dfpl.dbp.rtree.team_7.Point> results) {
        this.queryRect = query;
        this.visitedNodes = new ArrayList<>(visited);
        this.prunedNodes = new ArrayList<>(pruned);
        this.resultPoints = new ArrayList<>(results);
        repaint();
    }

    @Override
    public void onKnnStep(org.dfpl.dbp.rtree.team_7.Point source,
                          List<Rectangle> activeNodes,
                          List<org.dfpl.dbp.rtree.team_7.Point> candidates,
                          List<org.dfpl.dbp.rtree.team_7.Point> resultPoints,
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