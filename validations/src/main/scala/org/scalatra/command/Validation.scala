package org.scalatra
package command

import org.scalatra.util.conversion._
import scala.util.control.Exception._
import java.net.URI
import org.apache.commons.validator.routines.{ UrlValidator, EmailValidator }
import mojolly.inflector.InflectorImports._
import scala.util.matching.Regex
import java.util.Locale.ENGLISH
import scalaz._
import Scalaz._


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

object Validators {
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

  def nonEmptyString(fieldName: String): Validator[String] =
    new PredicateValidator[String](fieldName, s => s != null && s.trim.nonEmpty, "%s must be present.")

  def nonEmptyCollection[TResult <: Seq[_]](fieldName: String): Validator[TResult] =
    new PredicateValidator[TResult](fieldName, _.nonEmpty, "%s must not be empty.")

  def validEmail(fieldName: String): Validator[String] =
    new PredicateValidator[String](fieldName, EmailValidator.getInstance.isValid(_), "%s must be a valid email.")

  def validAbsoluteUrl(fieldName: String, allowLocalHost: Boolean, schemes: String*) =
    buildUrlValidator(fieldName, true, allowLocalHost, schemes: _*)

  def validUrl(fieldName: String, allowLocalHost: Boolean, schemes: String*) =
    buildUrlValidator(fieldName, false, allowLocalHost, schemes: _*)

  def validFormat(fieldName: String, regex: Regex, messageFormat: String = "%s is invalid."): Validator[String] =
    new PredicateValidator[String](fieldName, regex.findFirstIn(_).isDefined, messageFormat)

  def validConfirmation(fieldName: String, confirmationFieldName: String, confirmationValue: String): Validator[String] =
    new PredicateValidator[String](
      fieldName,
      _ == confirmationValue,
      "%s must match " + confirmationFieldName.underscore.humanize.toLowerCase(ENGLISH) + ".")

  def greaterThan[T <% Ordered[T]](fieldName: String, min: T): Validator[T] =
    new PredicateValidator[T](fieldName, _ > min, "%s must be greater than " + min.toString)

  def lessThan[T <% Ordered[T]](fieldName: String, max: T): Validator[T] =
    new PredicateValidator[T](fieldName, _ < max, "%s must be less than " + max.toString)

  def greaterThanOrEqualTo[T <% Ordered[T]](fieldName: String, min: T): Validator[T] =
    new PredicateValidator[T](fieldName, _ >= min, "%s must be greater than or equal to " + min.toString)

  def lessThanOrEqualTo[T <% Ordered[T]](fieldName: String, max: T): Validator[T] =
    new PredicateValidator[T](fieldName, _ <= max, "%s must be less than or equal to " + max.toString)

  def minLength(fieldName: String, min: Int): Validator[String] =
    new PredicateValidator[String](
      fieldName, _.size >= min, "%s must be at least " + min.toString + " characters long.")

  def oneOf[TResult](fieldName: String, expected: TResult*): Validator[TResult] =
    new PredicateValidator[TResult](
      fieldName, expected.contains, "%s must be one of " + expected.mkString("[", ", ", "]"))

  def enumValue(fieldName: String, enum: Enumeration): Validator[String] =
    oneOf(fieldName, enum.values.map(_.toString).toSeq: _*)


  private def buildUrlValidator(fieldName: String, absolute: Boolean, allowLocalHost: Boolean, schemes: String*): Validator[String] = {
    val validator = (url: String) ⇒ {
      (allCatch opt {
        val u = URI.create(url).normalize()
        !absolute || u.isAbsolute
      }).isDefined && (allowLocalHost || UrlValidator.getInstance().isValid(url))
    }
    new PredicateValidator[String](fieldName, validator, "%s must be a valid url.")
  }

}

object Validation {
  
  def nonEmptyString(fieldName: String, value: ⇒ String): FieldValidation[String] =
    Validators.nonEmptyString(fieldName).validate(value)

  def nonEmptyCollection[TResult <: Seq[_]](fieldName: String, value: ⇒ TResult): FieldValidation[TResult] =
    Validators.nonEmptyCollection(fieldName).validate(value)

