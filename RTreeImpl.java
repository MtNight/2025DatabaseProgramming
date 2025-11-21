package org.dfpl.dbp.rtree;

import java.util.*;

public class RTreeImpl implements RTree {

	// 요건 4-way R-Tree로 구현한다.
	// Maven Project로 만든다.
	// 기존의 R-Tree를 활용하지 않는다.
	// 여러분의 프로젝트에는 최소한의 dependency가 포함되어 있어야 함.
	// 멤버 변수의 활용은 어느정도 자유로움
	// 단, R-Tree 구현이어야 하고, 요행을 바라지 않는다.

	private static class Entry{
		Rectangle mbr;
		Point point;
		Node child;

		public Entry(Rectangle mbr, Point point, Node child) {
			this.mbr = mbr;
			this.point = point;
			this.child = child;
		}
	}


	private static class Node{
		boolean isLeaf; //리프 노드인지에 대한 변수
		List<Entry> entries; //엔트리에 대한 정보.
		Rectangle nodeMbr;		//해당 노드를 감싸는 MBR

		public Node(boolean isLeaf) {
			this.isLeaf = isLeaf;
			this.entries = new ArrayList<>();
			this.nodeMbr = null;
		}
	}

	private Node root;		//루트노드
	private static final int MAX_ENTRIES = 4; 	//최대 엔트리 수 4개(4-WAY R트리)
	private static final int MIN_ENTRIES = MAX_ENTRIES / 3;	//최대 엔트리의 30퍼센트

	public RTreeImpl(){
		root = new Node(true);	//첫 루트노드 생성(루트노드 = 리프노드)
	}

	//Rectangle에 구현할까 하다가 일단 여기에 구현(아래는 search에 사용되는 함수)
	private boolean intersects(Rectangle a, Rectangle b) {		//a와 b사각형이 서로 겹치는지 확인(겹치면 탐색 내려감)
		return !(a.getRightBottom().getX() < b.getLeftTop().getX() ||		//하나라도 겹치면 true
				a.getLeftTop().getX() > b.getRightBottom().getX() ||
				a.getRightBottom().getY() < b.getLeftTop().getY() ||
				a.getLeftTop().getY() > b.getRightBottom().getY());
	}

	private boolean contains(Rectangle r, Point p) {		//마지막에 확인, 사각형 안에 들어오는 점은 true로 반환
		return p.getX() >= r.getLeftTop().getX() &&
				p.getX() <= r.getRightBottom().getX() &&
				p.getY() >= r.getLeftTop().getY() &&
				p.getY() <= r.getRightBottom().getY();
	}

	//아래는 nearest에 포함되는 함수
	// 후보 객체 (엔트리 + 그 엔트리까지의 최소 거리)
	private static class Candidate {
		Entry entry;
		double minDist; // find에서 이 MBR까지의 최소 거리

		Candidate(Entry entry, double minDist) {
			this.entry = entry;
			this.minDist = minDist;
		}
	}

	// 점에서 사각형까지의 최소 거리 (0이면 겹치거나 안에 있음)
	private double minDistToRectangle(Point p, Rectangle rect) {
		double px = p.getX();
		double py = p.getY();

		double rx1 = rect.getLeftTop().getX();
		double ry1 = rect.getLeftTop().getY();
		double rx2 = rect.getRightBottom().getX();
		double ry2 = rect.getRightBottom().getY();

		//점이 사각형 밖에 있다면 가장 가까운 경계까지의 거리를 구한다
		//사각형 안에있으면 0
		double dx = Math.max(0, Math.max(rx1 - px, px - rx2));
		double dy = Math.max(0, Math.max(ry1 - py, py - ry2));

		return Math.sqrt(dx * dx + dy * dy);	//거리 반환
	}

	//add 간단히 구현
	@Override
	public void add(Point point) {
		// TODO Auto-generated method stub
		if (point == null) return;
		for (Entry e : root.entries) {
			if (e.point != null && e.point.getX() == point.getX() && e.point.getY() == point.getY()) {
				return;
			}
		}
		Rectangle mbr = pointToRectangle(point);
		root.entries.add(new Entry(mbr, point, null));
	}
	private Rectangle pointToRectangle(Point p) {
		return new Rectangle(new Point(p.getX(), p.getY()), new Point(p.getX(), p.getY()));
	}

