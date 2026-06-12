package dev.constructive.eo
package schemes

import cats.{Monad, Traverse}

import data.{Forget, ForgetK}
import optics.{Getter, Optic, Review}

/** Typed recursion schemes as composable optics, over a user-supplied **pattern functor** `F[_]` (+
  * `Traverse[F]`) and the [[Basis]] (`Project`/`Embed`) correspondence to the recursive type `S` —
  * algebras pattern-match `F`'s *named constructors*, no positional indexing.
  *
  *   - [[cata]] folds (`Cata`, Getter-shaped); [[ana]] builds (`Ana`, Review-shaped); [[hylo]] is
  *     the **fused** zero-`S` refold. `ana(c).cross(cata(a))` fuses (single pass, no full-tree
  *     retention).
  *   - The zoo: [[para]] (subterms paired from the walked nodes), [[apo]] (O(1) graft), [[histo]] /
  *     [[futu]] (course-of-value / multi-layer, via [[Attr]] / [[Coattr]]). Decorated generically
  *     through the [[Decor]] optic family (gather/scatter over the `BiAffine` carrier) —
  *     zygo/dyna/chrono are user-written [[DecorGather]] / [[DecorScatter]] values fed to the
  *     generic [[cata]] / [[ana]] overloads.
  *   - The M-generic drivers [[cataM]] / [[anaM]] / [[hyloM]] run the same machine lifted through
  *     `Monad[M].tailRecM` (effectful layers, single-pass linear Ms).
  *
  * All drivers run on one stack-safe engine family: a `< 512`-deep on-stack fast path falling back
  * per deep subtree to a heap `ArrayDeque` machine ([[foldLayered]] and siblings) — stack-safe to
  * 10⁶, tested.
  */