  def validEmail(fieldName: String, value: ⇒ String): FieldValidation[String] =
    Validators.validEmail(fieldName).validate(value)

  def validAbsoluteUrl(fieldName: String, value: ⇒ String, allowLocalHost: Boolean, schemes: String*) =
    Validators.validAbsoluteUrl(fieldName, allowLocalHost, schemes:_*).validate(value)

  def validUrl(fieldName: String, value: ⇒ String, allowLocalHost: Boolean, schemes: String*) =
    Validators.validUrl(fieldName, allowLocalHost, schemes:_*).validate(value)

  def validFormat(fieldName: String, value: ⇒ String, regex: Regex, messageFormat: String = "%s is invalid."): FieldValidation[String] =
    Validators.validFormat(fieldName, regex, messageFormat).validate(value)

  def validConfirmation(fieldName: String, value: ⇒ String, confirmationFieldName: String, confirmationValue: String): FieldValidation[String] =
    Validators.validConfirmation(fieldName, confirmationFieldName, confirmationValue).validate(value)

  def greaterThan[T <% Ordered[T]](fieldName: String, value: ⇒ T, min: T): FieldValidation[T] =
    Validators.greaterThan(fieldName, min).validate(value)

  def lessThan[T <% Ordered[T]](fieldName: String, value: ⇒ T, max: T): FieldValidation[T] =
    Validators.lessThan(fieldName, max).validate(value)

  def greaterThanOrEqualTo[T <% Ordered[T]](fieldName: String, value: ⇒ T, min: T): FieldValidation[T] =
    Validators.greaterThanOrEqualTo(fieldName, min).validate(value)

  def lessThanOrEqualTo[T <% Ordered[T]](fieldName: String, value: ⇒ T, max: T): FieldValidation[T] =
    Validators.lessThanOrEqualTo(fieldName, max).validate(value)

  def minLength(fieldName: String, value: ⇒ String, min: Int): FieldValidation[String] =
    Validators.minLength(fieldName, min).validate(value)

  def oneOf[TResult](fieldName: String, value: ⇒ TResult, expected: TResult*): FieldValidation[TResult] =
    Validators.oneOf(fieldName, expected:_*).validate(value)

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

trait CommandValidators { self: Command with ValidationSupport =>
  
  protected def nonEmptyString(fieldName: String): Validator[String] = {
    case s => Validation.nonEmptyString(fieldName, s getOrElse "")
  }

  protected def nonEmptyCollection[TResult <: Seq[_]](fieldName: String): Validator[TResult] = {
    case s => Validation.nonEmptyCollection(fieldName, s getOrElse Nil.asInstanceOf[TResult])
  }

  protected def validEmail(fieldName: String): Validator[String] = {
    case m => Validation.validEmail(fieldName, m getOrElse "")
  }

  protected def validAbsoluteUrl(fieldName: String, allowLocalHost: Boolean, schemes: String*): Validator[String] = {
    case value => Validators.validAbsoluteUrl(fieldName, allowLocalHost, schemes:_*).validate(value getOrElse "")
  }

  protected def validUrl(fieldName: String, allowLocalHost: Boolean, schemes: String*): Validator[String] = {
    case value => Validators.validUrl(fieldName, allowLocalHost, schemes:_*).validate(value getOrElse "")
  }

  protected def validFormat(fieldName: String, regex: Regex, messageFormat: String = "%s is invalid."): Validator[String] = {
    case value => Validators.validFormat(fieldName, regex, messageFormat).validate(value getOrElse "")
  }

  protected def validConfirmation(fieldName: String, confirmationFieldName: String, confirmationValue: String): Validator[String] = {
    case value => Validators.validConfirmation(fieldName, confirmationFieldName, confirmationValue).validate(value getOrElse "")
  }

  protected def greaterThan[T <% Ordered[T]](fieldName: String, min: T): Validator[T] = {
    case value => Validators.greaterThan(fieldName, min).validate(value getOrElse null.asInstanceOf[T])
  }

