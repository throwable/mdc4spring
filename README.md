<!-- ABOUT THE PROJECT -->

## MDC4Spring

Easy and secure creation of Mapped Diagnostic Contexts (MDCs) in declarative form for multiple logging systems using
Spring AOP. Supported logging systems: Slf4J/Logback, Log4J, Log4J2.

### Motivation

_Mapped Diagnostic Context_ provides a way to enrich log messages with contextual information about the execution scope.
This information could be useful to track the execution of the business operation especially when using ELK stack.

Example of SLF4J logging event that contains MDC:

```json
{
  "timestamp": "2022-07-26 09:01:51.482",
  "level": "INFO",
  "thread": "main",
  "mdc": {
    "requestUuid": "b8e0cd40-0cc6-11ed-861d-0242ac120002",
    "sourceIp": "127.0.0.1",
    "instance": "instance1.mybusinessdomain:8080",
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

Different logging libraries provide a similar way of setting MDC parameters using thread-local context and
require a user to control manually the right clearing of them when the execution flow leaves the scope.
So if one forget to clear them correctly it may pollute later traces generated outside the scope or even provoke memory
leaks in thread-pooled environments.

The idea is to use Spring AOP and annotations for MDCs and parameters definitions that automatically will control a
correct clearing of them.

##### Example

```java
class OrderProcessor {
    // define a new MDC that will be cleared automatically when the method returns
    @WithMDC
    public void assignToUser(
            // add method arguments with their values to current MDC
            @MDCParam String orderId, @MDCParam String userId
    ) {
        // programmatically add new parameter to current MDC
        MDC.param("transactionId", this.getCurrentTransactionId());

        // business logic...

        // The trace MDC will contain parameters: orderId, userId, transactionId 
        log.info("User was successfully assigned to order");
        // Parameters defined within the scope of the method
        // will automatically be removed when the method returns
    }
}
```

<p align="right">(<a href="#top">back to top</a>)</p>


<!-- GETTING STARTED -->

## Getting Started

### Prerequisites

The library works with Java 8+ and Spring Boot or Spring Framework environment (does not rely on any specific version).
Currently, it supports Slf4J, Log4j2 and Log4j underlying logging systems. By default, the logging system is detected
using classpath library resolution, but you can change this behavior specifying
the ```com.github.throwable.mdc4spring.loggers.LoggerMDCAdapter```
system property.

### Installation

Add the following dependency to your project's build file.

#### Maven

```xml
<dependency>
    <groupId>com.github.throwable.mdc4spring</groupId>
    <artifactId>mdc4spring</artifactId>
    <version>1.0</version>
</dependency>
```

#### Gradle

```groovy
dependencies {
    compile("com.github.throwable.mdc4spring:mdc4spring:1.0")
}
```

If you are using Spring Boot the configuration will be applied automatically by autoconfiguration mechanism.
In case if you are not using Spring Boot you have to import ```com.github.throwable.mdc4spring.spring.MDCAutoConfiguration```
manually.

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- USAGE EXAMPLES -->

## Usage


#### Method argument parameters

Simple usage: logging trace with will contain MDC with ```orderId``` and ```userId``` parameters that will automatically be removed after the method returns.

```java
class OrderProcessor {
    @WithMDC
    public void assignToUser(@MDCParam String orderId, @MDCParam String userId) {
        // business logic...
        log.info("User assigned to order");
    }
}
```

By default, argument names will be used as parameter names (SEE CONSIDERATIONS), but you can define custom names for them.

```java
class OrderProcessor {
    @WithMDC
    public void assignToUser(@MDCParam("order.id") String orderId, @MDCParam("user.id") String userId) {
        log.info("User assigned to order");
    }
}
```

The value of any argument can be converted inline using [Spring expression language](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions).
In this case ```order.id``` and ```user.id``` parameters will be set to ```order.getId()``` and ```user.getId()``` respectively.

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

#### Evaluated parameters

For any method a set of MDC parameters can be defined, whose values are evaluated and added to MDC during each method execution.
The ```eval``` attribute defines a SpEL expression that can contain references to the evaluation context. The following example contains:

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

1. Evaluating static method of some class.
2. Accessing Spring configuration property using ```#environment``` variable
3. Obtaining JVM system property using ```#systemProperties``` variable
4. Accessing a property of the local bean. The whole bean is set as a ```#root``` object for expression evaluation.
   A property may be a getter or a local field of any visibility scope.
5. All arguments are available within the expression using ```#argumentName``` variable.
6. Variables ```#className``` and ```#methodName``` contain fully-qualified class name and method name respectively.
7. Using ```@beanName``` notation you can reference any named bean within the Spring Application Context.

#### MDC and method scope

```@WithMDC``` and ```@MDCParam``` annotations applied to all methods of the class may be defined at class level.

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

Named MDC adds its name as a prefix to all its parameters.  

```java
@WithMDC("order")
class OrderProcessor {
    // MDC parameter name will be "order.id"
    public void createOrder(@MDCParam(name = "id", eval = "id") Order order) {
    }
}
```

