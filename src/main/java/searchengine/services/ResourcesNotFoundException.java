package searchengine.services;

public class ResourcesNotFoundException extends RuntimeException{

    public ResourcesNotFoundException(String message) {
        super(message);
    }
}
