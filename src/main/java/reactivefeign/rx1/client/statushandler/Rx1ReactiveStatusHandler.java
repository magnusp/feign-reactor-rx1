package reactivefeign.rx1.client.statushandler;

import reactivefeign.client.ReactiveHttpResponse;
import reactivefeign.client.statushandler.ReactiveStatusHandler;
import reactor.core.publisher.Mono;
import rx.RxReactiveStreams;
import rx.Single;

public class Rx1ReactiveStatusHandler implements ReactiveStatusHandler {

	private final Rx1StatusHandler statusHandler;

	public Rx1ReactiveStatusHandler(Rx1StatusHandler statusHandler) {
		this.statusHandler = statusHandler;
	}

	@Override
	public boolean shouldHandle(int status) {
		return statusHandler.shouldHandle(status);
	}

	@Override
	public Mono<? extends Throwable> decode(String methodKey, ReactiveHttpResponse response) {
		Single<? extends Throwable> decoded = statusHandler.decode(methodKey, response);
		return Mono
				.from(RxReactiveStreams.toPublisher(decoded));
	}
}
