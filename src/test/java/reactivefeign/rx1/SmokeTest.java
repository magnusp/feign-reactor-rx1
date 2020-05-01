/**
 * Copyright 2018 The Feign Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package reactivefeign.rx1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import reactivefeign.ReactiveFeign;
import reactivefeign.rx1.testcase.IcecreamServiceApi;
import reactivefeign.rx1.testcase.domain.Bill;
import reactivefeign.rx1.testcase.domain.Flavor;
import reactivefeign.rx1.testcase.domain.IceCreamOrder;
import reactivefeign.rx1.testcase.domain.Mixin;
import reactivefeign.rx1.testcase.domain.OrderGenerator;
import rx.Single;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.rx1.TestUtils.equalsComparingFieldByFieldRecursively;

/**
 * @author Sergii Karpenko
 */

public class SmokeTest {

  @ClassRule
  public static WireMockClassRule wireMockRule = new WireMockClassRule(
      wireMockConfig().dynamicPort());

  @Before
  public void resetServers() {
    wireMockRule.resetAll();
  }

  protected ReactiveFeign.Builder<IcecreamServiceApi> builder(){
    return Rx1ReactiveFeign.builder();
  }

  private IcecreamServiceApi client;

  private OrderGenerator generator = new OrderGenerator();
  private Map<Integer, IceCreamOrder> orders = generator.generateRange(10).stream()
      .collect(Collectors.toMap(IceCreamOrder::getId, o -> o));

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() {
    String targetUrl = "http://localhost:" + wireMockRule.port();
    client = builder()
        .decode404()
        .target(IcecreamServiceApi.class, targetUrl);
  }

  @Test
  public void testSimpleGet_success() throws JsonProcessingException, InterruptedException {

    wireMockRule.stubFor(get(urlEqualTo("/icecream/flavors"))
        .willReturn(aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.MAPPER.writeValueAsString(Flavor.values()))));

    wireMockRule.stubFor(get(urlEqualTo("/icecream/mixins"))
        .willReturn(aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.MAPPER.writeValueAsString(Mixin.values()))));

    client.getAvailableMixins().test()
            .awaitTerminalEvent()
            .assertResult(Mixin.values());
    }

  @Test
  public void testFindOrder_success() throws JsonProcessingException, InterruptedException {
    IceCreamOrder orderExpected = orders.get(1);
    wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/1"))
        .willReturn(aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.MAPPER.writeValueAsString(orderExpected))));

    client.findOrder(1).test()
            .awaitTerminalEvent()
            //.assertValue(equalsComparingFieldByFieldRecursivelyRx(orderExpected))
            .assertValueCount(1)
            .assertNoErrors()
            .assertCompleted();
  }

  @Test
  public void testFindOrder_empty() throws InterruptedException {

    client.findOrder(123).test()
            .awaitTerminalEvent()
            .assertNoValues()
            .assertError(NoSuchElementException.class)
            .assertTerminalEvent();
  }

  @Test
  public void testMakeOrder_success() throws JsonProcessingException {

    IceCreamOrder order = new OrderGenerator().generate(20);
    Bill billExpected = Bill.makeBill(order);

    wireMockRule.stubFor(post(urlEqualTo("/icecream/orders"))
        .withRequestBody(equalTo(TestUtils.MAPPER.writeValueAsString(order)))
        .willReturn(aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.MAPPER.writeValueAsString(billExpected))));

    Single<Bill> bill = client.makeOrder(order);
    assertThat(bill.toBlocking().value())
            .matches(equalsComparingFieldByFieldRecursively(billExpected));
  }

  @Test
  public void testPayBill_success() throws JsonProcessingException {

    Bill bill = Bill.makeBill(new OrderGenerator().generate(30));

    wireMockRule.stubFor(post(urlEqualTo("/icecream/bills/pay"))
            .withRequestBody(equalTo(TestUtils.MAPPER.writeValueAsString(bill)))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(Long.toString(321))));

    Single<Long> result = client.payBill(bill);
    assertThat(result.toBlocking().value()).isEqualTo(321);
  }
}
