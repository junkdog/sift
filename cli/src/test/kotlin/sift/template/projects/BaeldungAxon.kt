package sift.template.projects

val BAELDUNG_AXON = """
── controller
   └─ OrderRestEndpoint
      ├─ GET /
      │  └─ FindAllOrderedProductsQuery
      │     ├─ InMemoryOrdersEventHandler::handle
      │     ├─ InMemoryOrdersEventHandler::handleStreaming
      │     └─ MongoOrdersEventHandler::handle
      ├─ GET /
      │  └─ OrderUpdatesQuery
      │     ├─ InMemoryOrdersEventHandler::handle
      │     └─ MongoOrdersEventHandler::handle
      ├─ GET /all-orders
      │  └─ FindAllOrderedProductsQuery
      │     ├─ InMemoryOrdersEventHandler::handle
      │     ├─ InMemoryOrdersEventHandler::handleStreaming
      │     └─ MongoOrdersEventHandler::handle
      ├─ GET /total-shipped/{product-id}
      │  └─ TotalProductsShippedQuery
      │     ├─ InMemoryOrdersEventHandler::handle
      │     └─ MongoOrdersEventHandler::handle
      ├─ POST /order
      │  └─ CreateOrderCommand
      │     └─ OrderAggregate::<init>
      │        └─ OrderCreatedEvent
      │           ├─ InMemoryOrdersEventHandler::on
      │           ├─ MongoOrdersEventHandler::on
      │           └─ OrderAggregate::on
      ├─ POST /order/{order-id}
      │  └─ CreateOrderCommand
      │     └─ OrderAggregate::<init>
      │        └─ OrderCreatedEvent
      │           ├─ InMemoryOrdersEventHandler::on
      │           ├─ MongoOrdersEventHandler::on
      │           └─ OrderAggregate::on
      ├─ POST /order/{order-id}/confirm
      │  └─ ConfirmOrderCommand
      │     └─ OrderAggregate::handle
      │        └─ OrderConfirmedEvent
      │           ├─ InMemoryOrdersEventHandler::on
      │           ├─ MongoOrdersEventHandler::on
      │           ├─ OrderAggregate::on
      │           └─ OrderLine::on
      ├─ POST /order/{order-id}/product/{product-id}
      │  └─ AddProductCommand
      │     └─ OrderAggregate::handle
      │        └─ ProductAddedEvent
      │           ├─ InMemoryOrdersEventHandler::on
      │           ├─ MongoOrdersEventHandler::on
      │           └─ OrderAggregate::on
      ├─ POST /order/{order-id}/product/{product-id}/decrement
      │  └─ DecrementProductCountCommand
      │     └─ OrderLine::handle
      │        ├─ ProductCountDecrementedEvent
      │        │  ├─ InMemoryOrdersEventHandler::on
      │        │  ├─ MongoOrdersEventHandler::on
      │        │  └─ OrderLine::on
      │        └─ ProductRemovedEvent
      │           ├─ InMemoryOrdersEventHandler::on
      │           ├─ MongoOrdersEventHandler::on
      │           └─ OrderAggregate::on
      ├─ POST /order/{order-id}/product/{product-id}/increment
      │  └─ IncrementProductCountCommand
      │     └─ OrderLine::handle
      │        └─ ProductCountIncrementedEvent
      │           ├─ InMemoryOrdersEventHandler::on
      │           ├─ MongoOrdersEventHandler::on
      │           └─ OrderLine::on
      ├─ POST /order/{order-id}/ship
      │  └─ ShipOrderCommand
      │     └─ OrderAggregate::handle
      │        └─ OrderShippedEvent
      │           ├─ InMemoryOrdersEventHandler::on
      │           └─ MongoOrdersEventHandler::on
      ├─ POST /ship-order
      │  ├─ AddProductCommand
      │  │  └─ OrderAggregate::handle
      │  │     └─ ProductAddedEvent
      │  │        ├─ InMemoryOrdersEventHandler::on
      │  │        ├─ MongoOrdersEventHandler::on
      │  │        └─ OrderAggregate::on
      │  ├─ ConfirmOrderCommand
      │  │  └─ OrderAggregate::handle
      │  │     └─ OrderConfirmedEvent
      │  │        ├─ InMemoryOrdersEventHandler::on
      │  │        ├─ MongoOrdersEventHandler::on
      │  │        ├─ OrderAggregate::on
      │  │        └─ OrderLine::on
      │  ├─ CreateOrderCommand
      │  │  └─ OrderAggregate::<init>
      │  │     └─ OrderCreatedEvent
      │  │        ├─ InMemoryOrdersEventHandler::on
      │  │        ├─ MongoOrdersEventHandler::on
      │  │        └─ OrderAggregate::on
      │  └─ ShipOrderCommand
      │     └─ OrderAggregate::handle
      │        └─ OrderShippedEvent
      │           ├─ InMemoryOrdersEventHandler::on
      │           └─ MongoOrdersEventHandler::on
      └─ POST /ship-unconfirmed-order
         ├─ AddProductCommand
         │  └─ OrderAggregate::handle
         │     └─ ProductAddedEvent
         │        ├─ InMemoryOrdersEventHandler::on
         │        ├─ MongoOrdersEventHandler::on
         │        └─ OrderAggregate::on
         ├─ CreateOrderCommand
         │  └─ OrderAggregate::<init>
         │     └─ OrderCreatedEvent
         │        ├─ InMemoryOrdersEventHandler::on
         │        ├─ MongoOrdersEventHandler::on
         │        └─ OrderAggregate::on
         └─ ShipOrderCommand
            └─ OrderAggregate::handle
               └─ OrderShippedEvent
                  ├─ InMemoryOrdersEventHandler::on
                  └─ MongoOrdersEventHandler::on
"""

