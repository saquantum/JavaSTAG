package edu.uob;

import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.objects.Edge;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Node;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Document {
    private int vertices = 0;
    private Map<Integer, GameEntity> locations = new HashMap<>();
    private Map<String, Integer> reverseLocations = new HashMap<>();
    private List<Edge> edges = new LinkedList<>();
    private Map<String, List<Edge>> adj = new HashMap<>();

    private Set<String> allEntities = new HashSet<>(); // check duplicate name
    private Set<String> builtInActions = Set.of("inventory", "inv", "get", "drop", "goto", "look"); // check entity name clash with built-in actions

    public Document(Parser parser) throws MyExceptions {
        this.getDocument(parser.getGraphs().get(0));
    }

    public void addLocation(edu.uob.entities.Location e) {
        this.locations.put(this.vertices, e);
        this.reverseLocations.put(e.getName().toLowerCase(), this.vertices);
        this.vertices++;
    }

    public GameEntity getLocation(int i) {
        return this.locations.get(i);
    }

    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }

    public List<Edge> listEdges() {
        return this.edges;
    }

    public void addEdge(Edge e) throws MyExceptions {
        this.edges.add(e);

        String from = e.getSource().getNode().getId().getId().toLowerCase();
        String to = e.getTarget().getNode().getId().getId().toLowerCase();
        if (!reverseLocations.containsKey(from) || !reverseLocations.containsKey(to)) {
            throw new MyExceptions.InvalidEdgeException();
        }

        List<Edge> list = this.adj.get(from);
        if (list == null) {
            list = new LinkedList<>();
        }
        list.add(e);
        this.adj.put(from, list);
    }

    public List<Edge> getEdgesFrom(String location) {
        return this.adj.get(location.toLowerCase());
    }

    public int countVertex() {
        return this.vertices;
    }

    public int countEdge() {
        return this.edges.size();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.vertices; i++) {
            sb.append(locations.get(i).toString());
        }
        for (Edge edge : this.edges) {
            sb.append(edge.getSource().getNode().getId().getId()).append(" -> ").append(edge.getTarget().getNode().getId().getId()).append(System.lineSeparator());
        }
        return sb.toString();
    }

    // exclude subgraph nodes
    private List<Node> getDirectNodes(Graph G) {
        List<Node> nodes = G.getNodes(true);
        Set<String> reference = G.getSubgraphs().stream().map(new Function<Graph, String>() {
            @Override
            public String apply(Graph subgraph) {
                return subgraph.getId().getId();
            }
        }).collect(Collectors.toSet());
        return nodes.stream().filter(new Predicate<Node>() {
            @Override
            public boolean test(Node n) {
                return !reference.contains(n.getId().getId());
            }
        }).toList();
    }

    private enum Types {LAYOUT, LOCATION, PLAYER, FURNITURE, CHARACTER, ARTEFACT, PATH}

    private static final Map<String, Types> entityTypes = Map.ofEntries(Map.entry("layout", Types.LAYOUT),
            Map.entry("locations", Types.LOCATION), Map.entry("players", Types.PLAYER),
            Map.entry("furniture", Types.FURNITURE), Map.entry("characters", Types.CHARACTER),
            Map.entry("artefacts", Types.ARTEFACT), Map.entry("paths", Types.PATH));

    private void getDocument(Graph G) throws MyExceptions {
        if (G == null || entityTypes.get(G.getId().getId().toLowerCase()) != Types.LAYOUT) {
            throw new MyExceptions.InvalidParserException();
        }

        Graph locationGraph = G.getSubgraphs().get(0); // locations
        if (entityTypes.get(locationGraph.getId().getId().toLowerCase()) != Types.LOCATION) {
            throw new MyExceptions.NotLocationException();
        }
        List<Graph> locations = locationGraph.getSubgraphs(); // cluster001, cluster002, ...
        if (locations != null && locations.size() > 0) {
            for (Graph location : locations) {
                Node locationDetails = this.getDirectNodes(location).get(0);
                String locationName = locationDetails.getId().getId().toLowerCase();
                this.checkUniqueName(locationName);
                this.checkBuiltInAction(locationName);
                edu.uob.entities.Location current = new edu.uob.entities.Location(locationName);
                current.setAttributes(locationDetails.getAttributes());
                for (Graph subgraph : location.getSubgraphs()) {
                    List<GameEntity> items = this.getItems(subgraph);
                    if (items == null) continue;
                    for (GameEntity item : items) {
                        if (item != null) current.addItem(item);
                    }
                }
                this.addLocation(current);
            }
        }

        Graph pathGraph = G.getSubgraphs().get(1); // paths
        if (entityTypes.get(pathGraph.getId().getId().toLowerCase()) != Types.PATH) {
            throw new MyExceptions.NotPathException();
        }
        List<Edge> edges = pathGraph.getEdges();
        if (edges != null && edges.size() > 0) {
            edges.forEach(this::addEdge);
        }
    }

    private List<GameEntity> getItems(Graph G) throws MyExceptions {
        if (G == null) return null;

        List<GameEntity> items = new LinkedList<>();
        Types type = entityTypes.get(G.getId().getId().toLowerCase());
        if (type != Types.PLAYER && type != Types.FURNITURE && type != Types.CHARACTER && type != Types.ARTEFACT) {
            throw new MyExceptions.NoSuchTypeException(type.toString());
        }

        for (Node node : this.getDirectNodes(G)) {
            String name = node.getId().getId().toLowerCase();
            this.checkUniqueName(name);
            this.checkBuiltInAction(name);

            Class<? extends GameEntity> clazz = null;
            if (type == Types.PLAYER) {
                clazz = edu.uob.entities.Player.class;
            }
            if (type == Types.FURNITURE) {
                clazz = edu.uob.entities.Furniture.class;
            }
            if (type == Types.CHARACTER) {
                clazz = edu.uob.entities.Character.class;
            }
            if (type == Types.ARTEFACT) {
                clazz = edu.uob.entities.Artefact.class;
            }

            try {
                GameEntity item = clazz.getConstructor(String.class).newInstance(name);
                this.allEntities.add(name);
                item.setAttributes(node.getAttributes());
                items.add(item);
            } catch (Exception e) {
                throw new MyExceptions.NoConstructorException(type.toString());
            }
        }
        return items;
    }

    public void checkBuiltInAction(String name) throws MyExceptions{
        if(this.builtInActions.contains(name)) throw new MyExceptions.DuplicateEntityException();
    }

    // action triggers cannot be named after an entity -- check it using below method
    public void checkUniqueName(String name) throws MyExceptions{
        if(this.allEntities.contains(name)) throw new MyExceptions.DuplicateEntityException();
    }
}
