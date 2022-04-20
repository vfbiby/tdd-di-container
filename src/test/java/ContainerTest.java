import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    Context context;

    @BeforeEach
    public void setup() {
        context = new Context();
    }

    @Nested
    public class ComponentConstructionTest {
        @Test
        @DisplayName("should bind type to a specific instance")
        public void should_bind_type_to_a_specific_instance() {
            //Give
            Component instance = new Component() {
            };
            //When
            context.bind(Component.class, instance);
            //Then
            assertSame(instance, context.get(Component.class));
        }


        // TODO: 2022/4/19 abstract class
        // TODO: 2022/4/19 interface
        @Nested
        public class ConstructorInjectionTest {
            // TODO: 2022/4/19 No args constructor
            @Test
            @DisplayName("should bind type to a class with default constructor")
            public void should_bind_type_to_a_class_with_default_constructor() {
                //When
                context.bind(Component.class, ComponentWithDefaultConstructor.class);
                Component instance = context.get(Component.class);
                //Then
                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);
            }

            // TODO: 2022/4/19 with dependencies
            @Test
            @DisplayName("should bind type to a class with inject constructor")
            public void should_bind_type_to_a_class_with_inject_constructor() {
                Dependency dependency = new Dependency() {
                };

                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, dependency);

                Component instance = context.get(Component.class);
                assertNotNull(instance);
                assertSame(dependency, ((ComponentWithInjectConstructor)instance).getDependency());
            }

            // TODO: 2022/4/19 A -> B -> C
            @Test
            @DisplayName("should bind type to a class with transitive_dependencies")
            public void should_bind_type_to_a_class_with_transitive_dependencies(){
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectConstructor.class);
                context.bind(String.class, "indirect dependency");

                Component instance = context.get(Component.class);
                assertNotNull(instance);

                Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
                assertNotNull(dependency);

                assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
            }

        }

        @Nested
        public class FieldInjectionTest {
        }

        @Nested
        public class MethodInjectionTest {
        }

    }

    @Nested
    public class DependenciesSelectionTest {
    }

    @Nested
    public class LifecycleManagementTest {
    }

}

interface Component {
}

class ComponentWithDefaultConstructor implements Component {

    public ComponentWithDefaultConstructor() {
    }
}

interface Dependency {
}

class ComponentWithInjectConstructor implements Component {
    private Dependency dependency;

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}

class DependencyWithInjectConstructor implements Dependency{

    @Inject
    public DependencyWithInjectConstructor(String dependency) {
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }

    private String dependency;
}
