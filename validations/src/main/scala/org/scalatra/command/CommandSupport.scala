package org.scalatra.command

import org.scalatra.util.MultiMap
import org.scalatra.{ScalatraBase, RouteMatcher, ScalatraKernel}
import collection.mutable.Map
import org.scalatra.liftjson.{LiftJsonSupportWithoutFormats, LiftJsonSupport}
import net.liftweb.json.Formats

/**
 * Support for [[mm.scalatra.command.Command]] binding and validation.
 */
trait CommandSupport {

  this: ScalatraBase with LiftJsonSupportWithoutFormats =>

  type CommandValidatedType = Command with ValidationSupport

  /**
   * Implicitly convert a [[mm.scalatra.command.Binding]] value to an [[scala.Option]]
   */
  implicit def bindingValue[T](b: Binding[T]): Option[T] = b.converted

  /**
   * Create and bind a [[org.scalatra.command.Command]] of the given type with the current Scalatra params.
   *
   * For every command type, creation and binding is performed only once and then stored into
   * a request attribute.
   */
  def command[T <: Command](implicit mf: Manifest[T], formats: Formats): T = {
    commandOption[T].getOrElse {
      val newCommand = mf.erasure.newInstance.asInstanceOf[T]
      newCommand.doBinding(params)
      requestProxy.update(commandRequestKey[T], newCommand)
      newCommand
    }
  }

  def commandOption[T <: Command : Manifest] : Option[T] = requestProxy.get(commandRequestKey[T]).map(_.asInstanceOf[T])

  private[command] def requestProxy: Map[String, AnyRef] = request

  private[command] def commandRequestKey[T <: Command : Manifest] = "_command_" + manifest[T].erasure.getName

  private class CommandRouteMatcher[T <: CommandValidatedType](implicit mf: Manifest[T], formats: Formats) extends RouteMatcher {

    override def apply() = if (command[T].valid.getOrElse(true)) Some(MultiMap()) else None

    override def toString = "[valid command guard]"
  }

  /**
   * Create a [[org.scalatra.RouteMatcher]] that evaluates '''true''' only if a command is valid. See
   * [[mm.scalatra.command.validation.ValidationSupport]] for details.
   */
  def ifValid[T <: CommandValidatedType](implicit mf: Manifest[T], formats: Formats): RouteMatcher = new CommandRouteMatcher[T]
}