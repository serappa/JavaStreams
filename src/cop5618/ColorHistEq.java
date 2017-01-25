package cop5618;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class ColorHistEq {

    //Use these labels to instantiate you timers.  You will need 8 invocations of now()
    static String[] labels = { "getRGB", "convert to HSB", "create brightness map",
            "parallel prefix", "probability array", "equalize pixels", "setRGB" };
    static int numBins = 50;

    static Timer colorHistEq_serial(BufferedImage image, BufferedImage newImage) {
        Timer times = new Timer(labels);
        /**
         * IMPLEMENT SERIAL METHOD
         */

        ColorModel colorModel = ColorModel.getRGBdefault();
        int w = image.getWidth();
        int h = image.getHeight();
        int numPixels = w*h;
        int hist[] = new int[numBins];

        times.now();
        int[] sourcePixelArray = image.getRGB(0, 0, w, h, new int[w * h], 0, w);
        times.now();
        float[][] hsbPixelArray = Arrays.stream(sourcePixelArray).
                mapToObj(pixel->Color.RGBtoHSB(colorModel.getRed(pixel), colorModel.getGreen(pixel), colorModel.getBlue(pixel), null))
                .toArray(float[][]::new);
        times.now();
        Arrays.stream(hsbPixelArray).mapToInt(pixel-> (int) (pixel[2] * numBins))
                .mapToObj(Integer::new)
                .collect(Collectors.groupingBy(Integer::intValue, Collectors.counting()))
                .entrySet().stream()
                .forEach(e->hist[e.getKey()] = e.getValue().intValue());
        times.now();
        Arrays.parallelPrefix(hist,(x,y)->x+y);
        times.now();
        double[] probArray = Arrays.stream(hist).mapToDouble(i-> i/(double)numPixels).toArray();
        times.now();
        int[] enchacedPixelArray = Arrays.stream(hsbPixelArray).map(pixel-> {
            int bin = (int) (pixel[2] * numBins);
            pixel[2] = (float)probArray[bin];
            return pixel;
        }).mapToInt(enhancedPixel -> Color.HSBtoRGB(enhancedPixel[0], enhancedPixel[1], enhancedPixel[2])).toArray();
        times.now();
        // create a new Buffered image and set its pixels to the gray pixel
        // array
        newImage.setRGB(0, 0, w, h, enchacedPixelArray, 0, w);
        times.now();
        return times;

    }

    static Timer colorHistEq_parallel(FJBufferedImage image, FJBufferedImage newImage) {
        Timer times = new Timer(labels);
        /**
         * IMPLEMENT PARALLEL METHOD
         */
        ColorModel colorModel = ColorModel.getRGBdefault();
        int w = image.getWidth();
        int h = image.getHeight();
        int numPixels = w*h;
        int hist[] = new int[numBins];

        times.now();
        int[] sourcePixelArray = image.getRGB(0, 0, w, h, new int[w * h], 0, w);
        times.now();
        float[][] hsbPixelArray = Arrays.stream(sourcePixelArray).parallel()
                .mapToObj(pixel->Color.RGBtoHSB(colorModel.getRed(pixel), colorModel.getGreen(pixel), colorModel.getBlue(pixel), null))
                .toArray(float[][]::new);
        times.now();
        Arrays.stream(hsbPixelArray).mapToInt(pixel-> (int) (pixel[2] * numBins)).parallel()
                .mapToObj(Integer::new)
                .collect(Collectors.groupingBy(Integer::intValue, Collectors.counting()))
                .entrySet().stream().parallel()
                .forEach(e->hist[e.getKey()] = e.getValue().intValue());
        times.now();
        Arrays.parallelPrefix(hist,(x,y)->x+y);
        times.now();
        double[] probArray = Arrays.stream(hist).parallel()
                .mapToDouble(i-> i/(double)numPixels).toArray();
        times.now();
        int[] enchacedPixelArray = Arrays.stream(hsbPixelArray).parallel()
                .map(pixel-> {
                    int bin = (int) (pixel[2] * numBins);
                    pixel[2] = (float)probArray[bin];
                    return pixel;
                }).mapToInt(enhancedPixel -> Color.HSBtoRGB(enhancedPixel[0], enhancedPixel[1], enhancedPixel[2])).toArray();
        times.now();
        // create a new Buffered image and set its pixels to the gray pixel
        // array
        newImage.setRGB(0, 0, w, h, enchacedPixelArray, 0, w);
        times.now();
        return times;
    }

}