Each method in the execution chain annotated with ```@WithMDC``` will create a new 'nested' MDC that will be closed 
after the method returns removing all parameters defined inside it.

```java
class OrderProcessor {
    @WithMDC
    public void createOrder(@MDCParam(name = "order.id", eval = "id") Order order) {
        // Call method using 'nested' MDC
        Customer customer = customerRepository.findCustomerByName(order.getCustomerId());
        // after the 'nested' MDC call returns the customerId parameter will no longer exist in traces
    }
}
class CustomerRepository {
    // Defines a 'nested' MDC. The parameter customerId belongs to this new MDC and will be cleared
    // after the method ends
    @WithMDC
    public Customer findCustomerById(@MDCParam String customerId) {
        // here the logging trace will have the customerId parameter defined
    }
}
```

But without ```@WithMDC``` each ```@MDCParam``` parameter met in the execution chain remain in current MDC after the method returns.

```java
class OrderProcessor {
    @WithMDC
    public void createOrder(@MDCParam(name = "order.id", eval = "id") Order order) {
        // Call method using 'nested' MDC
        Customer customer = customerRepository.findCustomerByName(order.getCustomerId());
        // after the method call we still have a customerId parameter in our MDC
    }
}
class CustomerRepository {
    // The parameter customerId will be added to current MDC, and it will be kept after method returns.
    public Customer findCustomerById(@MDCParam String customerId) {
        // here the logging trace will have the customerId parameter defined
    }
}
```

#### Defining MDC programmatically

That gives you a full control over MDC scopes and parameter definitions.
Use try-with-resources block to ensure a correct clearing of all defined parameters.

```java
class OrderProcessor {
    public void createOrder(Order order) {
        try (CloseableMDC rootMdc = MDC.create()) {
            // Add param to current MDC
            MDC.param("order.id", order.getId());
            log.info("order.id is added to MDC");
            
            try (CloseableMDC nestedMdc = MDC.create()) {
                // Add param to nested MDC (nearest for scope)
                MDC.param("customer.id", order.getCustomerId());
                log.info("Both order.id and customer.id appear in logging traces");
            }
            log.info("order.id is still remains in traces but customer.id is removes with its MDC");
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
                log.info("Both order.id and customer.id appear in logging traces");
                return customerRepository.findCustomerById(order.getCustomerId());
            });
            log.info("order.id is still remains in traces but customer.id is removes with its MDC");
        });
    }
}
```


_For more examples, please refer to the [Documentation](https://example.com)_

<p align="right">(<a href="#top">back to top</a>)</p>


## Considerations and limitations

### Method invocations

The library uses Spring AOP to intercept annotated method invocations so these considerations must be token into account:

* The method must be invoked from outside the bean scope. All local calls are not intercepted by Spring AOP and any method annotation will be ignored in this case.
  ```java
  class MyBean {
    @Lazy @Autowired MyBean self;
  
    public void publicMethod(@MDCParam String someParam) {
      anotherPublicMethod("'anotherParam' will not be included in MDC because the call is local");
      self.anotherPublicMethod("this call is proxied, so 'anotherParam' will be included");
    }
    public void anotherPublicMethod(@MDCParam String anotherParam) {} 
  } 
  ```
* Spring AOP does not intercept private methods, so if you invoke an inner bean private method, it will have no effect on it.

### Method argument names

By default, Java compiler does not keep method argument names in generated bytecode, so it may cause
possible problems with parameter name resolutions when using ```@MDCParam``` for a method argument.
There are three ways to avoid this problem:

* If you are using Spring Boot and Spring Boot Maven or Gradle plugin all method arguments will already be saved
  with their names in generated bytecode, and no additional action is required.
* If you are not using Spring Boot plugin you may tell to the compiler to preserve method argument names
  by adding ```-parameters``` argument to ```javac``` invocation.
* You may also define explicitly the name of each parameter:
  ```
  public User findUserById({@MDCParam("userId") String userId)
  ```

## Acknowledgements

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
        <version>2.13.3</version>
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
JSON traces will include all MDC parameters.

### Configuring output for Elasticsearch

* [Configuring output for ELK](https://www.elastic.co/guide/en/ecs-logging/java/1.x/setup.html)
* [Adding APM Agent for log correlation](https://www.elastic.co/guide/en/apm/agent/java/master/log-correlation.html)


<!-- ROADMAP -->

## Roadmap

- [x] Add Changelog
- [x] Add back to top links
- [ ] Add Additional Templates w/ Examples
- [ ] Add "components" document to easily copy & paste sections of the readme
- [ ] Multi-language Support
    - [ ] Chinese
    - [ ] Spanish

See the [open issues](https://github.com/othneildrew/Best-README-Template/issues) for a full list of proposed features (
and known issues).

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- LICENSE -->

## License

Distributed under the Apache Version 2.0 License. See the license file `LICENSE.md` for more information.

<p align="right">(<a href="#top">back to top</a>)</p>
