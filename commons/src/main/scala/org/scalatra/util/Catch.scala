package org.scalatra.util

/**
 * Helper for wrapping exceptions into [[scala.Either]] type instance.
 */
object Catch {
  def apply[B](f: => B): Either[Exception, B] = try {
    Right(f)
  } catch {
    case e: Exception => Left(e)
  }
}
