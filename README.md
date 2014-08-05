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


## Bibliography ##
* [Shapiro, M., et. al.: *A comprehensive study of convergent and commutative replicated data types.* Technical Report, 2011.](http://pagesperso-systeme.lip6.fr/Marc.Shapiro/papers/Comprehensive-CRDTs-RR7506-2011-01.pdf)

