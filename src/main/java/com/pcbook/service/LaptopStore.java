package com.pcbook.service;

import com.pbj.Filter;
import com.pbj.Laptop;
import io.grpc.Context;


public interface LaptopStore {

    void save(Laptop laptop) throws Exception;
    Laptop find(String id);

    void Search(Context ctx, Filter filter, LaptopStream stream);
}

