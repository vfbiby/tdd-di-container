import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InjectionTest {

    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }

    @Nested
    public class ConstructorInjectionTest {
        // No args constructor
        @Test
        @DisplayName("should bind type to a class with default constructor")
        public void should_bind_type_to_a_class_with_default_constructor() {
            //When
            config.bind(Component.class, ComponentWithDefaultConstructor.class);
            Component instance = config.getContext().get(Component.class).get();
            //Then
            assertNotNull(instance);
            assertTrue(instance instanceof ComponentWithDefaultConstructor);
        }

        // with dependencies
        @Test
        @DisplayName("should bind type to a class with inject constructor")
        public void should_bind_type_to_a_class_with_inject_constructor() {
            Dependency dependency = new Dependency() {
            };

            config.bind(Component.class, ComponentWithInjectConstructor.class);
            config.bind(Dependency.class, dependency);

            Component instance = config.getContext().get(Component.class).get();
            assertNotNull(instance);
            assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
        }

        // A -> B -> C
        @Test
        @DisplayName("should bind type to a class with transitive_dependencies")
        public void should_bind_type_to_a_class_with_transitive_dependencies() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            config.bind(Dependency.class, DependencyWithInjectConstructor.class);
            config.bind(String.class, "indirect dependency");

            Component instance = config.getContext().get(Component.class).get();
            assertNotNull(instance);

            Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
            assertNotNull(dependency);

            assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
        }

        // multi inject constructors
        @Test
        @DisplayName("should throw exception if multi inject constructors provided")
        public void should_throw_exception_if_multi_inject_constructors_provided() {
            assertThrows(IllegalComponentException.class, () -> {
                new ConstructorInjectionProvider<>(ComponentWithMultiInjectConstructors.class);
            });
        }

        // no default constructor and inject constructor
        @Test
        @DisplayName("should throw exception if no inject nor default constructor provided")
        public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
            assertThrows(IllegalComponentException.class, () -> {
                new ConstructorInjectionProvider<>(ComponentWithNoInjectConstructorNorDefaultConstructor.class);
            });
        }

        @Test
        @DisplayName("should include dependency from inject constructor")
        public void should_include_dependency_from_inject_constructor() {
            ConstructorInjectionProvider<ComponentWithInjectConstructor> provider = new ConstructorInjectionProvider<>(ComponentWithInjectConstructor.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
        }

    }

    @Nested
    public class FieldInjectionTest {
        static class ComponentWithFieldInjection {
            @Inject
            Dependency dependency;
        }

        static class SubclassWithFieldInjection extends FieldInjectionTest.ComponentWithFieldInjection {
        }

        // inject field
        @Test
        @DisplayName("should inject dependency via field")
        public void should_inject_dependency_via_field() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(FieldInjectionTest.ComponentWithFieldInjection.class, FieldInjectionTest.ComponentWithFieldInjection.class);

            FieldInjectionTest.ComponentWithFieldInjection component = config.getContext().get(FieldInjectionTest.ComponentWithFieldInjection.class).get();
            assertSame(dependency, component.dependency);
        }

        @Test
        public void should_inject_dependency_via_superclass_inject_field() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(FieldInjectionTest.SubclassWithFieldInjection.class, FieldInjectionTest.SubclassWithFieldInjection.class);

            FieldInjectionTest.SubclassWithFieldInjection component = config.getContext().get(FieldInjectionTest.SubclassWithFieldInjection.class).get();
            assertSame(dependency, component.dependency);
        }

        // throw exception if field is final

        static class FinalInjectField {
            @Inject
            final Dependency dependency = null;
        }

        @Test
        @DisplayName("should throw exception if inject field is final")
        public void should_throw_exception_if_inject_field_is_final() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(FieldInjectionTest.FinalInjectField.class));
        }

        @Test
        @DisplayName("should include field dependency in dependencies")
        public void should_include_field_dependency_in_dependencies() {
            ConstructorInjectionProvider<FieldInjectionTest.ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(FieldInjectionTest.ComponentWithFieldInjection.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
        }

    }

    @Nested
    public class MethodInjectionTest {
        // inject method with no dependencies will be called

        static class InjectMethodWithNoDependency {
            boolean called = false;

            @Inject
            void install() {
                called = true;
            }
        }

        // inject method with dependencies will lbe called

        @Test
        @DisplayName("should call inject method even if no dependency declared")
        public void should_call_inject_method_even_if_no_dependency_declared() {
            config.bind(MethodInjectionTest.InjectMethodWithNoDependency.class, MethodInjectionTest.InjectMethodWithNoDependency.class);
            MethodInjectionTest.InjectMethodWithNoDependency component = config.getContext().get(MethodInjectionTest.InjectMethodWithNoDependency.class).get();
            assertTrue(component.called);
        }

        // override inject method from superclass
        static class SuperClassWithInjectMethod {
            int superCalled = 0;

            @Inject
            void install() {
                superCalled++;
            }
        }

        static class SubclassWithInjectMethod extends MethodInjectionTest.SuperClassWithInjectMethod {
            int subCalled = 0;

            @Inject
            void Install() {
                subCalled = superCalled + 1;
            }
        }

        static class SubclassOverrideSuperclassWithInject extends MethodInjectionTest.SuperClassWithInjectMethod {
            @Inject
            void install() {
                super.install();
            }
        }

        @Test
        @DisplayName("should inject dependencies via inject method from superclass")
        public void should_inject_dependencies_via_inject_method_from_superclass() {
            config.bind(MethodInjectionTest.SubclassWithInjectMethod.class, MethodInjectionTest.SubclassWithInjectMethod.class);
            MethodInjectionTest.SubclassWithInjectMethod component = config.getContext().get(MethodInjectionTest.SubclassWithInjectMethod.class).get();
            assertEquals(1, component.superCalled);
            assertEquals(2, component.subCalled);
        }

        @Test
        @DisplayName("should only call one if subclass override inject method with inject")
        public void should_only_call_one_if_subclass_override_inject_method_with_inject() {
            config.bind(MethodInjectionTest.SubclassOverrideSuperclassWithInject.class, MethodInjectionTest.SubclassOverrideSuperclassWithInject.class);
            MethodInjectionTest.SubclassOverrideSuperclassWithInject component = config.getContext().get(MethodInjectionTest.SubclassOverrideSuperclassWithInject.class).get();
            assertEquals(1, component.superCalled);
        }

        static class SubclassOverrideSuperClassWithNoInject extends MethodInjectionTest.SuperClassWithInjectMethod {
            void install() {
                super.install();
            }
        }

        @Test
        @DisplayName("should not call inject method if override with no inject")
        public void should_not_call_inject_method_if_override_with_no_inject() {
            config.bind(MethodInjectionTest.SubclassOverrideSuperClassWithNoInject.class, MethodInjectionTest.SubclassOverrideSuperClassWithNoInject.class);
            MethodInjectionTest.SubclassOverrideSuperClassWithNoInject component = config.getContext().get(MethodInjectionTest.SubclassOverrideSuperClassWithNoInject.class).get();
            assertEquals(0, component.superCalled);
        }

        static class InjectMethodWithDependency {
            Dependency dependency;

            @Inject
            void install(Dependency dependency) {
                this.dependency = dependency;
            }
        }

        @Test
        @DisplayName("should inject dependency via inject method")
        public void should_inject_dependency_via_inject_method() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(MethodInjectionTest.InjectMethodWithDependency.class, MethodInjectionTest.InjectMethodWithDependency.class);
            MethodInjectionTest.InjectMethodWithDependency component = config.getContext().get(MethodInjectionTest.InjectMethodWithDependency.class).get();

            assertSame(dependency, component.dependency);
        }

        // throw exception if type parameter defined

        static class InjectMethodWithTypeParameter {
            @Inject
            <T> void install() {
            }
        }

        @Test
        @DisplayName("should throw exception if inject method has type parameter")
        public void should_throw_exception_if_inject_method_has_type_parameter() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(MethodInjectionTest.InjectMethodWithTypeParameter.class));
        }

    }
}
