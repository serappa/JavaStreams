package cop5618;

import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.junit.BeforeClass;

public class FJBufferedImage extends BufferedImage {

    /**Constructors*/

    private ForkJoinPool pool;
    private static int threshold = 1024*4;

    public FJBufferedImage(int width, int height, int imageType) {
        super(width, height, imageType);
        pool = new ForkJoinPool(32);
    }

    public FJBufferedImage(int width, int height, int imageType, IndexColorModel cm) {
        super(width, height, imageType, cm);
        pool = new ForkJoinPool(32);
    }

    public FJBufferedImage(ColorModel cm, WritableRaster raster, boolean isRasterPremultiplied,
                           Hashtable<?, ?> properties) {
        super(cm, raster, isRasterPremultiplied, properties);
        pool = new ForkJoinPool();
    }


    /**
     * Creates a new FJBufferedImage with the same fields as source.
     * @param source
     * @return
     */
    public static FJBufferedImage BufferedImageToFJBufferedImage(BufferedImage source){
        Hashtable<String,Object> properties=null;
        String[] propertyNames = source.getPropertyNames();
        if (propertyNames != null) {
            properties = new Hashtable<String,Object>();
            for (String name: propertyNames){properties.put(name, source.getProperty(name));}
        }
        return new FJBufferedImage(source.getColorModel(), source.getRaster(), source.isAlphaPremultiplied(), properties);
    }

    @Override
    public void setRGB(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize){
        /****IMPLEMENT THIS METHOD USING PARALLEL DIVIDE AND CONQUER*****/
        ForkJoinTask task = new SetTaskRowSplit(xStart, yStart, xStart+w, yStart+h, rgbArray, offset, scansize);
        pool.invoke(task);
    }


    @Override
    public int[] getRGB(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize){
        /****IMPLEMENT THIS METHOD USING PARALLEL DIVIDE AND CONQUER*****/
        if(rgbArray == null)
            rgbArray = new int[offset + h * scansize];
        ForkJoinTask task = new GetTaskRowSplit(xStart, yStart, xStart+w, yStart+h, rgbArray, offset, scansize);
        pool.invoke(task);
        return rgbArray;
    }

    public class GetTaskRowSplit extends GetTask {
        public GetTaskRowSplit(int xStart, int yStart, int xEnd, int yEnd, int[] rgbArray, int offset, int scansize) {
            super(xStart, yStart, xEnd, yEnd, rgbArray, offset, scansize);
        }

        @Override
        protected void invokeAllSubTasks() {
            int xMid = xStart + (xEnd - xStart)/2;
            if( yEnd - yStart <= 1 ){
                //Cannot further split by rows, so splitting by columns

                invokeAll(new GetTaskRowSplit(xStart, yStart, xMid, yEnd, rgbArray, offset, scansize),
                        new GetTaskRowSplit(xMid, yStart, xEnd, yEnd, rgbArray, offset, scansize));
            } else{
                int yMid = yStart + (yEnd - yStart)/2;
                invokeAll(new GetTaskRowSplit(xStart, yStart, xEnd, yMid, rgbArray, offset, scansize),
                        new GetTaskRowSplit(xStart, yMid, xEnd, yEnd, rgbArray, offset, scansize));
            }

        }
    }

    public class GetTaskColumnSplit extends GetTask {
        public GetTaskColumnSplit(int xStart, int yStart, int xEnd, int yEnd, int[] rgbArray, int offset, int scansize) {
            super(xStart, yStart, xEnd, yEnd, rgbArray, offset, scansize);
        }

        @Override
        protected void invokeAllSubTasks() {
            int yMid = yStart + (yEnd - yStart)/2;

            if(xEnd - xStart <= 1){
                //Cannot further split by columns, so splitting by rows
                invokeAll(new GetTaskRowSplit(xStart, yStart, xEnd, yMid, rgbArray, offset, scansize),
                        new GetTaskRowSplit(xStart, yMid, xEnd, yEnd, rgbArray, offset, scansize));
            } else {
                int xMid = xStart + (xEnd - xStart)/2;
                invokeAll(new GetTaskColumnSplit(xStart, yStart, xMid, yEnd, rgbArray, offset, scansize),
                        new GetTaskColumnSplit(xMid, yStart, xEnd, yEnd, rgbArray, offset, scansize));
            }

        }
    }


    public class SetTaskRowSplit extends SetTask {
        public SetTaskRowSplit(int xStart, int yStart, int xEnd, int yEnd, int[] rgbArray, int offset, int scansize) {
            super(xStart, yStart, xEnd, yEnd, rgbArray, offset, scansize);
        }

