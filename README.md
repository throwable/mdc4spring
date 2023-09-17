<!-- ABOUT THE PROJECT -->
![build status](https://github.com/throwable/mdc4spring/actions/workflows/maven.yml/badge.svg)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.throwable.mdc4spring/mdc4spring/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.throwable.mdc4spring/mdc4spring/)


## MDC4Spring

Simple and secure management of Mapped Diagnostic Contexts (MDCs) for multiple logging systems with
Spring AOP. Supported logging systems: Slf4J/Logback, Log4J, Log4J2.

### Reasoning

_Mapped Diagnostic Context_ (MDC) provides a way to enrich log traces with contextual information about the execution scope.
Instead of including business parameters manually in each log message, we may consider as a good practice
to provide them inside the MDC associated with the trace.
This could be especially useful when you are using ELK stack, DataDog, or any other
log aggregation system to track the execution of your business flows.

Example of Logback logging trace that contains MDC with business data formatted by [JSON-based appender](#configuring-logback-for-json-output):

```json
{
  "timestamp": "2022-07-26 09:01:51.482",
  "level": "INFO",
  "thread": "main",
  "mdc": {
    "requestUuid": "b8e0cd40-0cc6-11ed-861d-0242ac120002",
    "sourceIp": "127.0.0.1",
    "instance": "instance1.mybusinessdomain:8080",
    "action" : "createOrder",
    "order.transactionId": "184325928574329523",
    "order.clientId": "web-57961e5e0242ac120002",
    "order.customerId": "A123456789",
    "order.assigneeUserId": "192385",
    "order.id": "2349682"
  },
  "logger": "com.github.throwable.mdc4springdemo.ShippingOrderController",
  "message": "Order created",
  "context": "default"
}
```

Different logging libraries provide a way of setting MDC parameters using thread-local context that
require a user to manually control the proper cleanup of them when the execution flow leaves the scope.
So, if one forgets to clean them properly, it may later pollute log messages with outdated information or even provoke memory
leaks in thread-pooled environments.

The idea is to use automatic MDC management by intercepting method calls specifying via annotations
the data from method arguments to include in the context.

##### Example

```java
class OrderProcessor {
    // define a new MDC that will be cleared automatically when the method returns
    @WithMDC
    public void assignToUser(
            // add method arguments with their values to the current MDC
            @MDCParam String orderId, @MDCParam String userId
    ) {
        // programmatically include additional parameter to the current MDC
        MDC.param("transactionId", this.getCurrentTransactionId());

        // business logic...

        // The MDC of the log message contains parameters: orderId, userId, transactionId 
        log.info("User was successfully assigned to order");
        // All these parameters defined within the scope of the method
        //  are automatically removed when the method returns.
    }
}
```

<p align="right">(<a href="#top">back to top</a>)</p>


<!-- GETTING STARTED -->

## Getting Started

### Prerequisites

The library works with Java 8+ and Spring Boot (2.x, 3.x) or Spring Framework (5.x, 6.x) environments.
Currently, it supports Slf4J/Logback, Log4j2, and Log4j logging systems.
By default, the logging system is detected using classpath library resolution, but you can change this behavior setting
`com.github.throwable.mdc4spring.loggers.LoggerMDCAdapter`
system property to a desired `LoggerMDCAdapter` implementation class: 
`Log4J2LoggerMDCAdapter`, `Log4JLoggerMDCAdapter`, `Slf4JLoggerMDCAdapter`.

### Installation

Add the following dependencies to your project's build file.

#### Maven

```xml
<dependency>
    <groupId>io.github.throwable.mdc4spring</groupId>
    <artifactId>mdc4spring</artifactId>
    <version>1.1</version>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

#### Gradle

```groovy
dependencies {
    // ...
    implementation("io.github.throwable.mdc4spring:mdc4spring:1.0")
    implementation("org.springframework.boot:spring-boot-starter-aop")
}
```

If you are using Spring Boot the configuration will be applied automatically by autoconfiguration mechanism.
In case you are not using Spring Boot you have to import `com.github.throwable.mdc4spring.spring.MDCConfiguration`
manually.

In case you're <u>not using Spring Boot</u>, replace the `spring-boot-starter-aop` dependency by the Spring Framework AOP library:

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aspects</artifactId>
    <version>${springframework-version}</version>
</dependency>
```
```groovy
dependencies {
    // ...
    implementation("org.springframework:spring-aspects:${springframework-version}")
}
```
You also need to configure your logging subsystem to see MDC parameters in your log traces.
[See an example of JSON-based appender](#configuring-logback-for-json-output). 

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- USAGE EXAMPLES -->

## Usage


#### Method argument parameters

Simple usage: log messages inside the method will contain MDC with `orderId` and `userId` parameters that will automatically be removed after the method returns.

```java
class OrderProcessor {
    @WithMDC
    public void assignToUser(@MDCParam String orderId, @MDCParam String userId) {
        // business logic...
        log.info("User assigned to order");
    }
}
```

By default, method argument names will be used as parameter names, [see considerations](#method-argument-names), but you can also define custom names for them.

```java
class OrderProcessor {
    @WithMDC
    public void assignToUser(@MDCParam("order.id") String orderId, @MDCParam("user.id") String userId) {
        log.info("User assigned to order");
    }
}
```

The value of any argument can be converted inline using [Spring expression language](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions).
In this case `order.id` and `user.id` parameters will be set to `order.getId()` and `user.getId()` respectively.
If any error occurs during the expression evaluation, the execution will not be interrupted, and a parameter value will 
contain a value like _#EVALUATION ERROR#: exception message_.
By default, NPE errors are excluded from this case, and the `null` value is returned if any referenced object in the path is null.

```java
class OrderProcessor {
    @WithMDC
    public void assignToUser(
            @MDCParam(name = "order.id", eval = "id") Order order,
            @MDCParam(name = "user.id", eval = "id") User user)
    {
        log.info("User assigned to order");
    }
}
```

#### Additional parameters

Also, additional MDC parameters can be defined for any method.
In this case, the `eval` attribute should define a SpEL expression that may contain references to the current evaluation context.

```java
class RequestProcessor {
    @WithMDC
    @MDCParam(name = "request.uid",   eval = "T(java.util.UUID).randomUUID()")                // (1)
    @MDCParam(name = "app.id",        eval = "#environment['spring.application.name']")       // (2)
    @MDCParam(name = "jvm.rt",        eval = "#systemProperties['java.runtime.version']")     // (3)
    @MDCParam(name = "tx.id",         eval = "transactionId")                                 // (4)
    @MDCParam(name = "source.ip",     eval = "#request.remoteIpAddr")                         // (5)
    @MDCParam(name = "operation",     eval = "#className + '/' + #methodName")                // (6)
    @MDCParam(name = "client.id",     eval = "@authenticationService.getCurrentClientId()")   // (7)
    public void processRequest(Request request) {
      // ...
    }
    
    private String getTransactionId() {
    // ...
    }
}
```
The example above contains:

1. Providing a parameter value by calling a static method of some class.
2. Accessing Spring configuration property using `#environment` variable.
3. Obtaining JVM system property using `#systemProperties` variable.
4. Accessing a property of the local bean. The whole bean object is available in expression evaluation as `#root` variable.
   The property may be a getter or a local field of any visibility level.
5. Each method argument is available within the expression using `#argumentName` variable.
6. Variables `#className` and `#methodName` contain the fully-qualified class name and the method name respectively.
7. Using `@beanName` notation, you can reference any named bean within the Spring Application Context.

#### MDC and the method scope

`@WithMDC` and `@MDCParam` annotations may also be defined at class level.
In this case, they provoke the same effect as being applied to each method.

```java
@WithMDC
@MDCParam(name = "request.uid",   eval = "T(java.util.UUID).randomUUID()")
@MDCParam(name = "app.id",        eval = "#environment['spring.application.name']")
@MDCParam(name = "operation",     eval = "#className + '/' + #methodName")
class OrderProcessor { 
    public void createOrder(@MDCParam(name = "order.id", eval = "id") Order order) {}
    public void updateOrder(@MDCParam(name = "order.id", eval = "id") Order order) {}
    public void removeOrder(@MDCParam(name = "order.id") String orderId) {}
    public Order getOrder(@MDCParam(name = "order.id") String orderId) {}
}
```

MDC may also be named. This adds the name as a prefix to any parameter defined within its scope.

```java
@WithMDC("order")
class OrderProcessor {
    // The name of the MDC parameter will be "order.id"
    public void createOrder(@MDCParam(name = "id", eval = "id") Order order) {
    }
}
```

In a cascade invocations of two methods when both annotated with `WithMDC`,
a new 'nested' MDC is created for the nested method invocation. 
This MDC will contain parameters defined inside the nested method, and it will be closed after the method returns
([see considerations](#method-invocations)).
All parameters created in the 'outer' MDC will remain as is in log messages.

```java
class OrderProcessor {
    @WithMDC
    public void createOrder(@MDCParam(name = "orderId", eval = "id") Order order) {
        // Call a method that defines a 'nested' MDC
        Customer customer = customerRepository.findCustomerByName(order.getCustomerId());
        log.info("after the 'nested' MDC call returns the customerId parameter will no longer exist in log messages");
    }
}
class CustomerRepository {
    // Defines a 'nested' MDC. The parameter customerId belongs to this new MDC
    //  and will be cleared after the method returns.
    @WithMDC
    public Customer findCustomerById(@MDCParam String customerId) {
        log.info("this log message will have orderId and customerId parameters defined");
    }
}
```

Any call to a 'nested' method that defines `@MDCParam` but is not annotated with `@WithMDC` annotation
will simply add a new parameter to the current MDC, and it remains there after the method returns.

```java
class OrderProcessor {
    @WithMDC
    public void createOrder(@MDCParam(name = "orderId", eval = "id") Order order) {
        // Call a method current MDC
        Customer customer = customerRepository.findCustomerByName(order.getCustomerId());
        log.info("after the method call we still have a customerId parameter in our MDC");
    }
}
class CustomerRepository {
    // The parameter customerId will be added to current MDC, and remains there after the method returns.
    public Customer findCustomerById(@MDCParam String customerId) {
        log.info("this log message will have orderId and customerId parameters defined");
    }
}
```

#### Output parameters

With `@MDCOutParam` annotation you can define an output parameter that will be added to the current MDC after the method returns.
Its value is evaluated using the value returned by this method.

```java
class OrderProcessor {
    @WithMDC
    public void createOrder(Order order) {
        User user = userRepository.findUserById(order.getUserId());
        log.info("this log message will have userId, userName and userGroup MDC parameters");
    }
}
class UserRepository {
    @MDCOutParam(name = "userName", eval = "name")
    @MDCOutParam(name = "userGroup", eval = "group.name")
    public User findUserById(@MDCParam String userId) {}
}
```

#### Defining MDC programmatically

That gives you a full control over MDC scopes and parameter definitions.
Use try-with-resources block to ensure a proper cleanup of all defined parameters.

```java
class OrderProcessor {
    public void createOrder(Order order) {
        try (CloseableMDC rootMdc = MDC.create()) {
            // Add a param to current MDC
            MDC.param("order.id", order.getId());
            log.info("order.id is added to MDC");
            
            try (CloseableMDC nestedMdc = MDC.create()) {
                // Add a param to nested MDC (nearest for current execution scope)
                MDC.param("customer.id", order.getCustomerId());
                log.info("Both order.id and customer.id appear in log messages");
            }
            log.info("order.id is still remains in messages but customer.id is removed with its MDC");
        }
    }
}
```

Alternatively you may use a lambda-based API to define MDC scopes.

```java
class OrderProcessor {
    public void createOrder(Order order) {
        MDC.with().param("order.id", order.getId()).run(() -> {
            log.info("order.id is added to MDC");
            Customer customer = MDC.with().param("customer.id", order.getCustomerId()).apply(() -> {
                log.info("Both order.id and customer.id appear in log messages");
                return customerRepository.findCustomerById(order.getCustomerId());
            });
            log.info("order.id is still remains in messages but customer.id is removes with its MDC");
        });
    }
}
```

<p align="right">(<a href="#top">back to top</a>)</p>


## Considerations and limitations

### Method invocations

The library uses Spring AOP to intercept annotated method invocations, so these considerations must be taken into account:

* The annotated method must be invoked from outside the bean scope. Local calls are not intercepted by Spring AOP, thus any method annotation will be ignored in this case.
  ```java
  class MyBean {
    @Lazy @Autowired MyBean self;
  
    public void publicMethod(@MDCParam String someParam) {
        anotherPublicMethod("this call is local, so 'anotherParam' will not be included in MDC");
        self.anotherPublicMethod("this call is proxied, so 'anotherParam' will be included hin MDC");
    }
    public void anotherPublicMethod(@MDCParam String anotherParam) {
        log.info("Some log trace");
    } 
  } 
  ```
* Spring AOP does not intercept private methods, so if you invoke an inner bean's private method, it will have no effect on it.

In both of the cases above you should define your parameters in an imperative way using `MDC.param()`.

### Method argument names

By default, Java compiler does not keep method argument names in generated bytecode, and it may cause
possible problems with parameter name resolutions when using `@MDCParam` for method's arguments.
There are three ways to avoid this problem:

* If you are using Spring Boot and Spring Boot Maven or Gradle plugin, the generated bytecode will already contain
  all method arguments with their names, and no additional action is required.
* If you are not using Spring Boot plugin you may tell your compiler to preserve method argument names
  by adding `-parameters` argument to `javac` invocation.
* You may also provide parameter names explicitly in your code:
  ```
  public User findUserById(@MDCParam("userId") String userId)
  ```

## Acknowledgements

<a name="configuring_logback_json"></a>
### Configuring Logback for JSON output

Add these dependencies to your pom.xml:
```xml
<dependencies>
    <dependency>
        <groupId>ch.qos.logback.contrib</groupId>
        <artifactId>logback-json-classic</artifactId>
        <version>0.1.5</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback.contrib</groupId>
        <artifactId>logback-jackson</artifactId>
        <version>0.1.5</version>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.1</version>
    </dependency>
</dependencies>
```

Create a new appender or modify an existing one setting JsonLayout:

```xml
<configuration>
    <appender name="jsonConsole" class="ch.qos.logback.core.ConsoleAppender">
        <layout class="ch.qos.logback.contrib.json.classic.JsonLayout">
            <jsonFormatter
                    class="ch.qos.logback.contrib.jackson.JacksonJsonFormatter">
                <prettyPrint>true</prettyPrint>
            </jsonFormatter>
            <timestampFormat>yyyy-MM-dd' 'HH:mm:ss.SSS</timestampFormat>
        </layout>
    </appender>

    <root level="DEBUG">
        <appender-ref ref="jsonConsole" />
    </root>
</configuration>
```
Now log messages will be written in JSON format that will include all MDC parameters.

### Configuring output for Elasticsearch

Please refer to these resources:

* [Configuring output for ELK](https://www.elastic.co/guide/en/ecs-logging/java/1.x/setup.html)
* [Adding APM Agent for log correlation](https://www.elastic.co/guide/en/apm/agent/java/master/log-correlation.html)


<!-- ROADMAP -->

## Roadmap

- [ ] Use low-level AspectJ load-time weaving instead of Spring AOP
- [ ] Make the library working with annotated interfaces
- [ ] Save and restore current MDC parameters to raw Map
- [ ] Intercept @Async calls maintaining the same MDC
- [ ] Spring WebFlux support?
- [ ] CDI & JakartaEE support?
  - [ ] Add jboss-log-manager support.
- [ ] Future research:
  - [ ] Annotation-processor based compile-time code enhancement
  - [ ] Agent-based runtime class transformations

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- LICENSE -->

## License

Distributed under the Apache Version 2.0 License. See the license file `LICENSE.md` for more information.

<p align="right">(<a href="#top">back to top</a>)</p>
