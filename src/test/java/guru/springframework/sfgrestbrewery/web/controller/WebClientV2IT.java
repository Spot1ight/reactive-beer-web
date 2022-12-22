package guru.springframework.sfgrestbrewery.web.controller;

import guru.springframework.sfgrestbrewery.web.functional.BeerRouterConfig;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class WebClientV2IT {

    public static final String BASE_URL = "http://localhost:8080";

    WebClient webClient;

    @BeforeEach
    void setUp() {
        webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().wiretap(true)))
                .build();
    }

    @Test
    void getBeerById() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerDto> beerDtoMono = webClient.get()
                .uri(BeerRouterConfig.BEER_V2_URL + "/" + 1)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(beerDto -> {
            assertThat(beerDto).isNotNull();
            assertThat(beerDto.getBeerName()).isNotNull();

            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void getBeerByIdNotFound() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerDto> beerDtoMono = webClient.get()
                .uri(BeerRouterConfig.BEER_V2_URL + "/" + 1333)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(beerDto -> {
        }, throwable -> {
            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }


    @Test
    void getBeerByUpc() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        String beerUpc = "0631234200036";
        Mono<BeerDto> beerDtoMono = webClient.get()
                .uri(BeerRouterConfig.BEER_V2_URL_UPC + "/" + beerUpc)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(beerDto -> {
            assertThat(beerDto).isNotNull();
            assertThat(beerDto.getUpc()).isEqualTo(beerUpc);
            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }


    @Test
    void getBeerByUpcNotFound() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        String beerUpc = "1111111";
        Mono<BeerDto> beerDtoMono = webClient.get()
                .uri(BeerRouterConfig.BEER_V2_URL_UPC + "/" + beerUpc)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(beerDto -> {
        }, throwable -> {
            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testSaveBeer() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        BeerDto beerDto = BeerDto.builder()
                .beerName("JTs Beer")
                .upc("12345")
                .beerStyle("PALE_ALE")
                .price(new BigDecimal("8.99"))
                .build();

        Mono<ResponseEntity<Void>> beerResponseMono = webClient.post()
                .uri(BeerRouterConfig.BEER_V2_URL)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(beerDto))
                .retrieve().toBodilessEntity();

        beerResponseMono.publishOn(Schedulers.parallel()).subscribe(voidResponseEntity -> {
            assertThat(voidResponseEntity.getStatusCode().is2xxSuccessful());
            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testSaveBeerBadRequest() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        BeerDto beerDto = BeerDto.builder()
                .price(new BigDecimal("8.99"))
                .build();

        Mono<ResponseEntity<Void>> beerResponseMono = webClient.post()
                .uri(BeerRouterConfig.BEER_V2_URL)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(beerDto))
                .retrieve().toBodilessEntity();

        beerResponseMono.subscribe(voidResponseEntity -> {
        }, throwable -> {
            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testUpdateBeer() throws InterruptedException {
        final String newBeerName = "JTs Beer";
        final Integer beerId = 1;
        CountDownLatch countDownLatch = new CountDownLatch(2);

        webClient.put()
                .uri(BeerRouterConfig.BEER_V2_URL + "/" + beerId)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(BeerDto.builder()
                        .beerName("JTs Beer")
                        .upc("12345")
                        .beerStyle("PALE_ALE")
                        .price(new BigDecimal("8.99"))
                        .build()))
                .retrieve().toBodilessEntity()
                .subscribe(voidResponseEntity -> {
                    assertThat(voidResponseEntity.getStatusCode().is2xxSuccessful());
                    countDownLatch.countDown();
                });

        countDownLatch.await(500, TimeUnit.MILLISECONDS);

        webClient.get().uri(BeerRouterConfig.BEER_V2_URL + "/" + beerId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerDto.class)
                .subscribe(beerDto -> {
                    assertThat(beerDto).isNotNull();
                    assertThat(beerDto.getBeerName()).isNotNull();
                    assertThat(beerDto.getBeerName()).isEqualTo(newBeerName);
                    countDownLatch.countDown();
                });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testUpdateBeerNotFound() throws InterruptedException {
        final String newBeerName = "JTs Beer";
        final Integer beerId = 999;
        CountDownLatch countDownLatch = new CountDownLatch(1);

        webClient.put()
                .uri(BeerRouterConfig.BEER_V2_URL + "/" + beerId)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(BeerDto.builder()
                        .beerName(newBeerName)
                        .upc("12345")
                        .beerStyle("PALE_ALE")
                        .price(new BigDecimal("8.99"))
                        .build()))
                .retrieve().toBodilessEntity()
                .subscribe(voidResponseEntity -> {
                    assertThat(voidResponseEntity.getStatusCode().is2xxSuccessful());
                }, throwable -> {
                    countDownLatch.countDown();
                });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testDeleteBeer() throws InterruptedException {
        Integer beerId = 3;
        CountDownLatch countDownLatch = new CountDownLatch(1);

        webClient.delete().uri(BeerRouterConfig.BEER_V2_URL + "/" + beerId)
                .retrieve().toBodilessEntity()
                .flatMap(voidResponseEntity -> {
                    countDownLatch.countDown();

                    return webClient.get().uri(BeerRouterConfig.BEER_V2_URL + "/" + beerId)
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve().bodyToMono(BeerDto.class);
                }).subscribe(savedDto -> {}, throwable -> {
                    countDownLatch.countDown();
                });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testDeleteBeerNotFound() {
        Integer beerId = 4;

        webClient.delete().uri(BeerRouterConfig.BEER_V2_URL + "/" + beerId)
                .retrieve().toBodilessEntity()
                .block();

        assertThrows(WebClientResponseException.NotFound.class, () -> {
            webClient.delete().uri(BeerRouterConfig.BEER_V2_URL + "/" + beerId)
                    .retrieve().toBodilessEntity()
                    .block();
        });
    }


}
