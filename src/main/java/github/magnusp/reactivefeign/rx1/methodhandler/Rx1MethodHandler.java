package github.magnusp.reactivefeign.rx1.methodhandler;

import org.reactivestreams.Publisher;
import reactivefeign.methodhandler.MethodHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.RxReactiveStreams;
import rx.Single;

import java.lang.reflect.Type;


public class Rx1MethodHandler implements MethodHandler {

	private final MethodHandler methodHandler;
	private final Type          returnPublisherType;

	public Rx1MethodHandler(MethodHandler methodHandler, Type returnPublisherType) {
		this.methodHandler = methodHandler;
		this.returnPublisherType = returnPublisherType;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object invoke(final Object[] argv) {
		try {
			Publisher<Object> publisher = (Publisher<Object>)methodHandler.invoke(argv);
			if(returnPublisherType == Observable.class){
				return RxReactiveStreams.toObservable(Flux.from(publisher));
			} else if(returnPublisherType == Single.class){
				return RxReactiveStreams.toObservable(Mono.from(publisher));
			} else {
				throw new IllegalArgumentException("Unexpected returnPublisherType="+returnPublisherType.getClass());
			}
		} catch (Throwable throwable) {
			if(returnPublisherType == Observable.class){
				return Observable.error(throwable);
			} else if(returnPublisherType == Single.class){
				return Single.error(throwable);
			} else {
				throw new IllegalArgumentException("Unexpected returnPublisherType="+returnPublisherType.getClass());
			}
		}
	}
}