        @Override
        protected void invokeAllSubTasks() {
            int xMid = xStart + (xEnd - xStart)/2;
            if(yEnd - yStart <=1){
                //Cannot further split by rows, so splitting by columns
                invokeAll(new SetTaskRowSplit(xStart, yStart, xMid, yEnd, rgbArray, offset, scansize),
                        new SetTaskRowSplit(xMid, yStart, xEnd, yEnd, rgbArray, offset, scansize));
            } else{
                int yMid = yStart + (yEnd - yStart)/2;
                invokeAll(new SetTaskRowSplit(xStart, yStart, xEnd, yMid, rgbArray, offset, scansize),
                        new SetTaskRowSplit(xStart, yMid, xEnd, yEnd, rgbArray, offset, scansize));
            }

        }
    }

    public class SetTaskColumnSplit extends SetTask {
        public SetTaskColumnSplit(int xStart, int yStart, int xEnd, int yEnd, int[] rgbArray, int offset, int scansize) {
            super(xStart, yStart, xEnd, yEnd, rgbArray, offset, scansize);
        }

        @Override
        protected void invokeAllSubTasks() {
            int yMid = yStart + (yEnd - yStart)/2;
            if(xEnd - xStart <= 1){
                //Cannot further split by columns, so splitting by rows
                invokeAll(new SetTaskColumnSplit(xStart, yStart, xEnd, yMid, rgbArray, offset, scansize),
                        new SetTaskColumnSplit(xStart, yMid, xEnd, yEnd, rgbArray, offset, scansize));
            } else {
                int xMid = xStart + (xEnd - xStart)/2;
                invokeAll(new SetTaskColumnSplit(xStart, yStart, xMid, yEnd, rgbArray, offset, scansize),
                        new SetTaskColumnSplit(xMid, yStart, xEnd, yEnd, rgbArray, offset, scansize));
            }
        }
    }


    public abstract class DCTask extends RecursiveAction{
        protected int[] rgbArray;
        protected int xStart, yStart, xEnd, yEnd, len;
        protected int offset, scansize;
        public DCTask(int xStart, int yStart, int xEnd, int yEnd, int[] rgbArray, int offset, int scansize){
            this.xStart = xStart;
            this.xEnd = xEnd;
            this.yEnd = yEnd;
            this.yStart = yStart;
            this.len = (xEnd - xStart) * (yEnd - yStart);
            this.rgbArray = rgbArray;
            this.offset = offset;
            this.scansize = scansize;
        }

        @Override
        protected void compute() {
            if(len <=  threshold){
                computeSerially();
                return;
            }
            invokeAllSubTasks();
        }
        protected abstract void computeSerially();
        protected abstract void invokeAllSubTasks();

    }




    public class GetTask extends DCTask{

        public GetTask(int xStart, int yStart, int xEnd, int yEnd, int[] rgbArray, int offset, int scansize){
            super(xStart, yStart, xEnd, yEnd, rgbArray, offset, scansize);
        }

        @Override
        protected void invokeAllSubTasks() {
            int xMid = xStart + (xEnd - xStart)/2;
            int yMid = yStart + (yEnd - yStart)/2;
            invokeAll(new GetTask(xStart, yStart, xMid, yMid, rgbArray, offset, scansize),
                    new GetTask(xMid, yStart, xEnd, yMid, rgbArray, offset, scansize),
                    new GetTask(xStart, yMid, xMid, yEnd, rgbArray, offset, scansize),
                    new GetTask(xMid, yMid, xEnd, yEnd, rgbArray, offset, scansize));
        }

        protected void computeSerially(){
            int yoff  = offset+ yStart * scansize + xStart;
            int off;
            int nbands = getRaster().getNumBands();
            Object data = new byte[nbands];
            for (int y = yStart; y<yEnd; y++, yoff+=scansize) {
                off = yoff;
                for (int x = xStart; x<xEnd; x++) {
                    rgbArray[off++] = getColorModel().getRGB(getRaster().getDataElements(x, y, data));
                }
            }
        }
    }

    public class SetTask extends DCTask{

        public SetTask(int xStart, int yStart, int xEnd, int yEnd, int[] rgbArray, int offset, int scansize) {
            super(xStart, yStart, xEnd, yEnd, rgbArray, offset, scansize);
        }

        @Override
        protected void computeSerially() {
            int yoff = offset + yStart * scansize + xStart;
            int off;
            Object pixel = null;
            for (int y = yStart; y < yEnd; y++, yoff += scansize) {
                off = yoff;
                for (int x = xStart; x < xEnd; x++) {
                    pixel = getColorModel().getDataElements(rgbArray[off++], pixel);
                    getRaster().setDataElements(x, y, pixel);
                }
            }
        }

        @Override
        protected void invokeAllSubTasks() {
            int xMid = xStart + (xEnd - xStart)/2;
            int yMid = yStart + (yEnd - yStart)/2;

            invokeAll(new SetTask(xStart, yStart, xMid, yMid, rgbArray, offset, scansize),
                    new SetTask(xMid, yStart, xEnd, yMid, rgbArray, offset, scansize),
                    new SetTask(xStart, yMid, xMid, yEnd, rgbArray, offset, scansize),
                    new SetTask(xMid, yMid, xEnd, yEnd, rgbArray, offset, scansize));
        }
    }


}
