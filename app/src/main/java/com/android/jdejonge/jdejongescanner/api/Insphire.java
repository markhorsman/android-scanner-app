package com.android.jdejonge.jdejongescanner.api;

import com.android.jdejonge.jdejongescanner.model.ContItemsInRent;
import com.android.jdejonge.jdejongescanner.model.CustomerContact;
import com.android.jdejonge.jdejongescanner.model.Status;
import com.android.jdejonge.jdejongescanner.model.Stock;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface Insphire {

    @GET("stock/{barcode}/{contno}/{acct}/{reference}")
    Call<Stock> getStockItem(
            @Header("Authorization") String authHeader,
            @Path("barcode") String barcode,
            @Path("contno") String contno,
            @Path("acct") String acct,
            @Path("reference") String reference
    );

    @GET("customer/{reference}")
    Call<CustomerContact> getCustomerContact(@Header("Authorization") String authHeader, @Path("reference") String reference);

    @GET("contitems/{reference}")
    Call<ContItemsInRent> getContItemsInRent(@Header("Authorization") String authHeader, @Path("reference") String reference);

    @PUT("stock/status/{itemno}/{contno}/{reference}/{qty}")
    Call<Status> updateStockStatus(
            @Header("Authorization") String authHeader,
            @Path("itemno") String itemno,
            @Path("contno") String contno,
            @Path("reference") String reference,
            @Path("qty") int qty,
            @Body Stock stock
    );

    @POST("contitem/{itemno}/{contstatus}/{stockstatus}/{qty}")
    Call<Status> insertContItem(
            @Header("Authorization") String authHeader,
            @Path("itemno") String itemno,
            @Path("contstatus") int contStatus,
            @Path("stockstatus") int stockStatus,
            @Path("qty") int qty,
            @Body CustomerContact customerContact
    );
}
