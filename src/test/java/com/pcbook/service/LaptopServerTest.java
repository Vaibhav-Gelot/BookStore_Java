package com.pcbook.service;

import com.pbj.*;
import com.pcbook.sample.Generator;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.LinkedList;
import java.util.List;

public class LaptopServerTest {

    @Rule
    public final GrpcCleanupRule grpcCleanupRule=new GrpcCleanupRule();
  private LaptopStore laptopStore;
  private ImageStore imageStore;

  private RatingStore ratingStore;
  private LaptopServer laptopServer;
  private ManagedChannel managedChannel;
    @Before
    public void setUp() throws Exception {
        String serverName= InProcessServerBuilder.generateName();
        InProcessServerBuilder serverBuilder=InProcessServerBuilder.forName(serverName).directExecutor();

        laptopStore=new InMemoryLaptopStore();
        imageStore=new DiskImageStore("img");
        ratingStore=new InMemoryRatingStore();
        laptopServer=new LaptopServer(serverBuilder,0,laptopStore,imageStore,ratingStore);
        laptopServer.start();
        managedChannel =grpcCleanupRule.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());

    }

    @After
    public void tearDown() throws Exception {
        laptopServer.stop();
    }

    @Test
    public void createLaptopWithValidID(){
        Generator generator=new Generator();
        Laptop laptop=generator.NewLaptop();
        CreateLaptopRequest createLaptopRequest=CreateLaptopRequest.newBuilder().setLaptop(laptop).build();
        LaptopServiceGrpc.LaptopServiceBlockingStub blockingStub = LaptopServiceGrpc.newBlockingStub(managedChannel);
        CreateLaptopResponse response=blockingStub.createLaptop(createLaptopRequest);
        assertNotNull(response);
        assertEquals(laptop.getId(),response.getId());
        Laptop found=laptopStore.find(response.getId());
        assertNotNull(found);
    }

    @Test
    public void createLaptopWithEmptyID(){
        Generator generator=new Generator();
        Laptop laptop=generator.NewLaptop().toBuilder().setId("").build();
        CreateLaptopRequest createLaptopRequest=CreateLaptopRequest.newBuilder().setLaptop(laptop).build();
        LaptopServiceGrpc.LaptopServiceBlockingStub blockingStub = LaptopServiceGrpc.newBlockingStub(managedChannel);
        CreateLaptopResponse response=blockingStub.createLaptop(createLaptopRequest);
        assertNotNull(response);
        assertFalse(response.getId().isEmpty());

        Laptop found = laptopStore.find(response.getId());
        assertNotNull(found);
    }
    @Test(expected = StatusRuntimeException.class)
    public void createLaptopWithInvalidID(){
        Generator generator=new Generator();
        Laptop laptop=generator.NewLaptop().toBuilder().setId("Invalid").build();
        CreateLaptopRequest createLaptopRequest=CreateLaptopRequest.newBuilder().setLaptop(laptop).build();
        LaptopServiceGrpc.LaptopServiceBlockingStub blockingStub = LaptopServiceGrpc.newBlockingStub(managedChannel);
        CreateLaptopResponse response=blockingStub.createLaptop(createLaptopRequest);}

    @Test(expected = StatusRuntimeException.class)
    public void createLaptopWithAlreadyExistsID() throws Exception {
        Generator generator=new Generator();
        Laptop laptop=generator.NewLaptop();
        laptopStore.save(laptop);
        CreateLaptopRequest createLaptopRequest=CreateLaptopRequest.newBuilder().setLaptop(laptop).build();
        LaptopServiceGrpc.LaptopServiceBlockingStub blockingStub = LaptopServiceGrpc.newBlockingStub(managedChannel);
        CreateLaptopResponse response=blockingStub.createLaptop(createLaptopRequest);
    }
    @Test
    public void rateLaptop() throws Exception {
        Generator generator = new Generator();
        Laptop laptop = generator.NewLaptop();
        laptopStore.save(laptop);

        LaptopServiceGrpc.LaptopServiceStub stub = LaptopServiceGrpc.newStub(managedChannel);
        RateLaptopResponseStreamObserver responseObserver = new RateLaptopResponseStreamObserver();
        StreamObserver<RateLaptopRequest> requestObserver = stub.rateLaptop(responseObserver);

        double[] scores = {8, 7.5, 10};
        double[] averages = {8, 7.75, 8.5};
        int n = scores.length;

        for (int i = 0; i < n; i++) {
            RateLaptopRequest request = RateLaptopRequest.newBuilder()
                    .setLaptopId(laptop.getId())
                    .setScore(scores[i])
                    .build();
            requestObserver.onNext(request);
        }

        requestObserver.onCompleted();
        assertNull(responseObserver.err);
        assertTrue(responseObserver.completed);
        assertEquals(n, responseObserver.responses.size());

        int idx = 0;
        for (RateLaptopResponse response : responseObserver.responses) {
            assertEquals(laptop.getId(), response.getLaptopId());
            assertEquals(idx + 1, response.getRatedCount());
            assertEquals(averages[idx], response.getAverageScore(), 1e-9);
            idx++;
        }
    }

    private class RateLaptopResponseStreamObserver implements StreamObserver<RateLaptopResponse> {
        public List<RateLaptopResponse> responses;
        public Throwable err;
        public boolean completed;

        public RateLaptopResponseStreamObserver() {
            responses = new LinkedList<>();
        }

        @Override
        public void onNext(RateLaptopResponse response) {
            responses.add(response);
        }

        @Override
        public void onError(Throwable t) {
            err = t;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }


}