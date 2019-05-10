package com.healthapp.datasender;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import ru.etu.parkinsonlibrary.database.MissClickEntity;
import ru.etu.parkinsonlibrary.database.OrientationEntity;
import ru.etu.parkinsonlibrary.database.TypingErrorEntity;

public interface SenderAPI {

    String missclick = "/missclick";
    String coordination = "/coordination";
    String textWatcher = "/text";

    @POST(missclick)
    Call<Void> sendMissclickData(@Header("Authorization") String authHeader, @Body List<MissClickEntity> data);

    @POST(coordination)
    Call<Void> sendOrientationData(@Header("Authorization") String authHeader, @Body List<OrientationEntity> data);

    @POST(textWatcher)
    Call<Void> sendTextTypingErrorsData(@Header("Authorization") String authHeader, @Body List<TypingErrorEntity> data);

}
