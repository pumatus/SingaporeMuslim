package co.muslimummah.android.network;

import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import co.muslimummah.android.network.Entity.body.UploadLog;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Created by Xingbo.Jie on 29/8/17.
 */

public class UserLogConverter extends Converter.Factory {

    public static UserLogConverter create() {
        return new UserLogConverter();
    }

    @Nullable
    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        if (UploadLog.class.equals(type)) {
            return new LogConverterFactory();
        }

        return super.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit);
    }

    private class LogConverterFactory implements Converter<UploadLog, RequestBody> {
        final MediaType MEDIA_TYPE = MediaType.parse("application/json;charset=UTF-8");

        @Override
        public RequestBody convert(UploadLog value) throws IOException {
            JSONStringer root = new JSONStringer();
            try {
                root.object();
                root.key("event_upload_time").value(System.currentTimeMillis());

                root.key("user_behavior_logs").array();

                BufferedReader bufferedReader = null;
                InputStreamReader inputStreamReader = null;
                FileInputStream fileInputStream = null;
                try {
                    fileInputStream = new FileInputStream(new File(value.getLogPath()));
                    inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
                    bufferedReader = new BufferedReader(inputStreamReader);
//                    File src = new File(value.getLogPath());
//                    File dst = new File(OracleApp.getInstance().getExternalCacheDir(), src.getName());
//                    FileUtils.copyFile(src, dst);
                    try {
                        bufferedReader.readLine();//first line keep the line count
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            try {
                                root.value(new JSONObject(line));
                            } catch (JSONException je) {
                                je.printStackTrace();
                            }
                        }
//                        src.delete();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (FileNotFoundException | UnsupportedEncodingException e) {
                    e.printStackTrace();
                } finally {
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (inputStreamReader != null) {
                        try {
                            inputStreamReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                root.endArray();

                root.endObject();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return RequestBody.create(MEDIA_TYPE, root.toString());
        }
    }
}
