import java.util.*;

public class BSCS2_Rodrigo_KruskalAlgorithm {
    private final Graph graph;
    private final List<Edge> mst;
    private final List<EdgeStatus> edgeStatuses;

    public static class EdgeStatus {
        private final Edge edge;
        private final boolean accepted;
        private final String status;

        public EdgeStatus(Edge edge, boolean accepted, String status) {
            this.edge = edge;
            this.accepted = accepted;
            this.status = status;
        }

        public Edge getEdge() { return edge; }
        public boolean isAccepted() { return accepted; }
        public String getStatus() { return status; }
    }

    public BSCS2_Rodrigo_KruskalAlgorithm(Graph graph) {
        this.graph = graph;
        this.mst = new ArrayList<>();
        this.edgeStatuses = new ArrayList<>();
    }

    public List<EdgeStatus> computeSteps() {
        edgeStatuses.clear();
        mst.clear();

        List<Edge> sortedEdges = new ArrayList<>(graph.getEdges());
        Collections.sort(sortedEdges);

        DisjointSet ds = new DisjointSet(graph.getVertices().size());

        for (Edge edge : sortedEdges) {
            int vertex1 = edge.getVertex1().getId();
            int vertex2 = edge.getVertex2().getId();

            if (ds.find(vertex1) != ds.find(vertex2)) {
                mst.add(edge);
                ds.union(vertex1, vertex2);
                edgeStatuses.add(new EdgeStatus(edge, true, "Accepted"));
            } else {
                edgeStatuses.add(new EdgeStatus(edge, false, "Rejected (forms a cycle)"));
            }
        }

        return edgeStatuses;
    }

    public List<Edge> getMST() {
        return mst;
    }

    public double getMSTWeight() {
        return mst.stream().mapToDouble(Edge::getWeight).sum();
    }
}