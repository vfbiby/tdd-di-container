import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContextConfig {
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();
    private final Map<Class<?>, ScopeProvider> scopes = new HashMap<>();

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
        Map<Class<?>, List<Annotation>> annotationGroups = Arrays.stream(annotations)
                .collect(Collectors.groupingBy(this::typeOf, Collectors.toList()));
        if (annotationGroups.containsKey(Illegal.class)) throw new IllegalComponentException();
        bind(type, annotationGroups.getOrDefault(Qualifier.class, List.of()),
                createScopedProvider(implementation, annotationGroups.getOrDefault(Scope.class, List.of())));
    }

    private <Type> ComponentProvider<Type> createScopedProvider(Class<Type> implementation, List<Annotation> scopes) {
        if (scopes.size() > 1) throw new IllegalComponentException();
        ComponentProvider<Type> injectionProvider = new InjectionProvider<>(implementation);
        return scopes.stream().findFirst().or(() -> scopeFrom(implementation))
                .map(s -> (ComponentProvider<Type>) getScopeProvider(s, injectionProvider)).orElse(injectionProvider);
    }

    private <Type, Implementation extends Type> void bind(Class<Type> type, List<Annotation> qualifiers, ComponentProvider<Implementation> provider) {
        if (qualifiers.isEmpty()) components.put(new Component(type, null), provider);
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), provider);
        }
    }

    private <Type> Optional<Annotation> scopeFrom(Class<Type> implementation) {
        return Arrays.stream(implementation.getAnnotations()).filter(a -> a.annotationType()
                .isAnnotationPresent(Scope.class)).findFirst();
    }

    private Class<?> typeOf(Annotation annotation) {
        Class<? extends Annotation> type = annotation.annotationType();
        return Stream.of(Qualifier.class, Scope.class).filter(type::isAnnotationPresent).findFirst().orElse(Illegal.class);
    }

    private @interface Illegal {
    }

    private ComponentProvider<?> getScopeProvider(Annotation scope, ComponentProvider<?> provider) {
        if (!scopes.containsKey(scope.annotationType())) throw new IllegalComponentException();
        return scopes.get(scope.annotationType()).create(provider);
    }

    public <ScopeType extends Annotation> void scope(Class<ScopeType> scope, ScopeProvider provider) {
        scopes.put(scope, provider);
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

}
