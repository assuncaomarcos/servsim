# ServSim (A Service Simulator)

This project provides a discrete event simulation API that can be used for simulating the scheduling of computational resources at a server, cluster or data center level. A simulation comprises a set of entities that communicate with one another by sending messages. The API provides basic classes for creating servers, users, and schedulers.

## Creating and Running a Simulation

`Simulation` is the class that must be extended in order to configure and run a simulation. A `SimEntity` must implement `onStart()`, `onShutdown()`, and `process(SimEvent)` that are invoked respectively when the entity starts, finishes and receives an event from another entity or from itself.

## Use Examples

For the time being, examples on how to use the API comprise the unit tests included in `src/test/java` directory.