object Schemes:

  /** Depth at which the on-stack recursion hands a subtree to the heap machine — mirrors
    * `Plated.transformRecursionLimit`. Balanced trees (depth ~log n) never reach it.
    */
  final private val OnStackLimit = 512

  /** Shared zero-length children array for leaf nodes — avoids a per-leaf empty-array allocation in
    * the typed [[foldLayered]] machine.
    */
  private val EmptyAnyRefs: Array[AnyRef] = new Array[AnyRef](0)

  /** Collect the children of typed layer `fn` into a flat `Array[AnyRef]`, single-pass via
    * `ObjArrBuilder`. Returns [[EmptyAnyRefs]] for leaf layers (zero children) to avoid a per-leaf
    * empty-array allocation. Used by [[foldLayered]], [[foldLayeredOr]], and [[foldLayeredM]] — one
    * definition replaces the three identical nested `def childrenArr` that previously lived inside
    * each engine.
    *
    * Leaf layers are a common case in typed pattern functors (every `LeafF`-like constructor
    * carries no recursive slots), so the shared `EmptyAnyRefs` guard pays for itself.
    */
  private def childrenArr[F[_], N](fn: F[N])(using F: Traverse[F]): Array[AnyRef] =
    val n = F.size(fn).toInt
    if n == 0 then EmptyAnyRefs
    else
      val b = new data.ObjArrBuilder(n)
      val _ = F.foldLeft(fn, ()) { (_, child) =>
        b.unsafeAppend(child.asInstanceOf[AnyRef])
      }
      b.freezeArr

  /** Sentinel op for [[foldLayeredM]]'s loop state — "ascend": consume `ret` against the top frame.
    * Anything else on the loop is the node to descend into.
    */
  private object AscendToken

  // ===========================================================================================
  // Typed pattern-functor path — the opt-in, type-safe complement to the PSVec schemes above.
  //
  // The user supplies a pattern functor `F[_]` + its `Traverse[F]`, and (for cata/ana)
  // `Project[F, S]` / `Embed[F, S]`. Algebras pattern-match `F`'s NAMED constructors
  // (`case BranchF(l, r) => l + r`) — no `PSVec[AnyRef]`, no positional indexing. See [[Basis]].
  //
  // These run on the SAME `< 512`-on-stack / heap-`ArrayDeque` hybrid as the PSVec schemes above
  // (see [[foldLayered]]) — NOT a `cats.Eval` trampoline. The deep recursion is driven by the
  // machine; the user's `Traverse[F]` is used only per *layer* (bounded fanout: `foldLeft` to read a
  // node's children, `map` to rebuild the typed `F[result]` for the algebra), never across the
  // spine. So stack-safety needs no `Eval`-lazy `foldRight` from the user — any lawful `Traverse[F]`
  // works — and allocation is close to the PSVec path (one children/result array + the typed `F`
  // layers per node), not the ~Eval-node-per-layer a trampoline would cost.
  // ===========================================================================================

  /** Rebuild a typed `F[R]` from the original layer `fn: F[N]` and its children's results, stored
    * positionally in `out` in `Foldable` order — which `Functor.map` matches for a lawful
    * `Traverse`. Lets the schemes hand the algebra a typed `F[R]` (named constructors) rather than
    * a positional vector.
    */
  private def rebuildLayer[F[_], N, R](fn: F[N], out: Array[AnyRef])(using F: Traverse[F]): F[R] =
    if out.length == 0 then
      fn.asInstanceOf[F[R]] // leaf: no N-slots, so F[N] is phantom-recast to F[R]
    else
      var i = -1
      F.map(fn) { _ =>
        i += 1
        out(i).asInstanceOf[R]
      }

  /** [[rebuildLayer]]'s paramorphic sibling: pair each original child `N` with its folded result
    * from `out` (positional, `Foldable` order). The subterms come from the layer the machine
    * already holds — no re-`embed`.
    */
  private def rebuildLayerPaired[F[_], N, R](fn: F[N], out: Array[AnyRef])(using
      F: Traverse[F]
  ): F[(N, R)] =
    if out.length == 0 then fn.asInstanceOf[F[(N, R)]] // leaf: no N-slots, phantom-recast
    else
      var i = -1
      F.map(fn) { n =>
        i += 1
        (n, out(i).asInstanceOf[R])
      }

  /** Shared typed engine for the `F`-path schemes. `expand` peels a node into one typed layer of
    * child nodes; the engine folds each child to an `R` (post-order), then calls `combine` with the
    * node, its layer `F[N]`, and the children's results (positional, `Foldable` order). `combine`
    * rebuilds the typed `F[R]` via [[rebuildLayer]] and applies the user's algebra / embed. Same
    * `< 512`-on-stack / heap-`ArrayDeque` hybrid (and stack-safety) as [[unfoldFold]] /
    * [[foldInPlace]]; the per-node child array is reused as the result accumulator (folded in
    * place).
    */
  private def foldLayered[F[_], N, R](
      expand: N => F[N],
      combine: (N, F[N], Array[AnyRef]) => R,
  )(using F: Traverse[F]): N => R =

    def heap(root: N): R =
      final class Frame(val node: N, val layer: F[N], val arr: Array[AnyRef], var i: Int)
      val stack = new java.util.ArrayDeque[Frame]()
      var ret: AnyRef = null.asInstanceOf[AnyRef]
      def enter(n: N): Unit =
        val layer = expand(n)
        val arr = childrenArr(layer)
        if arr.length == 0 then ret = combine(n, layer, arr).asInstanceOf[AnyRef]
        else stack.push(new Frame(n, layer, arr, 0))
      enter(root)
      while !stack.isEmpty do
        val fr = stack.peek()
        if fr.i > 0 then fr.arr(fr.i - 1) = ret // overwrite the just-folded child's slot
        if fr.i < fr.arr.length then
          val child = fr.arr(fr.i).asInstanceOf[N]
          fr.i += 1
          enter(child)
        else
          ret = combine(fr.node, fr.layer, fr.arr).asInstanceOf[AnyRef]
          val _ = stack.pop()
      ret.asInstanceOf[R]

    def rec(n: N, depth: Int): R =
      if depth >= OnStackLimit then heap(n)
      else
        val layer = expand(n)
        val arr = childrenArr(layer)
        val k = arr.length
        if k == 0 then combine(n, layer, arr)
        else
          var i = 0
          while i < k do
            arr(i) = rec(arr(i).asInstanceOf[N], depth + 1).asInstanceOf[AnyRef]
            i += 1
          combine(n, layer, arr)

    n => rec(n, 0)

  /** [[foldLayered]]'s graft-aware sibling — the apomorphism engine. `expandOr` answers each node
    * event with `Left(r)` (an **already-finished result**: placed into its slot directly — O(1), no
    * recursion, no projection) or `Right(layer)` (keep going). Same `< 512`-on-stack /
    * heap-`ArrayDeque` hybrid and stack-safety as [[foldLayered]].
    */
  private def foldLayeredOr[F[_], N, R](
      expandOr: N => Either[R, F[N]],
      combine: (F[N], Array[AnyRef]) => R,
  )(using F: Traverse[F]): N => R =

    def heap(root: N): R =
      final class Frame(val layer: F[N], val arr: Array[AnyRef], var i: Int)
      val stack = new java.util.ArrayDeque[Frame]()
      var ret: AnyRef = null.asInstanceOf[AnyRef]
      def enter(n: N): Unit = expandOr(n) match
        case Left(r)      => ret = r.asInstanceOf[AnyRef] // graft: finished, by reference
        case Right(layer) =>
          val arr = childrenArr(layer)
          if arr.length == 0 then ret = combine(layer, arr).asInstanceOf[AnyRef]
          else stack.push(new Frame(layer, arr, 0))
      enter(root)
      while !stack.isEmpty do
        val fr = stack.peek()
        if fr.i > 0 then fr.arr(fr.i - 1) = ret
        if fr.i < fr.arr.length then
          val child = fr.arr(fr.i).asInstanceOf[N]
          fr.i += 1
          enter(child)
        else
          ret = combine(fr.layer, fr.arr).asInstanceOf[AnyRef]
          val _ = stack.pop()
      ret.asInstanceOf[R]

    def rec(n: N, depth: Int): R =
      if depth >= OnStackLimit then heap(n)
      else
        expandOr(n) match
          case Left(r)      => r // graft: finished, by reference
          case Right(layer) =>
            val arr = childrenArr(layer)
            val k = arr.length
            if k == 0 then combine(layer, arr)
            else
              var i = 0
              while i < k do
                arr(i) = rec(arr(i).asInstanceOf[N], depth + 1).asInstanceOf[AnyRef]
                i += 1
              combine(layer, arr)

    n => rec(n, 0)

  // ===========================================================================================
  // The M-generic path — the foldLayered state machine LIFTED into a Monad[M] (no M = Id
  // special-case: that is what makes the fast-path agreement laws a real cross-architecture
  // pin). State = the explicit frame deque, threaded through Monad[M].tailRecM, one iteration
  // per node event (each paying tailRecM's per-step Either — the structural B/op floor vs the
  // pure machine). NOT droste's hyloM (flatMap-recursive: O(depth) call stack on a strict M).
  // Stack-safety reduces to the lawfulness of M's tailRecM — per-M and tested (Id/Eval to 10^6).
  //
  // Supported Ms are SINGLE-PASS and LINEAR: the machine's state is mutable, so a branching /
  // replaying M (List, retrying or streaming effects) shares it across branches and corrupts
  // the fold — the documented contract, exercised by the boundary test in SchemesMSpec.
  // M must also be SEQUENTIALLY evaluated — each map/flatMap callback completes before the next
  // tailRecM step (true of Id/Eval/State/IO); async/concurrent step evaluation is unsupported
  // even for lawful Monads.
  //
  // The expand is Or-SHAPED (N => M[Either[R, F[N]]]) per the elgot-seam gate
  // (docs/brainstorms/2026-06-12-elgot-seam-sketch.md): v1 drivers always pass Right; the
  // elgot/apoM follow-up supplies Left answers with no re-architecture.
  // ===========================================================================================

  /** The lifted machine. One `M`-action per `tailRecM` iteration: `Down(n)` runs `expandOr`, exits
    * run `combine`; the mutable frame deque is allocated per-force (inside the `M`) so re-forcing
    * the same `M[R]` value allocates fresh state. Concurrent forcing of a single `M[R]` value
    * remains unsupported (mutable state, linear-M contract); each `run(s)` call is independent.
    */
  private def foldLayeredM[M[_], F[_], N, R](
      expandOr: N => M[Either[R, F[N]]],
      combine: (N, F[N], Array[AnyRef]) => M[R],
  )(using M: Monad[M], F: Traverse[F]): N => M[R] =

    final class Frame(val node: N, val layer: F[N], val arr: Array[AnyRef], var i: Int)

    n0 =>
      M.flatMap(M.unit) { _ =>
        val stack = new java.util.ArrayDeque[Frame]()
        var ret: AnyRef = null.asInstanceOf[AnyRef]
        // Op encoding (allocation-lean — CI 2026-06-12: per-event Either allocation
        // dominated the M path's 1.6M B/op): the loop state is a bare AnyRef — the
        // AscendToken sentinel means "consume ret against the top frame", anything else
        // is the node to descend into. One Left per descend (vs nested Left(Right(n)));
        // the ascend step is the hoisted constant.
        val ascend: Either[AnyRef, R] = Left(AscendToken)
        M.tailRecM[AnyRef, R](n0.asInstanceOf[AnyRef]) { op =>
          if op.asInstanceOf[AnyRef] ne AscendToken then
            val n = op.asInstanceOf[N]
            M.flatMap(expandOr(n)) {
              case Left(r) => // graft/short-circuit arm (unused by v1 drivers)
                ret = r.asInstanceOf[AnyRef]
                M.pure(ascend)
              case Right(layer) =>
                val arr = childrenArr(layer)(using F)
                if arr.length == 0 then
                  // leaf: combine INLINE (a constant second bind — no frame, no extra event)
                  M.map(combine(n, layer, arr)) { r =>
                    ret = r.asInstanceOf[AnyRef]
                    ascend
                  }
                else
                  stack.push(new Frame(n, layer, arr, 0))
                  M.pure(Left(arr(0)))
            }
          else if stack.isEmpty then M.pure(Right(ret.asInstanceOf[R]))
          else
            val fr = stack.peek()
            fr.arr(fr.i) = ret // store the just-folded child's result
            fr.i += 1
            if fr.i < fr.arr.length then M.pure(Left(fr.arr(fr.i)))
            else
              // last child stored: combine NOW (merged — no intermediate pure event)
              M.map(combine(fr.node, fr.layer, fr.arr)) { r =>
                val _ = stack.pop()
                ret = r.asInstanceOf[AnyRef]
                ascend
              }
        }
      }

  /** Effectful catamorphism — the algebra runs in `M` (`(S, F[A]) => M[A]`); the layer peel stays
    * the pure `Project`. Returns the `Forget[M]`-carried [[CataM]] citizen; consume via `.run`.
    */
  def cataM[M[_], F[_], S, A](
      algM: (S, F[A]) => M[A]
  )(using M: Monad[M], F: Traverse[F], P: Project[F, S]): CataM[M, F, S, A] =
    new CataM[M, F, S, A](
      foldLayeredM[M, F, S, A](
        s => M.pure(Right(P.project(s))),
        (s, fs, out) => algM(s, rebuildLayer[F, S, A](fs, out)),
      ),
      algM,
    )

  /** Effectful anamorphism — the coalgebra (the layer producer) runs in `M` (`Seed => M[F[Seed]]`,
    * the arbo `GetSellOptions` shape: fetching children is effectful). Returns the [[AnaM]]
    * citizen; consume via `.run`, fuse via `.andThen(cataM(...))`.
    */
  def anaM[M[_], F[_], Seed, S](
      coalgM: Seed => M[F[Seed]]
  )(using M: Monad[M], F: Traverse[F], E: Embed[F, S]): AnaM[M, F, Seed, S] =
    new AnaM[M, F, Seed, S](
      foldLayeredM[M, F, Seed, S](
        seed => M.map(coalgM(seed))(Right(_)),
        (_, fSeed, out) => M.pure(E.embed(rebuildLayer[F, Seed, S](fSeed, out))),
      ),
      coalgM,
    )

  /** Effectful hylomorphism — the always-fused M spelling (what the D6 `eoHyloM` bench row runs):
    * `Seed => M[A]` with **no intermediate `S`**, seed-typed algebra.
    */
  def hyloM[M[_], F[_], Seed, A](
      coalgM: Seed => M[F[Seed]],
      algM: (Seed, F[A]) => M[A],
  )(using M: Monad[M], F: Traverse[F]): FoldM[M, Seed, A] =
    new FoldM[M, Seed, A](
      foldLayeredM[M, F, Seed, A](
        seed => M.map(coalgM(seed))(Right(_)),
        (seed, fSeed, out) => algM(seed, rebuildLayer[F, Seed, A](fSeed, out)),
      )
    )

  /** Single-pass paired machine in `M` backing the fused `AnaM.andThen(CataM)` — the M mirror of
    * [[fusedPairedFold]]: each node built once, folded immediately, no `M[S]` whole-structure
    * materialization. Mirrors the pure version exactly: `F[S]` and `F[A]` are built straight from
    * the out-array with two `F.map(fSeed)` passes and `var i = -1` counters, avoiding the
    * `F[(S,A)]` intermediate. Leaf layers are phantom-recast (valid because pattern-functor leaves
    * have no recursive slots by definition).
    */
  private[schemes] def fusedPairedFoldM[M[_], F[_], Seed, S, A](
      coalgM: Seed => M[F[Seed]],
      algM: (S, F[A]) => M[A],
  )(using M: Monad[M], F: Traverse[F], E: Embed[F, S]): Seed => M[(S, A)] =
    foldLayeredM[M, F, Seed, (S, A)](
      seed => M.map(coalgM(seed))(Right(_)),
      (_, fSeed, out) =>
        // Build F[S] and F[A] straight from the out-array — no F[(S,A)] intermediate.
        val fS =
          if out.length == 0 then fSeed.asInstanceOf[F[S]]
          else
            var i = -1
            F.map(fSeed) { _ =>
              i += 1
              out(i).asInstanceOf[(S, A)]._1
            }
        val fA =
          if out.length == 0 then fSeed.asInstanceOf[F[A]]
          else
            var i = -1
            F.map(fSeed) { _ =>
              i += 1
              out(i).asInstanceOf[(S, A)]._2
            }
        val s = E.embed(fS)
        M.map(algM(s, fA))(a => (s, a)),
    )

  /** The single *layer* optic for a pattern functor `F`: `project`/`embed` worn as the existing
    * [[dev.constructive.eo.data.Forget]] carrier. `to = project: S => F[S]`, `from = embed: F[S] =>
    * S`, so it is a genuine `Optic[S, S, S, S, Forget[F]]` with **no change to the `Optic` trait**.
    *
    * It is a single-layer *peel/glue* (like `Plated`'s `plate`, but one layer, not the recursion).
    * The recursive schemes below drive `to`/`from` themselves and return `Direct`-carried optics,
    * so `fLayer` is mainly the concrete proof that a typed `F` is an optic carrier, plus an
    * observational read: given `Foldable[F]` it reads its layer's foci via `.foldMap`. Note it does
    * NOT compose as freely as `plate` (a `Traversal`): same-carrier `andThen` over `Forget[F]`
    * needs `Monad[F]`, which most pattern functors are not — so `fLayer` is a one-layer lens on the
    * structure, not a composable traversal.
    */
  def fLayer[F[_], S](using P: Project[F, S], E: Embed[F, S]): Optic[S, S, S, S, Forget[F]] =
    new Optic[S, S, S, S, Forget[F]]:
      type X = Any
      def to(s: S): Forget[F][X, S] = ForgetK(P.project(s))
      def from(fs: Forget[F][X, S]): S = E.embed(fs.value)

  /** Catamorphism over a typed pattern functor `F`, as a composable `Getter`. `alg` sees the
    * original node `S` (paramorphism-flavored) plus its already-folded children as a typed `F[A]`.
    * Pure `F[A] => A` folds ignore the `S`. Stack-safe to arbitrary depth (the [[foldLayered]]
    * machine, not a trampoline). Requires `Project[F, S]` (to peel each layer) and `Traverse[F]`
    * (any lawful instance — the machine, not the user's `foldRight`, provides stack-safety).
    */
  def cata[F[_], S, A](
      alg: (S, F[A]) => A
  )(using F: Traverse[F], P: Project[F, S]): Cata[F, S, A] =
    new Cata[F, S, A](cata[F, S, A, A](Decor.cata[F, A])(alg).get, alg)

  /** Generalized (decorated) catamorphism — the gcata of the typed path, with the decoration
    * supplied as a [[DecorGather]] optic value. Interior nodes apply `gather ∘ galg` (the
    * decoration's `from` consuming `Step(layer, result)`); the **root applies `galg` alone**
    * (droste's `gcata` shape). The named zoo members are instances: `cata(alg)` routes here with
    * [[Decor.cata]] (recognised by identity — the direct, decoration-free engine path), `histo`
    * with [[Decor.histo]]; user-written decorations (zygo, dyna, …) run the generic route, which
    * pays one decoration dispatch + `Step` per node.
    *
    * (type-param order: `[F, S, W, A]` — compare [[ana]] `[F, A, W, S]`, which mirrors these in
    * input-before-output order: `A` is the input seed there, `S` the built output.)
    */
  def cata[F[_], S, W, A](
      decor: DecorGather[F, W, A]
  )(galg: (S, F[W]) => A)(using F: Traverse[F], P: Project[F, S]): Getter[S, A] =
    if decor.asInstanceOf[AnyRef] eq Decor.cata[F, A] then
      // W =:= A by construction of the singleton — the direct engine path, no decoration cost.
      val alg = galg.asInstanceOf[(S, F[A]) => A]
      Getter[S, A](
        foldLayered[F, S, A](P.project, (s, fs, out) => alg(s, rebuildLayer[F, S, A](fs, out)))
      )
    else
      val toW: S => W = foldLayered[F, S, W](
        P.project,
        (s, fs, out) =>
          val fw = rebuildLayer[F, S, W](fs, out)
          decor.from(new data.BiAffine.Step[(Unit, F[W]), A](fw, galg(s, fw))),
      )
      Getter[S, A] { s =>
        val layer = P.project(s)
        galg(s, F.map(layer)(toW))
      }

  /** Paramorphism over a typed pattern functor `F` — each child slot pairs the **original subterm**
    * with its folded result. Native route: the machine already walks real `S` nodes and keeps each
    * frame's projected layer, so subterms are paired positionally — no per-node re-`embed`
    * (droste's `Gather.para` must reconstruct the subterm it threw away). Stack-safe (the
    * [[foldLayered]] machine).
    */
  def para[F[_], S, A](
      alg: (S, F[(S, A)]) => A
  )(using F: Traverse[F], P: Project[F, S]): Getter[S, A] =
    Getter[S, A](
      foldLayered[F, S, A](
        P.project,
        (s, fs, out) => alg(s, rebuildLayerPaired[F, S, A](fs, out)),
      )
    )

  /** Histomorphism over a typed pattern functor `F` — the algebra sees each child's **full
    * decorated history** ([[Attr]]: result + that child's own decorated layer).
    *
    * Native route: the combine builds the `Attr` directly, the root projects its head — one less
    * dispatch than the generic [[Decor.histo]] route (whose `Step` is EA-elided: B/op identical;
    * law-pinned equal in `DecorLawsSpec`). The remaining gap to droste's histo (558k vs 361k B/op
    * on the 8k-node fixture) is the stack-safe machine's per-node child array — droste's zoo
    * recursion is stack-UNSAFE plain recursion; the ~24 B/node is the price of the guarantee.
    *
    * Space honesty: course-of-value recursion retains O(n) `Attr` cells by nature.
    */
  def histo[F[_], S, A](
      alg: (S, F[Attr[F, A]]) => A
  )(using F: Traverse[F], P: Project[F, S]): Getter[S, A] =
    val toAttr: S => Attr[F, A] = foldLayered[F, S, Attr[F, A]](
      P.project,
      (s, fs, out) =>
        val layer = rebuildLayer[F, S, Attr[F, A]](fs, out)
        Attr(alg(s, layer), layer),
    )
    Getter[S, A](s => toAttr(s).head)

  /** Anamorphism over a typed pattern functor `F`, as a `Review`. The single fused coalgebra `Seed
    * => F[Seed]` yields one typed layer of child seeds; [[Embed]] assembles each layer into the
    * built `S`. Materializing — the built `S` is O(nodes). Stack-safe (the [[foldLayered]] machine).
    * Requires `Embed[F, S]` and `Traverse[F]`. Type params are `[F, Seed, S]` (input before output)
    * to match [[hylo]] and the `PSVec` [[ana]].
    */
  def ana[F[_], Seed, S](
      coalg: Seed => F[Seed]
  )(using F: Traverse[F], E: Embed[F, S]): Ana[F, Seed, S] =
    new Ana[F, Seed, S](ana[F, Seed, Seed, S](Decor.ana[F, Seed])(coalg).reverseGet, coalg)

  /** Single-pass paired machine backing the fused `Ana.cross(Cata)`: each node is built once (the
    * algebra is node-supplied — construction is semantically required), folded immediately, and
    * released as the fold ascends. No full-tree retention, no second traversal. Leaf layers are
    * phantom-recast (valid because pattern-functor leaves have no recursive slots by definition).
    */
  private[schemes] def fusedPairedFold[F[_], Seed, S, A](
      coalg: Seed => F[Seed],
      alg: (S, F[A]) => A,
  )(using F: Traverse[F], E: Embed[F, S]): Seed => (S, A) =
    foldLayered[F, Seed, (S, A)](
      coalg,
      (_, fSeed, out) =>
        // Build F[S] and F[A] straight from the out-array — no F[(S, A)] intermediate
        // (CI 2026-06-12: that third F-alloc per node put the fused cross ABOVE the
        // materializing composition in B/op, 1049k vs 886k).
        val fS =
          if out.length == 0 then fSeed.asInstanceOf[F[S]]
          else
            var i = -1
            F.map(fSeed) { _ =>
              i += 1
              out(i).asInstanceOf[(S, A)]._1
            }
        val fA =
          if out.length == 0 then fSeed.asInstanceOf[F[A]]
          else
            var i = -1
            F.map(fSeed) { _ =>
              i += 1
              out(i).asInstanceOf[(S, A)]._2
            }
        val s = E.embed(fS)
        (s, alg(s, fA)),
    )

  /** Apomorphism over a typed pattern functor `F` — per child slot the coalgebra answers
    * `Right(seed)` (keep unfolding) or `Left(s)` (an **already-finished subtree**). Native O(1)
    * graft: `Left` subtrees are prefilled into their result slots **by reference** — never
    * recursed, never projected ([[foldLayeredOr]]). Contrast droste's scatter-apo, which re-walks
    * grafts through `project` (O(graft) per graft — the route [[Decor.apo]] documents). Stack-safe.
    */
  def apo[F[_], A, S](
      coalg: A => F[Either[S, A]]
  )(using F: Traverse[F], E: Embed[F, S]): Review[S, A] =
    val run = foldLayeredOr[F, Either[S, A], S](
      {
        case Left(s)  => Left(s)
        case Right(a) => Right(coalg(a))
      },
      (fw, out) => E.embed(rebuildLayer[F, Either[S, A], S](fw, out)),
    )
    Review[S, A](a => run(Right(a)))

  /** Futumorphism over a typed pattern functor `F` — the coalgebra may emit **multiple layers per
    * step** ([[Coattr]]: `Pure` keeps unfolding, `Roll` is a prebuilt layer unrolled with no
    * coalgebra call).
    *
    * Native route: the expand matches `Coattr` directly — one less dispatch than the generic
    * [[Decor.futu]] route (whose per-slot `Step` is EA-elided: B/op identical; law-pinned equal in
    * `DecorLawsSpec`). The gap to droste's futu (655k vs 459k B/op) is the stack-safe machine's
    * per-node child array — droste's zoo recursion is stack-unsafe.
    */
  def futu[F[_], A, S](
      coalg: A => F[Coattr[F, A]]
  )(using F: Traverse[F], E: Embed[F, S]): Review[S, A] =
    val expand: Coattr[F, A] => F[Coattr[F, A]] =
      case Coattr.Pure(a)     => coalg(a)
      case Coattr.Roll(layer) => layer
    val build = foldLayered[F, Coattr[F, A], S](
      expand,
      (_, fw, out) => E.embed(rebuildLayer[F, Coattr[F, A], S](fw, out)),
    )
    Review[S, A](a => build(Coattr.Pure(a)))

  /** Generalized (decorated) anamorphism — the gana of the typed path, with the decoration supplied
    * as a [[DecorScatter]] optic value. Each `W` slot is scattered (the decoration's `to`):
    * `Step(_, seed)` calls `gcoalg`, `Done(layer)` unrolls the prebuilt layer with **no coalgebra
    * call**. The root seed enters through the decoration's pointed unit (`from` on the Step arm —
    * gana's `pure`). `ana(coalg)` routes here with [[Decor.ana]] (identity-recognised direct path);
    * `futu` with [[Decor.futu]]; `Decor.apo` runs the generic distApo route — the O(1) graft
    * belongs to the native `apo` engine.
    *
    * For user-written [[DecorScatter]] values, `Done.fst` MUST carry `F[W]` at runtime — the engine
    * unrolls it directly as the next layer.
    *
    * (type-param order: compare [[cata]] `[F, S, W, A]` — the fold mirror swaps `Seed`/`A`.)
    */
  def ana[F[_], A, W, S](
      decor: DecorScatter[F, W, A]
  )(gcoalg: A => F[W])(using F: Traverse[F], E: Embed[F, S]): Review[S, A] =
    if decor.asInstanceOf[AnyRef] eq Decor.ana[F, A] then
      // W =:= A by construction of the singleton — the direct engine path.
      val coalg = gcoalg.asInstanceOf[A => F[A]]
      Review[S, A](
        foldLayered[F, A, S](coalg, (_, fSeed, out) => E.embed(rebuildLayer[F, A, S](fSeed, out)))
      )
    else
      val expand: W => F[W] = w =>
        decor.to(w) match
          case st: data.BiAffine.Step[(F[W], Unit), A] => gcoalg(st.b)
          case dn: data.BiAffine.Done[(F[W], Unit), A] => dn.fst
      val build: W => S = foldLayered[F, W, S](
        expand,
        (_, fw, out) => E.embed(rebuildLayer[F, W, S](fw, out)),
      )
      Review[S, A](a => build(decor.from(new data.BiAffine.Step[(F[W], Unit), A]((), a))))

  /** Hylomorphism over a typed pattern functor `F` — the **fused** refold `Seed => A`, building
    * **no intermediate `S`** (so it needs neither `Project` nor `Embed`, only `Traverse[F]`).
    * `coalg` unfolds a seed into one typed layer; `alg` folds the layer's results to `A` (the seed
    * is supplied, paramorphism-flavored). Stack-safe (the [[foldLayered]] machine). Equal to
    * `ana(coalg).cross(cata(alg))` for a *pure* algebra (the hylo law); for a node-reading para
    * algebra the two agree only under the seed↔`embed(coalg(seed))` correspondence.
    */
  def hylo[F[_], Seed, A](
      coalg: Seed => F[Seed],
      alg: (Seed, F[A]) => A,
  )(using F: Traverse[F]): Getter[Seed, A] =
    Getter[Seed, A](
      foldLayered[F, Seed, A](
        coalg,
        (seed, fSeed, out) => alg(seed, rebuildLayer[F, Seed, A](fSeed, out)),
      )
    )
