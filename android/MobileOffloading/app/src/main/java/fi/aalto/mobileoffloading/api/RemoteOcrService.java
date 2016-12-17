package fi.aalto.mobileoffloading.api;

import java.util.List;

import fi.aalto.mobileoffloading.models.FacebookLoginData;
import fi.aalto.mobileoffloading.models.LoginData;
import fi.aalto.mobileoffloading.models.OcrEntry;
import fi.aalto.mobileoffloading.models.OcrHistoryEntry;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Url;

public interface RemoteOcrService  {
    @POST("/api/login")
    Call<LoginData> login(@Body LoginData loginData);

    @GET("/api/logout")
    Call<ResponseBody> logout();

    @Multipart
    @POST("/api/scan")
    Call<List<OcrEntry>> scan(@Part List<MultipartBody.Part> filePart);

    @POST("/api/login/facebook")
    Call<FacebookLoginData> facebooklogin(@Body FacebookLoginData loginData);

    @GET("/api/history")
    Call<List<OcrHistoryEntry>> history();

    @GET
    Call<ResponseBody> downloadImage(@Url String fileUrl);
}
