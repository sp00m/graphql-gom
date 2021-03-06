package com.qudini.gom;

import com.qudini.gom.utils.Context;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.qudini.gom.Gom.newGom;
import static com.qudini.gom.utils.QueryRunner.callExpectingData;
import static com.qudini.gom.utils.QueryRunner.callExpectingErrors;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.PRIVATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class DataLoaderTest {

    @RequiredArgsConstructor(access = PRIVATE)
    @Getter
    public static final class MyType {

        private final String name;

    }

    @RequiredArgsConstructor(access = PRIVATE)
    @TypeResolver("Query")
    public static final class QueryResolver {

        private final boolean fooFirst;

        @FieldResolver("myTypes")
        public List<MyType> myTypes() {
            return asList(
                    fooFirst ? new MyType("foo") : new MyType("bar"),
                    fooFirst ? new MyType("bar") : new MyType("foo")
            );
        }

    }

    @Test
    public void withSources() {
        AtomicInteger callCount = new AtomicInteger(0);
        @NoArgsConstructor(access = PRIVATE)
        @TypeResolver("MyType")
        final class MyTypeResolver {

            @Batched
            @FieldResolver("name")
            public Map<MyType, String> name(Set<MyType> myTypes) {
                callCount.incrementAndGet();
                return myTypes
                        .stream()
                        .collect(toMap(
                                identity(),
                                myType -> myType.getName() + "bar"
                        ));
            }

        }
        Gom gom = newGom()
                .resolvers(asList(new QueryResolver(true), new MyTypeResolver()))
                .build();
        List<Map<String, Object>> myTypes = (List<Map<String, Object>>) callExpectingData(gom, new Context()).get("myTypes");
        assertEquals("foobar", myTypes.get(0).get("name"));
        assertEquals("barbar", myTypes.get(1).get("name"));
        assertEquals(1, callCount.get());
    }

    @Test
    public void withArguments() {
        AtomicInteger callCount = new AtomicInteger(0);
        @NoArgsConstructor(access = PRIVATE)
        @TypeResolver("MyType")
        final class MyTypeResolver {

            @Batched
            @FieldResolver("name")
            public Map<MyType, String> name(Arguments arguments) {
                callCount.incrementAndGet();
                return new HashMap<>();
            }

        }
        Gom gom = newGom()
                .resolvers(asList(new QueryResolver(true), new MyTypeResolver()))
                .build();
        assertFalse(callExpectingErrors(gom, Context::new).isEmpty());
        assertEquals(0, callCount.get());
    }

    @Test
    public void withSelection() {
        AtomicInteger callCount = new AtomicInteger(0);
        @NoArgsConstructor(access = PRIVATE)
        @TypeResolver("MyType")
        final class MyTypeResolver {

            @Batched
            @FieldResolver("name")
            public Map<MyType, String> name(Selection selection) {
                callCount.incrementAndGet();
                return new HashMap<>();
            }

        }
        Gom gom = newGom()
                .resolvers(asList(new QueryResolver(true), new MyTypeResolver()))
                .build();
        assertFalse(callExpectingErrors(gom, Context::new).isEmpty());
        assertEquals(0, callCount.get());
    }

    @Test
    public void withSourcesAndArguments() {
        AtomicInteger callCount = new AtomicInteger(0);
        @NoArgsConstructor(access = PRIVATE)
        @TypeResolver("MyType")
        final class MyTypeResolver {

            @Batched
            @FieldResolver("name")
            public Map<MyType, String> name(Set<MyType> myTypes, Arguments arguments) {
                callCount.incrementAndGet();
                return myTypes
                        .stream()
                        .collect(toMap(
                                identity(),
                                myType -> myType.getName() + arguments.get("suffix")
                        ));
            }

        }
        Gom gom = newGom()
                .resolvers(asList(new QueryResolver(true), new MyTypeResolver()))
                .build();
        List<Map<String, Object>> myTypes = (List<Map<String, Object>>) callExpectingData(gom, new Context()).get("myTypes");
        assertEquals("foobar", myTypes.get(0).get("name"));
        assertEquals("barbar", myTypes.get(1).get("name"));
        assertEquals(1, callCount.get());
    }

    @Test
    public void withSourcesAndSelection() {
        AtomicInteger callCount = new AtomicInteger(0);
        AtomicBoolean validSelection = new AtomicBoolean(false);
        @RequiredArgsConstructor(access = PRIVATE)
        @Getter
        final class MyName {

            private final String value;

        }
        @NoArgsConstructor(access = PRIVATE)
        @TypeResolver("MyType")
        final class MyTypeResolver {

            @Batched
            @FieldResolver("name")
            public Map<MyType, MyName> name(Set<MyType> myTypes, Selection selection) {
                callCount.incrementAndGet();
                validSelection.set(selection.contains("value"));
                return myTypes
                        .stream()
                        .collect(toMap(
                                identity(),
                                myType -> new MyName(myType.getName())
                        ));
            }

        }
        Gom gom = newGom()
                .resolvers(asList(new QueryResolver(true), new MyTypeResolver()))
                .build();
        List<Map<String, Map<String, Object>>> myTypes = (List<Map<String, Map<String, Object>>>) callExpectingData(gom, new Context()).get("myTypes");
        assertEquals("foo", myTypes.get(0).get("name").get("value"));
        assertEquals("bar", myTypes.get(1).get("name").get("value"));
        assertEquals(1, callCount.get());
        assertTrue(validSelection.get());
    }

    @Test
    public void withSourcesAndDeeperSelection() {
        AtomicInteger callCount = new AtomicInteger(0);
        AtomicBoolean validSelection = new AtomicBoolean(false);
        @RequiredArgsConstructor(access = PRIVATE)
        @Getter
        final class MyName {

            private final String value;
            private final String content;

            MyName(String value) {
                this(value, value);
            }

            public MyName getSelf() {
                return this;
            }

        }
        @NoArgsConstructor(access = PRIVATE)
        @TypeResolver("MyType")
        final class MyTypeResolver {

            @Batched
            @FieldResolver("name")
            public Map<MyType, MyName> name(Set<MyType> myTypes, @Depth(2) Selection selection) {
                callCount.incrementAndGet();
                validSelection.set(
                        selection.size() == 5
                                && selection.contains("value")
                                && selection.contains("self")
                                && selection.contains("self/value")
                                && selection.contains("self/content")
                                && selection.contains("self/self")
                );
                return myTypes
                        .stream()
                        .collect(toMap(
                                identity(),
                                myType -> new MyName(myType.getName())
                        ));
            }

        }
        Gom gom = newGom()
                .resolvers(asList(new QueryResolver(true), new MyTypeResolver()))
                .build();
        List<Map<String, Map<String, Object>>> myTypes = (List<Map<String, Map<String, Object>>>) callExpectingData(gom, new Context()).get("myTypes");
        assertEquals("foo", myTypes.get(0).get("name").get("value"));
        assertEquals("bar", myTypes.get(1).get("name").get("value"));
        assertEquals(1, callCount.get());
        assertTrue(validSelection.get());
    }

    @Test
    public void withSourcesArgumentsAndSelection() {
        AtomicInteger callCount = new AtomicInteger(0);
        AtomicBoolean validSelection = new AtomicBoolean(false);
        @RequiredArgsConstructor(access = PRIVATE)
        @Getter
        final class MyName {

            private final String value;

        }
        @NoArgsConstructor(access = PRIVATE)
        @TypeResolver("MyType")
        final class MyTypeResolver {

            @Batched
            @FieldResolver("name")
            public Map<MyType, MyName> name(Set<MyType> myTypes, Arguments arguments, Selection selection) {
                callCount.incrementAndGet();
                validSelection.set(selection.contains("value"));
                return myTypes
                        .stream()
                        .collect(toMap(
                                identity(),
                                myType -> new MyName(myType.getName() + arguments.get("suffix"))
                        ));
            }

        }
        Gom gom = newGom()
                .resolvers(asList(new QueryResolver(true), new MyTypeResolver()))
                .build();
        List<Map<String, Map<String, Object>>> myTypes = (List<Map<String, Map<String, Object>>>) callExpectingData(gom, new Context()).get("myTypes");
        assertEquals("foobar", myTypes.get(0).get("name").get("value"));
        assertEquals("barbar", myTypes.get(1).get("name").get("value"));
        assertEquals(1, callCount.get());
        assertTrue(validSelection.get());
    }

    @Test
    public void sourceOrder() {
        AtomicInteger callCount = new AtomicInteger(0);
        @NoArgsConstructor(access = PRIVATE)
        @TypeResolver("MyType")
        final class MyTypeResolver {

            @Batched
            @FieldResolver("name")
            public Map<MyType, String> name(Set<MyType> myTypes) {
                callCount.incrementAndGet();
                return myTypes
                        .stream()
                        .collect(toMap(
                                identity(),
                                myType -> myType.getName() + "bar"
                        ));
            }

        }
        Gom gom = newGom()
                .resolvers(asList(new QueryResolver(false), new MyTypeResolver()))
                .build();
        List<Map<String, Object>> myTypes = (List<Map<String, Object>>) callExpectingData(gom, new Context()).get("myTypes");
        assertEquals("barbar", myTypes.get(0).get("name"));
        assertEquals("foobar", myTypes.get(1).get("name"));
        assertEquals(1, callCount.get());
    }

    @Test
    public void distinctByArguments() {
        AtomicInteger callCount = new AtomicInteger(0);
        @NoArgsConstructor(access = PRIVATE)
        @TypeResolver("MyType")
        final class MyTypeResolver {

            @Batched
            @FieldResolver("name")
            public Map<MyType, String> name(Set<MyType> myTypes, Arguments arguments) {
                callCount.incrementAndGet();
                return myTypes
                        .stream()
                        .collect(toMap(
                                identity(),
                                myType -> myType.getName() + arguments.getOptional("suffix").orElse("")
                        ));
            }

        }
        Gom gom = newGom()
                .resolvers(asList(new QueryResolver(true), new MyTypeResolver()))
                .build();
        List<Map<String, Object>> myTypes = (List<Map<String, Object>>) callExpectingData(gom, new Context()).get("myTypes");
        assertEquals("foo", myTypes.get(0).get("nameWithoutSuffix"));
        assertEquals("foofoo", myTypes.get(0).get("nameWithFooSuffix"));
        assertEquals("foobar", myTypes.get(0).get("nameWithBarSuffix"));
        assertEquals("bar", myTypes.get(1).get("nameWithoutSuffix"));
        assertEquals("barfoo", myTypes.get(1).get("nameWithFooSuffix"));
        assertEquals("barbar", myTypes.get(1).get("nameWithBarSuffix"));
        assertEquals(3, callCount.get());
    }

    @Test
    public void sameByArguments() {
        AtomicInteger count = new AtomicInteger(0);
        @NoArgsConstructor(access = PRIVATE)
        @TypeResolver("MyType")
        final class MyTypeResolver {

            @Batched
            @FieldResolver("name")
            public Map<MyType, String> name(Set<MyType> myTypes, Arguments arguments) {
                count.incrementAndGet();
                return myTypes
                        .stream()
                        .collect(toMap(
                                identity(),
                                myType -> myType.getName() + arguments.get("suffix")
                        ));
            }

        }
        Gom gom = newGom()
                .resolvers(asList(new QueryResolver(true), new MyTypeResolver()))
                .build();
        List<Map<String, Object>> myTypes = (List<Map<String, Object>>) callExpectingData(gom, new Context()).get("myTypes");
        assertEquals("foobar", myTypes.get(0).get("nameWithSuffix1"));
        assertEquals("foobar", myTypes.get(0).get("nameWithSuffix2"));
        assertEquals("barbar", myTypes.get(1).get("nameWithSuffix1"));
        assertEquals("barbar", myTypes.get(1).get("nameWithSuffix2"));
        assertEquals(1, count.get());
    }

    @Test
    public void distinctBySelection() {
        AtomicInteger callCount = new AtomicInteger(0);
        @RequiredArgsConstructor(access = PRIVATE)
        @Getter
        final class MyName {

            private final int id;

            private final String value;

        }
        @NoArgsConstructor(access = PRIVATE)
        @TypeResolver("MyType")
        final class MyTypeResolver {

            @Batched
            @FieldResolver("name")
            public Map<MyType, MyName> name(Set<MyType> myTypes, Selection selection) {
                callCount.incrementAndGet();
                return myTypes
                        .stream()
                        .collect(toMap(
                                identity(),
                                myType -> new MyName(selection.contains("id") ? 1 : 2, myType.getName())
                        ));
            }

        }
        Gom gom = newGom()
                .resolvers(asList(new QueryResolver(true), new MyTypeResolver()))
                .build();
        List<Map<String, Map<String, Object>>> myTypes = (List<Map<String, Map<String, Object>>>) callExpectingData(gom, new Context()).get("myTypes");
        assertEquals(1, myTypes.get(0).get("nameId").get("id"));
        assertNull(myTypes.get(0).get("nameId").get("value"));
        assertNull(myTypes.get(1).get("nameValue").get("id"));
        assertEquals("bar", myTypes.get(1).get("nameValue").get("value"));
        assertEquals(2, callCount.get());
    }

    @Test
    public void sameBySelection() {
        AtomicInteger callCount = new AtomicInteger(0);
        @RequiredArgsConstructor(access = PRIVATE)
        @Getter
        final class MyName {

            private final int id;

            private final String value;

        }
        @NoArgsConstructor(access = PRIVATE)
        @TypeResolver("MyType")
        final class MyTypeResolver {

            @Batched
            @FieldResolver("name")
            public Map<MyType, MyName> name(Set<MyType> myTypes) {
                callCount.incrementAndGet();
                return myTypes
                        .stream()
                        .collect(toMap(
                                identity(),
                                myType -> new MyName("foo".equals(myType.getName()) ? 1 : 2, myType.getName())
                        ));
            }

        }
        Gom gom = newGom()
                .resolvers(asList(new QueryResolver(true), new MyTypeResolver()))
                .build();
        List<Map<String, Map<String, Object>>> myTypes = (List<Map<String, Map<String, Object>>>) callExpectingData(gom, new Context()).get("myTypes");
        assertEquals(1, myTypes.get(0).get("name1").get("id"));
        assertEquals("foo", myTypes.get(0).get("name1").get("value"));
        assertEquals(2, myTypes.get(1).get("name2").get("id"));
        assertEquals("bar", myTypes.get(1).get("name2").get("value"));
        assertEquals(1, callCount.get());
    }

    @Test
    public void sameByArgumentsAndSelection() {
        AtomicInteger callCount = new AtomicInteger(0);
        @RequiredArgsConstructor(access = PRIVATE)
        @Getter
        final class MyName {

            private final int id;

            private final String value;

        }
        @NoArgsConstructor(access = PRIVATE)
        @TypeResolver("MyType")
        final class MyTypeResolver {

            @Batched
            @FieldResolver("name")
            public Map<MyType, MyName> name(Set<MyType> myTypes, Arguments arguments) {
                callCount.incrementAndGet();
                return myTypes
                        .stream()
                        .collect(toMap(
                                identity(),
                                myType -> new MyName("foo".equals(myType.getName()) ? 1 : 2, myType.getName() + arguments.get("suffix"))
                        ));
            }

        }
        Gom gom = newGom()
                .resolvers(asList(new QueryResolver(true), new MyTypeResolver()))
                .build();
        List<Map<String, Map<String, Object>>> myTypes = (List<Map<String, Map<String, Object>>>) callExpectingData(gom, new Context()).get("myTypes");
        assertEquals(1, myTypes.get(0).get("name1").get("id"));
        assertEquals("foobar", myTypes.get(0).get("name1").get("value"));
        assertEquals(2, myTypes.get(1).get("name2").get("id"));
        assertEquals("barbar", myTypes.get(1).get("name2").get("value"));
        assertEquals(1, callCount.get());
    }

}
