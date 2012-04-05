package org.scalatra.command

import scalaz._
import org.scalatra.util.conversion._

case class FieldError(message: String, args: Any*)


object `package` {

  type FieldValidation[T] = Validation[FieldError, T]

  type Validator[T] = PartialFunction[Option[T], FieldValidation[T]]

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
