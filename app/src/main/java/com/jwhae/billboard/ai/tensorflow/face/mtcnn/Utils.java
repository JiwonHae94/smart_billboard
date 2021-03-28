package com.jwhae.billboard.ai.tensorflow.face.mtcnn;

import java.util.Vector;

public class Utils {

    // Flip diagonally.
    // The data size was originally h*w*stride, but it becomes w*h*stride after flipping
    public static void flipDiag(float[]data, int h, int w, int stride){
        float[] tmp=new float[w*h*stride];
        for (int i=0;i<w*h*stride;i++) tmp[i]=data[i];
        for (int y=0;y<h;y++)
            for (int x=0;x<w;x++){
                for (int z=0;z<stride;z++)
                    data[(x*h+y)*stride+z]=tmp[(y*w+x)*stride+z];
            }
    }

    // src is converted to 3D and stored in dst
    public static void expand(float[] src,float[][][] dst){
        int idx=0;
        for (int y=0;y<dst.length;y++)
            for (int x=0;x<dst[0].length;x++)
                for (int c=0;c<dst[0][0].length;c++)
                    dst[y][x][c]=src[idx++];

    }
    // dst=src[:,:,1]
    public static void expandProb(float[] src,float[][]dst){
        int idx=0;
        for (int y=0;y<dst.length;y++)
            for (int x=0;x<dst[0].length;x++)
                dst[y][x]=src[idx++*2+1];
    }

    // Delete the box marked with delete
    public static Vector<Box> updateBoxes(Vector<Box> boxes){
        Vector<Box> b= new Vector<>();
        for (int i=0;i<boxes.size();i++)
            if (!boxes.get(i).deleted)
                b.addElement(boxes.get(i));
        return b;
    }

}
