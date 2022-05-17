import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

public class ContainerTest {

    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }

    @Nested
    public class DependenciesSelectionTest {
    }

    @Nested
    public class LifecycleManagementTest {
    }

}

interface TestComponent {
    default Dependency dependency() {
        return null;
    }
}

interface Dependency {
}

interface AnotherDependency {
}

