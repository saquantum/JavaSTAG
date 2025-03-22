package edu.uob;

import com.alexmerz.graphviz.objects.Edge;
import com.alexmerz.graphviz.objects.Id;
import com.alexmerz.graphviz.objects.Node;
import com.alexmerz.graphviz.objects.PortNode;

import java.util.List;

public class Util {

    private Util() {
    }

    public static <E> boolean ListEquals(List<E> list1, List<E> list2) {
        if (list1.size() != list2.size()) return false;

        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).equals(list2.get(i))) return false;
        }

        return true;
    }

    public static Edge newEdge(String from, String to) {
        Id sourceId = new Id();
        sourceId.setId(from);
        Node sourceNode = new Node();
        sourceNode.setId(sourceId);

        Id targetId = new Id();
        targetId.setId(to);
        Node targetNode = new Node();
        targetNode.setId(targetId);

        Edge e = new Edge();
        e.setSource(new PortNode(sourceNode));
        e.setTarget(new PortNode(targetNode));
        return e;
    }
}
