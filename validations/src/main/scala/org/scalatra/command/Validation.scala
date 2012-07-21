package org.scalatra
package command

import scalaz._
import Scalaz._
import org.scalatra.util.conversion._
import scala.util.control.Exception._
import java.net.URI
import org.apache.commons.validator.routines.{ UrlValidator, EmailValidator }
import mojolly.inflector.InflectorImports._
import scala.util.matching.Regex
import java.util.Locale.ENGLISH

object FieldError {
  def apply(message: String, args: Any*) = {
    args.headOption match {
      case Some(a: String) if a != null && a.trim.nonEmpty => ValidationError(message, a, args.drop(1):_*)
      case _ => SimpleError(message, args:_*)
    }
  }
}
trait FieldError {
  def message: String
  def args: Seq[Any]
}
case class ValidationError(message: String, field: String, args: Any*) extends FieldError
case class SimpleError(message: String, args: Any*) extends FieldError

//case class FieldError(message: String, args: Any*)


object `package` {

  type FieldValidation[T] = Validation[FieldError, T]

  type Validator[T] = PartialFunction[Option[T], FieldValidation[T]]

}

object Validation {
  trait Validator[TValue] {
    def validate[TResult >: TValue <: TValue](subject: TResult): FieldValidation[TResult]
  }

  class PredicateValidator[TValue](fieldName: String, isValid: TValue ⇒ Boolean, messageFormat: String)
      extends Validator[TValue] {
    override def validate[TResult >: TValue <: TValue](value: TResult): FieldValidation[TResult] = {
      if (isValid(value)) value.success
      else ValidationError(messageFormat.format(fieldName.humanize), fieldName.underscore).fail[TResult]
    }
  }

  def nonEmptyString(fieldName: String, value: ⇒ String): FieldValidation[String] =
    new PredicateValidator[String](fieldName, s => s != null && s.trim.nonEmpty, "%s must be present.").validate(value)

  def nonEmptyCollection[TResult <: Seq[_]](fieldName: String, value: ⇒ TResult): FieldValidation[TResult] =
    new PredicateValidator[TResult](fieldName, _.nonEmpty, "%s must not be empty.").validate(value)

  def validEmail(fieldName: String, value: ⇒ String): FieldValidation[String] =
    new PredicateValidator[String](fieldName, EmailValidator.getInstance.isValid(_), "%s must be a valid email.").validate(value)

  def validAbsoluteUrl(fieldName: String, value: ⇒ String, allowLocalHost: Boolean, schemes: String*) =
    buildUrlValidator(fieldName, value, true, allowLocalHost, schemes: _*)

  def validUrl(fieldName: String, value: ⇒ String, allowLocalHost: Boolean, schemes: String*) =
    buildUrlValidator(fieldName, value, false, allowLocalHost, schemes: _*)

  private def buildUrlValidator(fieldName: String, value: ⇒ String, absolute: Boolean, allowLocalHost: Boolean, schemes: String*): FieldValidation[String] = {
    val validator = (url: String) ⇒ {
      (allCatch opt {
        val u = URI.create(url).normalize()
        !absolute || u.isAbsolute
      }).isDefined && (!allowLocalHost || UrlValidator.getInstance().isValid(url))
    }
    new PredicateValidator[String](fieldName, validator, "%s must be a valid url.").validate(value)
  }

  def validFormat(fieldName: String, value: ⇒ String, regex: Regex, messageFormat: String = "%s is invalid."): FieldValidation[String] =
    new PredicateValidator[String](fieldName, regex.findFirstIn(_).isDefined, messageFormat).validate(value)

  def validConfirmation(fieldName: String, value: ⇒ String, confirmationFieldName: String, confirmationValue: String): FieldValidation[String] =
    new PredicateValidator[String](
      fieldName,
      _ == confirmationValue,
      "%s must match " + confirmationFieldName.underscore.humanize.toLowerCase(ENGLISH) + ".").validate(value)

  def greaterThan[T <% Ordered[T]](fieldName: String, value: ⇒ T, min: T): FieldValidation[T] =
    new PredicateValidator[T](fieldName, _ > min, "%s must be greater than " + min.toString).validate(value)

