package de.pschild.adessocommutingnotifier.api;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import de.pschild.adessocommutingnotifier.BuildConfig;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HttpClient {
  private static final String API_BASE_URL = BuildConfig.endpoint;

  private static final Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(API_BASE_URL)
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
      .addConverterFactory(GsonConverterFactory.create())
      .client(new OkHttpClient()
          .newBuilder()
          .readTimeout(30, TimeUnit.SECONDS)
          .connectTimeout(30, TimeUnit.SECONDS)
          .build()
      )
      .build();

  private static final Api api = retrofit.create(Api.class);

  public static Api getApiService() {
    return api;
  }
}
