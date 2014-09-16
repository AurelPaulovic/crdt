[![Build Status](https://travis-ci.org/AurelPaulovic/crdt.svg?branch=develop)](https://travis-ci.org/AurelPaulovic/crdt)
[![License](http://img.shields.io/badge/license-Apache%202.0-brightgreen.svg)](https://github.com/AurelPaulovic/crdt/blob/master/LICENSE)

# CRDT #
**WARNING** currently, this is work in progress and should not be used!

This is a Scala library implementing Convergent and Commutative Replicated Data Types (CRDT) [1].

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

### GSet ###
Simple grow-only set in which elements can be only added but not removed. GSet is the basis of many other CRDT Set implementations.

### MPNSet ###
MPNSet is a modified version of PNSet based on the modification by Molli, Weiss and Skaf but further augmented to account for the remove anomaly. An almost identical version has been later designed by Molli, Weiss and Skaf in C-Set [2].

MPNSet is a set of unique elements that can be added or removed. Whether the element is present in a set is determined by a PNCounter for that element where a positive value greater than 0 denotes that the element is present in the set, 0 or negative value means that the element is not in the set.

When adding an element that is not in the set and its PNCounter is 0, the counter of the element is incremented to 1. All replicas will recieve an increment of the element's counter by 1. 

When adding an element that is no in the set and its PNCounter is less than 0 = X, the counter will be incremented to 1 by a value Y (difference 1 - X where X is negative = Y) and all replicas will receive an increment of the element's counter by Y.

When adding an element that is present in the set, the associated PNCounter stays the same.

Remove of an element is symmetric to adding an element. When removing an element that is present in the set, the counter value is set to 0 and the difference between 0 and the original value of counter is sent as counter update to all replicas.

The set should not suffer from any anomalies associated with PNSet or the version by Molli, Weiss and Skaf. When an element is added by a replica, it is guaranteed to be in the set (for that replica and version of set). When an element is removed by a replica, it is guaranteed to be not present in the set (for that replica and version of set).

### COSet ###
Causaly Ordered Set is that uses a logical clock attached to the set to tag every element added to the set. When an element is removed from the set, it can be simply removed from the payload without having to store any kind of tombstone. This is basically an implementation of Optimized OR-Set [3].

When two replicas of COSet are merged, then all elements that are present in both replicas will be kept in the merged replica as well but the tags of the elements will be merged with each other for each pair shared pair. Every element that is present in only one of the replicas and is concurrent with the other replica (tag of the element is concurrent with current logical clock of the other replica) will be in kept in the resulting merged set as well. Every element that is present in only one of the replicas but has its tag smaller than the current clock of the other replica (meaning that the other replica has already seen it but removed it afterwards) will be discarded.
 
## Bibliography ##
1. [Shapiro, M., et. al.: *A comprehensive study of convergent and commutative replicated data types.* Technical Report, 2011.](http://pagesperso-systeme.lip6.fr/Marc.Shapiro/papers/Comprehensive-CRDTs-RR7506-2011-01.pdf)
2. [Aslan, K., et. al.: *C-Set: a Commutative Replicated Data Type for Semantics Stores.* Red: Fourth International Workshop on RESource Discovery, 2011.](http://hal.inria.fr/docs/00/59/45/90/PDF/main.pdf)
3. [Bieniusa, A., et. al.: *An optimized conflict-free replicated set* CoRR, 2012](http://arxiv.org/pdf/1210.3368v1.pdf)

