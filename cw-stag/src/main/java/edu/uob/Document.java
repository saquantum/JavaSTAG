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
    private final Map<String, Set<String>> adj = new HashMap<>();

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

    public void addEdge(String from, String to) throws MyExceptions {
        if (!this.hasLocation(from) || !this.hasLocation(to)) {
            throw new MyExceptions.InvalidEdgeException();
        }

        Set<String> pathTo = this.adj.get(from);
        if (pathTo == null) {
            pathTo = new HashSet<>();
        }
        pathTo.add(to);
        this.adj.put(from, pathTo);
    }

    public boolean removeEdge(String from, String to) {
        if (!this.adj.containsKey(from)) return false;

        for (String dest : this.adj.get(from)) {
            if (dest.equals(to)) {
                this.adj.get(from).remove(to);
                return true;
            }
        }
        return false;
    }

    public boolean hasEdge(String from, String to) {
        if (!this.hasLocation(from)) return false;
        if (!this.adj.containsKey(from)) return false;
        for (String dest : this.adj.get(from)) {
            if (dest.equals(to)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getEdgesFrom(String from) {
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
        for (Map.Entry<String, Set<String>> entry : this.adj.entrySet()) {
            String from = entry.getKey();
            for (String to : entry.getValue()) {
                sb.append(from).append(" -> ").append(to).append(System.lineSeparator());
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

    private void parseLocations(List<Graph> locations) {
        if (locations != null && !locations.isEmpty()) {
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
    }

    private void parsePaths(List<Edge> edges) {
        if (edges != null && !edges.isEmpty()) {
            for (Edge edge : edges) {
                this.addEdge(edge.getSource().getNode().getId().getId(), edge.getTarget().getNode().getId().getId());
            }
        }
    }

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
        this.parseLocations(locations);

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
        this.parsePaths(edges);
    }

    private void parseItem(Types type, Node node, String name, Location location, List<MovableEntity> items) throws MyExceptions {
        Class<? extends MovableEntity> clazz = null;
        if (type == Types.PLAYER) clazz = Player.class;
        if (type == Types.FURNITURE) clazz = Furniture.class;
        if (type == Types.CHARACTER) clazz = Character.class;
        if (type == Types.ARTEFACT) clazz = Artefact.class;
        if (clazz == null) throw new MyExceptions.NoSuchTypeException(type.toString());

        try {
            MovableEntity item = clazz.getConstructor(String.class).newInstance(name);
            // insert player into players, insert other movable to all entities
            if (type == Types.PLAYER) {
                this.players.put(name, (Player) item);
            } else {
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
            this.parseItem(type, node, name, location, items);
        }
        return items;
    }

    private void checkBuiltInAction(String name) throws MyExceptions {
        if (GameActions.builtInActions.containsKey(name)) throw new MyExceptions.DuplicateEntityException();
    }
}
