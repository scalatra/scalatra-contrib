Scalatra is a tiny, [Sinatra](http://www.sinatrarb.com/)-like web framework for
[Scala](http://www.scala-lang.org/). Here are some contributed extensions to the library.

## Scalatra Documentation

If you're new to Scalatra, check out [The Scalatra Book](http://www.scalatra.org/stable/book/) for more.

# Contribs

## Params

## Product to Json Support

Including this trait will automatically render any Product (case class) to a JSON object. It uses the default formatters provided by Lift JSON library that is included in Scalatra. This keeps your controller class free of content negotiation if you are building an API, so you can focus on the business logic.

```scala
import org.scalatra._
import org.scalatra.extension.__

case class Foo(bar: String)

class ScalatraExample extends ScalatraServlet with ProductToJsonSupport {
  get("/") {
	Foo("baz")
  }
}
```

## Community

* Mailing list: [scalatra-user](http://groups.google.com/scalatra-user)
* IRC: #scalatra on irc.freenode.org
