package edu.uob;

public class MyExceptions extends RuntimeException{
    public MyExceptions(String message) {
        super(message);
    }

    public static class NoConstructorException extends MyExceptions {
        public NoConstructorException(String type) {
            super(new StringBuilder().append("The constructor of ").append(type).append(" is not recognized!").toString());
        }
    }

    public static class InvalidParserException extends MyExceptions {
        public InvalidParserException() {
            super("Invalid parsing result!");
        }
    }

    public static class NotLocationException extends MyExceptions {
        public NotLocationException() {
            super("The zero-th item of layout graph is not locations!");
        }
    }

    public static class NotPathException extends MyExceptions {
        public NotPathException() {
            super("The first item of layout graph is not paths!");
        }
    }

    public static class NoSuchTypeException extends MyExceptions {
        public NoSuchTypeException(String type) {
            super(new StringBuilder().append("The type ").append(type).append(" is not recognized!").toString());
        }
    }

    public static class InvalidEdgeException extends MyExceptions {
        public InvalidEdgeException() {
            super("The source or target of one path does not exist!");
        }
    }

    public static class DuplicateEntityException extends MyExceptions {
        public DuplicateEntityException() {
            super("Duplicate name for entities!");
        }
    }
}
