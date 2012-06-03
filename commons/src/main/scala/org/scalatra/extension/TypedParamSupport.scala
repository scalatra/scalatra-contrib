package org.scalatra.extension

import org.scalatra.extension.params.ScalatraParamsImplicits
import org.scalatra.util.conversion.DefaultImplicitConversions
import org.scalatra.ScalatraBase

/**
 * Support trait for typed params.
 */
trait TypedParamSupport extends ScalatraBase with ScalatraParamsImplicits with DefaultImplicitConversions
