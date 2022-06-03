import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Function;

public class ContextConfig {
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();
    private final Map<Class<?>, Function<ComponentProvider<?>, ComponentProvider<?>>> scopes = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        components.put(new Component(type, null), context -> instance);
    }

    public ContextConfig() {
        scope(Singleton.class, SingletonProvider::new);
    }

    public <Type> void bind(Class<Type> type, Type instance, Annotation... qualifiers) {
        if (Arrays.stream(qualifiers).anyMatch(q -> !q.annotationType().isAnnotationPresent(Qualifier.class)))
            throw new IllegalComponentException();
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), context -> instance);
        }
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        bind(type, implementation, implementation.getAnnotations());
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation, Annotation... annotations) {
        if (Arrays.stream(annotations).map(Annotation::annotationType)
                .anyMatch(t -> !t.isAnnotationPresent(Qualifier.class) && !t.isAnnotationPresent(Scope.class)))
            throw new IllegalComponentException();
        Optional<Annotation> scopeForType = Arrays.stream(implementation.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).findFirst();

        List<Annotation> qualifiers = Arrays.stream(annotations).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).toList();
        Optional<Annotation> scope = Arrays.stream(annotations).filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).findFirst()
                .or(() -> scopeForType);

        ComponentProvider<Implementation> injectionProvider = new InjectionProvider<>(implementation);
        ComponentProvider<Implementation> provider = scope.map(s ->
                (ComponentProvider<Implementation>) getScopeProvider(s, injectionProvider)).orElse(injectionProvider);
        if (qualifiers.isEmpty()) components.put(new Component(type, null), provider);
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), provider);
        }
    }

    private ComponentProvider<?> getScopeProvider(Annotation scope, ComponentProvider<?> provider) {
        return scopes.get(scope.annotationType()).apply(provider);
    }

    public <ScopeType extends Annotation> void scope(Class<ScopeType> scope, Function<ComponentProvider<?>, ComponentProvider<?>> provider) {
        scopes.put(scope, provider);
    }

    static class SingletonProvider<T> implements ComponentProvider<T> {
        private T singleton;
        private ComponentProvider<T> provider;

        public SingletonProvider(ComponentProvider<T> provider) {
            this.provider = provider;
        }

        @Override
        public T get(Context context) {
            if (singleton == null) singleton = provider.get(context);
            return singleton;
        }

        @Override
        public List<ComponentRef<?>> getDependencies() {
            return provider.getDependencies();
        }
    }

    public Context getContext() {
        components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {

            @Override
            public <ComponentType> Optional<ComponentType> get(ComponentRef<ComponentType> componentRef) {
                if (componentRef.isContainer()) {
                    if (componentRef.getContainer() != Provider.class) return Optional.empty();
                    return (Optional<ComponentType>) Optional.ofNullable(getProvider(componentRef))
                            .map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(getProvider(componentRef)).map(provider -> (ComponentType) provider.get(this));
            }

            private <ComponentType> ComponentProvider<?> getProvider(ComponentRef<ComponentType> componentRef) {
                return components.get(componentRef.component());
            }

        };
    }

    public void checkDependencies(Component component, Stack<Component> visiting) {
        for (ComponentRef<?> dependency : components.get(component).getDependencies()) {
            if (!components.containsKey(dependency.component()))
                throw new DependencyNotFoundException(dependency.component(), component);
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.component())) throw new CyclicDependencyFoundException(visiting);
                visiting.push(dependency.component());
                checkDependencies(dependency.component(), visiting);
                visiting.pop();
            }
        }
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<ComponentRef<?>> getDependencies() {
            return List.of();
        }

    }

}
