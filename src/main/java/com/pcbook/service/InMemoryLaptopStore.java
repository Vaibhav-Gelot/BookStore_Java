package com.pcbook.service;

import com.pbj.Filter;
import com.pbj.Laptop;
import com.pbj.Memory;
import io.grpc.Context;

import java.nio.file.FileAlreadyExistsException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class InMemoryLaptopStore implements LaptopStore {
    private final ConcurrentMap<String ,Laptop> data;
    private static final Logger logger=Logger.getLogger(LaptopClient.class.getName());
    public  InMemoryLaptopStore(){
        data=new ConcurrentHashMap<>(0);
    }
    @Override
    public void save(Laptop laptop) throws Exception {
       if(data.containsKey(laptop.getId())){
           throw new AlreadyExistsException("Laptop ID already exists");
       }
       //deep-copy
        Laptop other=laptop.toBuilder().build();
        data.put(other.getId(),other);
    }

    @Override
    public Laptop find(String id) {
        if (!data.containsKey(id)) {
            return null;
        }
        return data.get(id).toBuilder().build();
    }

    @Override
    public void Search(Context ctx, Filter filter, LaptopStream stream) {
    for(Map.Entry<String, Laptop> entry: data.entrySet()){
        if(ctx.isCancelled()){
             logger.info("Request cancelled");
             return;
        }
//        try {
//            TimeUnit.SECONDS.sleep(1);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        Laptop laptop=entry.getValue();
        if(isQualified(filter, laptop)){
            stream.Send(laptop.toBuilder().build());
        }
    }
    }

    private boolean isQualified(Filter filter, Laptop laptop) {
        if (laptop.getPriceUsd() > filter.getMaxPriceUsd()) {
            return false;
        }

        if (laptop.getCpu().getNumberCores() < filter.getMinCpuCores()) {
            return false;
        }

        if (laptop.getCpu().getMinGhz() < filter.getMinCpuGhz()) {
            return false;
        }

        return toBit(laptop.getRam()) >= toBit(filter.getMinRam());
    }

    private long toBit(Memory memory) {
        long value = memory.getValue();

        switch (memory.getUnit()) {
            case BIT:
                return value;
            case BYTE:
                return value << 3;
            case KILOBYTE:
                return value << 13;
            case MEGABYTE:
                return value << 23;
            case GIGABYTE:
                return value << 33;
            case TERABYTE:
                return value << 43;
            default:
                return 0;
        }
    }

}
