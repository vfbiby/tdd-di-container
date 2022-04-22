import java.util.HashSet;
import java.util.Set;

public class CyclicDependencyFoundException extends RuntimeException {
    private final Set<Class<?>> components = new HashSet<>();

    public CyclicDependencyFoundException(Class<?> component) {
        components.add(component);
    }

    public CyclicDependencyFoundException(Class<?> component, CyclicDependencyFoundException e) {
        components.add(component);
        components.addAll(e.components);
    }

    public Class<?>[] getComponents() {
        return components.toArray(Class<?>[]::new);
    }
}
