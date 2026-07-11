package dev.constructive.eo

/** Weakened functor hierarchy for carriers that cannot honour full `map`: [[ForgetfulFunctor]],
  * [[ForgetfulFold]], [[ForgetfulApplicative]] and [[ForgetfulTraverse]] let read-only carriers
  * (`Forget`, `Direct`-collapsed reads) participate in the generic composition machinery by
  * "mapping" in ways that may legally discard or refuse the function.
  */
package object forgetful
