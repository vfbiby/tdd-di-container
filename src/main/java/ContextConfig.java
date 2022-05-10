import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, context -> instance);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public Optional get(Type type) {
                if (isContainerType(type)) return getContainer((ParameterizedType) type);
                return getComponent((Class<?>) type);
            }

            private <Type> Optional<Type> getComponent(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).map(provider -> (Type) provider.get(this));
            }

            private Optional getContainer(ParameterizedType type) {
                if (type.getRawType() != Provider.class) return Optional.empty();
                Class<?> componentType = getComponentType(type);
                return Optional.ofNullable(providers.get(componentType)).map(provider -> (Provider<Object>) () -> provider.get(this));
            }

        };
    }

    private Class<?> getComponentType(Type type) {
        return (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
    }

    private boolean isContainerType(Type type) {
        return type instanceof ParameterizedType;
    }

    public void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencies()) {
            if (isContainerType(dependency)) checkContainerType(component, dependency);
            else checkDependency(component, visiting, (Class<?>) dependency);
        }
    }

    private void checkContainerType(Class<?> component, Type dependency) {
        if (!providers.containsKey(getComponentType(dependency)))
            throw new DependencyNotFoundException(getComponentType(dependency), component);
    }

    private void checkDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
        if (!providers.containsKey(dependency)) throw new DependencyNotFoundException(dependency, component);
        if (visiting.contains(dependency)) throw new CyclicDependencyFoundException(visiting);
        visiting.push(dependency);
        checkDependencies(dependency, visiting);
        visiting.pop();
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Type> getDependencies() {
            return List.of();
        }
    }

}
