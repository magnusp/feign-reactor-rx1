package github.magnusp.reactivefeign.rx1.methodhandler;

import feign.MethodMetadata;
import feign.Target;
import reactivefeign.methodhandler.DefaultMethodHandler;
import reactivefeign.methodhandler.MethodHandler;
import reactivefeign.methodhandler.MethodHandlerFactory;
import reactivefeign.publisher.PublisherClientFactory;

import java.lang.reflect.Method;

import static reactivefeign.utils.FeignUtils.returnPublisherType;

public class Rx1MethodHandlerFactory implements MethodHandlerFactory {

    private final PublisherClientFactory publisherClientFactory;
    private       Target<?>                 target;

    public Rx1MethodHandlerFactory(PublisherClientFactory publisherClientFactory) {
        this.publisherClientFactory = publisherClientFactory;
    }

    @Override
    public void target(Target target) {
        this.target = target;
    }

    @Override
    public MethodHandler create(final MethodMetadata metadata) {
        MethodHandler methodHandler = new Rx1PublisherClientMethodHandler(
                target, metadata, publisherClientFactory.create(metadata));

        return new Rx1MethodHandler(methodHandler, returnPublisherType(metadata));
    }

    @Override
    public MethodHandler createDefault(Method method) {
        return new DefaultMethodHandler(method);
    }
}
