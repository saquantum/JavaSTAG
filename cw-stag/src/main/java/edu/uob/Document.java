package edu.uob;

import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.objects.Edge;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Node;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Document {
    private int vertices = 0;
    private final Map<Integer, Location> locations = new HashMap<>();
    private final Map<String, Integer> reverseLocations = new HashMap<>();
    private final Map<String, List<Edge>> adj = new HashMap<>();

    private final Map<String, GameEntity> allEntities = new HashMap<>();

    private final Map<String, Player> players = new HashMap<>();

    public Document(File entitiesFile) throws MyExceptions {
        try {
            Parser parser = new Parser();
            FileReader reader = new FileReader(entitiesFile);
            parser.parse(reader);
            this.setDocument(parser.getGraphs().get(0));
            // init player location
            for (Map.Entry<String, Player> entry : this.players.entrySet()) {
                entry.getValue().setCurrent(this.getLocation(0));
            }
        } catch (Exception e) {
            throw new MyExceptions(e.getMessage());
        }
    }

    public void addLocation(Location e) {
        this.locations.put(this.vertices, e);
        this.reverseLocations.put(e.getName().toLowerCase(), this.vertices);
        this.vertices++;
    }

    public boolean hasLocation(String name) {
        return this.reverseLocations.containsKey(name);
    }

    public Location getLocation(String name) {
        return this.locations.get(this.reverseLocations.get(name));
    }

    public Location getLocation(int index) {
        return this.locations.get(index);
    }

    public void addEdge(Edge e) throws MyExceptions {
        String from = e.getSource().getNode().getId().getId().toLowerCase();
        String to = e.getTarget().getNode().getId().getId().toLowerCase();
        if (!this.hasLocation(from) || !this.hasLocation(to)) {
            throw new MyExceptions.InvalidEdgeException();
        }

        List<Edge> list = this.adj.get(from);
        if (list == null) {
            list = new LinkedList<>();
        }
        list.add(e);
        this.adj.put(from, list);
    }

    public Edge removeEdge(String from, String to) {
        if (!this.adj.containsKey(from)) return null;

        for (Edge edge : this.adj.get(from)) {
            if (edge.getTarget().getNode().getId().getId().equals(to)) {
                this.adj.get(from).remove(edge);
                return edge;
            }
        }
        return null;
    }

    public boolean hasEdge(String from, String to) {
        if (!this.hasLocation(from)) return false;
        for (Edge edge : this.adj.get(from)) {
            if (edge.getTarget().getNode().getId().getId().equals(to)) {
                return true;
            }
        }
        return false;
    }

    public List<Edge> getEdgesFrom(String from) {
        return this.adj.get(from.toLowerCase());
    }

    public Player newPlayer(String name) throws MyExceptions {
        if (this.hasEntity(name)) throw new MyExceptions.DuplicateEntityException();

        Player player = new Player(name);
        player.setCurrent(this.getLocation(0));
        this.players.put(name, player);
        return player;
    }

    public Map<String, Player> getPlayers() {
        return this.players;
    }

    // action triggers cannot be named after an entity -- check it using below method
    public boolean hasEntity(String name) {
        return this.allEntities.containsKey(name);
    }

    public GameEntity getEntity(String name) {
        return this.allEntities.get(name);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.vertices; i++) {
            sb.append(locations.get(i).toString());
        }
        for (Map.Entry<String, List<Edge>> entry : this.adj.entrySet()) {
            for (Edge edge : entry.getValue()) {
                sb
                        .append(edge.getSource().getNode().getId().getId())
                        .append(" -> ")
                        .append(edge.getTarget().getNode().getId().getId())
                        .append(System.lineSeparator());
            }
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

    private void setDocument(Graph G) throws MyExceptions {
        if (G == null || entityTypes.get(G.getId().getId().toLowerCase()) != Types.LAYOUT) {
            throw new MyExceptions.InvalidParserException();
        }

        // store locations
        Graph locationGraph = G.getSubgraphs().get(0);
        if (entityTypes.get(locationGraph.getId().getId().toLowerCase()) != Types.LOCATION) {
            throw new MyExceptions.NotLocationException();
        }
        List<Graph> locations = locationGraph.getSubgraphs(); // cluster001, cluster002, ...
        if (locations != null && locations.size() > 0) {
            for (Graph location : locations) {
                // some checks
                Node locationDetails = this.getDirectNodes(location).get(0);
                String locationName = locationDetails.getId().getId().toLowerCase();
                if (this.hasEntity(locationName)) {
                    throw new MyExceptions.DuplicateEntityException();
                }
                this.checkBuiltInAction(locationName);
                // create separate location object
                Location current = new Location(locationName);
                current.setAttributes(locationDetails.getAttributes());
                // get items at the location
                for (Graph subgraph : location.getSubgraphs()) {
                    List<MovableEntity> items = this.getItems(subgraph, current);
                    if (items == null) continue;
                    for (MovableEntity item : items) {
                        if (item != null) current.addItem(item);
                    }
                }
                this.addLocation(current);
            }
        }

        // if storeroom is not specified, generate one
        if (!this.hasLocation("storeroom")) {
            this.addLocation(new Location("storeroom"));
        }

        // store paths
        Graph pathGraph = G.getSubgraphs().get(1);
        if (entityTypes.get(pathGraph.getId().getId().toLowerCase()) != Types.PATH) {
            throw new MyExceptions.NotPathException();
        }
        List<Edge> edges = pathGraph.getEdges();
        if (edges != null && edges.size() > 0) {
            edges.forEach(this::addEdge);
        }
    }

    private List<MovableEntity> getItems(Graph G, Location location) throws MyExceptions {
        if (G == null) return null;
        Types type = entityTypes.get(G.getId().getId().toLowerCase());
        if (type != Types.PLAYER && type != Types.FURNITURE && type != Types.CHARACTER && type != Types.ARTEFACT) {
            throw new MyExceptions.NoSuchTypeException(type.toString());
        }

        List<MovableEntity> items = new LinkedList<>();
        for (Node node : this.getDirectNodes(G)) {
            // check item not duplicate
            String name = node.getId().getId().toLowerCase();
            if (this.hasEntity(name)) {
                throw new MyExceptions.DuplicateEntityException();
            }
            this.checkBuiltInAction(name);

            Class<? extends MovableEntity> clazz = null;
            if (type == Types.PLAYER) {
                clazz = Player.class;
            }
            if (type == Types.FURNITURE) {
                clazz = Furniture.class;
            }
            if (type == Types.CHARACTER) {
                clazz = Character.class;
            }
            if (type == Types.ARTEFACT) {
                clazz = Artefact.class;
            }

            try {
                MovableEntity item = clazz.getConstructor(String.class).newInstance(name);

                // insert player into players, insert other movable to all entities
                if (type == Types.PLAYER) {
                    this.players.put(name, (Player) item);
                }else{
                    this.allEntities.put(name, item);
                }
                // set item's attributes and initial location
                item.setAttributes(node.getAttributes());
                item.setCurrent(location);
                items.add(item);
            } catch (Exception e) {
                throw new MyExceptions.NoConstructorException(type.toString());
            }
        }
        return items;
    }

    private void checkBuiltInAction(String name) throws MyExceptions {
        if (GameActions.builtInActions.contains(name)) throw new MyExceptions.DuplicateEntityException();
    }
}