  def lessThan[T <% Ordered[T]](fieldName: String, value: ⇒ T, max: T): FieldValidation[T] =
    new PredicateValidator[T](fieldName, _ < max, "%s must be less than " + max.toString).validate(value)

  def greaterThanOrEqualTo[T <% Ordered[T]](fieldName: String, value: ⇒ T, min: T): FieldValidation[T] =
    new PredicateValidator[T](fieldName, _ >= min, "%s must be greater than or equal to " + min.toString).validate(value)

  def lessThanOrEqualTo[T <% Ordered[T]](fieldName: String, value: ⇒ T, max: T): FieldValidation[T] =
    new PredicateValidator[T](fieldName, _ <= max, "%s must be less than or equal to " + max.toString).validate(value)

  def minLength(fieldName: String, value: ⇒ String, min: Int): FieldValidation[String] =
    new PredicateValidator[String](
      fieldName, _.size >= min, "%s must be at least " + min.toString + " characters long.").validate(value)

  def oneOf[TResult](fieldName: String, value: ⇒ TResult, expected: TResult*): FieldValidation[TResult] =
    new PredicateValidator[TResult](
      fieldName, expected.contains, "%s must be one of " + expected.mkString("[", ", ", "]")).validate(value)

  def enumValue(fieldName: String, value: ⇒ String, enum: Enumeration): FieldValidation[String] =
    oneOf(fieldName, value, enum.values.map(_.toString).toSeq: _*)
}

/**
 * A field [[mm.scalatra.command.Binding]] which value has been validated.
 */
trait ValidatedBinding[T] extends Binding[T]  {

  /**
   * Result of command. Either one of @Rejected or @Accepted
   */
  def validation: FieldValidation[T]

  /**
   * Check whether the the field value conforms to the user requirements.
   */
  def valid = validation.isSuccess

  /**
   * The rejected message, if any.
   */
  def rejected = validation.fail.toOption
}


class ValidatedBindingDecorator[T](val validator: Validator[T], binding: Binding[T]) extends ValidatedBinding[T] {

  lazy val validation = validator(converted)

  def name = binding.name

  def original = binding.original

  def converted = binding.converted

  def apply(value: String) = binding.apply(value)

  override def hashCode() = binding.hashCode()

  override def equals(obj: Any) = binding.equals(obj)
}

trait ValidationSupport extends Validations {

  this: Command =>

  private var _valid: Option[Boolean] = None

  private var _errors: List[ValidatedBinding[_]] = Nil

  /**
   * Check whether this command is valid.
   */
  def valid: Option[Boolean] = _valid

  /**
   * Return a Map of all field command error keyed by field binding name (NOT the name of the variable in command
   * object).
   */
  def errors: Seq[ValidatedBinding[_]] = _errors

  def accept[T](value: T): FieldValidation[T] = success(value)

  def reject[T](message: String, args: Any*): FieldValidation[T] = failure(FieldError(message, args:_*))

  /**
   * Support class for 'validate' method provided by the implicit below.
   */
  sealed class BindingValidationSupport[T](command: Command, binding: Binding[T]) extends Validations {

    private def acceptAsDefault: Validator[T] = {
      case Some(x) => accept[T](x)
      case None => accept(null.asInstanceOf[T])
    }

    /**
     * Validate this binding with the given partial function.
     */
    def validate(v: Validator[T]): ValidatedBinding[T] = {
      val validator = v orElse acceptAsDefault
      val newBinding = new ValidatedBindingDecorator[T](validator, binding)
      command.bindings = command.bindings :+ newBinding
      newBinding
    }

  }

  /**
   * Implicit enhancement for [[mm.scalatra.command.Binding]]
   */
  implicit def binding2Validated[T](binding: Binding[T]) = new BindingValidationSupport[T](this, binding)


  /**
   * Perform command as afterBinding task.
   */
  afterBinding {
    _errors = bindings.collect { case (b: ValidatedBinding[_]) if !b.valid => b }
    _valid = Some(_errors.isEmpty)
  }

}
