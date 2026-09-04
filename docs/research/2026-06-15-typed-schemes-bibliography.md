# Typed recursion schemes × optics — bibliography

Research artifact for the `feat/typed-recursion-schemes` merge (PR #24). Two jobs:
anchor the branch's design claims in the literature, and fix the reference list the
plan docs point at (the anchor paper URL in the PR thread, `arXiv:1103.2841`, is
**O'Connor's Multiplate paper** — a lens paper, not a recursion-schemes paper — which
is itself the point: it is the categorical charter for the branch's *optics half*).

Compiled 2026-06-15. Sources: the paper itself, Semantic Scholar (references +
citations of 1103.2841), and the canon the zoo's schemes come from.

## 0. The anchor paper

- **Russell O'Connor, "Functor is to Lens as Applicative is to Biplate: Introducing
  Multiplate"** (WGP 2011; [arXiv:1103.2841](https://arxiv.org/abs/1103.2841)).
  Two categorical characterisations of lenses — coalgebra of the store comonad, and
  monoidal natural transformation on a category of coalgebras — generalized to the
  Cartesian store comonad (whose coalgebras are Uniplate's **Biplates**) and to
  Compos's `compos` type. Proves van Laarhoven's conjecture that the two
  generalizations are isomorphic; proposes Multiplate for mutually recursive types
  (rank-3 polymorphism + type classes).

  Why this paper is the right anchor for PR #24 despite being "about" plates:

  - **Lens = store-comonad coalgebra** (§2.2) is exactly the reading the branch's
    para-as-Lens claim leans on: `paraLens` is lawful as a Lens because para's
    retained subterms are the store's "position" complement
    (`docs/brainstorms/2026-06-12-existential-x-is-the-decoration.md`, reading 2).
  - **Biplate = coalgebra of the Cartesian store comonad** (§3) is the generic
    single-layer self-traversal — eo's `Plated.plate` / `Schemes.fLayer` on the
    `MultiFocus` carrier, one layer of `F[S]` children plus a context. The paper is
    the citation for "one-layer-plate" as an *optic family*, not an ad-hoc library.
  - **The van-Laarhoven-style theorem** (§4): `CartesianStore B A ≅ ∀κ. Applicative κ
    => (B -> κ B) -> κ A`, and `Store B A ≅ ∀κ. Functor κ => (B -> κ B) -> κ A`.
    This is the moral charter for eo's existential-carrier encoding
    (`Optic[S, T, A, B, C[_]]` with leftover `X`): the paper proves the polymorphic
    (Kleisli/existential) and coalgebraic (store) presentations of the same optic
    coincide — the "two readings of the same fact" move the whole branch makes at
    the X seam. Wadler's free-theorem machinery ([16] in the paper) is what carries
    the proof.
  - **Mutually recursive types** (§5, Multiplate proper) map onto the generics
    module's macro derivation and the pattern-functor requirement: the branch's
    `Project`/`Embed` basis is the same shape Multiplate demands per plate.

## 1. References *of* the anchor paper that matter to this codebase

- Mitchell, Runciman, *Uniform boilerplate and list processing* (Haskell Workshop
  2007) — **Uniplate**, the origin of Biplates and of the `Plated` name/class
  family (`Plated.plate`, circe's `Plated[Json]`, the `PlatedBridgeSpec`).
- Bringert, Ranta, *A pattern for almost compositional functions* (ICFP 2006) —
  **Compos**, the other half of the isomorphism theorem.
- Yakushev, Holdermans, Löh, Jeuring, *Generic programming with fixed points for
  mutually recursive datatypes* (ICFP 2009) — multiparameter fixed points; the
  general shape the `generics` module's derivation must respect for
  mutually-recursive ADTs.
- Foster, Greenwald, Moore, Pierce, Schmitt, *Combinators for bi-directional tree
  transformations* (POPL 2005) — the lens view-update problem; the put/get laws
  `paraLens`'s get-put/put-get pinning instantiates.
- Johnson, Rosebrugh, Wood, *Algebras and Update Strategies* (JUCS 2010) — lenses
  as algebras of a monad on a slice category; the coalgebra/algebra duality the
  fold/unfold optic families sit on.
- McBride, Paterson, *Applicative programming with effects* (JFP 2008) — the
  applicative half of the title theorem; `Traverse[F]` per-layer lawfulness.
- Bird, Meertens, *Nested Datatypes* (MPC 1998) — the nested-type obstacle the
  Cartesian store comonad clears in Haskell 98; relevant to `Tree[+N]`-style
  recursive parameterised ADTs in `eo-generics`.
- Uustalu, Vene, *Signals and Comonads* (JUCS 2005) — comonad machinery adjacent
  to the decoration towers (zygo/histo).
- Lämmel, Kort, Visser, *Dealing with Large Bananas* (WGP 2000) — generalized
  folds at scale; an early "zoo" unification attempt.
- Wadler, *Theorems for free!* (FPCA 1989) — the engine behind the §4 isomorphism
  proof technique.

## 2. Citations *of* the anchor paper relevant to the branch

(From Semantic Scholar; filtered to what PR #24 actually builds on.)

- Riley, *Categories of Optics* (2019) — optics as mixed optics; the store comonad
  is the mixed choice for lenses, which is the "X is the leftover" story in
  categorical dress. Cited in the plan's references (§4.10 achromatic variant).
- Pickering, Gibbons, Wu, *Profunctor Optics: Modular Data Accessors* (Programming
  Journal 2017) — the profunctor reformulation of exactly O'Connor's theorem; the
  "read-only-optics convention" the BiAffine carrier's sub-shape pinning cites.
- Kiss, Pickering, Wu, *Generic deriving of generic traversals* (Haskell 2018) —
  deriving Traversal/Plate structure generically at compile time; the citation
  for `eo-generics`' derivation ambitions beyond Lens/Prism.
- Gibbons, Johnson, *Relating algebraic and coalgebraic descriptions of lenses*
  (BX 2012) — the get/put vs coalgebra duality spelled out.
- Ahman, Uustalu, *Coalgebraic update lenses* (ENTCS 2014) and *Taking Updates
  Seriously* (MPCS 2017) — update-lens coalgebras; where put-get lawfulness for
  decorated folds (paraLens, the memoized-refolds follow-up) would be grounded.
- Clarke, *Delta Lenses as Coalgebras for a Comonad*
  ([arXiv:2108.00390](https://arxiv.org/abs/2108.00390), 2021) — modern successor;
  cite if paraLens grows a delta-lens face.
- Capriotti, Danielsson, Vezzosi, *Higher Lenses* (LICS 2021) — the store comonad
  iterated; the categorical limit of the "histo = iterated store" reading.
- López-González, Serrano, *Towards Optic-Based Algebraic Theories: The Case of
  Lenses* (PSC 2018) and *The optics of language-integrated query* (SCP 2020) —
  Scala-side optics theory; closest published kin to eo's carrier design.
- Morris, *Asymmetric Lenses in Scala* (2012) — the Scala lens lineage the
  migration-from-monocle docs sit in.
- Ahman, Bauer, *Runners in Action* (ESOP 2020) — comonad-as-context machinery;
  peripheral but in the same store-comonad generalization family.

## 3. The recursion-schemes canon (the zoo's sources of truth)

These are the papers the schemes themselves come from; the branch's plan docs
already cite the starred ones — collected here so the bibliography is complete
in one place.

- Meertens, *Paramorphisms* (Formal Aspects of Computing 4(5), 1992) — para.
- Fokkinga, *Tupling and mutumorphisms* (The Squiggolist 1(4), 1990) — mutu.
- Vene, Uustalu, *Functional programming with apomorphisms (corecursion)* (Proc.
  Estonian Acad. Sci. 47(3), 1998) — apo.
- ★ Uustalu, Vene, Pardo, *Recursion schemes from comonads* (ENTCS 2001) — the
  comonadic-fold framework; zygo/histo/dyna as comonadic folds; the Decor
  family's formal ancestor.
- Bartels, *Generalised coinduction* (MSCS 13, 2003) — gcata/gana; the g-
  machinery the decorated unfolds (futu/apo) ride.
- Capretta, Uustalu, Vene, *Recursive coalgebras from comonads* (Information and
  Computation 204, 2006) — **when a fold is productive/stack-safe**: the formal
  counterpart of FusionSpec's "the scalar neck is the barrier" finding. Cite for
  the fusion-law side conditions.
- Gibbons, *Metamorphisms: streaming representation-changers* (SCP 2007) — meta.
- ★ Hinze, Wu, Gibbons, *Unifying structured recursion schemes* (ICFP 2013) —
  adjoint folds subsume comonadic folds; the matrix that BiAffine's
  composition-matrix row targets.
- Hinze, Wu, *Histo- and dynamorphisms revisited* (WGP 2013) — histo/dyna/chrono
  details, dynamic-programming framing; grounds the space-honesty note on `Attr`.
- Hinze, *Adjoint folds and unfolds — an extended study* (SCP 2013) — the
  calculational toolkit behind the degeneration laws.
- ★ Yang, Wu, *Fantastic morphisms and where to find them*
  ([arXiv:2202.13633](https://arxiv.org/abs/2202.13633), 2022) — the
  practitioner's zoo catalogue; the naming reference for the `zoo/` package.
- Adámek, Milius, Vene, *Elgot algebras* (LMCS 2006) — the formal source for
  elgot/coelgot; Kmett's 2008 *Elgot (Co)Algebras* post is the practical
  rendering the branch cites.
- Kmett, `recursion-schemes` (Haskell library) — `distPara`/`distApo`/`micro`;
  the reference shapes the zoo's bench rows compare against.
- Eades, Stump, Oliver, *Hylomorphisms in the wild* (MSFP 2020) — production
  hylomorphism concerns (fusion, effects); adjacent to the M-driver story.

## 4. Scala-ecosystem implementations to cite honestly

- **droste** (<https://github.com/higherkindness/droste>) — Gather/Scatter,
  `hyloM`, the kernel design D4's bench rows and the Decor/Gather/Scatter
  honest-encoding note compare against; also the stack-unsafe-basic-schemes
  caveat the docs repeat.
- **Monocle** — the benchmarks' comparison baseline.
- **cats** (`Traverse`/`Monad.tailRecM`) — the lawful instances the machines
  ride; stack-safety reduces to tailRecM lawfulness (tested per M, per D3/D5).

## 5. How the bibliography should ship

- `site/docs/schemes.md` "Further reading" section: the §3 canon + O'Connor §2
  (store-comonad lens) + Riley + Pickering–Gibbons–Wu. Nothing else; the docs'
  claims must stay scoped to the shipped seams.
- Scaladoc pointers only where a claim is literally a theorem from a paper
  (fusion law → Capretta–Uustalu–Vene; paraLens lawfulness → O'Connor §2.2 +
  Riley).
- This file is the long-form reference; update it when the follow-ups (elgot
  port, BiAffine matrix row, higher-order decoration) land so the citations
  grow with the surface.
