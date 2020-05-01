package reactivefeign.rx1.methodhandler;

import feign.MethodMetadata;
import feign.Target;
import org.reactivestreams.Publisher;
import reactivefeign.methodhandler.PublisherClientMethodHandler;
import reactivefeign.publisher.PublisherHttpClient;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.RxReactiveStreams;
import rx.Single;

public class Rx1PublisherClientMethodHandler extends PublisherClientMethodHandler {


    public Rx1PublisherClientMethodHandler(
            Target target, MethodMetadata methodMetadata,
            PublisherHttpClient publisherClient) {
        super(target, methodMetadata, publisherClient);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Publisher<Object> body(Object body) {
        if (body instanceof Observable) {
            return RxReactiveStreams.toPublisher((Observable<Object>) body);
        } else if (body instanceof Single) {
            return RxReactiveStreams.toPublisher((Single<Object>) body);
        } else {
            return Mono.just(body);
        }
    }
}
