package cats
package free

/**
 * The dual view of the Yoneda lemma. Also a free functor on `F`.
 * This is isomorphic to `F` as long as `F` itself is a functor.
 * The homomorphism from `F[A]` to `Coyoneda[F,A]` exists even when
 * `F` is not a functor.
 */
sealed abstract class Coyoneda[F[_], A] extends Serializable { self =>

  /** The pivot between `fi` and `k`, usually existential. */
  type Pivot

  /** The underlying value. */
  val fi: F[Pivot]

  /** The transformer function, to be lifted into `F` by `run`. */
  val k: Pivot => A

  import Coyoneda.{Aux, apply}

  /** Converts to `F[A]` given that `F` is a functor */
  final def run(implicit F: Functor[F]): F[A] = F.map(fi)(k)

  /** Converts to `Yoneda[F,A]` given that `F` is a functor */
  final def toYoneda(implicit F: Functor[F]): Yoneda[F, A] =
    new Yoneda[F, A] {
      def apply[B](f: A => B) = F.map(fi)(k andThen f)
    }

  /**
   * Simple function composition. Allows map fusion without touching
   * the underlying `F`.
   */
  final def map[B](f: A => B): Aux[F, B, Pivot] =
    apply(fi)(f compose k)

  final def transform[G[_]](f: F ~> G): Aux[G, A, Pivot] =
    apply(f(fi))(k)

}

object Coyoneda {
  /** Lift the `Pivot` type member to a parameter. It is usually more
    * convenient to use `Aux` than a structural type.
    */
  type Aux[F[_], A, B] = Coyoneda[F, A] { type Pivot = B }

  /** `F[A]` converts to `Coyoneda[F,A]` for any `F` */
  def lift[F[_], A](fa: F[A]): Coyoneda[F, A] = apply(fa)(identity[A])

  /**
   * Represents a partially-built Coyoneda instance. Used in the `by` method.
   */
  final class By[F[_]] {
    def apply[A, B](k: A => B)(implicit F: F[A]): Aux[F, B, A] = Coyoneda(F)(k)
  }

  /**
   * Partial application of type parameters to `apply`.
   *
   * It can be nicer to say `Coyoneda.by[F]{ x: X => ... }`
   *
   * ...instead of `Coyoneda[...](...){ x => ... }`.
   */
  def by[F[_]]: By[F] = new By[F]

  /** Like `lift(fa).map(_k)`. */
  def apply[F[_], A, B](fa: F[A])(k0: A => B): Aux[F, B, A] =
    new Coyoneda[F, B] {
      type Pivot = A
      val k = k0
      val fi = fa
    }
}