  protected def lessThan[T <% Ordered[T]](fieldName: String, max: T): Validator[T] = {
    case value => Validators.lessThan(fieldName, max).validate(value getOrElse  null.asInstanceOf[T])
  }

  protected def greaterThanOrEqualTo[T <% Ordered[T]](fieldName: String, min: T): Validator[T] = {
    case value => Validators.greaterThanOrEqualTo(fieldName, min).validate(value getOrElse  null.asInstanceOf[T])
  }

  protected def lessThanOrEqualTo[T <% Ordered[T]](fieldName: String, max: T): Validator[T] = {
    case value => Validators.lessThanOrEqualTo(fieldName, max).validate(value getOrElse  null.asInstanceOf[T])
  }

  protected def minLength(fieldName: String, min: Int): Validator[String] = {
    case value => Validators.minLength(fieldName, min).validate(value getOrElse  "")
  }

  protected def oneOf[TResult](fieldName: String, expected: TResult*): Validator[TResult] = {
    case value => Validators.oneOf(fieldName, expected:_*).validate(value getOrElse Nil.asInstanceOf[TResult])
  }

  protected def enumValue(fieldName: String, enum: Enumeration): Validator[String] =
    oneOf(fieldName, enum.values.map(_.toString).toSeq: _*)
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

    def withBinding(bf: Binding[T] => Binding[T]) = bf(binding)
    
      
    def nonEmptyString: Validator[String] = {
      case s => Validation.nonEmptyString(binding.name, s getOrElse "")
    }
  
    def nonEmptyCollection[TResult <: Seq[_]]: Validator[TResult] = {
      case s => Validation.nonEmptyCollection(binding.name, s getOrElse Nil.asInstanceOf[TResult])
    }
  
    def validEmail: Validator[String] = {
      case m => Validation.validEmail(binding.name, m getOrElse "")
    }
  
    def validAbsoluteUrl(allowLocalHost: Boolean, schemes: String*): Validator[String] = {
      case value => Validators.validAbsoluteUrl(binding.name, allowLocalHost, schemes:_*).validate(value getOrElse "")
    }
  
    def validUrl(allowLocalHost: Boolean, schemes: String*): Validator[String] = {
      case value => Validators.validUrl(binding.name, allowLocalHost, schemes:_*).validate(value getOrElse "")
    }
  
    def validFormat(regex: Regex, messageFormat: String = "%s is invalid."): Validator[String] = {
      case value => Validators.validFormat(binding.name, regex, messageFormat).validate(value getOrElse "")
    }
  
    def validConfirmation(confirmationFieldName: String, confirmationValue: String): Validator[String] = {
      case value => Validators.validConfirmation(binding.name, confirmationFieldName, confirmationValue).validate(value getOrElse "")
    }
  
    def greaterThan[T <% Ordered[T]](min: T): Validator[T] = {
      case value => Validators.greaterThan(binding.name, min).validate(value getOrElse null.asInstanceOf[T])
    }
  
    def lessThan[T <% Ordered[T]](max: T): Validator[T] = {
      case value => Validators.lessThan(binding.name, max).validate(value getOrElse  null.asInstanceOf[T])
    }
  
    def greaterThanOrEqualTo[T <% Ordered[T]](min: T): Validator[T] = {
      case value => Validators.greaterThanOrEqualTo(binding.name, min).validate(value getOrElse  null.asInstanceOf[T])
    }
  
    def lessThanOrEqualTo[T <% Ordered[T]](max: T): Validator[T] = {
      case value => Validators.lessThanOrEqualTo(binding.name, max).validate(value getOrElse  null.asInstanceOf[T])
    }
  
    def minLength(min: Int): Validator[String] = {
      case value => Validators.minLength(binding.name, min).validate(value getOrElse  "")
    }
  
    def oneOf[TResult](expected: TResult*): Validator[TResult] = {
      case value => Validators.oneOf(binding.name, expected:_*).validate(value getOrElse Nil.asInstanceOf[TResult])
    }
  
    def enumValue(enum: Enumeration): Validator[String] = oneOf(enum.values.map(_.toString).toSeq: _*)
    

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
