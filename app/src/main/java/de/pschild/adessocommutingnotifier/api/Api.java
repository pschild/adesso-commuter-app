package de.pschild.adessocommutingnotifier.api;

import de.pschild.adessocommutingnotifier.api.model.AuthResult;
import de.pschild.adessocommutingnotifier.api.model.CommutingResult;
import de.pschild.adessocommutingnotifier.api.model.CommutingStatusResult;
import de.pschild.adessocommutingnotifier.api.model.Credentials;
import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface Api {
  @POST("authenticate")
  Observable<AuthResult> login(@Body Credentials post);

  @GET("commuter/from/{startLat},{startLng}/to/{destLat},{destLng}")
  Observable<CommutingResult> commute(
      @Header("Authorization") String auth,
      @Path("startLat") double startLat,
      @Path("startLng") double startLng,
      @Path("destLat") double destLat,
      @Path("destLng") double destLng
  );

  @GET("commuter/commuting-state/{state}")
  Observable<CommutingStatusResult> updateCommutingStatus(
      @Header("Authorization") String auth,
      @Path("state") String state
  );
}
