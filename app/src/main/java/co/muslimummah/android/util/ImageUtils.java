package co.muslimummah.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Environment;
import android.text.TextUtils;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import co.muslimummah.android.R;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.NonNull;
import timber.log.Timber;


/**
 * Created by tysheng
 * Date: 9/10/17 7:55 PM.
 * Email: tyshengsx@gmail.com
 */

public class ImageUtils {

    public static File addWatermark(Context context, File originFile) {

        // TODO: 10/10/17 cache
        String name = Utils.md5(originFile.getAbsolutePath());
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), name + ".jpg");
        if (file.exists()) {
            return file;
        }
        Bitmap origin = BitmapFactory.decodeFile(originFile.getAbsolutePath());
        Bitmap result = createWatermarkLeft(context, origin, "Muslim Ummah App", R.drawable.ic_ummah);

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            result.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    private static Bitmap createWatermarkLeft(Context context, Bitmap bitmap, String markText, int markBitmapId) {

        // 当水印文字与水印图片都没有的时候，返回原图
        if (TextUtils.isEmpty(markText) && markBitmapId == 0) {
            return bitmap;
        }

        // 获取图片的宽高
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        // 创建一个和图片一样大的背景图
        Bitmap bmp = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        // 画背景图
        canvas.drawBitmap(bitmap, 0, 0, null);
        //-------------开始绘制文字-------------------------------

        // 文字开始的坐标,默认为左上角
        float textX = 0;
        float textY = 0;
//------------开始绘制图片-------------------------
        if (markBitmapId != 0) {
            // 载入水印图片
            Bitmap markBitmap = BitmapFactory.decodeResource(context.getResources(), markBitmapId);

//            // 如果图片的大小小于水印的3倍，就不添加水印
//            if (markBitmap.getWidth() > bitmapWidth / 3 || markBitmap.getHeight() > bitmapHeight / 3) {
//                return bitmap;
//            }

            int markBitmapWidth = markBitmap.getWidth();
            int markBitmapHeight = markBitmap.getHeight();
            Timber.d("createWatermark: w " + markBitmapWidth + " h " + markBitmapHeight);

            float scale = 0.75f;

            // 图片开始的坐标
            textX = (float) (15);//这里的-10和下面的-20都是微调的结果
            textY =

                    (float) (bitmapHeight - markBitmapHeight + 8);
            Rect rect = new Rect((int) textX, (int) textY, (int) (markBitmapWidth * scale + textX), (int) (markBitmapHeight * scale + textY));
            // 画图
            textX += markBitmapWidth;
            canvas.drawBitmap(markBitmap, null, rect, null);
        }

        if (!TextUtils.isEmpty(markText)) {
            // 创建画笔
            Paint mPaint = new Paint();
            // 文字矩阵区域
            Rect textBounds = new Rect();
            // 获取屏幕的密度，用于设置文本大小
            //float scale = context.getResources().getDisplayMetrics().density;
            // 水印的字体大小
            //mPaint.setTextSize((int) (11 * scale));
            mPaint.setTextSize(40);
            // 文字阴影
            mPaint.setShadowLayer(0.5f, 0f, 1f, Color.BLACK);
            // 抗锯齿
            mPaint.setAntiAlias(true);
            // 水印的区域
            mPaint.getTextBounds(markText, 0, markText.length(), textBounds);
            // 水印的颜色
            mPaint.setColor(Color.WHITE);
//            float extraWidth = 18;
//            float width = mPaint.measureText(markText) + extraWidth;
            float[] extras = new float[]{2, 9};

            // 文字开始的坐标
            textX = textX + extras[0];//这里的-10和下面的+6都是微调的结果
            textY = bitmapHeight - textBounds.height() + extras[1];
            // 画文字
            canvas.drawText(markText, textX, textY, mPaint);
        }


        //保存所有元素
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();

        return bmp;
    }

    public static Observable<File> getFileObservable(final Context context, final String url) {
        return Observable.create(new FileSubscribe(context, url));
    }

    private static class FileSubscribe implements ObservableOnSubscribe<File> {
        private Context context;
        private String url;

        FileSubscribe(Context context, String url) {
            this.context = context;
            this.url = url;
        }

        @Override
        public void subscribe(@NonNull ObservableEmitter<File> e) throws Exception {
            File file = Glide.with(context)
                    .asFile()
                    .load(url)
                    .submit().get();
            if (file == null || !file.exists()) {
                e.onError(new RuntimeException("Download failed"));
            } else {
                // Generate a temp file, can be overridden.
                File tmp = new File(context.getCacheDir(), "ShareTmp.jpg");
                FileUtils.copy(file,tmp,false);
                e.onNext(tmp);
                e.onComplete();
            }
        }
    }
}