	@Override
	public Iterator<Point> search(Rectangle rectangle) {		//반환값이 이터레이터
		// TODO 탐색함수 구현
		// 	1. 여기서 포인터 리스트 리턴
		//  2. 만약 루트가 없거나, 엔트리가 없다면 빈이터레이터 리턴
		//  3. 점 구하는 함수 부르고, 돌아오면 해당하는 리스트 리턴
		if(root == null || root.entries.isEmpty()) return Collections.emptyIterator();

		List<Point> result = new ArrayList<>();
		searchPoints(root, rectangle, result);
		return result.iterator();
	}

	private void searchPoints(Node node, Rectangle rectangle, List<Point> result) {
		//TODO 재귀 탐색 함수 구현
		// 1. 받아온 사각형에 엔트리들 순회하며, 겹치는 mbr 탐색
		// 2. 리프노드면 mbr의 포인트가 사각형에 들어오는지 확인
		// 3. 포함되면 결과값에 추가
		for(Entry entry : node.entries){
			if(intersects(entry.mbr, rectangle)){		//사용자가 그린 사각형에 MBR이 겹치는 지 확인
				if(node.isLeaf){				//그게 리프노드라면
					if(entry.point != null && contains(rectangle, entry.point)){	//entry의 포인트들이 사각형에 들어오는지 확인
						result.add(entry.point);		//있으면 결과값에 추가
					}
				}
				else{		//내부노드라면
					if(entry.child != null){	//만약 자식노드가 존재한다면
						searchPoints(entry.child, rectangle, result);	//재귀
					}
				}
			}
			//MBR이 겹치지 않으면 다음 MBR로 넘어감
		}
	}

	//최근접 이웃 알고리즘(KNN)으로 탐색
	@Override
	public Iterator<Point> nearest(Point source, int maxCount) {
		// TODO 가장 가까운 점 탐색
		// 1. 루트노드의 모든 엔트리를 후보로 넣은 후
		// 2. 가장 가까운 루트 mbr탐색
		// 3. 점을 발견할 시 result로 넣어주고, result중 가장 먼 점과의 거리가 기준
		// 4. 넣어준 mbr이 기준보다 먼 mbr이면 탐색하지 않음.
		// 5. 모든 루트 mbr탐색
		if (source == null || root == null || root.entries.isEmpty() || maxCount <= 0) {
			return Collections.emptyIterator();
		}
		// 결과 저장용 우선순위 큐
		PriorityQueue<Point> result = new PriorityQueue<>(maxCount,	(p1, p2) -> Double.compare(p2.distance(source), p1.distance(source))); // 내림차순
		// 탐색 중 후보 관리하는 우선순위 큐
		PriorityQueue<Candidate> candidates = new PriorityQueue<>(Comparator.comparingDouble(c -> c.minDist));
		// 루트 노드의 모든 엔트리를 후보로 넣고 시작
		for (Entry entry : root.entries) {
			double minDist = minDistToRectangle(source, entry.mbr);
			candidates.offer(new Candidate(entry, minDist));
		}
		int found = 0;	//찾은 개수
		while (!candidates.isEmpty() && found < maxCount) {
			Candidate cand = candidates.poll();
			double currentCandidateDist = cand.minDist;

			// 현재 후보의 minDist가 result의 가장 먼 점(peek)보다 크면 pruning
			if (result.size() == maxCount && currentCandidateDist > result.peek().distance(source)) {
				break;
			}

			if (cand.entry.point != null) { // 리프: 실제 점 발견
				Point p = cand.entry.point;
				result.offer(p);
				if (result.size() > maxCount) {
					result.poll(); // 가장 먼 것 제거 → 항상 maxCount개 유지
				}
				found++;
			} else if (cand.entry.child != null) { // 내부 노드: 자식들 추가
				for (Entry childEntry : cand.entry.child.entries) {
					double childMinDist = minDistToRectangle(source, childEntry.mbr);
					candidates.offer(new Candidate(childEntry, childMinDist));
				}
			}
		}
		// 결과는 가까운 순으로 정렬된 상태로 반환
		List<Point> sortedResult = new ArrayList<>(result);
		sortedResult.sort(Comparator.comparingDouble(p -> p.distance(source)));
		return sortedResult.iterator();
	}

	@Override
	public void delete(Point point) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}
}
