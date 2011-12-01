package com.osinka.subset

import com.mongodb.DBObject
import DBO._

class Tuple2Subset[T1,T2](val f1: String, val f2: String) { tuple =>
  /*
   * Experimental "value (de)serializer" for Tuple2.
   * 
   * No go, because it's very confusing and requires creating manual "implicits"
  def serializer(implicit s1: ValueSerializer[T1], s2: ValueSerializer[T2]) =
    new Serializer[(T1,T2)] {
      override def apply(t2: (T1,T2)): DBObject => DBObject = tuple.apply(t2)(s1,s2)
    }

  def deserializer(implicit g1: ValueDeserializer[T1], g2: ValueDeserializer[T2]) =
    new Deserializer[(T1,T2)] {
      override def unapply(dbo: DBObject): Option[(T1,T2)] = tuple.unapply(dbo)(g1,g2)
    }
  */
  def apply(t2: (T1,T2))(implicit s1: ValueSerializer[T1], s2: ValueSerializer[T2]): DBObject => DBObject =
    (dbo: DBObject) => dbo.write(f1, s1.serialize(t2._1)).write(f2, s2.serialize(t2._2))

  def unapply(dbo: DBObject)(implicit g1: ValueDeserializer[T1], g2: ValueDeserializer[T2]): Option[(T1,T2)] =
    for {o1 <- Option(dbo get f1)
         x1 <- g1.deserialize(o1)
         o2 <- Option(dbo get f2)
         x2 <- g2.deserialize(o2)}
    yield (x1, x2)

//  def ~[T3](f3: Field[T3]) = new Tuple3Subset[T1,T2,T3](this, f3.name)
}


/*
class Tuple3Subset[T1,T2,T3](val e2: Tuple2Subset[T1,T2], val f3: String) {
  def apply(t3: (T1,T2,T3))(implicit s1: Setter[T1], s2: Setter[T2], s3: Setter[T3]): DBObject => DBObject =
    e2.apply( (t3._1, t3._2) )(s1,s2) andThen {s3.set _}.curried(f3)(t3._3)

  def unapply(dbo: DBObject)(implicit g1: Getter[T1], g2: Getter[T2], g3: Getter[T3]): Option[(T1,T2,T3)] =
    for {t2 <- e2.unapply(dbo); x3 <- g3.get(f3, dbo)} yield (t2._1, t2._2, x3)

  def ~[T4](f4: Field[T4]) = new Tuple4Subset[T1,T2,T3,T4](this, f4.name)
}

class Tuple4Subset[T1,T2,T3,T4](val e3: Tuple3Subset[T1,T2,T3], val f4: String) {
  def apply(t4: (T1,T2,T3,T4))(implicit s1: Setter[T1], s2: Setter[T2], s3: Setter[T3], s4: Setter[T4]): DBObject => DBObject =
    e3.apply( (t4._1, t4._2, t4._3) )(s1,s2,s3) andThen {s4.set _}.curried(f4)(t4._4)

  def unapply(dbo: DBObject)(implicit g1: Getter[T1], g2: Getter[T2], g3: Getter[T3], g4: Getter[T4]): Option[(T1,T2,T3,T4)] =
    for {t3 <- e3.unapply(dbo); x4 <- g4.get(f4, dbo)} yield (t3._1, t3._2, t3._3, x4)
  
  def ~[T5](f5: Field[T5]) = new Tuple5Subset[T1,T2,T3,T4,T5](this, f5.name)
}

class Tuple5Subset[T1,T2,T3,T4,T5](val e4: Tuple4Subset[T1,T2,T3,T4], val f5: String) {
  def apply(t5: (T1,T2,T3,T4,T5))(implicit s1: Setter[T1], s2: Setter[T2], s3: Setter[T3], s4: Setter[T4], s5: Setter[T5]): DBObject => DBObject =
    e4.apply( (t5._1, t5._2, t5._3, t5._4) )(s1,s2,s3,s4) andThen {s5.set _}.curried(f5)(t5._5)

  def unapply(dbo: DBObject)(implicit g1: Getter[T1], g2: Getter[T2], g3: Getter[T3], g4: Getter[T4], g5: Getter[T5]): Option[(T1,T2,T3,T4,T5)] =
    for {t4 <- e4.unapply(dbo); x5 <- g5.get(f5, dbo)} yield (t4._1, t4._2, t4._3, t4._4, x5)
  
//  def ~[T5](f5: Subset[T5]) = new TupleSubset5[T1,T2,T3,T4,T5](this, f5.name)
}

// TODO: Tuple6..22

object TupleSetters extends TupleSerializer

trait TupleSerializer {
  import DBO._

  implicit def tuple2setter[T1,T2](t2subset: Tuple2Subset[T1,T2])(implicit s1: Setter[T1], s2: Setter[T2]): Setter[(T1,T2)] =
    new Setter[(T1,T2)] {
      override def set(key: String, t: (T1,T2), dbo: DBObject): DBObject =
        dbo.write(key, t2subset.apply(t)(s1,s2).apply(empty))
    }

  implicit def tuple3setter[T1,T2,T3](t3subset: Tuple3Subset[T1,T2,T3])(implicit s1: Setter[T1], s2: Setter[T2], s3: Setter[T3]): Setter[(T1,T2,T3)] =
    new Setter[(T1,T2,T3)] {
      override def set(key: String, t: (T1,T2,T3), dbo: DBObject): DBObject =
        dbo.write(key, t3subset.apply(t)(s1,s2,s3).apply(empty))
    }

  implicit def tuple4setter[T1,T2,T3,T4](t4subset: Tuple4Subset[T1,T2,T3,T4])(implicit s1: Setter[T1], s2: Setter[T2], s3: Setter[T3], s4: Setter[T4]): Setter[(T1,T2,T3,T4)] =
    new Setter[(T1,T2,T3,T4)] {
      override def set(key: String, t: (T1,T2,T3,T4), dbo: DBObject): DBObject =
        dbo.write(key, t4subset.apply(t)(s1,s2,s3,s4).apply(empty))
    }

  implicit def tuple5setter[T1,T2,T3,T4,T5](t5subset: Tuple5Subset[T1,T2,T3,T4,T5])(implicit s1: Setter[T1], s2: Setter[T2], s3: Setter[T3], s4: Setter[T4], s5: Setter[T5]): Setter[(T1,T2,T3,T4,T5)] =
    new Setter[(T1,T2,T3,T4,T5)] {
      override def set(key: String, t: (T1,T2,T3,T4,T5), dbo: DBObject): DBObject =
        dbo.write(key, t5subset.apply(t)(s1,s2,s3,s4,s5).apply(empty))
    }
}
*/