val BAELDUNG_AXON_PROFILE = """
     exec  ety#    in      out
 12.54 ms    68     0 ->     0  ── template
  0.00 ms     0     0 ->     0     ├─ template-scope
  0.00 ms     0     0 ->     0     ├─ template-scope
                    0 ->     0     ├─ fork
  0.00 ms     0     0 ->    34     │  ├─ classes
  0.00 ms     0    34 ->    34     │  ├─ class-scope
                   34 ->    34     │  └─ fork("register controllers")
  0.00 ms     0    34 ->    34     │     ├─ class-scope
  0.01 ms     0    34 ->     1     │     ├─ annotated-by(org.springframework.(stereotype.Controller|web.bind.annotation.RestController)$)
  0.00 ms     1     1 ->     1     │     ├─ register-entity(controller)
                    1 ->     1     │     └─ fork
  0.09 ms     1     1 ->    13     │        ├─ methods(declared)
  0.00 ms     1    13 ->    13     │        ├─ method-scope
                   13 ->    13     │        ├─ fork("register endpoints")
  0.00 ms     1    13 ->    13     │        │  ├─ method-scope
  0.03 ms     1    13 ->    13     │        │  ├─ annotated-by(org.springframework.web.bind.annotation\.[A-Z][^.]*Mapping)
  0.03 ms    14    13 ->    13     │        │  ├─ register-entity(endpoint)
                   13 ->    13     │        │  ├─ fork
  0.10 ms    14    13 ->    11     │        │  │  ├─ read-annotation(org.springframework.web.bind.annotation\.[A-Z][^.]*Mapping::value)
  0.01 ms    14    11 ->    11     │        │  │  └─ update-property(path)
                   13 ->    13     │        │  ├─ fork("extract http method name from annotation")
  0.10 ms    14    13 ->    13     │        │  │  ├─ annotations(org.springframework.web.bind.annotation\.[A-Z][^.]*Mapping)
  0.00 ms    14    13 ->    13     │        │  │  ├─ annotation-scope
                   13 ->    13     │        │  │  └─ fork
  0.13 ms    14    13 ->    13     │        │  │     ├─ read-name
  0.02 ms    14    13 ->    13     │        │  │     ├─ edit-text((replace(\QMapping\E -> )))
  0.02 ms    14    13 ->    13     │        │  │     ├─ edit-text((change-case(uppercase)))
  0.10 ms    14    13 ->    13     │        │  │     └─ update-property(http-method, endpoint)
                   13 ->    13     │        │  ├─ fork("read base path from class-level @RequestMapping")
  0.02 ms    14    13 ->     1     │        │  │  ├─ outer-class
  0.00 ms    14     1 ->     1     │        │  │  ├─ class-scope
                    1 ->     1     │        │  │  └─ fork
  0.01 ms    14     1 ->     0     │        │  │     ├─ read-annotation(RequestMapping::value)
  0.01 ms    14     0 ->     0     │        │  │     └─ update-property(base-path, endpoint)
                   13 ->    13     │        │  └─ fork
  0.02 ms    14    13 ->    11     │        │     ├─ parameters(all)
  0.00 ms    14    11 ->    11     │        │     ├─ parameter-scope
  0.08 ms    14    11 ->     0     │        │     ├─ annotated-by(RequestBody)
                    0 ->     0     │        │     └─ fork
  0.00 ms    14     0 ->     0     │        │        ├─ read-type
  0.01 ms    14     0 ->     0     │        │        └─ update-property(request-object, endpoint)
                   13 ->    13     │        └─ fork-conditional(endpoint exists)
  0.00 ms    14    13 ->    13     │           ├─ method-scope
  0.18 ms    14    13 ->    13     │           └─ register-children(controller[endpoints], endpoint)
                    0 ->     0     ├─ fork("dot graph property configuration")
  0.00 ms    14     0 ->     0     │  ├─ template-scope
                    0 ->     0     │  └─ fork
  0.00 ms    14     0 ->    13     │     ├─ elements-of(endpoint)
  0.00 ms    14    13 ->    13     │     ├─ element-scope
                   13 ->    13     │     ├─ fork
  0.00 ms    14    13 ->    13     │     │  ├─ with-value(0)
  0.08 ms    14    13 ->    13     │     │  └─ update-property(dot-rank, endpoint)
                   13 ->    13     │     └─ fork
  0.00 ms    14    13 ->    13     │        ├─ with-value(node)
  0.07 ms    14    13 ->    13     │        └─ update-property(dot-type, endpoint)
                    0 ->     0     ├─ fork
  0.00 ms    14     0 ->    34     │  ├─ classes
  0.00 ms    14    34 ->    34     │  ├─ class-scope
                   34 ->    34     │  ├─ fork("register aggregates")
  0.00 ms    14    34 ->    34     │  │  ├─ class-scope
  0.09 ms    14    34 ->     1     │  │  ├─ annotated-by(Aggregate)
  0.00 ms    15     1 ->     1     │  │  ├─ register-entity(aggregate)
                    1 ->     1     │  │  ├─ fork
  0.06 ms    15     1 ->     9     │  │  │  ├─ methods(declared + constructors)
  0.00 ms    15     9 ->     9     │  │  │  ├─ method-scope
                    9 ->     9     │  │  │  ├─ fork("register command handlers with aggregate")
  0.00 ms    15     9 ->     9     │  │  │  │  ├─ method-scope
  0.06 ms    15     9 ->     4     │  │  │  │  ├─ annotated-by(CommandHandler)
  0.01 ms    19     4 ->     4     │  │  │  │  ├─ register-entity(command-handler)
                    4 ->     4     │  │  │  │  ├─ fork
  0.00 ms    19     4 ->     4     │  │  │  │  │  ├─ with-value(aggregate)
  0.00 ms    19     4 ->     4     │  │  │  │  │  └─ update-property(dot-id-as)
                    4 ->     4     │  │  │  │  ├─ fork("register command handlers")
  0.01 ms    19     4 ->     4     │  │  │  │  │  ├─ parameters(all)
  0.00 ms    19     4 ->     4     │  │  │  │  │  ├─ parameter-scope
  0.00 ms    19     4 ->     4     │  │  │  │  │  ├─ filter-nth(0)
                    4 ->     4     │  │  │  │  │  ├─ fork
  0.00 ms    19     4 ->     4     │  │  │  │  │  │  ├─ read-type
  0.06 ms    19     4 ->     4     │  │  │  │  │  │  └─ update-property(type, command-handler)
                    4 ->     4     │  │  │  │  │  └─ fork
  0.00 ms    19     4 ->     4     │  │  │  │  │     ├─ explode-type(synthesize)
  0.00 ms    19     4 ->     4     │  │  │  │  │     ├─ class-scope
  0.01 ms    23     4 ->     4     │  │  │  │  │     ├─ register-entity(command)
  0.07 ms    23     4 ->     4     │  │  │  │  │     └─ register-children(command[received-by], command-handler)
  0.07 ms    23     4 ->     4     │  │  │  │  └─ register-children(aggregate[commands], command-handler)
                    9 ->     9     │  │  │  └─ fork("register event sourcing handlers with aggregate")
  0.00 ms    23     9 ->     9     │  │  │     ├─ method-scope
  0.06 ms    23     9 ->     4     │  │  │     ├─ annotated-by(EventSourcingHandler)
  0.01 ms    27     4 ->     4     │  │  │     ├─ register-entity(event-sourcing-handler)
                    4 ->     4     │  │  │     ├─ fork
  0.00 ms    27     4 ->     4     │  │  │     │  ├─ with-value(aggregate)
  0.00 ms    27     4 ->     4     │  │  │     │  └─ update-property(dot-id-as)
                    4 ->     4     │  │  │     ├─ fork("register event handlers")
  0.00 ms    27     4 ->     4     │  │  │     │  ├─ parameters(all)
  0.00 ms    27     4 ->     4     │  │  │     │  ├─ parameter-scope
  0.00 ms    27     4 ->     4     │  │  │     │  ├─ filter-nth(0)
                    4 ->     4     │  │  │     │  ├─ fork
  0.00 ms    27     4 ->     4     │  │  │     │  │  ├─ read-type
  0.02 ms    27     4 ->     4     │  │  │     │  │  └─ update-property(type, event-sourcing-handler)
                    4 ->     4     │  │  │     │  └─ fork
  0.00 ms    27     4 ->     4     │  │  │     │     ├─ explode-type(synthesize)
  0.00 ms    27     4 ->     4     │  │  │     │     ├─ class-scope
  0.01 ms    31     4 ->     4     │  │  │     │     ├─ register-entity(event)
  0.08 ms    31     4 ->     4     │  │  │     │     └─ register-children(event[received-by], event-sourcing-handler)
  0.04 ms    31     4 ->     4     │  │  │     └─ register-children(aggregate[events], event-sourcing-handler)
                    1 ->     1     │  │  ├─ fork("register member aggregates")
  0.00 ms    31     1 ->     3     │  │  │  ├─ fields()
  0.00 ms    31     3 ->     3     │  │  │  ├─ field-scope
  0.02 ms    31     3 ->     1     │  │  │  ├─ annotated-by(AggregateMember)
                    1 ->     1     │  │  │  └─ fork
  0.00 ms    31     1 ->     1     │  │  │     ├─ field-into-signature
  0.00 ms    31     1 ->     1     │  │  │     ├─ signature-scope
                    1 ->     1     │  │  │     ├─ fork
  0.00 ms    31     1 ->     1     │  │  │     │  ├─ filter-nth(0)
  0.00 ms    31     1 ->     1     │  │  │     │  ├─ signature-scope
                    1 ->     1     │  │  │     │  └─ fork
  0.00 ms    31     1 ->     0     │  │  │     │     ├─ explode-raw-type
  0.00 ms    31     0 ->     0     │  │  │     │     ├─ class-scope
  0.00 ms    31     0 ->     0     │  │  │     │     ├─ register-entity(aggregate-member)
                    0 ->     0     │  │  │     │     ├─ fork
  0.00 ms    31     0 ->     0     │  │  │     │     │  ├─ methods(declared + constructors)
  0.00 ms    31     0 ->     0     │  │  │     │     │  ├─ method-scope
                    0 ->     0     │  │  │     │     │  ├─ fork("register command handlers with aggregate")
  0.00 ms    31     0 ->     0     │  │  │     │     │  │  ├─ method-scope
  0.00 ms    31     0 ->     0     │  │  │     │     │  │  ├─ annotated-by(CommandHandler)
  0.00 ms    31     0 ->     0     │  │  │     │     │  │  ├─ register-entity(command-handler)
                    0 ->     0     │  │  │     │     │  │  ├─ fork
  0.00 ms    31     0 ->     0     │  │  │     │     │  │  │  ├─ with-value(aggregate-member)
  0.00 ms    31     0 ->     0     │  │  │     │     │  │  │  └─ update-property(dot-id-as)
                    0 ->     0     │  │  │     │     │  │  ├─ fork("register command handlers")
  0.00 ms    31     0 ->     0     │  │  │     │     │  │  │  ├─ parameters(all)
  0.00 ms    31     0 ->     0     │  │  │     │     │  │  │  ├─ parameter-scope
  0.00 ms    31     0 ->     0     │  │  │     │     │  │  │  ├─ filter-nth(0)
                    0 ->     0     │  │  │     │     │  │  │  ├─ fork
  0.00 ms    31     0 ->     0     │  │  │     │     │  │  │  │  ├─ read-type
  0.05 ms    31     0 ->     0     │  │  │     │     │  │  │  │  └─ update-property(type, command-handler)
                    0 ->     0     │  │  │     │     │  │  │  └─ fork
  0.00 ms    31     0 ->     0     │  │  │     │     │  │  │     ├─ explode-type(synthesize)
  0.00 ms    31     0 ->     0     │  │  │     │     │  │  │     ├─ class-scope
  0.00 ms    31     0 ->     0     │  │  │     │     │  │  │     ├─ register-entity(command)
  0.00 ms    31     0 ->     0     │  │  │     │     │  │  │     └─ register-children(command[received-by], command-handler)
  0.00 ms    31     0 ->     0     │  │  │     │     │  │  └─ register-children(aggregate-member[commands], command-handler)
                    0 ->     0     │  │  │     │     │  └─ fork("register event sourcing handlers with aggregate")
  0.00 ms    31     0 ->     0     │  │  │     │     │     ├─ method-scope
  0.00 ms    31     0 ->     0     │  │  │     │     │     ├─ annotated-by(EventSourcingHandler)
  0.00 ms    31     0 ->     0     │  │  │     │     │     ├─ register-entity(event-sourcing-handler)
                    0 ->     0     │  │  │     │     │     ├─ fork
  0.00 ms    31     0 ->     0     │  │  │     │     │     │  ├─ with-value(aggregate-member)
  0.00 ms    31     0 ->     0     │  │  │     │     │     │  └─ update-property(dot-id-as)
                    0 ->     0     │  │  │     │     │     ├─ fork("register event handlers")
  0.00 ms    31     0 ->     0     │  │  │     │     │     │  ├─ parameters(all)
  0.00 ms    31     0 ->     0     │  │  │     │     │     │  ├─ parameter-scope
  0.00 ms    31     0 ->     0     │  │  │     │     │     │  ├─ filter-nth(0)
                    0 ->     0     │  │  │     │     │     │  ├─ fork
  0.00 ms    31     0 ->     0     │  │  │     │     │     │  │  ├─ read-type
  0.02 ms    31     0 ->     0     │  │  │     │     │     │  │  └─ update-property(type, event-sourcing-handler)
                    0 ->     0     │  │  │     │     │     │  └─ fork
  0.00 ms    31     0 ->     0     │  │  │     │     │     │     ├─ explode-type(synthesize)
  0.00 ms    31     0 ->     0     │  │  │     │     │     │     ├─ class-scope
  0.00 ms    31     0 ->     0     │  │  │     │     │     │     ├─ register-entity(event)
  0.00 ms    31     0 ->     0     │  │  │     │     │     │     └─ register-children(event[received-by], event-sourcing-handler)
  0.00 ms    31     0 ->     0     │  │  │     │     │     └─ register-children(aggregate-member[events], event-sourcing-handler)
                    0 ->     0     │  │  │     │     └─ fork
  0.00 ms    31     0 ->     0     │  │  │     │        ├─ read-name
  0.01 ms    31     0 ->     0     │  │  │     │        └─ update-property(member, aggregate-member)
  0.01 ms    31     1 ->     1     │  │  │     ├─ filter-signature(^(.+\.|)Map<|$)
                    1 ->     1     │  │  │     └─ fork
  0.00 ms    31     1 ->     1     │  │  │        ├─ filter-nth(1)
  0.00 ms    31     1 ->     1     │  │  │        ├─ signature-scope
                    1 ->     1     │  │  │        └─ fork
  0.00 ms    31     1 ->     1     │  │  │           ├─ explode-raw-type
  0.00 ms    31     1 ->     1     │  │  │           ├─ class-scope
  0.00 ms    32     1 ->     1     │  │  │           ├─ register-entity(aggregate-member)
                    1 ->     1     │  │  │           ├─ fork
  0.06 ms    32     1 ->     8     │  │  │           │  ├─ methods(declared + constructors)
  0.00 ms    32     8 ->     8     │  │  │           │  ├─ method-scope
                    8 ->     8     │  │  │           │  ├─ fork("register command handlers with aggregate")
  0.00 ms    32     8 ->     8     │  │  │           │  │  ├─ method-scope
  0.04 ms    32     8 ->     2     │  │  │           │  │  ├─ annotated-by(CommandHandler)
  0.00 ms    34     2 ->     2     │  │  │           │  │  ├─ register-entity(command-handler)
                    2 ->     2     │  │  │           │  │  ├─ fork
  0.00 ms    34     2 ->     2     │  │  │           │  │  │  ├─ with-value(aggregate-member)
  0.00 ms    34     2 ->     2     │  │  │           │  │  │  └─ update-property(dot-id-as)
                    2 ->     2     │  │  │           │  │  ├─ fork("register command handlers")
  0.00 ms    34     2 ->     2     │  │  │           │  │  │  ├─ parameters(all)
  0.00 ms    34     2 ->     2     │  │  │           │  │  │  ├─ parameter-scope
  0.00 ms    34     2 ->     2     │  │  │           │  │  │  ├─ filter-nth(0)
                    2 ->     2     │  │  │           │  │  │  ├─ fork
  0.00 ms    34     2 ->     2     │  │  │           │  │  │  │  ├─ read-type
  0.05 ms    34     2 ->     2     │  │  │           │  │  │  │  └─ update-property(type, command-handler)
                    2 ->     2     │  │  │           │  │  │  └─ fork
  0.00 ms    34     2 ->     2     │  │  │           │  │  │     ├─ explode-type(synthesize)
  0.00 ms    34     2 ->     2     │  │  │           │  │  │     ├─ class-scope
  0.00 ms    36     2 ->     2     │  │  │           │  │  │     ├─ register-entity(command)
  0.09 ms    36     2 ->     2     │  │  │           │  │  │     └─ register-children(command[received-by], command-handler)
  0.03 ms    36     2 ->     2     │  │  │           │  │  └─ register-children(aggregate-member[commands], command-handler)
                    8 ->     8     │  │  │           │  └─ fork("register event sourcing handlers with aggregate")
  0.00 ms    36     8 ->     8     │  │  │           │     ├─ method-scope
  0.07 ms    36     8 ->     3     │  │  │           │     ├─ annotated-by(EventSourcingHandler)
  0.01 ms    39     3 ->     3     │  │  │           │     ├─ register-entity(event-sourcing-handler)
                    3 ->     3     │  │  │           │     ├─ fork
  0.00 ms    39     3 ->     3     │  │  │           │     │  ├─ with-value(aggregate-member)
  0.00 ms    39     3 ->     3     │  │  │           │     │  └─ update-property(dot-id-as)
                    3 ->     3     │  │  │           │     ├─ fork("register event handlers")
  0.00 ms    39     3 ->     3     │  │  │           │     │  ├─ parameters(all)
  0.00 ms    39     3 ->     3     │  │  │           │     │  ├─ parameter-scope
  0.00 ms    39     3 ->     3     │  │  │           │     │  ├─ filter-nth(0)
                    3 ->     3     │  │  │           │     │  ├─ fork
  0.00 ms    39     3 ->     3     │  │  │           │     │  │  ├─ read-type
  0.06 ms    39     3 ->     3     │  │  │           │     │  │  └─ update-property(type, event-sourcing-handler)
                    3 ->     3     │  │  │           │     │  └─ fork
  0.00 ms    39     3 ->     3     │  │  │           │     │     ├─ explode-type(synthesize)
  0.00 ms    39     3 ->     3     │  │  │           │     │     ├─ class-scope
  0.01 ms    41     3 ->     3     │  │  │           │     │     ├─ register-entity(event)
  0.09 ms    41     3 ->     3     │  │  │           │     │     └─ register-children(event[received-by], event-sourcing-handler)
  0.08 ms    41     3 ->     3     │  │  │           │     └─ register-children(aggregate-member[events], event-sourcing-handler)
                    1 ->     1     │  │  │           └─ fork
  0.01 ms    41     1 ->     1     │  │  │              ├─ read-name
  0.05 ms    41     1 ->     1     │  │  │              └─ update-property(member, aggregate-member)
                    1 ->     1     │  │  └─ fork
  0.01 ms    41     1 ->     1     │  │     ├─ read-name
  0.04 ms    41     1 ->     1     │  │     └─ update-property(aggregate, aggregate-member)
                   34 ->    34     │  └─ fork("register projections")
  0.00 ms    41    34 ->    34     │     ├─ class-scope
                   34 ->    34     │     ├─ fork("scan suspect classes")
  0.00 ms    41    34 ->    34     │     │  ├─ class-scope
  0.02 ms    41    34 ->     0     │     │  ├─ filter(Projection)
                    0 ->     0     │     │  ├─ fork
  0.00 ms    41     0 ->     0     │     │  │  ├─ methods(declared)
  0.00 ms    41     0 ->     0     │     │  │  ├─ method-scope
  0.00 ms    41     0 ->     0     │     │  │  ├─ annotated-by(EventHandler)
                    0 ->     0     │     │  │  └─ fork("identified projection")
  0.00 ms    41     0 ->     0     │     │  │     ├─ outer-class
  0.00 ms    41     0 ->     0     │     │  │     ├─ class-scope
  0.00 ms    41     0 ->     0     │     │  │     ├─ register-entity(projection)
                    0 ->     0     │     │  │     └─ fork
  0.00 ms    41     0 ->     0     │     │  │        ├─ methods(declared)
  0.00 ms    41     0 ->     0     │     │  │        ├─ method-scope
                    0 ->     0     │     │  │        ├─ fork("register event handlers with projection")
  0.00 ms    41     0 ->     0     │     │  │        │  ├─ method-scope
  0.00 ms    41     0 ->     0     │     │  │        │  ├─ annotated-by(EventHandler)
  0.00 ms    41     0 ->     0     │     │  │        │  ├─ register-entity(event-handler)
                    0 ->     0     │     │  │        │  ├─ fork
  0.00 ms    41     0 ->     0     │     │  │        │  │  ├─ with-value(projection)
  0.00 ms    41     0 ->     0     │     │  │        │  │  └─ update-property(dot-id-as)
                    0 ->     0     │     │  │        │  ├─ fork("register event handlers")
  0.00 ms    41     0 ->     0     │     │  │        │  │  ├─ parameters(all)
  0.00 ms    41     0 ->     0     │     │  │        │  │  ├─ parameter-scope
  0.00 ms    41     0 ->     0     │     │  │        │  │  ├─ filter-nth(0)
                    0 ->     0     │     │  │        │  │  ├─ fork
  0.00 ms    41     0 ->     0     │     │  │        │  │  │  ├─ read-type
  0.01 ms    41     0 ->     0     │     │  │        │  │  │  └─ update-property(type, event-handler)
                    0 ->     0     │     │  │        │  │  └─ fork
  0.00 ms    41     0 ->     0     │     │  │        │  │     ├─ explode-type(synthesize)
  0.00 ms    41     0 ->     0     │     │  │        │  │     ├─ class-scope
  0.00 ms    41     0 ->     0     │     │  │        │  │     ├─ register-entity(event)
  0.00 ms    41     0 ->     0     │     │  │        │  │     └─ register-children(event[received-by], event-handler)
  0.00 ms    41     0 ->     0     │     │  │        │  └─ register-children(projection[events], event-handler)
                    0 ->     0     │     │  │        └─ fork("register event sourcing handlers with projection")
  0.00 ms    41     0 ->     0     │     │  │           ├─ method-scope
  0.00 ms    41     0 ->     0     │     │  │           ├─ annotated-by(QueryHandler)
  0.00 ms    41     0 ->     0     │     │  │           ├─ register-entity(query-handler)
                    0 ->     0     │     │  │           ├─ fork
  0.00 ms    41     0 ->     0     │     │  │           │  ├─ with-value(projection)
  0.00 ms    41     0 ->     0     │     │  │           │  └─ update-property(dot-id-as)
                    0 ->     0     │     │  │           ├─ fork("register query handlers")
  0.00 ms    41     0 ->     0     │     │  │           │  ├─ parameters(all)
  0.00 ms    41     0 ->     0     │     │  │           │  ├─ parameter-scope
  0.00 ms    41     0 ->     0     │     │  │           │  ├─ filter-nth(0)
                    0 ->     0     │     │  │           │  ├─ fork
  0.00 ms    41     0 ->     0     │     │  │           │  │  ├─ read-type
  0.01 ms    41     0 ->     0     │     │  │           │  │  └─ update-property(type, query-handler)
                    0 ->     0     │     │  │           │  └─ fork
  0.00 ms    41     0 ->     0     │     │  │           │     ├─ explode-type(synthesize)
  0.00 ms    41     0 ->     0     │     │  │           │     ├─ class-scope
  0.00 ms    41     0 ->     0     │     │  │           │     ├─ register-entity(query)
  0.00 ms    41     0 ->     0     │     │  │           │     └─ register-children(query[received-by], query-handler)
  0.00 ms    41     0 ->     0     │     │  │           └─ register-children(projection[queries], query-handler)
                    0 ->     0     │     │  └─ fork
  0.00 ms    41     0 ->     0     │     │     ├─ methods(declared)
  0.00 ms    41     0 ->     0     │     │     ├─ method-scope
  0.00 ms    41     0 ->     0     │     │     ├─ annotated-by(QueryHandler)
                    0 ->     0     │     │     └─ fork("identified projection")
  0.00 ms    41     0 ->     0     │     │        ├─ outer-class
  0.00 ms    41     0 ->     0     │     │        ├─ class-scope
  0.00 ms    41     0 ->     0     │     │        ├─ register-entity(projection)
                    0 ->     0     │     │        └─ fork
  0.00 ms    41     0 ->     0     │     │           ├─ methods(declared)
  0.00 ms    41     0 ->     0     │     │           ├─ method-scope
                    0 ->     0     │     │           ├─ fork("register event handlers with projection")
  0.00 ms    41     0 ->     0     │     │           │  ├─ method-scope
  0.00 ms    41     0 ->     0     │     │           │  ├─ annotated-by(EventHandler)
  0.00 ms    41     0 ->     0     │     │           │  ├─ register-entity(event-handler)
                    0 ->     0     │     │           │  ├─ fork
  0.00 ms    41     0 ->     0     │     │           │  │  ├─ with-value(projection)
  0.00 ms    41     0 ->     0     │     │           │  │  └─ update-property(dot-id-as)
                    0 ->     0     │     │           │  ├─ fork("register event handlers")
  0.00 ms    41     0 ->     0     │     │           │  │  ├─ parameters(all)
  0.00 ms    41     0 ->     0     │     │           │  │  ├─ parameter-scope
  0.00 ms    41     0 ->     0     │     │           │  │  ├─ filter-nth(0)
                    0 ->     0     │     │           │  │  ├─ fork
  0.00 ms    41     0 ->     0     │     │           │  │  │  ├─ read-type
  0.01 ms    41     0 ->     0     │     │           │  │  │  └─ update-property(type, event-handler)
                    0 ->     0     │     │           │  │  └─ fork
  0.00 ms    41     0 ->     0     │     │           │  │     ├─ explode-type(synthesize)
  0.00 ms    41     0 ->     0     │     │           │  │     ├─ class-scope
  0.00 ms    41     0 ->     0     │     │           │  │     ├─ register-entity(event)
  0.00 ms    41     0 ->     0     │     │           │  │     └─ register-children(event[received-by], event-handler)
  0.00 ms    41     0 ->     0     │     │           │  └─ register-children(projection[events], event-handler)
                    0 ->     0     │     │           └─ fork("register event sourcing handlers with projection")
  0.00 ms    41     0 ->     0     │     │              ├─ method-scope
  0.00 ms    41     0 ->     0     │     │              ├─ annotated-by(QueryHandler)
  0.00 ms    41     0 ->     0     │     │              ├─ register-entity(query-handler)
                    0 ->     0     │     │              ├─ fork
  0.00 ms    41     0 ->     0     │     │              │  ├─ with-value(projection)
  0.00 ms    41     0 ->     0     │     │              │  └─ update-property(dot-id-as)
                    0 ->     0     │     │              ├─ fork("register query handlers")
  0.00 ms    41     0 ->     0     │     │              │  ├─ parameters(all)
  0.00 ms    41     0 ->     0     │     │              │  ├─ parameter-scope
  0.00 ms    41     0 ->     0     │     │              │  ├─ filter-nth(0)
                    0 ->     0     │     │              │  ├─ fork
  0.00 ms    41     0 ->     0     │     │              │  │  ├─ read-type
  0.01 ms    41     0 ->     0     │     │              │  │  └─ update-property(type, query-handler)
                    0 ->     0     │     │              │  └─ fork
  0.00 ms    41     0 ->     0     │     │              │     ├─ explode-type(synthesize)
  0.00 ms    41     0 ->     0     │     │              │     ├─ class-scope
  0.00 ms    41     0 ->     0     │     │              │     ├─ register-entity(query)
  0.00 ms    41     0 ->     0     │     │              │     └─ register-children(query[received-by], query-handler)
  0.00 ms    41     0 ->     0     │     │              └─ register-children(projection[queries], query-handler)
                   34 ->    34     │     └─ fork("id by @ProcessingGroup")
  0.00 ms    41    34 ->    34     │        ├─ class-scope
  0.15 ms    41    34 ->     2     │        ├─ annotated-by(ProcessingGroup)
                    2 ->     2     │        ├─ fork
  0.06 ms    41     2 ->    33     │        │  ├─ methods(declared)
  0.00 ms    41    33 ->    33     │        │  ├─ method-scope
  0.20 ms    41    33 ->    14     │        │  ├─ annotated-by(EventHandler)
                   14 ->    14     │        │  └─ fork("identified projection")
  0.02 ms    41    14 ->     2     │        │     ├─ outer-class
  0.00 ms    41     2 ->     2     │        │     ├─ class-scope
  0.01 ms    43     2 ->     2     │        │     ├─ register-entity(projection)
                    2 ->     2     │        │     └─ fork
  0.10 ms    43     2 ->    33     │        │        ├─ methods(declared)
  0.00 ms    43    33 ->    33     │        │        ├─ method-scope
                   33 ->    33     │        │        ├─ fork("register event handlers with projection")
  0.00 ms    43    33 ->    33     │        │        │  ├─ method-scope
  0.13 ms    43    33 ->    14     │        │        │  ├─ annotated-by(EventHandler)
  0.03 ms    57    14 ->    14     │        │        │  ├─ register-entity(event-handler)
                   14 ->    14     │        │        │  ├─ fork
  0.00 ms    57    14 ->    14     │        │        │  │  ├─ with-value(projection)
  0.01 ms    57    14 ->    14     │        │        │  │  └─ update-property(dot-id-as)
                   14 ->    14     │        │        │  ├─ fork("register event handlers")
  0.07 ms    57    14 ->    14     │        │        │  │  ├─ parameters(all)
  0.00 ms    57    14 ->    14     │        │        │  │  ├─ parameter-scope
  0.00 ms    57    14 ->    14     │        │        │  │  ├─ filter-nth(0)
                   14 ->    14     │        │        │  │  ├─ fork
  0.05 ms    57    14 ->    14     │        │        │  │  │  ├─ read-type
  0.16 ms    57    14 ->    14     │        │        │  │  │  └─ update-property(type, event-handler)
                   14 ->    14     │        │        │  │  └─ fork
  0.07 ms    57    14 ->    14     │        │        │  │     ├─ explode-type(synthesize)
  0.00 ms    57    14 ->    14     │        │        │  │     ├─ class-scope
  0.02 ms    58    14 ->    14     │        │        │  │     ├─ register-entity(event)
  0.20 ms    58    14 ->    14     │        │        │  │     └─ register-children(event[received-by], event-handler)
  0.07 ms    58    14 ->    14     │        │        │  └─ register-children(projection[events], event-handler)
                   33 ->    33     │        │        └─ fork("register event sourcing handlers with projection")
  0.00 ms    58    33 ->    33     │        │           ├─ method-scope
  0.13 ms    58    33 ->     7     │        │           ├─ annotated-by(QueryHandler)
  0.01 ms    65     7 ->     7     │        │           ├─ register-entity(query-handler)
                    7 ->     7     │        │           ├─ fork
  0.00 ms    65     7 ->     7     │        │           │  ├─ with-value(projection)
  0.00 ms    65     7 ->     7     │        │           │  └─ update-property(dot-id-as)
                    7 ->     7     │        │           ├─ fork("register query handlers")
  0.05 ms    65     7 ->     7     │        │           │  ├─ parameters(all)
  0.00 ms    65     7 ->     7     │        │           │  ├─ parameter-scope
  0.00 ms    65     7 ->     7     │        │           │  ├─ filter-nth(0)
                    7 ->     7     │        │           │  ├─ fork
  0.01 ms    65     7 ->     7     │        │           │  │  ├─ read-type
  0.07 ms    65     7 ->     7     │        │           │  │  └─ update-property(type, query-handler)
                    7 ->     7     │        │           │  └─ fork
  0.02 ms    65     7 ->     7     │        │           │     ├─ explode-type(synthesize)
  0.00 ms    65     7 ->     7     │        │           │     ├─ class-scope
  0.01 ms    68     7 ->     7     │        │           │     ├─ register-entity(query)
  0.06 ms    68     7 ->     7     │        │           │     └─ register-children(query[received-by], query-handler)
  0.04 ms    68     7 ->     7     │        │           └─ register-children(projection[queries], query-handler)
                    2 ->     2     │        └─ fork
  0.09 ms    68     2 ->    33     │           ├─ methods(declared)
  0.00 ms    68    33 ->    33     │           ├─ method-scope
  0.13 ms    68    33 ->     7     │           ├─ annotated-by(QueryHandler)
                    7 ->     7     │           └─ fork("identified projection")
  0.07 ms    68     7 ->     2     │              ├─ outer-class
  0.00 ms    68     2 ->     2     │              ├─ class-scope
  0.01 ms    68     2 ->     2     │              ├─ register-entity(projection)
                    2 ->     2     │              └─ fork
  0.18 ms    68     2 ->    33     │                 ├─ methods(declared)
  0.00 ms    68    33 ->    33     │                 ├─ method-scope
                   33 ->    33     │                 ├─ fork("register event handlers with projection")
  0.00 ms    68    33 ->    33     │                 │  ├─ method-scope
  0.14 ms    68    33 ->    14     │                 │  ├─ annotated-by(EventHandler)
  0.02 ms    68    14 ->    14     │                 │  ├─ register-entity(event-handler)
                   14 ->    14     │                 │  ├─ fork
  0.00 ms    68    14 ->    14     │                 │  │  ├─ with-value(projection)
  0.00 ms    68    14 ->    14     │                 │  │  └─ update-property(dot-id-as)
                   14 ->    14     │                 │  ├─ fork("register event handlers")
  0.11 ms    68    14 ->    14     │                 │  │  ├─ parameters(all)
  0.00 ms    68    14 ->    14     │                 │  │  ├─ parameter-scope
  0.00 ms    68    14 ->    14     │                 │  │  ├─ filter-nth(0)
                   14 ->    14     │                 │  │  ├─ fork
  0.02 ms    68    14 ->    14     │                 │  │  │  ├─ read-type
  0.41 ms    68    14 ->    14     │                 │  │  │  └─ update-property(type, event-handler)
                   14 ->    14     │                 │  │  └─ fork
  0.19 ms    68    14 ->    14     │                 │  │     ├─ explode-type(synthesize)
  0.00 ms    68    14 ->    14     │                 │  │     ├─ class-scope
  0.02 ms    68    14 ->    14     │                 │  │     ├─ register-entity(event)
  0.31 ms    68    14 ->    14     │                 │  │     └─ register-children(event[received-by], event-handler)
  0.09 ms    68    14 ->    14     │                 │  └─ register-children(projection[events], event-handler)
                   33 ->    33     │                 └─ fork("register event sourcing handlers with projection")
  0.00 ms    68    33 ->    33     │                    ├─ method-scope
  0.14 ms    68    33 ->     7     │                    ├─ annotated-by(QueryHandler)
  0.01 ms    68     7 ->     7     │                    ├─ register-entity(query-handler)
                    7 ->     7     │                    ├─ fork
  0.00 ms    68     7 ->     7     │                    │  ├─ with-value(projection)
  0.00 ms    68     7 ->     7     │                    │  └─ update-property(dot-id-as)
                    7 ->     7     │                    ├─ fork("register query handlers")
  0.05 ms    68     7 ->     7     │                    │  ├─ parameters(all)
  0.00 ms    68     7 ->     7     │                    │  ├─ parameter-scope
  0.00 ms    68     7 ->     7     │                    │  ├─ filter-nth(0)
                    7 ->     7     │                    │  ├─ fork
  0.01 ms    68     7 ->     7     │                    │  │  ├─ read-type
  0.14 ms    68     7 ->     7     │                    │  │  └─ update-property(type, query-handler)
                    7 ->     7     │                    │  └─ fork
  0.16 ms    68     7 ->     7     │                    │     ├─ explode-type(synthesize)
  0.00 ms    68     7 ->     7     │                    │     ├─ class-scope
  0.01 ms    68     7 ->     7     │                    │     ├─ register-entity(query)
  0.15 ms    68     7 ->     7     │                    │     └─ register-children(query[received-by], query-handler)
  0.09 ms    68     7 ->     7     │                    └─ register-children(projection[queries], query-handler)
                    0 ->     0     ├─ fork("wire commands, events and queries")
  0.00 ms    68     0 ->     0     │  ├─ template-scope
                    0 ->     0     │  ├─ fork
  0.00 ms    68     0 ->     6     │  │  ├─ methods-of(command-handler)
  0.00 ms    68     6 ->     6     │  │  ├─ method-scope
  0.16 ms    68     6 ->     6     │  │  └─ register-children(command-handler, command.instantiations)
                    0 ->     0     │  ├─ fork
  0.00 ms    68     0 ->     6     │  │  ├─ methods-of(command-handler)
  0.00 ms    68     6 ->     6     │  │  ├─ method-scope
  0.04 ms    68     6 ->     6     │  │  └─ register-children(command-handler, event.instantiations)
                    0 ->     0     │  ├─ fork
  0.00 ms    68     0 ->     6     │  │  ├─ methods-of(command-handler)
  0.00 ms    68     6 ->     6     │  │  ├─ method-scope
  0.03 ms    68     6 ->     6     │  │  └─ register-children(command-handler, query.instantiations)
                    0 ->     0     │  ├─ fork
  0.00 ms    68     0 ->    13     │  │  ├─ methods-of(endpoint)
  0.00 ms    68    13 ->    13     │  │  ├─ method-scope
  0.35 ms    68    13 ->    13     │  │  └─ register-children(endpoint, command.instantiations)
                    0 ->     0     │  ├─ fork
  0.00 ms    68     0 ->    13     │  │  ├─ methods-of(endpoint)
  0.00 ms    68    13 ->    13     │  │  ├─ method-scope
  0.07 ms    68    13 ->    13     │  │  └─ register-children(endpoint, event.instantiations)
                    0 ->     0     │  ├─ fork
  0.00 ms    68     0 ->    13     │  │  ├─ methods-of(endpoint)
  0.00 ms    68    13 ->    13     │  │  ├─ method-scope
  0.06 ms    68    13 ->    13     │  │  └─ register-children(endpoint, query.instantiations)
                    0 ->     0     │  ├─ fork
  0.00 ms    68     0 ->    14     │  │  ├─ methods-of(event-handler)
  0.00 ms    68    14 ->    14     │  │  ├─ method-scope
  1.16 ms    68    14 ->    14     │  │  └─ register-children(event-handler, command.instantiations)
                    0 ->     0     │  ├─ fork
  0.00 ms    68     0 ->    14     │  │  ├─ methods-of(event-handler)
  0.00 ms    68    14 ->    14     │  │  ├─ method-scope
  0.14 ms    68    14 ->    14     │  │  └─ register-children(event-handler, event.instantiations)
                    0 ->     0     │  ├─ fork
  0.00 ms    68     0 ->    14     │  │  ├─ methods-of(event-handler)
  0.00 ms    68    14 ->    14     │  │  ├─ method-scope
  0.14 ms    68    14 ->    14     │  │  └─ register-children(event-handler, query.instantiations)
                    0 ->     0     │  ├─ fork
  0.00 ms    68     0 ->     7     │  │  ├─ methods-of(event-sourcing-handler)
  0.00 ms    68     7 ->     7     │  │  ├─ method-scope
  0.05 ms    68     7 ->     7     │  │  └─ register-children(event-sourcing-handler, command.instantiations)
                    0 ->     0     │  ├─ fork
  0.00 ms    68     0 ->     7     │  │  ├─ methods-of(event-sourcing-handler)
  0.00 ms    68     7 ->     7     │  │  ├─ method-scope
  0.02 ms    68     7 ->     7     │  │  └─ register-children(event-sourcing-handler, event.instantiations)
                    0 ->     0     │  ├─ fork
  0.00 ms    68     0 ->     7     │  │  ├─ methods-of(event-sourcing-handler)
  0.00 ms    68     7 ->     7     │  │  ├─ method-scope
  0.02 ms    68     7 ->     7     │  │  └─ register-children(event-sourcing-handler, query.instantiations)
                    0 ->     0     │  ├─ fork
  0.00 ms    68     0 ->     7     │  │  ├─ methods-of(query-handler)
  0.00 ms    68     7 ->     7     │  │  ├─ method-scope
  0.23 ms    68     7 ->     7     │  │  └─ register-children(query-handler, command.instantiations)
                    0 ->     0     │  ├─ fork
  0.00 ms    68     0 ->     7     │  │  ├─ methods-of(query-handler)
  0.00 ms    68     7 ->     7     │  │  ├─ method-scope
  0.03 ms    68     7 ->     7     │  │  └─ register-children(query-handler, event.instantiations)
                    0 ->     0     │  └─ fork
  0.00 ms    68     0 ->     7     │     ├─ methods-of(query-handler)
  0.00 ms    68     7 ->     7     │     ├─ method-scope
  0.03 ms    68     7 ->     7     │     └─ register-children(query-handler, query.instantiations)
                    0 ->     0     └─ fork("dot graph property configuration")
  0.00 ms    68     0 ->     0        ├─ template-scope
                    0 ->     0        ├─ fork
  0.00 ms    68     0 ->     1        │  ├─ elements-of(aggregate)
  0.00 ms    68     1 ->     1        │  ├─ element-scope
                    1 ->     1        │  ├─ fork
  0.00 ms    68     1 ->     1        │  │  ├─ with-value(1)
  0.05 ms    68     1 ->     1        │  │  └─ update-property(dot-rank, aggregate)
                    1 ->     1        │  ├─ fork
  0.00 ms    68     1 ->     1        │  │  ├─ with-value(node)
  0.02 ms    68     1 ->     1        │  │  └─ update-property(dot-type, aggregate)
                    1 ->     1        │  ├─ fork
  0.00 ms    68     1 ->     1        │  │  ├─ with-value(component)
  0.02 ms    68     1 ->     1        │  │  └─ update-property(dot-shape, aggregate)
                    1 ->     1        │  └─ fork
  0.00 ms    68     1 ->     1        │     ├─ editor(replace(Aggregate$ -> ))
  0.05 ms    68     1 ->     1        │     └─ update-property(dot-label-transform, aggregate)
                    0 ->     0        ├─ fork
  0.00 ms    68     0 ->     1        │  ├─ elements-of(aggregate-member)
  0.00 ms    68     1 ->     1        │  ├─ element-scope
                    1 ->     1        │  ├─ fork
  0.00 ms    68     1 ->     1        │  │  ├─ with-value(1)
  0.02 ms    68     1 ->     1        │  │  └─ update-property(dot-rank, aggregate-member)
                    1 ->     1        │  ├─ fork
  0.00 ms    68     1 ->     1        │  │  ├─ with-value(node)
  0.02 ms    68     1 ->     1        │  │  └─ update-property(dot-type, aggregate-member)
                    1 ->     1        │  ├─ fork
  0.00 ms    68     1 ->     1        │  │  ├─ with-value(component)
  0.01 ms    68     1 ->     1        │  │  └─ update-property(dot-shape, aggregate-member)
                    1 ->     1        │  └─ fork
  0.00 ms    68     1 ->     1        │     ├─ editor(replace(Aggregate$ -> ))
  0.01 ms    68     1 ->     1        │     └─ update-property(dot-label-transform, aggregate-member)
                    0 ->     0        ├─ fork
  0.00 ms    68     0 ->     7        │  ├─ elements-of(event)
  0.00 ms    68     7 ->     7        │  ├─ element-scope
                    7 ->     7        │  ├─ fork
  0.00 ms    68     7 ->     7        │  │  ├─ with-value(2)
  0.26 ms    68     7 ->     7        │  │  └─ update-property(dot-rank, event)
                    7 ->     7        │  ├─ fork
  0.00 ms    68     7 ->     7        │  │  ├─ with-value(node)
  0.26 ms    68     7 ->     7        │  │  └─ update-property(dot-type, event)
                    7 ->     7        │  ├─ fork
  0.00 ms    68     7 ->     7        │  │  ├─ with-value(folder)
  0.27 ms    68     7 ->     7        │  │  └─ update-property(dot-shape, event)
                    7 ->     7        │  └─ fork
  0.08 ms    68     7 ->     7        │     ├─ editor(replace(Event$ -> ))
  0.28 ms    68     7 ->     7        │     └─ update-property(dot-label-transform, event)
                    0 ->     0        ├─ fork
  0.00 ms    68     0 ->     2        │  ├─ elements-of(projection)
  0.00 ms    68     2 ->     2        │  ├─ element-scope
                    2 ->     2        │  ├─ fork
  0.00 ms    68     2 ->     2        │  │  ├─ with-value(3)
  0.06 ms    68     2 ->     2        │  │  └─ update-property(dot-rank, projection)
                    2 ->     2        │  ├─ fork
  0.00 ms    68     2 ->     2        │  │  ├─ with-value(node)
  0.06 ms    68     2 ->     2        │  │  └─ update-property(dot-type, projection)
                    2 ->     2        │  ├─ fork
  0.00 ms    68     2 ->     2        │  │  ├─ with-value(box3d)
  0.06 ms    68     2 ->     2        │  │  └─ update-property(dot-shape, projection)
                    2 ->     2        │  └─ fork
  0.00 ms    68     2 ->     2        │     ├─ editor(replace(Projection$ -> ))
  0.06 ms    68     2 ->     2        │     └─ update-property(dot-label-transform, projection)
                    0 ->     0        ├─ fork
  0.00 ms    68     0 ->     6        │  ├─ elements-of(command)
  0.00 ms    68     6 ->     6        │  ├─ element-scope
                    6 ->     6        │  ├─ fork
  0.00 ms    68     6 ->     6        │  │  ├─ with-value(edge)
  0.03 ms    68     6 ->     6        │  │  └─ update-property(dot-type, command)
                    6 ->     6        │  └─ fork
  0.01 ms    68     6 ->     6        │     ├─ editor(replace(Command$ -> ))
  0.03 ms    68     6 ->     6        │     └─ update-property(dot-label-transform, command)
                    0 ->     0        └─ fork
  0.00 ms    68     0 ->     3           ├─ elements-of(query)
  0.00 ms    68     3 ->     3           ├─ element-scope
                    3 ->     3           ├─ fork
  0.00 ms    68     3 ->     3           │  ├─ with-value(edge)
  0.09 ms    68     3 ->     3           │  └─ update-property(dot-type, query)
                    3 ->     3           ├─ fork
  0.00 ms    68     3 ->     3           │  ├─ with-value(dashed)
  0.09 ms    68     3 ->     3           │  └─ update-property(dot-style, query)
                    3 ->     3           ├─ fork
  0.00 ms    68     3 ->     3           │  ├─ with-value(onormal)
  0.09 ms    68     3 ->     3           │  └─ update-property(dot-arrowhead, query)
                    3 ->     3           └─ fork
  0.00 ms    68     3 ->     3              ├─ editor(replace(Query$ -> ))
  0.09 ms    68     3 ->     3              └─ update-property(dot-label-transform, query)
"""

val BAELDUNG_AXON_STATS = """
allClasses                                     34
classByType                                    34
methodInvocationsCache                         47
methodInvocationsCache.flatten                319
methodFieldCache                                0
methodFieldCache.flatten                        0
parents                                        34
parents.flatten                                 5
implementedInterfaces                          34
implementedInterfaces.flatten                   7
traced-elements                               148
traces                                      1 823
traces.p50                                      1
traces.p90                                     28
traces.p95                                     49
traces.p99                                     51
traces.max                                     56
traces.depth.p50                                7
traces.depth.p90                                8
traces.depth.p95                                8
traces.depth.p99                                8
traces.depth.max                                8
traces.flatten                             12 047
"""