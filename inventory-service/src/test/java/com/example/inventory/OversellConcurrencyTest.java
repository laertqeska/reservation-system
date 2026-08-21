package com.example.inventory;

import com.example.inventory.dto.CreateHoldRequest;
import com.example.inventory.dto.CreateHoldResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
public class OversellConcurrencyTest {
    private static final int STOCK = 50;
    private static final int THREADS = 200;

    @Autowired
    TestRestTemplate http;

    @Autowired
    DataSource dataSource;

    private JdbcTemplate jdbc;

    private Long itemId;


    @BeforeEach
    void setUp(){
        jdbc = new JdbcTemplate(dataSource);

        //wipe state from previous round
        jdbc.execute("DELETE FROM hold");
        jdbc.execute("DELETE FROM inventory_items");

        //reset sequences so id's are predictable
        jdbc.execute("ALTER SEQUENCE inventory_item_id_seq RESTART WITH 1");
        jdbc.execute("ALTER SEQUENCE hold_id_seq RESTART WITH 1");

        //seed one item with exactly STOCK units
        jdbc.execute("""
            INSERT INTO inventory_items (id,sku,total,available,version,created_at,updated_at)
            VALUES(nextval('inventory_item_id_seq'),'TEST-SKU',50,50,0,now(),now())
        """);

        itemId = jdbc.queryForObject("SELECT id FROM inventory_items WHERE sku = 'TEST-SKU'",Long.class);
    }

    @RepeatedTest(10)
    void neverOversellUnderConcurrentLoad() throws InterruptedException {
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch allDone = new CountDownLatch(THREADS);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger rejections = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        for(int i = 0;i<THREADS;i++){
            pool.submit(() -> {
                try{
                    startGun.await(); // all threads wait here

                    ResponseEntity<CreateHoldResponse> response = http.postForEntity(
                            "/holds",
                            new CreateHoldRequest(itemId,1, UUID.randomUUID().toString()),
                            CreateHoldResponse.class
                    );

                    switch (response.getStatusCode().value()){
                        case 201 -> successes.incrementAndGet();
                        case 409 -> rejections.incrementAndGet();
                        default -> errors.incrementAndGet();
                    }
                }
                catch (Exception e){
                    errors.incrementAndGet();
                }
                finally{
                    allDone.countDown();
                }
            });
        }
        startGun.countDown(); // release all threads simultaneously
        allDone.await(); // wait for all to finish
        pool.shutdownNow();

        //--- ground truth: read the database directly ---
        Integer availableInDb = jdbc.queryForObject(
                "SELECT available FROM inventory_items WHERE id=?",
                Integer.class,itemId
        );

        Integer heldQtyInDb = jdbc.queryForObject(
                "SELECT COALESCE(SUM(qty),0) FROM hold WHERE item_id = ? AND status != 'RELEASED'",
                Integer.class,itemId
        );

        //no unexpected status codes
        assertThat(errors.get())
                .as("unexpected HTTP status codes").isZero();

        //available never went below zero
        assertThat(availableInDb)
                .as("available in database").isZero();

        // sum of held units equals stock
        assertThat(heldQtyInDb)
                .as("total held qty in database").isEqualTo(STOCK);

        // exactly STOCK requests succeeded
        assertThat(successes.get())
                .as("successful holds").isEqualTo(STOCK);
        //the rest were rejected
        assertThat(rejections.get())
                .as("rejected holds").isEqualTo(THREADS - STOCK);
    }

}
