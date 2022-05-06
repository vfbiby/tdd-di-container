import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, new ComponentProvider<>() {
            @Override
            public Object get(Context context) {
                return instance;
            }

            @Override
            public List<Class<?>> getDependencies() {
                return List.of();
            }
        });
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public <Type> Optional<Type> get(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).map(provider -> (Type) provider.get(this));
            }

            @Override
            public Optional get(ParameterizedType type) {
                if (type.getRawType() != Provider.class) return Optional.empty();
                Class<?> componentType = (Class<?>) type.getActualTypeArguments()[0];
                return Optional.ofNullable(providers.get(componentType)).map(provider -> (Provider<Object>) () -> provider.get(this));
            }

        };
    }

    public void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencyTypes()) {
            if (dependency instanceof Class)
                checkDependency(component, visiting, (Class<?>) dependency);
            if (dependency instanceof ParameterizedType) {
                Class<?> type = (Class<?>) ((ParameterizedType) dependency).getActualTypeArguments()[0];
                if (!providers.containsKey(type)) throw new DependencyNotFoundException(type, component);
            }
        }
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

        default List<Class<?>> getDependencies() {
            return List.of();
        }

        default List<Type> getDependencyTypes() {
            return List.of();
        }
    }

}
