package org.scalatra.command

import org.scalatra.util.conversion._

trait Binding[T] {

  type ValueType = T


  def name: String
  
  def original: String
  
  def converted: Option[T]
  
  def apply(value: String)

  override def toString() = "Binding(name: %s, original: %s, converted: %s)".format(name, original, converted)

}

class BasicBinding[T](val name: String)(implicit val conversion: TypeConverter[T]) extends Binding[T] {

  var original = null.asInstanceOf[String]

  def converted = conversion(original)

  def apply(value: String) {
    original = value
  }

  override def hashCode() = 13 + 17 * name.hashCode()

  override def equals(obj: Any) = obj match {
    case b : BasicBinding[_] => b.name == this.name
    case _ => false
  }
}

object Binding {

  def apply[T](name: String)(implicit converter: TypeConverter[T]): Binding[T] = new BasicBinding[T](name)

}
