package com.android.jdejonge.jdejongescanner.model;

public class Stock {

    public static final int STATUS_AVAILABLE = 0;
    public static final int STATUS_IN_RENT = 1;
    public static final int STATUS_IN_REPAIR = 2;
    public static final int STATUS_IN_MAINTANANCE = 3;
    public static final int STATUS_DISAPPROVED = 6;
    public static final int STATUS_RETURN = 7;
    public static final int STATUS_IN_TRANSIT = 4;
    public static final int STATUS_RESERVED = 5;
    public static final int STATUS_SOLD = 8;
    public static final int STATUS_MISSING = 9;
    public static final int STATUS_INACTIVE = 10;
    public static final int STATUS_CHECK = 11;

    public String BARCODE;
    public String CALCODE;
    public String DESC1;
    public String DESC2;
    public String DESC3;
    public String ITEMNO;
    public int STATUS;
    public String NLCODE;
    public ContItem CONTITEM;
    public int UNIQUE;
    public int QTYHIRE;
    public Boolean IN_RENT_BY_CUSTOMER;
}
