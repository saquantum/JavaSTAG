package edu.uob;

import com.alexmerz.graphviz.Parser;

import java.io.File;
import java.io.FileReader;

public class Controller {
    private Document document;

    public Controller(File entitiesFile, File actionsFile) throws Exception {
        Parser parser = new Parser();
        FileReader reader = new FileReader(entitiesFile);
        parser.parse(reader);
        this.document = new Document(parser);
    }

    public String handleCommand(String command) {
        return command;
    }
}
