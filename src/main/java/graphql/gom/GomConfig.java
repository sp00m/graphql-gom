package graphql.gom;

import graphql.schema.idl.RuntimeWiring;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoaderRegistry;

import java.util.Collection;
import java.util.HashSet;

import static graphql.gom.utils.Reductions.fail;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;
import static java.util.stream.Collectors.groupingBy;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public final class GomConfig {

    @NoArgsConstructor(access = PRIVATE)
    public static final class Builder<C extends DataLoaderRegistryGetter> {

        private Collection<Object> resolvers = new HashSet<>();
        private GomConverters<C> converters = GomConverters.<C>newGomConverters().build();

        public Builder<C> resolvers(Collection<Object> resolvers) {
            this.resolvers = resolvers;
            return this;
        }

        public Builder<C> converters(GomConverters<C> converters) {
            this.converters = converters;
            return this;
        }

        public GomConfig build() {
            GomResolverInspection<C> inspection = GomResolverInspection.inspect(resolvers, converters);
            return new GomConfig(inspection.getFieldWirings(), inspection.getDataLoaderRegistrars());
        }

    }

    private final Collection<FieldWiring> fieldWirings;

    private final Collection<DataLoaderRegistrar> dataLoaderRegistrars;

    public void decorateRuntimeWiringBuilder(RuntimeWiring.Builder builder) {
        fieldWirings
                .stream()
                .collect(groupingBy(FieldWiring::getTypeName))
                .entrySet()
                .stream()
                .map(entry -> entry
                        .getValue()
                        .stream()
                        .reduce(
                                newTypeWiring(entry.getKey()),
                                (typeWiring, fieldWiring) -> typeWiring.dataFetcher(fieldWiring.getFieldName(), fieldWiring.getDataFetcher()),
                                fail()
                        ))
                .forEach(builder::type);
    }

    public void decorateDataLoaderRegistry(DataLoaderRegistry registry) {
        dataLoaderRegistrars.forEach(registrar -> registrar.register(registry));
    }

    public static <C extends DataLoaderRegistryGetter> Builder<C> newGomConfig() {
        return new Builder<>();
    }

}
