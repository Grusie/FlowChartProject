package com.example.imageclassifier;

import static org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod.NEAREST_NEIGHBOR;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.model.Model;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassifierWithModel {
    private static final String MODEL_NAME = "mobilenet_imagenet_model.tflite";
    Context context;

    public ClassifierWithModel(Context context) {
        this.context = context;
    }

    /*public ClassifierWithModel(GalleryActivity context) {
        this.context = context;
    }

    public ClassifierWithModel(CameraActivity context) {
        this.context = context;
    }*/

    Model model;

    public void init() throws IOException {
        model = Model.createModel(context, MODEL_NAME);
        initModelShape();

        labels = FileUtil.loadLabels(context, LABEL_FILE);
        labels.remove(0);
    }

    int modelInputWidth, modelInputHeight, modelInputChannel;
    TensorImage inputImage;     //Bitmap 이미지를 입력받고 전처리하여 모델에 입력하기까지의 이미지 관련 데이터 저장
    TensorBuffer outputBuffer;  //출력 값을 담을 텐서버퍼

    private void initModelShape() {
        Tensor inputTensor = model.getInputTensor(0);
        int[] shape = inputTensor.shape();
        modelInputChannel = shape[0];
        modelInputWidth = shape[1];
        modelInputHeight = shape[2];

        inputImage = new TensorImage(inputTensor.dataType());

        Tensor outputTensor = model.getOutputTensor(0);
        outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType());
    }

    private TensorImage loadImage(final Bitmap bitmap) {
        if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            inputImage.load(convertBitmapToARGB8888(bitmap));
        } else {
            inputImage.load(bitmap);
        }
        ImageProcessor imageProcessor = new ImageProcessor.Builder().add(new ResizeOp(modelInputWidth, modelInputHeight, NEAREST_NEIGHBOR)).add(new NormalizeOp(0.0f, 255.0f)).build();
        return imageProcessor.process(inputImage);
    }

    private static final String LABEL_FILE = "labels.txt";
    private List<String> labels;

    //Map<String, Float> 자료구조를 처리하는 argmax 메소드
    private Pair<String, Float> argmax(Map<String, Float> map) {
        String maxKey = "";
        float maxVal = -1;
        for (Map.Entry<String, Float> entry : map.entrySet()) {
            float f = entry.getValue();
            if (f > maxVal) {
                maxKey = entry.getKey();
                maxVal = f;
            }
        }
        return new Pair<>(maxKey, maxVal);
    }

    //모델 출력과 label 매핑
    //필요시 뒤에 Orientation 넣을 것
    public Pair<String, Float> classify(Bitmap image) {
        inputImage = loadImage(image);
        Object[] inputs = new Object[]{inputImage.getBuffer()};
        Map<Integer, Object> outputs = new HashMap();
        outputs.put(0, outputBuffer.getBuffer().rewind());

        model.run(inputs, outputs);

        Map<String, Float> output =
                new TensorLabel(labels, outputBuffer).getMapWithFloatValue();
        return argmax(output);
    }
    //자원해제
    public void finish() {
        if (model != null) {
            model.close();
        }
    }

    private Bitmap convertBitmapToARGB8888(Bitmap bitmap) {
        return bitmap.copy(Bitmap.Config.ARGB_8888, true);
    }

}
