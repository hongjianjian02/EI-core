package com.mysterious.mystic.compat.anvilguide.util;

import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.List;

/** Replace only the owned query pages; keep other providers' order and never mutate their result lists. */
public final class QueryResults {
    private QueryResults() {
    }

    public static <T> List<T> replace(List<T> original, Predicate<T> owned, List<? extends T> focused) {
        List<T> results = new ArrayList<>(original.size() + focused.size());
        original.stream().filter(recipe -> !owned.test(recipe)).forEach(results::add);
        results.addAll(focused);
        return List.copyOf(results);
    }
}
