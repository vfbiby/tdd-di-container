import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Context {
    private final Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, (Provider<Type>) () -> instance);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<?>[] injectConstructors = Arrays.stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class))
                .toArray(Constructor<?>[]::new);
        if (injectConstructors.length > 1) throw new IllegalComponentException();
        if (injectConstructors.length == 0 && Arrays.stream(implementation.getConstructors())
                .filter(c -> c.getParameters().length == 0).findFirst().map(c -> false).orElse(true))
            throw new IllegalComponentException();
        providers.put(type, (Provider<Type>) () -> {
            try {
                Constructor<Implementation> injectConstructor = getConstructor(implementation);
                Object[] dependencies = Arrays.stream(injectConstructor.getParameters())
                        .map(p -> get(p.getType()))
                        .toArray(Object[]::new);
                return (Type) injectConstructor.newInstance(dependencies);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private <Type> Constructor<Type> getConstructor(Class<Type> implementation) {
        Stream<Constructor<?>> injectConstructors = Arrays.stream(implementation.getConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class));
        return (Constructor<Type>) injectConstructors.findFirst().orElseGet(() -> {
            try {
                return implementation.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <Type> Type get(Class<Type> type) {
        return (Type) providers.get(type).get();
    }

}
