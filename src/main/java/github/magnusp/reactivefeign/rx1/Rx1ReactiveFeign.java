package github.magnusp.reactivefeign.rx1;

import feign.Contract;
import feign.MethodMetadata;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactivefeign.ReactiveOptions;
import reactivefeign.client.ReactiveHttpClient;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.methodhandler.MethodHandlerFactory;
import reactivefeign.publisher.FluxPublisherHttpClient;
import reactivefeign.publisher.MonoPublisherHttpClient;
import reactivefeign.publisher.PublisherClientFactory;
import reactivefeign.publisher.PublisherHttpClient;
import reactivefeign.retry.ReactiveRetryPolicy;
import github.magnusp.reactivefeign.rx1.client.statushandler.Rx1ReactiveStatusHandler;
import github.magnusp.reactivefeign.rx1.client.statushandler.Rx1StatusHandler;
import github.magnusp.reactivefeign.rx1.methodhandler.Rx1MethodHandlerFactory;
import reactivefeign.webclient.WebClientFeignCustomizer;
import reactivefeign.webclient.WebReactiveFeign;
import reactivefeign.webclient.client.WebReactiveHttpClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.Single;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static feign.Util.resolveLastTypeParameter;
import static java.util.Optional.ofNullable;
import static reactivefeign.utils.FeignUtils.getBodyActualType;
import static reactivefeign.utils.FeignUtils.returnPublisherType;

public final class Rx1ReactiveFeign {

    private Rx1ReactiveFeign(){}

    public static <T> Builder<T> builder() {
        return new Builder<>(WebClient.builder());
    }

    public static <T> Builder<T> builder(WebClient.Builder webClientBuilder) {
        return new Builder<>(webClientBuilder);
    }

    public static <T> Builder<T> builder(WebClient.Builder webClientBuilder, WebClientFeignCustomizer webClientCustomizer) {
        return new Builder<>(webClientBuilder, webClientCustomizer);
    }

    public static class Builder<T> extends WebReactiveFeign.Builder<T> {


        protected Builder(WebClient.Builder webClientBuilder) {
            super(webClientBuilder);
        }

        protected Builder(WebClient.Builder webClientBuilder, WebClientFeignCustomizer webClientCustomizer) {
            super(webClientBuilder, webClientCustomizer);
        }



        @Override
        public MethodHandlerFactory buildReactiveMethodHandlerFactory(PublisherClientFactory publisherClientFactory) {
            return new Rx1MethodHandlerFactory(publisherClientFactory);
        }

        @Override
        public Builder<T> contract(final Contract contract) {
            this.contract = new Rx1Contract(contract);
            return this;
        }

        @Override
        public Builder<T> addRequestInterceptor(ReactiveHttpRequestInterceptor requestInterceptor) {
            super.addRequestInterceptor(requestInterceptor);
            return this;
        }

        @Override
        public Builder<T> decode404() {
            super.decode404();
            return this;
        }

        public Builder<T> statusHandler(Rx1StatusHandler statusHandler) {
            super.statusHandler(new Rx1ReactiveStatusHandler(statusHandler));
            return this;
        }

        @Override
        public Builder<T> retryWhen(ReactiveRetryPolicy retryPolicy){
            super.retryWhen(retryPolicy);
            return this;
        }

        @Override
        public Builder<T> options(final ReactiveOptions options) {
            super.options(options);
            return this;
        }

        @Override
        protected PublisherHttpClient toPublisher(ReactiveHttpClient reactiveHttpClient, MethodMetadata methodMetadata){
            Type returnType = returnPublisherType(methodMetadata);
            if(returnType == Single.class){
                return new MonoPublisherHttpClient(reactiveHttpClient);
            } else if(returnType == Observable.class){
                return new FluxPublisherHttpClient(reactiveHttpClient);
            } else {
                throw new IllegalArgumentException("Unknown returnType: " + returnType);
            }
        }

        @Override
        protected void updateClientFactory(){
            clientFactory(methodMetadata -> webClient(methodMetadata, webClientBuilder.build()));
        }

        public static WebReactiveHttpClient webClient(MethodMetadata methodMetadata, WebClient webClient) {

            final Type returnType = methodMetadata.returnType();
            Type returnPublisherType = ((ParameterizedType) returnType).getRawType();
            ParameterizedTypeReference<Object> returnActualType = ParameterizedTypeReference.forType(
                    resolveLastTypeParameter(returnType, (Class<?>) returnPublisherType));
            ParameterizedTypeReference<Object> bodyActualType = ofNullable(
                    getBodyActualType(methodMetadata.bodyType()))
                    .map(ParameterizedTypeReference::forType)
                    .orElse(null);

            return new WebReactiveHttpClient(webClient,
                    bodyActualType, rx1ToReactor(returnPublisherType), returnActualType);
        }

        private static Class<?> rx1ToReactor(Type type){
            if(type == Observable.class){
                return Flux.class;
            } else if(type == Single.class){
                return Mono.class;
            } else {
                throw new IllegalArgumentException("Unexpected type="+type);
            }
        }
    }

}
