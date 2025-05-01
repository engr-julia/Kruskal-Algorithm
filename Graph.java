import java.util.*;

public class Graph {
    private final List<Vertex> vertices;
    private final List<Edge> edges;

    public Graph() {
        vertices = new ArrayList<>();
        edges = new ArrayList<>();
    }

    public void addVertex(Vertex vertex) {
        vertices.add(vertex);
    }

    public void addEdge(Vertex v1, Vertex v2, double weight) {
        Edge edge = new Edge(v1, v2, weight);
        edges.add(edge);
        v1.addEdge(edge);
        v2.addEdge(edge);
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public List<Edge> getEdges() {
        return edges;
    }
}