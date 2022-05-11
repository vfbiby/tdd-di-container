import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InjectionTest {

    private final Dependency dependency = mock(Dependency.class);
    private final Context context = mock(Context.class);
    private final Provider<Dependency> dependencyProvider = mock(Provider.class);
    ParameterizedType dependencyProviderType;

    @BeforeEach
    public void setup() throws NoSuchFieldException {
        dependencyProviderType = (ParameterizedType) InjectionTest.class.getDeclaredField("dependencyProvider").getGenericType();
        when(context.get(eq(Context.Ref.of(Dependency.class)))).thenReturn(Optional.of(dependency));
        when(context.get(eq(Context.Ref.of(dependencyProviderType)))).thenReturn(Optional.of(dependencyProvider));
    }

    @Nested
    public class ConstructorInjection {

        @Nested
        public class Injection {

            // No args constructor
            @Test
            @DisplayName("should call default constructor if no inject constructor")
            public void should_call_default_constructor_if_no_inject_constructor() {
                ComponentWithDefaultConstructor instance = new InjectionProvider<>(ComponentWithDefaultConstructor.class).get(context);
                assertNotNull(instance);
            }

            // with dependencies
            @Test
            @DisplayName("should inject dependency via inject constructor")
            public void should_inject_dependency_via_inject_constructor() {
                ComponentWithInjectConstructor instance = new InjectionProvider<>(ComponentWithInjectConstructor.class).get(context);
                assertSame(dependency, instance.getDependency());
            }

            @Test
            @DisplayName("should include dependency from inject constructor")
            public void should_include_dependency_from_inject_constructor() {
                InjectionProvider<ComponentWithInjectConstructor> provider = new InjectionProvider<>(ComponentWithInjectConstructor.class);
                assertArrayEquals(new Context.Ref[]{Context.Ref.of(Dependency.class)}, provider.getDependencies().toArray(Context.Ref[]::new));
            }

            // include dependency type from inject constructor
            @Test
            @DisplayName("should include provider type from inject constructor")
            public void should_include_provider_type_from_inject_constructor() {
                InjectionProvider<ProviderInjectConstructor> provider = new InjectionProvider<>(ProviderInjectConstructor.class);
                assertArrayEquals(new Context.Ref[]{Context.Ref.of(dependencyProviderType)}, provider.getDependencies().toArray(Context.Ref[]::new));
            }

            //support inject constructor
            static class ProviderInjectConstructor {
                Provider<Dependency> dependency;

                @Inject
                public ProviderInjectConstructor(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            @DisplayName("should inject provider via inject constructor")
            public void should_inject_provider_via_inject_constructor() {
                InjectionProvider<ProviderInjectConstructor> provider = new InjectionProvider<>(ProviderInjectConstructor.class);
                ProviderInjectConstructor instance = provider.get(context);
                assertSame(dependencyProvider, instance.dependency);
            }

        }

        @Nested
        public class IllegalInjectionConstructor {

            // abstract class
            abstract class AbstractComponent implements Component {
                @Inject
                public AbstractComponent() {
                }
            }

            @Test
            @DisplayName("should throw exception if component is abstract")
            public void should_throw_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(AbstractComponent.class));
            }

            // interface
            @Test
            @DisplayName("should throw exception if component is interface")
            public void should_throw_exception_if_component_is_interface() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(Component.class));
            }

            // multi inject constructors
            @Test
            @DisplayName("should throw exception if multi inject constructors provided")
            public void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    new InjectionProvider<>(ComponentWithMultiInjectConstructors.class);
                });
            }

            // no default constructor and inject constructor
            @Test
            @DisplayName("should throw exception if no inject nor default constructor provided")
            public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    new InjectionProvider<>(ComponentWithNoInjectConstructorNorDefaultConstructor.class);
                });
            }

        }

    }

    @Nested
    public class FieldInjection {

        static class ComponentWithFieldInjection {
            @Inject
            Dependency dependency;
        }

        @Nested
        public class Injection {

            @Test
            @DisplayName("should include dependency from field dependency")
            public void should_include_dependency_from_field_dependency() {
                InjectionProvider<ComponentWithFieldInjection> provider = new InjectionProvider<>(ComponentWithFieldInjection.class);
                assertArrayEquals(new Context.Ref[]{Context.Ref.of(Dependency.class)}, provider.getDependencies().toArray(Context.Ref[]::new));
            }

            // inject field
            @Test
            @DisplayName("should inject dependency via field")
            public void should_inject_dependency_via_field() {
                ComponentWithFieldInjection component = new InjectionProvider<>(ComponentWithFieldInjection.class).get(context);
                assertSame(dependency, component.dependency);
            }

            static class SubclassWithFieldInjection extends ComponentWithFieldInjection {
            }

            @Test
            public void should_inject_dependency_via_superclass_inject_field() {
                SubclassWithFieldInjection component = new InjectionProvider<>(SubclassWithFieldInjection.class).get(context);
                assertSame(dependency, component.dependency);
            }

            //support inject field
            static class ProviderInjectField {
                @Inject
                Provider<Dependency> dependency;
            }

            @Test
            @DisplayName("should inject provider via inject field")
            public void should_inject_provider_via_inject_field() {
                InjectionProvider<ProviderInjectField> provider = new InjectionProvider<>(ProviderInjectField.class);
                ProviderInjectField instance = provider.get(context);
                assertSame(dependencyProvider, instance.dependency);
            }

            // include dependency type from inject field
            @Test
            @DisplayName("should include provider type from inject field")
            public void should_include_provider_type_from_inject_field() {
                InjectionProvider<ProviderInjectField> provider = new InjectionProvider<>(ProviderInjectField.class);
                assertArrayEquals(new Context.Ref[]{Context.Ref.of(dependencyProviderType)}, provider.getDependencies().toArray(Context.Ref[]::new));
            }


        }

        @Nested
        public class IllegalInjectFields {

            // throw exception if field is final
            static class FinalInjectField {
                @Inject
                final Dependency dependency = null;
            }

            @Test
            @DisplayName("should throw exception if inject field is final")
            public void should_throw_exception_if_inject_field_is_final() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(FinalInjectField.class));
            }

        }

    }

    @Nested
    public class MethodInjection {

        @Nested
        public class Injection {

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
                InjectMethodWithNoDependency component = new InjectionProvider<>(InjectMethodWithNoDependency.class).get(context);
                assertTrue(component.called);
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
                InjectMethodWithDependency component = new InjectionProvider<>(InjectMethodWithDependency.class).get(context);
                assertSame(dependency, component.dependency);
            }

            static class SubclassWithInjectMethod extends SuperClassWithInjectMethod {
                int subCalled = 0;

                @Inject
                void Install() {
                    subCalled = superCalled + 1;
                }
            }

            @Test
            @DisplayName("should inject dependencies via inject method from superclass")
            public void should_inject_dependencies_via_inject_method_from_superclass() {
                SubclassWithInjectMethod component = new InjectionProvider<>(SubclassWithInjectMethod.class).get(context);
                assertEquals(1, component.superCalled);
                assertEquals(2, component.subCalled);
            }

            static class SubclassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {
                void install() {
                    super.install();
                }
            }

            @Test
            @DisplayName("should not call inject method if override with no inject")
            public void should_not_call_inject_method_if_override_with_no_inject() {
                SubclassOverrideSuperClassWithNoInject component = new InjectionProvider<>(SubclassOverrideSuperClassWithNoInject.class).get(context);
                assertEquals(0, component.superCalled);
            }

            // override inject method from superclass
            static class SuperClassWithInjectMethod {
                int superCalled = 0;

                @Inject
                void install() {
                    superCalled++;
                }
            }

            static class SubclassOverrideSuperclassWithInject extends SuperClassWithInjectMethod {
                @Inject
                void install() {
                    super.install();
                }

            }

            @Test
            @DisplayName("should only call one if subclass override inject method with inject")
            public void should_only_call_one_if_subclass_override_inject_method_with_inject() {
                SubclassOverrideSuperclassWithInject component = new InjectionProvider<>(SubclassOverrideSuperclassWithInject.class).get(context);
                assertEquals(1, component.superCalled);
            }

            @Test
            @DisplayName("should include dependencies from inject method")
            public void should_include_dependencies_from_inject_method() {
                InjectionProvider<InjectMethodWithDependency> provider = new InjectionProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new Context.Ref[]{Context.Ref.of(Dependency.class)}, provider.getDependencies().toArray(Context.Ref[]::new));
            }

            // include dependency type from inject method
            @Test
            @DisplayName("should include provider type from inject method")
            public void should_include_provider_type_from_inject_method() {
                InjectionProvider<ProviderInjectMethod> provider = new InjectionProvider<>(ProviderInjectMethod.class);
                assertArrayEquals(new Context.Ref[]{Context.Ref.of(dependencyProviderType)}, provider.getDependencies().toArray(Context.Ref[]::new));
            }

            //support inject method
            static class ProviderInjectMethod {
                private Provider<Dependency> dependency;

                @Inject
                void install(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            @DisplayName("should inject provider via inject Method")
            public void should_inject_provider_via_inject_Method() {
                InjectionProvider<ProviderInjectMethod> provider = new InjectionProvider<>(ProviderInjectMethod.class);
                ProviderInjectMethod instance = provider.get(context);
                assertSame(dependencyProvider, instance.dependency);
            }

        }

        @Nested
        public class IllegalInjectMethod {

            // throw exception if type parameter defined
            static class InjectMethodWithTypeParameter {
                @Inject
                <T> void install() {
                }
            }

            @Test
            @DisplayName("should throw exception if inject method has type parameter")
            public void should_throw_exception_if_inject_method_has_type_parameter() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(InjectMethodWithTypeParameter.class));
            }

        }
    }

    static class ComponentWithMultiInjectConstructors implements Component {
        @Inject
        public ComponentWithMultiInjectConstructors(String name) {
        }

        @Inject
        public ComponentWithMultiInjectConstructors(String name, double value) {
        }
    }

    static class ComponentWithNoInjectConstructorNorDefaultConstructor implements Component {
        public ComponentWithNoInjectConstructorNorDefaultConstructor(String name) {
        }
    }

    static class ComponentWithInjectConstructor implements Component {
        private Dependency dependency;

        @Inject
        public ComponentWithInjectConstructor(Dependency dependency) {
            this.dependency = dependency;
        }

        public Dependency getDependency() {
            return dependency;
        }
    }

    static class ComponentWithDefaultConstructor implements Component {

        public ComponentWithDefaultConstructor() {
        }

    }
}
