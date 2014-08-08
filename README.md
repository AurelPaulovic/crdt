[![Build Status](https://travis-ci.org/AurelPaulovic/crdt.svg?branch=develop)](https://travis-ci.org/AurelPaulovic/crdt)

# CRDT #
**WARNING** currently, this is work in progress and should not be used!

This is a Scala library implementing Convergent and Commutative Replicated Data Types (CRDT).

This library tries to be self-contained and without any dependencies or assumptions about communication/messaging middleware and can be used in distributed environment as well as for inter-process communication.

## Implemented CRDTs ##
### GCounter ###
GCounter is an increment-only distributed counter.

### PNCounter ###
PNCounter is a distributed counter that supports both increments and decrements.

### MergeableRegister ###
MergeableRegister is a simple register that uses vector clocks for partial ordering of replica updates and in case of concurrent replicas it merges the concurrent replicas into a single resulting value using a user-defined merge strategy (similar to scala.math.Ordered).

### TotallyOrderedRegister ###
TotallyOrderedRegister is a register that uses total ordering currently implemented via TotalTimeClock to deterministically choose which replica is newer and upon merge use the value of newer replica as the new value.

TotalTimeClock is implemented using system clock time, an incremental counter to account for low system clock resolution and a replica name. In order to estabilish a total order the clock performs the following comparison:
```scala
if (this.time != other.time) this.time.compare(other.time)
else if (this.replica != other.replica) this.replica.compare(other.replica)
else this.counter.compare(other.counter)  
```
In order to create a new TotalTimeClock the factory remembers the last highest seen system clock time and compares it with current system clock and uses the later one. When a TotalTimeClock is created using the `TotalTimeClock.makeGreaterThan` factory method and a compared clock, the highest seen system clock in TotalTimeClock factory will be update to be at least as high as the compared clock, so that the resulting created clock as well as any following created clock will be guaranteed higher than the compared clock. 

The system clock time used in the clock is the real time as served by the operating system. That means that the time is, unless using the same source of real time which is possible only for replicas on the same computer, not perfectly synchronized and a particular replica can be consistently favored, but other replias will be generally closely follow it thanks to the highest seen system clock time mechanism.

The reason to use real time system clock is that it can help arbitrate between concurrent updates made on temporarily non-synchronized replicas. As an example consider two clients, one mobile and one web client. The mobile client is temporarily offline and knows only the latest value assigned to the register before it went offline. The web application has up to date data. If no update has been made after the mobile app went offline, both replicas (mobile and web) will have the same value and its associated clock. The user than performs an update of the register in his web application. Some time later, he decides that he will update the value from mobile app. Since the user knows, that the mobile is offline, he can probably understand why the value in the mobile app is stale. If he then updates it from the mobile and subsequently goes online with the mobile app, the register will end up with two concurrent replicas. If we used normal lamport clocks, these replicas would be concurrent and only partially ordered and we would have to perform an arbitrary decision which of the replicas will be kept as the newest. However, using system clock time which is reasonably in sync with real time we can have different values for the replica clocks and determine which of the replicas is really newer (limited by the accuracy of the system clocks). 

## Bibliography ##
* [Shapiro, M., et. al.: *A comprehensive study of convergent and commutative replicated data types.* Technical Report, 2011.](http://pagesperso-systeme.lip6.fr/Marc.Shapiro/papers/Comprehensive-CRDTs-RR7506-2011-01.pdf)

