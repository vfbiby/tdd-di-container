import jakarta.inject.Provider;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private final Map<Class<?>, Class<?>> componentImplementations = new HashMap<>();
    private final Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> type, ComponentType instance) {
        providers.put(type, (Provider<ComponentType>) () -> instance);
    }

    public <ComponentType, ComponentImplementation>
    void bind(Class<ComponentType> type, Class<ComponentImplementation> implementation) {
        componentImplementations.put(type, implementation);
        providers.put(type, (Provider<ComponentImplementation>) () -> {
            try {
                return (ComponentImplementation) ((Class<?>) implementation).getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <ComponentType> ComponentType get(Class<ComponentType> type) {
        return (ComponentType) providers.get(type).get();
    }

}
