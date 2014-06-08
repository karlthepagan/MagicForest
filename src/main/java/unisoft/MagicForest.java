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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MagicForest {
    private static final int[] WOLF_DEVOURS_GOAT = new int[]{-1,-1,+1};
    private static final int[] LION_DEVOURS_GOAT = new int[]{-1,+1,-1};
    private static final int[] LION_DEVOURS_WOLF = new int[]{+1,-1,-1};

    private static final int[][] MEALS = new int[][]{WOLF_DEVOURS_GOAT,LION_DEVOURS_GOAT,LION_DEVOURS_WOLF};

    static final class Forest implements Comparable<Forest> {
        private final int[] animals;

        public Forest(int goats, int wolves, int lions) {
            this.animals = new int[3];
            this.animals[0] = goats;
            this.animals[1] = wolves;
            this.animals[2] = lions;
        }

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

        public Optional<Forest> eat(int[] meal) {
            int[] next = new int[meal.length];
            for(int i = meal.length-1; i>=0; i--) {
                int n = animals[i] + meal[i];
                if(n < 0) return Optional.empty();
                next[i] = n;
            }
            return Optional.of(new Forest(next));
        }

        public Optional<Forest> wolfDevoursGoat() {
            return eat(WOLF_DEVOURS_GOAT);
        }

        public Optional<Forest> lionDevoursGoat() {
            return eat(LION_DEVOURS_GOAT);
        }

        public Optional<Forest> lionDevoursWolf() {
            return eat(LION_DEVOURS_WOLF);
        }

        public Collection<Forest> meal() {
            ArrayList<Forest> result = new ArrayList<>(3);
            Consumer<Forest> nextForests = result::add;
            for(int[] meal : MEALS) {
                eat(meal).ifPresent(nextForests);
            }
            return result;
        }

        public boolean isStable() {
            for(int[] meal : MEALS) {
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

    static Collection<Forest> meal(Collection<Forest> forests) {
        return forests.stream().map(Forest::meal)
                .reduce(new HashSet<>(forests.size()*2),
                        (a,i)->{a.addAll(i); return a;});
    }

    static Collection<Forest> parallelMeal(Collection<Forest> forests) {
        return forests.parallelStream().map(Forest::meal)
                .reduce(new ConcurrentSkipListSet<>(),
                        (a,i)->{a.addAll(i); return a;});
    }

    static boolean devouringPossible(Collection<Forest> forests) {
        return !forests.isEmpty() && !forests.stream().anyMatch(Forest::isStable);
    }

    static Collection<Forest> stableForests(Collection<Forest> forests) {
        return forests.stream().filter(Forest::isStable).collect(Collectors.toList());
    }

    static public Collection<Forest> findStableForests(Forest forest) {
        Collection<Forest> initialForests = Collections.singletonList(forest);
        Optional<Collection<Forest>> solution =
                Stream.iterate(initialForests, MagicForest::meal).filter(
                        forests->!devouringPossible(forests)).findFirst();
        return solution.isPresent()? stableForests(solution.get()) : Collections.emptyList();
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("USAGE: " + MagicForest.class.getSimpleName() +
                    " <goats> <wolves> <lions>");
            System.exit(-1);
        }
        try {
            Forest initialForest = new Forest(Integer.parseInt(args[0]),
                    Integer.parseInt(args[1]), Integer.parseInt(args[2]));
            Collection<Forest> stableForests = findStableForests(initialForest);
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