import jakarta.inject.Provider;

import java.lang.annotation.Annotation;
import java.util.*;

public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, context -> instance);
    }

    public <Type> void bind(Class<Type> type, Type instance, Annotation... qualifiers) {
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), context -> instance);
        }
    }

    record Component(Class<?> type, Annotation qualifier) {
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation, Annotation... qualifiers) {
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), new InjectionProvider<>(implementation));
        }
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {

            @Override
            public <ComponentType> Optional<ComponentType> get(Ref<ComponentType> ref) {
                if (ref.getQualifier() != null) {
                    return Optional.ofNullable(components.get(new Component(ref.getComponent(), ref.getQualifier())))
                            .map(provider -> (ComponentType) provider.get(this));
                }
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return (Optional<ComponentType>) Optional.ofNullable(providers.get(ref.getComponent())).map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(providers.get(ref.getComponent())).map(provider -> (ComponentType) provider.get(this));
            }

        };
    }

    public void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Context.Ref dependency : providers.get(component).getDependencies()) {
            if (!providers.containsKey(dependency.getComponent()))
                throw new DependencyNotFoundException(dependency.getComponent(), component);
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.getComponent())) throw new CyclicDependencyFoundException(visiting);
                visiting.push(dependency.getComponent());
                checkDependencies(dependency.getComponent(), visiting);
                visiting.pop();
            }
        }
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Context.Ref> getDependencies() {
            return List.of();
        }

    }

}
