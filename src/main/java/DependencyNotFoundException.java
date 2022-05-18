public class DependencyNotFoundException extends RuntimeException {
    private Component dependency;
    private Component component;

    public DependencyNotFoundException(Component dependency, Component component) {
        this.dependency = dependency;
        this.component = component;
    }

    public Component getDependency() {
        return dependency;
    }

    public Component getComponent() {
        return component;
    }
}
