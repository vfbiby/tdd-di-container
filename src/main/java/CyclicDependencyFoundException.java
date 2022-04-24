import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class CyclicDependencyFoundException extends RuntimeException {
    private final Set<Class<?>> components = new HashSet<>();

    public CyclicDependencyFoundException(Stack<Class<?>> visiting) {
        components.addAll(visiting);
    }

    public Class<?>[] getComponents() {
        return components.toArray(Class<?>[]::new);
    }
}
