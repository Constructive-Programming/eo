package eo

object Getter {

  import Function.const

  def apply[S, A](get: S => A): Optic[S, Unit, A, A, Forgetful] =
    new Optic[S, Unit, A, A, Forgetful] {
      type X = Nothing
      def to: S => A = get
      def from: A => Unit = const(())
    }

}
