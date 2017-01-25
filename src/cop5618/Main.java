package cop5618;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by hsitas444 on 12/2/2016.
 */
public class Main {
    public static void main(String args[]){


        int numBins = 4;
        int[] hist = new int[numBins];
        float[][] f = {new float[]{0.1f}, new float[]{0.2f}, new float[]{.27f}, new float[]{.80f}, new float[]{.90f}, new float[]{.2f}};
        int sum = f.length;
        Map<Integer, Integer> histMap = Arrays.stream(f).mapToInt(pixel-> (int) (pixel[0] * numBins))
                .mapToObj(Integer::new)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(i-> 1)));
        for(int i: histMap.keySet()){
            hist[i] = histMap.get(i);
        }

//        Arrays.parallelPrefix(hist,(x,y)->x+y);
//
//
//        double[] prob = Arrays.stream(hist).mapToDouble(i-> i/(float)sum).toArray();
//        System.out.println("Parallel: "+Arrays.stream(f).parallel().isParallel());
        System.out.println();
    }

}
