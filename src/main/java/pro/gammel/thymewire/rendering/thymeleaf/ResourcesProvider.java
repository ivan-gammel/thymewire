package pro.gammel.thymewire.rendering.thymeleaf;

import com.github.resource4j.objects.providers.AbstractFileResourceObjectProvider;
import com.github.resource4j.resources.RefreshableResources;

import static com.github.resource4j.objects.providers.resolvers.DefaultObjectNameResolver.javaPropertiesLocaleResolver;
import static com.github.resource4j.resources.ResourcesConfigurationBuilder.configure;
import static com.github.resource4j.resources.processors.BasicValuePostProcessor.macroSubstitution;

public class ResourcesProvider {

    public static RefreshableResources get(AbstractFileResourceObjectProvider<?> provider) {
        AbstractFileResourceObjectProvider<?> javaStyleProvider = provider.with(javaPropertiesLocaleResolver());
        var configuration = configure()
                .sources(javaStyleProvider)
                .postProcessingBy(macroSubstitution())
                .get();
        return new RefreshableResources(configuration);
    }

}
