package org.scalatra.extension

import org.scalatra._

import net.liftweb.json.Extraction
import net.liftweb.json._
import net.liftweb.json.JsonAST.{ JInt, JValue }
import net.liftweb.json.Printer._

trait ProductToJsonSupport extends ScalatraKernel {

  override protected def renderPipeline = ({
    case p: Product => {
      implicit val formats = DefaultFormats
      contentType = "application/json; charset=utf-8"
      val decomposed = Extraction.decompose(p)
      val rendered = JsonAST.render(decomposed)
      net.liftweb.json.compact(rendered).getBytes("UTF-8")
    }
  }: RenderPipeline) orElse super.renderPipeline
}