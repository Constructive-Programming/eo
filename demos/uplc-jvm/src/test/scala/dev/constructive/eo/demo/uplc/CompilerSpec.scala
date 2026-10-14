package dev.constructive.eo.demo.uplc

import dev.constructive.eo.demo.uplc.Oracle.Verdict
import org.specs2.mutable.Specification
import scalus.cardano.ledger.Word64
import scalus.uplc.eval.Result
import scalus.uplc.{Constant, DeBruijn, DefaultFun, NamedDeBruijn, Program, ProgramFlatCodec, Term}

/** Hand-written programs, one per compilation path, each checked against the CEK machine. */
class CompilerSpec extends Specification:

  private def flat(uplc: String): Array[Byte] =
    Program.parseUplc(s"(program 1.1.0 $uplc)").fold(e => sys.error(e), p => p.flatEncoded)

  private def term(uplc: String): Term =
    DeBruijn.deBruijnTerm(Program.parseUplc(s"(program 1.1.0 $uplc)").fold(sys.error, _.term))

  private def agrees(program: Term | String, expected: Option[String]) =
    val bytes = program match
      case t: Term   => ProgramFlatCodec.unsafeEncodeFlat(Program((1, 1, 0), t))
      case s: String => flat(s)
    Oracle.compare(bytes) must beLike {
      case Verdict.BothSucceed(t) if expected.nonEmpty => t.α_==(term(expected.get)) must beTrue
      case Verdict.BothFail(_, _) if expected.isEmpty  => ok
    }

  private def yields(uplc: String, expected: String) = agrees(uplc, Some(expected))
  private def fails(program: Term | String) = agrees(program, None)

  "the UPLC → JVM translation agrees with the CEK machine on" >> {
    "let-bound integer arithmetic (typed fast path)" >> yields(
      "[(lam x [(builtin addInteger) x (con integer 1)]) (con integer 41)]",
      "(con integer 42)"
    )
    "a multi-binding let whose right-hand sides see only the outer scope" >> yields(
      "[(lam a [(lam x (lam y [(builtin subtractInteger) x y])) (con integer 10) a]) (con integer 3)]",
      "(con integer 7)"
    )
    "if-then-else over delayed branches" >> yields(
      "(force [(force (builtin ifThenElse)) [(builtin lessThanInteger) (con integer 1) (con integer 2)] (delay (con string \"yes\")) (delay (error))])",
      "(con string \"yes\")"
    )
    "a closure returned as the result (discharge substitutes captures)" >>
      yields("[(lam x (lam y x)) (con integer 5)]", "(lam y (con integer 5))")
    "a partially applied lambda chain" >> yields(
      "[(lam x (lam y (lam z [(builtin addInteger) x z]))) (con integer 1)]",
      "(lam y (lam z [(builtin addInteger) (con integer 1) z]))"
    )
    "a partially applied builtin" >>
      yields("[(builtin addInteger) (con integer 1)]", "[(builtin addInteger) (con integer 1)]")
    "a forced polymorphic builtin" >>
      yields("(force (builtin ifThenElse))", "(force (builtin ifThenElse))")
    "a builtin applied as a value, through its CEK meaning" >> yields(
      "[(lam f [f (con bool True) (con integer 1) (lam x x)]) (force (builtin ifThenElse))]",
      "(con integer 1)"
    )
    "constr / case with a lambda branch" >> yields(
      "(case (constr 1 (con integer 2) (con integer 3)) (lam a a) (lam a (lam b [(builtin multiplyInteger) a b])))",
      "(con integer 6)"
    )
    "case whose branch takes more fields than given (partial result)" >> yields(
      "(case (constr 0 (con integer 2) (con integer 3)) (lam a (lam b (lam c a))))",
      "(lam c (con integer 2))"
    )
    "case whose branch is not a lambda (fields first, then the branch)" >> yields(
      "(case (constr 0 (con integer 2) (con integer 3)) [(lam f f) (builtin addInteger)])",
      "(con integer 5)"
    )
    "case whose branch takes fewer fields than given" >>
      fails("(case (constr 0 (con integer 2) (con integer 3)) (lam a a))")
    "case on an integer (PV11)" >> yields(
      "(case (con integer 1) (con string \"zero\") (con string \"one\"))",
      "(con string \"one\")"
    )
    "case on a boolean (PV11)" >> yields(
      "(case [(builtin equalsInteger) (con integer 1) (con integer 1)] (con integer 0) (con integer 1))",
      "(con integer 1)"
    )
    "case on a list (PV11)" >>
      yields(
        "(case (con (list integer) [7, 8]) (lam h (lam t h)) (con integer 0))",
        "(con integer 7)"
      )
    "recursion through a Z combinator" >> {
      val z = "(lam f [(lam x [f (lam v [x x v])]) (lam x [f (lam v [x x v])])])"
      val fact =
        "(lam self (lam n (force [(force (builtin ifThenElse)) [(builtin lessThanEqualsInteger) n (con integer 0)] (delay (con integer 1)) (delay [(builtin multiplyInteger) n [self [(builtin subtractInteger) n (con integer 1)]]])])))"
      yields(s"[[$z $fact] (con integer 20)]", "(con integer 2432902008176640000)")
    }
    "a crypto builtin (sha2_256)" >> yields(
      "[(builtin lengthOfByteString) [(builtin sha2_256) (con bytestring #00ff)]]",
      "(con integer 32)"
    )
    "data builtins and chooseData over delays" >> yields(
      "(force [(force (builtin chooseData)) (con data (I 5)) (delay (con integer 0)) (delay (con integer 1)) (delay (con integer 2)) (delay [(builtin unIData) (con data (I 5))]) (delay (con integer 4))])",
      "(con integer 5)"
    )
    "trace" >>
      yields("[(force (builtin trace)) (con string \"hello\") (con unit ())]", "(con unit ())")
    "error" >> fails("(error)")
    "division by zero on the typed path" >>
      fails("[(builtin divideInteger) (con integer 1) (con integer 0)]")
    "a type error on the typed path" >>
      fails("[(builtin addInteger) (con integer 1) (con string \"one\")]")
    "applying a non-function" >> fails("[(con integer 1) (con integer 2)]")
    "forcing a builtin that wants an argument" >> fails("(force (builtin addInteger))")
    "case on a constructor without a branch" >> fails("(case (constr 2) (con integer 0))")
    // The ledger's scope check skips case/constr bodies, so open terms do reach evaluation.
    "an unbound variable that is evaluated" >>
      fails(Term.Case(Term.Constr(Word64(0), Nil), List(Term.Var(NamedDeBruijn("free", 7)))))
  }

  "large programs: a 20 000-deep let chain still fits the JVM's 64 KB method limit" >> {
    // let x1 = x0 + 1 in let x2 = x1 + 1 in … — the layout compilers give top-level helpers.
    // Built as a term (Scalus' text parser recurses too deeply for it), de Bruijn throughout.
    val (x, one) = (Term.Var(NamedDeBruijn("x", 1)), Term.Const(Constant.Integer(1)))
    val next = Term.Apply(Term.Apply(Term.Builtin(DefaultFun.AddInteger), x), one)
    val chain =
      (1 to 20000).foldLeft[Term](x)((body, _) => Term.Apply(Term.LamAbs("x", body), next))
    val program = Term.Apply(Term.LamAbs("x", chain), Term.Const(Constant.Integer(0)))
    Exec.onLargeStack(agrees(program, Some("(con integer 20000)")))
  }

  "the compiled script can be applied after loading, like a validator" >> {
    val classFile = UplcJvm.translate(flat("(lam x [(builtin addInteger) x (con integer 1)])"))
    Exec.run(classFile, List(Constant.Integer(41))) must beLike {
      case Result.Success(t, _, _, _) => t.α_==(term("(con integer 42)")) must beTrue
    }
  }

  "malformed bytes are rejected as a value, not an exception" >> {
    UplcJvm.compile(Array[Byte](1, 2, 3)) must beLeft
  }
