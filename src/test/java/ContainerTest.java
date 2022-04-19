import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    interface Component{ }

    static class ComponentWithDefaultConstructor implements Component{
        public ComponentWithDefaultConstructor() {
        }
    }

    @Nested
    public class ComponentConstructionTest{
        @Test
        @DisplayName("should bind type to a specific instance")
        public void should_bind_type_to_a_specific_instance() {
            //Given
            Component instance = new Component() {
            };
            Context context = new Context();
            //When
            context.bind(Component.class, instance);
            //Then
            assertSame(instance, context.get(Component.class));
        }


        // TODO: 2022/4/19 abstract class
        // TODO: 2022/4/19 interface
        @Nested
        public class ConstructorInjectionTest{
            // TODO: 2022/4/19 No args constructor
            @Test
            @DisplayName("should bind type to a class with default constructor")
            public void should_bind_type_to_a_class_with_default_constructor() {
                //Given
                Context context = new Context();
                //When
                context.bind(Component.class, ComponentWithDefaultConstructor.class);
                Component instance = context.get(Component.class);
                //Then
                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);
            }


            // TODO: 2022/4/19 with dependencies
            // TODO: 2022/4/19 A -> B -> C
        }

        @Nested
        public class FieldInjectionTest{ }

        @Nested
        public class MethodInjectionTest{ }

    }

    @Nested
    public class DependenciesSelectionTest{ }

    @Nested
    public class LifecycleManagementTest{ }

}
