package dev.constructive.eo.generics

import scala.quoted.Quotes

/** The base of eo's Hearth-backed macro implementations: Hearth's macro commons plus the names
  * every one of them uses (`Id` for `construct[Id]`, Hearth's `fp` instances), so an implementation
  * states only `import quotes.reflect.*` and its own logic.
  */
abstract private[eo] class HearthMacro(q: Quotes) extends _root_.hearth.MacroCommonsScala3(using q):
  export _root_.hearth.fp.Id
  export _root_.hearth.fp.instances.{*, given}
