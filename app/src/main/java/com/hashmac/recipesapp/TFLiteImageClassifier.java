package com.hashmac.recipesapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TFLiteImageClassifier {
    private static final int IMAGE_SIZE = 224;
    private final Interpreter interpreter;
    private final List<String> labels;
    private final int MAX_RESULTS = 3;

    public TFLiteImageClassifier(Context context) {
        try {
            interpreter = new Interpreter(FileUtil.loadMappedFile(context, "food101_model.tflite"));
            labels = FileUtil.loadLabels(context, "labels.txt");
        } catch (IOException e) {
            throw new RuntimeException("Error initializing TensorFlow Lite model", e);
        }
    }

    public List<String> classify(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true);
        ByteBuffer input = convertBitmapToByteBuffer(resized);

        float[][] output = new float[1][labels.size()];
        interpreter.run(input, output);

        return getTopLabels(output[0]);
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * IMAGE_SIZE * IMAGE_SIZE * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] pixels = new int[IMAGE_SIZE * IMAGE_SIZE];
        bitmap.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE);
        for (int pixel : pixels) {
            byteBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
            byteBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
            byteBuffer.putFloat((pixel & 0xFF) / 255.0f);
        }
        return byteBuffer;
    }

    private List<String> getTopLabels(float[] probs) {
        List<LabelProb> labelProbs = new ArrayList<>();
        for (int i = 0; i < probs.length; i++) {
            labelProbs.add(new LabelProb(labels.get(i), probs[i]));
        }
        Collections.sort(labelProbs, (a, b) -> Float.compare(b.prob, a.prob));
        List<String> topLabels = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_RESULTS, labelProbs.size()); i++) {
            topLabels.add(labelProbs.get(i).label);
        }
        return topLabels;
    }

    private static class LabelProb {
        String label;
        float prob;

        LabelProb(String label, float prob) {
            this.label = label;
            this.prob = prob;
        }
    }
}
