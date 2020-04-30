/*
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
package github.magnusp.reactivefeign.rx1.client.statushandler;

import reactivefeign.client.ReactiveHttpResponse;
import rx.Single;

import java.util.function.BiFunction;
import java.util.function.IntPredicate;

public final class Rx1StatusHandlers {

  private Rx1StatusHandlers(){}

  public static Rx1StatusHandler throwOnStatus(
          IntPredicate statusPredicate,
          BiFunction<String, ReactiveHttpResponse, Throwable> errorFunction) {
    return new Rx1StatusHandler() {
      @Override
      public boolean shouldHandle(int status) {
        return statusPredicate.test(status);
      }

      @Override
      public Single<Throwable> decode(String methodKey, ReactiveHttpResponse response) {
        return Single.just(errorFunction.apply(methodKey, response));
      }
    };
  }
}
