public class DependencyNotFoundException extends RuntimeException {
    private final Class<?> dependency;
    private final Class<?> component;

    public DependencyNotFoundException(Class<?> dependency, Class<?> component) {
        this.dependency = dependency;
        this.component = component;
    }

    public Class<?> getDependency() {
        return dependency;
    }

    public Class<?> getComponent() {
        return component;
    }
}
