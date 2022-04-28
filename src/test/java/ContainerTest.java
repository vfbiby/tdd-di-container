import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
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
            config.bind(Component.class, instance);
            //Then
            assertSame(instance, config.getContext().get(Component.class).get());
        }


        // abstract class

        abstract class AbstractComponent implements Component {
            @Inject
            public AbstractComponent() {
            }
        }

        @Test
        @DisplayName("should throw exception if component is abstract")
        public void should_throw_exception_if_component_is_abstract() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(AbstractComponent.class));
        }

        // interface

        @Test
        @DisplayName("should throw exception if component is interface")
        public void should_throw_exception_if_component_is_interface() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(Component.class));
        }

        // component does not exist
        @Test
        public void should_return_empty_if_component_not_found() {
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isEmpty());
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
                    config.bind(Component.class, ComponentWithMultiInjectConstructors.class);
                });
            }

            // no default constructor and inject constructor
            @Test
            @DisplayName("should throw exception if no inject nor default constructor provided")
            public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithNoInjectConstructorNorDefaultConstructor.class);
                });
            }

            // dependencies not exist
            @Test
            @DisplayName("should throw exception if dependency not found")
            public void should_throw_exception_if_dependency_not_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                    config.getContext();
                });
                assertEquals(Dependency.class, exception.getDependency());
                assertEquals(Component.class, exception.getComponent());
            }

            @Test
            @DisplayName("should throw exception if transitive dependency not found")
            public void should_throw_exception_if_transitive_dependency_not_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectConstructor.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                    config.getContext();
                });
                assertEquals(String.class, exception.getDependency());
                assertEquals(Dependency.class, exception.getComponent());
            }

            // cycle dependencies
            @Test
            @DisplayName("should throw exception if cyclic dependencies found")
            public void should_throw_exception_if_cyclic_dependencies_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnComponent.class);

                CyclicDependencyFoundException exception = assertThrows(CyclicDependencyFoundException.class, () ->
                        config.getContext());
                Set<Class<?>> classes = Sets.newSet(exception.getComponents());

                assertEquals(2, classes.size());
                assertTrue(classes.contains(Component.class));
                assertTrue(classes.contains(Dependency.class));
            }

            @Test
            @DisplayName("should throw exception if transitive cyclic dependencies found")
            public void should_throw_exception_if_transitive_cyclic_dependencies_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
                config.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);

                CyclicDependencyFoundException exception = assertThrows(CyclicDependencyFoundException.class, () ->
                        config.getContext());
                List<Class<?>> components = asList(exception.getComponents());

                assertEquals(3, components.size());
                assertTrue(components.contains(Component.class));
                assertTrue(components.contains(Dependency.class));
                assertTrue(components.contains(AnotherDependency.class));
            }

        }

        @Nested
        public class FieldInjectionTest {
            static class ComponentWithFieldInjection {
                @Inject
                Dependency dependency;
            }

            static class SubclassWithFieldInjection extends ComponentWithFieldInjection {
            }

            // inject field
            @Test
            @DisplayName("should inject dependency via field")
            public void should_inject_dependency_via_field() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);

                ComponentWithFieldInjection component = config.getContext().get(ComponentWithFieldInjection.class).get();
                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_inject_dependency_via_superclass_inject_field() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(SubclassWithFieldInjection.class, SubclassWithFieldInjection.class);

                SubclassWithFieldInjection component = config.getContext().get(SubclassWithFieldInjection.class).get();
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
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(FinalInjectField.class));
            }

            @Test
            @DisplayName("should include field dependency in dependencies")
            public void should_include_field_dependency_in_dependencies() {
                ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
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
                config.bind(InjectMethodWithNoDependency.class, InjectMethodWithNoDependency.class);
                InjectMethodWithNoDependency component = config.getContext().get(InjectMethodWithNoDependency.class).get();
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

            static class SubclassWithInjectMethod extends SuperClassWithInjectMethod {
                int subCalled = 0;

                @Inject
                void Install() {
                    subCalled = superCalled + 1;
                }
            }

            static class SubclassOverrideSuperclassWithInject extends SuperClassWithInjectMethod {
                @Inject
                void install() {
                    super.install();
                }
            }

            @Test
            @DisplayName("should inject dependencies via inject method from superclass")
            public void should_inject_dependencies_via_inject_method_from_superclass() {
                config.bind(SubclassWithInjectMethod.class, SubclassWithInjectMethod.class);
                SubclassWithInjectMethod component = config.getContext().get(SubclassWithInjectMethod.class).get();
                assertEquals(1, component.superCalled);
                assertEquals(2, component.subCalled);
            }

            @Test
            @DisplayName("should only call one if subclass override inject method with inject")
            public void should_only_call_one_if_subclass_override_inject_method_with_inject() {
                config.bind(SubclassOverrideSuperclassWithInject.class, SubclassOverrideSuperclassWithInject.class);
                SubclassOverrideSuperclassWithInject component = config.getContext().get(SubclassOverrideSuperclassWithInject.class).get();
                assertEquals(1, component.superCalled);
            }

            static class SubclassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {
                void install() {
                    super.install();
                }
            }

            @Test
            @DisplayName("should not call inject method if override with no inject")
            public void should_not_call_inject_method_if_override_with_no_inject() {
                config.bind(SubclassOverrideSuperClassWithNoInject.class, SubclassOverrideSuperClassWithNoInject.class);
                SubclassOverrideSuperClassWithNoInject component = config.getContext().get(SubclassOverrideSuperClassWithNoInject.class).get();
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
                config.bind(InjectMethodWithDependency.class, InjectMethodWithDependency.class);
                InjectMethodWithDependency component = config.getContext().get(InjectMethodWithDependency.class).get();

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
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(InjectMethodWithTypeParameter.class));
            }

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

interface Dependency {
}

interface AnotherDependency {
}

class ComponentWithDefaultConstructor implements Component {

    public ComponentWithDefaultConstructor() {
    }

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

class DependencyWithInjectConstructor implements Dependency {

    @Inject
    public DependencyWithInjectConstructor(String dependency) {
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }

    private String dependency;
}

class ComponentWithMultiInjectConstructors implements Component {
    @Inject
    public ComponentWithMultiInjectConstructors(String name) {
    }

    @Inject
    public ComponentWithMultiInjectConstructors(String name, double value) {
    }
}

class ComponentWithNoInjectConstructorNorDefaultConstructor implements Component {
    public ComponentWithNoInjectConstructorNorDefaultConstructor(String name) {
    }
}

class DependencyDependedOnComponent implements Dependency {
    public Component getComponent() {
        return component;
    }

    @Inject
    public DependencyDependedOnComponent(Component component) {
        this.component = component;
    }

    private Component component;
}

class AnotherDependencyDependedOnComponent implements AnotherDependency {
    public Component getComponent() {
        return component;
    }

    @Inject
    public AnotherDependencyDependedOnComponent(Component component) {
        this.component = component;
    }

    private Component component;
}

class DependencyDependedOnAnotherDependency implements Dependency {
    public AnotherDependency getAnotherDependency() {
        return anotherDependency;
    }

    @Inject
    public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }

    private AnotherDependency anotherDependency;
}