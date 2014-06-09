package unisoft;

// See http://unriskinsight.blogspot.com/2014/06/fast-functional-goats-lions-and-wolves.html
// Sascha Kratky (kratky@unrisk.com), uni software plus GmbH & MathConsult GmbH
//
// compilation requires Java 8.
//
// compile with Oracle JDK 8:
// javac MagicForest.java
//
// run with Oracle JVM 8:
// java MagicForest 117 155 106

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MagicForest {
    private List<int[]> meals;

    public MagicForest(int[] ... meals) {
        this.meals = Arrays.asList(meals);
    }

    final class Forest implements Comparable<Forest> {
        private final int[] animals;

        public Forest(int[] animals) {
            this.animals = animals;
        }

        public int getGoats() {
            return animals[0];
        }

        public int getWolves() {
            return animals[1];
        }

        public int getLions() {
            return animals[2];
        }

        public int sum() {
            int count = 0;
            for(int i = animals.length-1; i>=0; i--) {
                count += animals[i];
            }
            return count;
        }

        public Optional<Forest> eat(int[] meal) {
            int[] next = new int[meal.length];
            for(int i = meal.length-1; i>=0; i--) {
                int n = animals[i] + meal[i];
                if(n < 0) return Optional.empty();
                next[i] = n;
            }
            return Optional.of(new Forest(next));
        }

        public Collection<Forest> meal(Collection<int[]> meals) {
            ArrayList<Forest> result = new ArrayList<>(meals.size());
            Consumer<Forest> nextForests = result::add;
            for(int[] meal : meals) {
                eat(meal).ifPresent(nextForests);
            }
            return result;
        }

        public boolean isStable() {
            for(int[] meal : meals()) {
                if(eat(meal).isPresent()) return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(animals);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (this.getClass() != obj.getClass())
                return false;
            return Arrays.equals(animals,((Forest)obj).animals);
        }

        @Override
        public String toString() {
            return "Forest " + Arrays.toString(animals);
        }

        @Override
        public int compareTo(Forest o) {
            for(int i = animals.length - 1; i>=0; i--) {
                int n = animals[i] - o.animals[i];
                if(n != 0) return n;
            }
            return 0;
        }
    }

    public Forest makeForest(int[] animals) {
        assert animals.length == meals().size();
        return new Forest(animals);
    }

    public List<int[]> meals() {
        return meals;
    }

    private static final BinaryOperator<? extends Collection> REDUCER = (a, i) -> {a.addAll(i); return a; };
    private static <T> BinaryOperator<Collection<T>> reducer() {
        return (BinaryOperator<Collection<T>>)REDUCER;
    }

    Collection<Forest> meal(Collection<Forest> forests) {
        return forests.stream().map(forest -> forest.meal(meals))
                .reduce(new HashSet<Forest>(forests.size() * 2), reducer());
    }

    Collection<Forest> parallelMeal(Collection<Forest> forests) {
        return forests.parallelStream().map(forest -> forest.meal(meals))
                .reduce(new ConcurrentSkipListSet<Forest>(), reducer());
    }

    Collection<Forest> pipelinedMeal(Collection<Forest> forests) {
        Function<int[],Stream<Collection<Forest>>> pipeline = it -> forests.stream().map(
                forest -> forest.meal(Collections.singletonList(it))
        );
        return meals.parallelStream().flatMap(pipeline)
                .reduce(new ConcurrentSkipListSet<Forest>(), reducer());
    }

    static boolean devouringPossible(Collection<Forest> forests) {
        return !forests.isEmpty() && !forests.stream().anyMatch(Forest::isStable);
    }

    static Collection<Forest> stableForests(Collection<Forest> forests) {
        return forests.stream().filter(Forest::isStable).collect(Collectors.toList());
    }

    public Collection<Forest> findStableForests(Forest forest, boolean parallel, boolean pipelined) {

        UnaryOperator<Collection<Forest>> iterateFunction;
        if(pipelined) {
            iterateFunction = this::pipelinedMeal;
        } else {
            iterateFunction = parallel?this::parallelMeal:this::meal;
        }

        Collection<Forest> initialForests = Collections.singletonList(forest);
        Optional<Collection<Forest>> solution =
                Stream.iterate(initialForests, iterateFunction).filter(
                        forests->!devouringPossible(forests)).findAny();
        return solution.isPresent()? stableForests(solution.get()) : Collections.emptyList();
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("USAGE: " + MagicForest.class.getSimpleName() +
                    " <goats> <wolves> <lions>");
            System.exit(-1);
        }
        MagicForest universe = new MagicForest(new int[][]
                {{-1,-1,1},
                {-1,1,-1},
                {1,-1,-1}}
        );
        try {
            Forest initialForest = universe.makeForest(new int[]{Integer.parseInt(args[0]),
                    Integer.parseInt(args[1]), Integer.parseInt(args[2])});
            Collection<Forest> stableForests = universe.findStableForests(initialForest, false, false);
            if (stableForests.isEmpty()) {
                System.out.println("no stable forests found.");
            }
            else {
                stableForests.forEach(forest -> System.out.println(forest));
            }
        } catch (Exception ex) {
            System.err.println("ERROR: " + ex.toString());
            System.exit(-1);
        }
    }

}