package scalaz

trait Cokleisli[F[_], A, B] { self =>
  def run(fa: F[A]): B

  def contramapValue[C](f: F[C] => F[A]): Cokleisli[F, C,  B] = new Cokleisli[F, C, B] {
    def run(fc: F[C]): B = self.run(f(fc))
  }

  def map[C](f: B => C): Cokleisli[F, A, C] = new Cokleisli[F, A, C] {
    def run(fa: F[A]) = f(self.run(fa))
  }

  def flatMap[C](f: B => Cokleisli[F, A, C]): Cokleisli[F, A, C] = new Cokleisli[F, A, C] {
    def run(fa: F[A]) = f(self.run(fa)).run(fa)
  }

//  def redaer(implicit i: Identity[A] =:= W[A]): A => B =
//    a => run(id(a))

  def <<=(a: F[A])(implicit F: Functor[F], FC: CoJoin[F]): F[B] =
    F.map(FC.cojoin(a))(run)

  def =>=[C](c: Cokleisli[F, B, C])(implicit F: Functor[F], FC: CoJoin[F]): Cokleisli[F, A, C] =
    Cokleisli(fa => c run (<<=(fa)))

  def compose[C](c: Cokleisli[F, C, A])(implicit F: Functor[F], FC: CoJoin[F]): Cokleisli[F, C, B] =
    c =>= this

  def =<=[C](c: Cokleisli[F, C, A])(implicit F: Functor[F], FC: CoJoin[F]): Cokleisli[F, C, B] =
    compose(c)
}

object Cokleisli extends CokleisliFunctions with CokleisliInstances {
  def apply[F[_], A, B](f: F[A] => B): Cokleisli[F, A, B] = new Cokleisli[F, A, B] {
    def run(fa: F[A]): B = f(fa)
  }
}

trait CokleisliInstances1 {
  implicit def cokleisliArr[F[_]](implicit F0: CoPointed[F]) = new CokleisliArr[F] {
    override implicit def F = F0
  }
  implicit def cokleisliFirst[F[_]](implicit F0: CoPointed[F]) = new CokleisliFirst[F] {
    override implicit def F = F0
  }
  implicit def cokleisliArrId[F[_]](implicit F0: CoPointed[F]) = new CokleisliArrId[F] {
    override implicit def F = F0
  }
}

trait CokleisliInstances0 extends CokleisliInstances1 {
  implicit def cokleisliCompose[F[_]](implicit F0: CoJoin[F] with Functor[F]) = new CokleisliCompose[F] {
    override implicit def F = F0
  }
}

trait CokleisliInstances extends CokleisliInstances0 {
  implicit def cokleisliMonad[F[_], R] = new CokleisliMonad[F, R] {}
  
  implicit def cokleisliArrow[F[_]](implicit F0: CoMonad[F]) = new CokleisliArrow[F] {
    override implicit def F = F0
  }
}

trait CokleisliFunctions {
  // TODO
//  type RedaerT[A, F[_], B] = Cokleisli[F, A, B]
//  type Redaer[A, B] = Cokleisli[Need, A, B]

//  def redaer[A, B](r: A => B): Redaer[A, B] =
//    Cokleisli[A, Identity, B](a => r(a.value))
//
//  def ksa[F[_] : CoPointed, A]: Cokleisli[A, F, A] =
//    Cokleisli(a => implicitly[CoPointed[F]].coPoint(a))
}

trait CokleisliMonad[F[_], R] extends Monad[({type λ[α] = Cokleisli[F, R, α]})#λ] {
  override def ap[A, B](fa: => Cokleisli[F, R, A])(f: => Cokleisli[F, R, (A) => B]) = f flatMap (fa map _)
  def point[A](a: => A) = Cokleisli(_ => a)
  def bind[A, B](fa: Cokleisli[F, R, A])(f: (A) => Cokleisli[F, R, B]) = fa flatMap f
}

trait CokleisliArr[F[_]] extends Arr[({type λ[α, β] = Cokleisli[F, α, β]})#λ] {
  implicit def F: CoPointed[F]
  def arr[A, B](f: (A) => B) = Cokleisli(a => f(F.copoint(a)))
}

trait CokleisliFirst[F[_]] extends First[({type λ[α, β] = Cokleisli[F, α, β]})#λ] {
  implicit def F: CoPointed[F]

  def first[A, B, C](f: Cokleisli[F, A, B]) =
    Cokleisli[F, (A, C), (B, C)](w => (f.run(F.map(w)(ac => ac._1)), F.copoint(w)._2))
}

trait CokleisliArrId[F[_]] extends ArrId[({type λ[α, β] = Cokleisli[F, α, β]})#λ] {
  implicit def F: CoPointed[F]

  override def id[A] = Cokleisli(F.copoint)
}

trait CokleisliCompose[F[_]] extends Compose[({type λ[α, β] = Cokleisli[F, α, β]})#λ] {
  implicit def F: CoJoin[F] with Functor[F]

  override def compose[A, B, C](f: Cokleisli[F, B, C], g: Cokleisli[F, A, B]) = f compose g
}


trait CokleisliArrow[F[_]] 
  extends Arrow[({type λ[α, β] = Cokleisli[F, α, β]})#λ] 
  with CokleisliArr[F] 
  with CokleisliArrId[F] 
  with CokleisliCompose[F]
  with CokleisliFirst[F] {
  
  implicit def F: CoMonad[F]
}
