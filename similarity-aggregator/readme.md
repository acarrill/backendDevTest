# Similarity Aggregator Microservice

## Components and design decisions

Similarity Aggregator is a microservice designed to recover products detail ordered by similarity scores.

### The components used in this microservice are:

- WebFlux: I have chose this library in order to be able to request product details in parallel and aggregate the results efficiently.
- Resilience4j: Since we call external services that can fail or be slow we need a way to avoid call this services when they are  or overloaded.
- Caffeine cache: This provides an in-memory cache to store product details and reduce the number of calls to the external product service.
In a real application we could use a distributed cache like Redis to be able to share the cache between multiple instances of the service.
Also, this kind of caches provide a non-memory based storage, that is useful when the data to cache is big.

### Some other design decisions made:

- Use records instead of lombok. Eliminating an unnecessary dependency that can be annoying upgrading JDK in future versions.
- Use WebClient instead of RestTemplate. RestTemplate is deprecated in favor of WebClient.
- From a hexagonal perspective, I have made the decision of having orchestration logic in the application layer. 
Orchestration is something inherently related with application layer, since this service haven't any logic to transform data, I have decided to keep it simple and put all the orchestration logic in the application layer.
From my point of view, this decision makes clear that this service does not add any additional domain logic and just orchestrate calls. If in the future we need to add some logic to transform data, we can create domain services not mixing orchestration and domain logic.
Notice that if in the future we have domain logic that is in charge of recovering similar products, we can create a domain service that encapsulates this logic and use it from the application layer through the ports interfaces, with minimal changes to the current code.

### Configuration

We have two main concerns to configure for this microservice:

- Timeouts: currently I configured 5 seconds, time that is long enough to cache the slow case but not long enough for the very slow case.
I see here a trade-off between giving time to the cache to store slow data and the circuit breaker configured to open when the service is slow.
- Circuit breaker thresholds: For me the most relevant configuration for our case is how to treat slow calls and 4xx errors.
About 4xx errors (welp better said 404 for our case) the decision made is ignored them from circuit breaker perspective, since these errors are not related to service unavailability but to client errors.

## Future improvements

- Add a correlator id to the requests to be able to trace requests through the system. I tried it but the approach used was flaky.
I could trace better our request, but I have also errors related to context propagation due to use reactor so I reverted the commit.
- Add test tasks to be able to execute different kind of tests like integration tests separately. Also, I would order their execution taking into account their cost of execute.
- A lot of metrics like cache hit/miss ratio can be added in a more visual way, for this POV I think it was out of scope.
- During the implementation I discovered a pattern called Bulkhead, so it's something interesting to spike in the future.