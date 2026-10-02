package external

import dev.constructive.eo.data.{MultiFocus, MultiFocusK}

/** The carrier's public surface as a *downstream* module sees it — outside the `eo` package tree,
  * so the opaque type is opaque, the sum behind it is unreachable, and every accessor has to work
  * through what the library publishes. Pins three things a library user depends on:
  *
  *   - `MultiFocus(x, fa)` / `MultiFocus.broadcast(x, a)` build bundles;
  *   - `.context` / `.foci` / `.broadcast` read them (and inline from here at all — the inlined
  *     bodies must not reference `private[eo]` internals);
  *   - the sum's cases cannot be named or constructed from here.
  */
class EoOpaqueSurfaceSpec extends org.specs2.mutable.Specification:

  "MultiFocus's public surface is usable from outside the eo package" >> {
    val tabulated: MultiFocusK[Function1[Int, *], Unit, String] =
      MultiFocus[Function1[Int, *], Unit, String]((), (i: Int) => s"v$i")
    val indexFree: MultiFocusK[Function1[Int, *], Unit, String] =
      MultiFocus.broadcast[Int, Unit, String]((), "const")

    val tabulatedOk =
      tabulated.broadcast.isEmpty && tabulated.foci(0) == "v0" && tabulated.foci(1) == "v1"
    val indexFreeOk = indexFree.broadcast.contains("const") &&
      indexFree.foci(0) == "const" && indexFree.foci(7) == "const"
    val contextOk = tabulated.context == (())

    tabulatedOk && indexFreeOk && contextOk
  }
