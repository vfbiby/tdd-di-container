import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

public class ComponentRef<ComponentType> {
    private Type container;
    private Component component;

    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component) {
        return new ComponentRef<>(component, null);
    }

    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component, Annotation qualifier) {
        return new ComponentRef<>(component, qualifier);
    }

    static ComponentRef of(Type type) {
        return new ComponentRef<>(type, null);
    }

    ComponentRef(Type type, Annotation qualifier) {
        init(type, qualifier);
    }

    protected ComponentRef() {
        this(null);
    }

    protected ComponentRef(Annotation qualifier) {
        Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        init(type, qualifier);
    }

    public static ComponentRef of(Type type, Annotation qualifier) {
        return new ComponentRef<>(type, qualifier);
    }

    private void init(Type type, Annotation qualifier) {
        if (type instanceof ParameterizedType container) {
            this.container = container.getRawType();
            this.component = new Component((Class<?>) container.getActualTypeArguments()[0], qualifier);
        } else {
            this.component = new Component((Class<?>) type, qualifier);
        }
    }

    public Type getContainer() {
        return container;
    }

    public Component component() {
        return component;
    }

    public boolean isContainer() {
        return container != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentRef<?> that = (ComponentRef<?>) o;
        return Objects.equals(container, that.container) && component.equals(that.component);
    }

    @Override
    public int hashCode() {
        return Objects.hash(container, component);
    }
}
