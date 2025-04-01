package edu.uob;

import java.io.Serial;

public class MyExceptions extends RuntimeException{
    @Serial
    private static final long serialVersionUID = 1;
    public MyExceptions(String message) {
        super(message);
    }

    public static class InvalidCommandException extends MyExceptions {
        @Serial
        private static final long serialVersionUID = 1;
        public InvalidCommandException(String message) {
            super(message);
        }
    }

    public static class NoConstructorException extends MyExceptions {
        @Serial
        private static final long serialVersionUID = 1;
        public NoConstructorException(String type) {
            super(new StringBuilder().append("The constructor of ").append(type).append(" is not recognized!").toString());
        }
    }

    public static class InvalidParserException extends MyExceptions {
        @Serial
        private static final long serialVersionUID = 1;
        public InvalidParserException() {
            super("Invalid parsing result!");
        }
    }

    public static class NotLocationException extends MyExceptions {
        @Serial
        private static final long serialVersionUID = 1;
        public NotLocationException() {
            super("The zero-th item of layout graph is not locations!");
        }
    }

    public static class NotPathException extends MyExceptions {
        @Serial
        private static final long serialVersionUID = 1;
        public NotPathException() {
            super("The first item of layout graph is not paths!");
        }
    }

    public static class NoSuchTypeException extends MyExceptions {
        @Serial
        private static final long serialVersionUID = 1;
        public NoSuchTypeException(String type) {
            super(new StringBuilder().append("The type ").append(type).append(" is not recognized!").toString());
        }
    }

    public static class InvalidEdgeException extends MyExceptions {
        @Serial
        private static final long serialVersionUID = 1;
        public InvalidEdgeException() {
            super("The source or target of one path does not exist!");
        }
    }

    public static class DuplicateEntityException extends MyExceptions {
        @Serial
        private static final long serialVersionUID = 1;
        public DuplicateEntityException() {
            super("Duplicate name for entities!");
        }
    }

    public static class InvalidBasicAction extends MyExceptions {
        @Serial
        private static final long serialVersionUID = 1;
        public InvalidBasicAction() {
            super("Invalid usage of basic action!");
        }
    }
}
