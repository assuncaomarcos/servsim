# ServSim (A Service Simulator)

This project provides a discrete event simulation API that can be used for 
simulating the scheduling of computational resources at a server, cluster or 
data center level. A simulation comprises a set of entities that communicate 
with one another by sending messages. The API provides basic classes for 
creating servers, users, and schedulers.

## Importing the Simulation Core

To use the ServSim simulation core, you can either clone this project or 
include the following dependency in the `pom.xml` file of your Maven 
project:

```
<dependencies>
    <dependency>
        <groupId>me.marcosassuncao.servsim</groupId>
        <artifactId>servsim-core</artifactId>
        <version>LATEST</version>
    </dependency>
    ...
</dependencies>
```

## Creating and Running a Simulation

`Simulation` is the class that must be extended in order to configure and run 
a simulation. A `SimEntity` must implement `onStart()`, `onShutdown()`, 
and `process(SimEvent)` that are invoked respectively when the entity starts, 
finishes and receives an event from another entity or from itself.

## Use Examples

The following examples illustrate how to use the simulation core to create 
more elaborate simulations.

### Ping-Pong Example

This example creates an entity called `PingEntity` that sends ping messages 
to a another entity named `PongEntity`, which responds with pong messages. An 
event of type `SimEvent.Type.TASK_ARRIVE` is used to indicate a ping. Upon 
receipt of a `TASK_ARRIVE` event, `PongEntity` replies with a `SimEvent.Type
.TASK_COMPLETE`. To implement this example, we first create the `PongEntity`:

```
class PongEntity extends SimEntity {

    public PongEntity(String name) throws IllegalArgumentException {
        super(name);
    }
    
    @Override
    public void onStart() { }
    
    // Method invoked if the entity receives an event
    @Override
    public void process(SimEvent ev) {
        // We are interested in processing only SimEvent.Type.TASK_ARRIVE events
        if (ev.type() == SimEvent.Type.TASK_ARRIVE) {
            // send pong
            System.out.println("Received ping, sending pong...");
            super.send(ev.source(), SimEvent.SEND_NOW,
                    SimEvent.Type.TASK_COMPLETE, null);
        }
    }
    
    @Override
    public void onShutdown() { }
}

```
Then we create the `PingEntity` that will send `TASK_ARRIVE` events (i.e. 
pings) to the `PongEntity`. Note that the constructor receives the
 `PongEntity`'s entity ID as it is required to send the events.

```
// Simple entity that sends a task to another
class PingEntity extends PongEntity {
    private int dstEntity;
    private long interval;
    private int numberPing;
    
    // 
    public PingEntity(String name, int dstEntity,
                      long interval, int numberPing)
            throws IllegalArgumentException {
        super(name);
        this.dstEntity = dstEntity;
        this.interval = interval;
        this.numberPing = numberPing;
    }
    
    @Override
    public void onStart() {
        // Sends a number of events to dstEntity every *interval* time units
        for (int i = 1; i <= numberPing; i++) {
            super.send(dstEntity, interval * i,
                    SimEvent.Type.TASK_ARRIVE, null);
        }
    }
    
    @Override
    public void onShutdown() { }
}
```

Then we need to extend `Simulation` and implement the `onConfigure()` method 
where we wire and register all entities:

```
public class PingExample {

    public static final void main(String[] args) {
    
        Simulation sim = new Simulation() {
            
            @Override
            public void onConfigure() {
                long interval = 5;  // send a ping every 5 time units
                int numberPing = 3; // total number of pings to send

                PongEntity pong = new PongEntity("Pong");
                PingEntity ping = new PingEntity("Ping", pong.getId(),
                        interval, numberPing);
                        
                // register the pong and ping entities
                registerEntity(pong);
                registerEntity(ping);
            }
        };
        
        // We can finally run the simulation
        sim.run();
    }
}
```

