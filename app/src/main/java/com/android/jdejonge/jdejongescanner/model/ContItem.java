package com.android.jdejonge.jdejongescanner.model;

public class ContItem {

    public static final int STATUS_OFFER = 0;
    public static final int STATUS_CONTRACT_CREATED = 1;
    public static final int STATUS_IN_RENT = 2;
    public static final int STATUS_FROM_RENT = 3;
    public static final int STATUS_COMPLETED = 4;
    public static final int STATUS_CANCELED = 9;

    public String CONTNO;
    public String ACCT;
    public int TYPE;
    public String ITEMNO;
    public String ITEMDESC;
    public int QTY;
    public double DISCOUNT;
    public int STATUS;
}
