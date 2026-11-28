/**
 * @name CSRF name-based path graph self-edge probe
 * @description Diagnostic-only query testing whether an explicit self-edge can serialize
 *              the name-based same-node path used by the official CSRF query.
 * @kind path-problem
 * @problem.severity warning
 * @id growlab/csrf-namebased-self-edge-probe
 */

import java
import semmle.code.java.security.CsrfUnprotectedRequestTypeQuery

query predicate edges(CallPathNode pred, CallPathNode succ) {
  relevantEdge(pred, succ)
  or
  pred = succ and unprotectedStateChange(pred, succ)
}

from CallPathNode source, CallPathNode sink
where unprotectedStateChange(source, sink)
select source.asMethod(), source, sink,
  "Diagnostic name-based CSRF path graph probe for $@.",
  sink, "state-change candidate"
