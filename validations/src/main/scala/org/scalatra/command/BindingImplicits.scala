package org.scalatra.command

import org.scalatra.util.conversion._
import java.text.DateFormat
import java.util.Date

/**
 * Commonly-used field implementations factory.
 *
 * @author mmazzarolo
 */
trait BindingImplicits extends DefaultImplicitConversions {

  private def blankStringConverter(blankAsNull: Boolean): TypeConverter[String] = (s: String) => Option(s) match {
    case x@Some(value: String) if (!blankAsNull || value.trim.nonEmpty) => x
    case _ => None
  }

  def asGeneric[T](name: String, f: (String) => T): Binding[T] = asImplicitGeneric(name)(f)

  def asImplicitGeneric[T](name: String)(implicit tc: TypeConverter[T]): Binding[T] = Binding[T](name)

  implicit def asType[T <: Any : TypeConverter](name: String): Binding[T] = Binding[T](name)

  implicit def asString(name: String, blankAsNull: Boolean = true): Binding[String] = Binding(name)(blankStringConverter(blankAsNull))

  implicit def asString(param: (String, Boolean)): Binding[String] = asString(param._1, param._2)

  def asDate(name: String, format: DateFormat = DateFormat.getInstance()): Binding[Date] = Binding(name)(stringToDateFormat(format))

  def asDate(name: String, format: String): Binding[Date] = Binding(name)(stringToDate(format))

  implicit def asDateWithStringFormat(param: (String, String)): Binding[Date] = asDate(param._1, param._2)

  implicit def asDateWithDateFormat(param: (String, DateFormat)): Binding[Date] = asDate(param._1, param._2)

}

object BindingImplicits extends BindingImplicits
